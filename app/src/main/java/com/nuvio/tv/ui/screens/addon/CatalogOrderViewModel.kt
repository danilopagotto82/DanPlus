package com.nuvio.tv.ui.screens.addon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.sync.HomeCatalogSettingsSyncService
import com.nuvio.tv.core.sync.homeCatalogKey
import com.nuvio.tv.core.sync.homeLegacyDisabledCatalogKey
import com.nuvio.tv.core.sync.isDeviceOnlyCatalogKey
import com.nuvio.tv.data.local.CollectionsDataStore
import com.nuvio.tv.data.local.LayoutPreferenceDataStore
import com.nuvio.tv.data.local.HomeOrderSnapshot
import com.nuvio.tv.data.mediaserver.ServerCatalog
import com.nuvio.tv.domain.model.Addon
import com.nuvio.tv.domain.model.Collection
import com.nuvio.tv.domain.model.enabledAddons
import com.nuvio.tv.domain.repository.AddonRepository
import com.nuvio.tv.ui.screens.settings.OrderDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogOrderViewModel @Inject constructor(
    private val addonRepository: AddonRepository,
    private val collectionsDataStore: CollectionsDataStore,
    private val layoutPreferenceDataStore: LayoutPreferenceDataStore,
    private val homeCatalogSettingsSyncService: HomeCatalogSettingsSyncService,
    private val serverCatalog: ServerCatalog,
    private val builtInCatalogs: com.nuvio.tv.core.catalog.BuiltInCatalogs = com.nuvio.tv.core.catalog.BuiltInCatalogs.None
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogOrderUiState())
    val uiState: StateFlow<CatalogOrderUiState> = _uiState.asStateFlow()
    private var disabledKeysCache: Set<String> = emptySet()
    private val mutableDraft = MutableStateFlow<OrderDraft?>(null)
    internal val orderDraft = mutableDraft.asStateFlow()
    private var catalogSnapshot: HomeOrderSnapshot? = null
    private var draftSnapshot: HomeOrderSnapshot? = null
    private val mutableSaving = MutableStateFlow(false)
    internal val savingOrder = mutableSaving.asStateFlow()
    private val mutableOrderFailed = MutableStateFlow(false)
    internal val orderFailed = mutableOrderFailed.asStateFlow()

    init {
        observeCatalogs()
    }

    fun moveUp(key: String) {
        if (_uiState.value.followAddonsOrder) {
            moveCollectionBetweenAddons(key, -1)
        } else {
            moveCatalog(key, -1)
        }
    }

    fun moveDown(key: String) {
        if (_uiState.value.followAddonsOrder) {
            moveCollectionBetweenAddons(key, 1)
        } else {
            moveCatalog(key, 1)
        }
    }

    fun toggleFollowAddonsOrder(enabled: Boolean) {
        viewModelScope.launch {
            layoutPreferenceDataStore.setFollowAddonsOrder(enabled)
        }
    }

    fun toggleCatalogEnabled(disableKey: String) {
        val updatedDisabled = disabledKeysCache.toMutableSet().apply {
            if (disableKey in this) remove(disableKey) else add(disableKey)
        }
        viewModelScope.launch {
            layoutPreferenceDataStore.setDisabledHomeCatalogKeys(updatedDisabled.toList())
            homeCatalogSettingsSyncService.triggerPush()
        }
    }

    private fun moveCatalog(key: String, direction: Int) {
        val currentKeys = itemsForDraft().map { it.key }
        val currentIndex = currentKeys.indexOf(key)
        if (currentIndex == -1) return

        val newIndex = currentIndex + direction
        if (newIndex !in currentKeys.indices) return

        previewMove(key, newIndex)
    }

    /**
     * When followAddonsOrder is enabled, collections jump between addon boundaries.
     * Moving up means jumping above the previous addon block (all catalogs from one addon).
     * Moving down means jumping below the next addon block.
     */
    private fun moveCollectionBetweenAddons(key: String, direction: Int) {
        if (!key.startsWith("collection_")) return

        val items = itemsForDraft()
        val currentIndex = items.indexOfFirst { it.key == key }
        if (currentIndex == -1) return

        val currentKeys = items.map { it.key }

        val newIndex: Int
        if (direction < 0) {
            // Moving up: find the start of the previous addon block
            // Skip any adjacent collections above
            var scanIdx = currentIndex - 1
            while (scanIdx >= 0 && currentKeys[scanIdx].startsWith("collection_")) {
                scanIdx--
            }
            if (scanIdx < 0) return // already at top

            // scanIdx is now pointing at an addon catalog. Find the start of its addon block.
            val targetAddonName = items[scanIdx].addonName
            while (scanIdx > 0 && !currentKeys[scanIdx - 1].startsWith("collection_") &&
                items[scanIdx - 1].addonName == targetAddonName) {
                scanIdx--
            }
            newIndex = scanIdx
        } else {
            // Moving down: find the end of the next addon block
            var scanIdx = currentIndex + 1
            while (scanIdx < currentKeys.size && currentKeys[scanIdx].startsWith("collection_")) {
                scanIdx++
            }
            if (scanIdx >= currentKeys.size) return // already at bottom

            // scanIdx is now pointing at an addon catalog. Find the end of its addon block.
            val targetAddonName = items[scanIdx].addonName
            while (scanIdx < currentKeys.lastIndex && !currentKeys[scanIdx + 1].startsWith("collection_") &&
                items[scanIdx + 1].addonName == targetAddonName) {
                scanIdx++
            }
            newIndex = scanIdx
        }

        if (newIndex == currentIndex) return

        previewMove(key, newIndex)
    }

    private fun itemsForDraft(): List<CatalogOrderItem> {
        val saved = _uiState.value.items
        return mutableDraft.value?.display(saved, CatalogOrderItem::key) ?: saved
    }

    private fun previewMove(key: String, destination: Int) {
        if (mutableSaving.value) return
        val baseline = _uiState.value.items.map { it.key }
        val snapshot = catalogSnapshot?.takeIf { it.owner == layoutPreferenceDataStore.homeOrderOwner() } ?: return
        val draft = mutableDraft.value?.takeIf { it.baseline == baseline && it.owner == snapshot.owner }
            ?: OrderDraft(snapshot.owner, baseline).also { draftSnapshot = snapshot }
        val updated = draft.move(key, destination)
        mutableOrderFailed.value = false
        mutableDraft.value = updated.takeUnless { it.keys == it.baseline }
    }

    internal fun cancelOrder() {
        if (!mutableSaving.value) mutableDraft.value = null
        mutableOrderFailed.value = false
    }

    internal fun saveOrder() {
        val draft = mutableDraft.value ?: return
        val expected = draftSnapshot ?: return
        if (mutableSaving.value) return
        mutableSaving.value = true
        viewModelScope.launch {
            try {
                val syncUser = homeCatalogSettingsSyncService.orderSyncUser(draft.owner)
                if (isStillCatalogOrder(draft, expected) &&
                    layoutPreferenceDataStore.saveDraftHomeOrder(expected, draft.baseline, draft.keys)) {
                    if (syncUser != null) homeCatalogSettingsSyncService.triggerOrderPush(draft.owner, syncUser)
                } else {
                    mutableOrderFailed.value = true
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableOrderFailed.value = true
            } finally {
                mutableDraft.value = null
                mutableSaving.value = false
            }
        }
    }

    private suspend fun isStillCatalogOrder(draft: OrderDraft, expected: HomeOrderSnapshot): Boolean {
        val usePrimary = layoutPreferenceDataStore.homeOrderUsesPrimaryAddons(draft.owner) ?: return false
        val addons = addonRepository.installedAddonsForOrderSync(draft.owner.first, usePrimary) ?: return false
        val collections = collectionsDataStore.getCollections(draft.owner.first)
        val localKeys = buildOrderedCatalogItems(
            addons, collections, expected.settings.orderKeys, emptySet(),
            followAddonsOrder = expected.followAddonsOrder
        ).map(CatalogOrderItem::key)
        val visibleKeys = draft.baseline.filterNot(::isDeviceOnlyCatalogKey)
        return draft.owner == layoutPreferenceDataStore.homeOrderOwner() &&
            draft.baseline == _uiState.value.items.map(CatalogOrderItem::key) &&
            visibleKeys.toSet() == localKeys.toSet() &&
            visibleKeys.filterNot { it.startsWith("collection_") } ==
                localKeys.filterNot { it.startsWith("collection_") }
    }

    private fun observeCatalogs() {
        viewModelScope.launch {
            combine(
                combine(addonRepository.getInstalledAddons(), serverCatalog.addons, builtInCatalogs.addons) { installed, servers, builtIn ->
                    layoutPreferenceDataStore.hideNewCatalogsByDefault(
                        ServerCatalog.hiddenByDefaultKeys(servers) + builtInCatalogs.hiddenByDefaultKeys(builtIn)
                    )
                    installed.enabledAddons() + servers + builtIn
                },
                combine(collectionsDataStore.collections, builtInCatalogs.collections) { saved, builtIn -> saved + builtIn },
                layoutPreferenceDataStore.observeHomeOrderSnapshot()
            ) { addons, collections, snapshot ->
                val items = buildOrderedCatalogItems(
                    addons = addons,
                    collections = collections,
                    savedOrderKeys = builtInCatalogs.withDefaultFirst(snapshot.settings.orderKeys),
                    disabledKeys = snapshot.settings.disabledKeys,
                    customTitles = snapshot.settings.customTitles,
                    followAddonsOrder = snapshot.followAddonsOrder
                )
                items to snapshot
            }.collectLatest { (orderedItems, snapshot) ->
                if (snapshot.owner != layoutPreferenceDataStore.homeOrderOwner()) return@collectLatest
                catalogSnapshot = snapshot
                mutableDraft.value?.let { draft ->
                    if (draft.owner != snapshot.owner || draft.baseline != orderedItems.map { it.key } ||
                        draftSnapshot?.settings?.orderKeys != snapshot.settings.orderKeys ||
                        draftSnapshot?.followAddonsOrder != snapshot.followAddonsOrder) cancelOrder()
                }
                disabledKeysCache = orderedItems.filter { it.isDisabled }.map { it.disableKey }.toSet()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = orderedItems,
                        followAddonsOrder = snapshot.followAddonsOrder
                    )
                }
            }
        }
    }

    private fun buildOrderedCatalogItems(
        addons: List<Addon>,
        collections: List<Collection> = emptyList(),
        savedOrderKeys: List<String>,
        disabledKeys: Set<String>,
        customTitles: Map<String, String> = emptyMap(),
        followAddonsOrder: Boolean = false
    ): List<CatalogOrderItem> {
        val defaultEntries = buildDefaultCatalogEntries(addons)
        val collectionEntries = collections.map { collection ->
            CatalogOrderEntry(
                key = "collection_${collection.id}",
                disableKey = "collection_${collection.id}",
                catalogName = collection.title,
                addonName = "${collection.folders.size} folder${if (collection.folders.size != 1) "s" else ""}",
                typeLabel = "collection"
            )
        }
        val allEntries = defaultEntries + collectionEntries
        val availableMap = allEntries.associateBy { it.key }
        val defaultOrderKeys = allEntries.map { it.key }

        val effectiveOrder: List<String>
        if (followAddonsOrder) {
            // In follow mode, addon catalogs stay in manifest order.
            // Collections are positioned based on their relative position in savedOrderKeys.
            val addonKeys = defaultEntries.map { it.key }
            val collectionKeys = collectionEntries.map { it.key }.toSet()

            val savedValid = savedOrderKeys.filter { it in availableMap }.distinct()

            if (savedValid.isNotEmpty()) {
                // Rebuild order: take saved order but replace addon sequence with manifest order.
                // Strategy: walk through savedValid, output addon keys in manifest order,
                // insert collections at their saved positions relative to addon boundaries.
                val result = mutableListOf<String>()
                var addonPointer = 0 // pointer into addonKeys (manifest order)

                for (savedKey in savedValid) {
                    if (savedKey in collectionKeys) {
                        // Place collection here - but first flush any addon keys up to this point
                        // that haven't been placed yet
                        result.add(savedKey)
                    } else {
                        // It's an addon catalog key in saved order - advance manifest pointer
                        // to include all addon keys up to and including this one
                        val targetManifestIdx = addonKeys.indexOf(savedKey)
                        if (targetManifestIdx >= 0) {
                            while (addonPointer <= targetManifestIdx) {
                                val ak = addonKeys[addonPointer]
                                if (ak !in result) {
                                    result.add(ak)
                                }
                                addonPointer++
                            }
                        }
                    }
                }
                // Append any remaining addon keys not yet placed
                while (addonPointer < addonKeys.size) {
                    val ak = addonKeys[addonPointer]
                    if (ak !in result) {
                        result.add(ak)
                    }
                    addonPointer++
                }
                // Append any collections not in savedValid
                for (ck in collectionKeys) {
                    if (ck !in result) {
                        result.add(ck)
                    }
                }
                // Normalize: ensure collections sit at addon block boundaries, not mid-block.
                // If a collection is between two catalogs of the same addon, push it after that block.
                effectiveOrder = normalizeCollectionPositions(result, availableMap)
            } else {
                // No saved order - addon manifest order + collections at end
                effectiveOrder = addonKeys + collectionKeys.toList()
            }
        } else {
            val savedValid = savedOrderKeys
                .asSequence()
                .filter { it in availableMap }
                .distinct()
                .toList()

            val savedKeySet = savedValid.toSet()
            val missing = defaultOrderKeys.filterNot { it in savedKeySet }
            effectiveOrder = savedValid + missing
        }

        return effectiveOrder.mapIndexedNotNull { index, key ->
            val entry = availableMap[key] ?: return@mapIndexedNotNull null
            val displayName = customTitles[key]?.takeIf { it.isNotBlank() } ?: entry.catalogName
            val isCollection = key.startsWith("collection_")

            val canMoveUp: Boolean
            val canMoveDown: Boolean
            if (followAddonsOrder) {
                if (isCollection) {
                    canMoveUp = index > 0
                    canMoveDown = index < effectiveOrder.lastIndex
                } else {
                    canMoveUp = false
                    canMoveDown = false
                }
            } else {
                canMoveUp = index > 0
                canMoveDown = index < effectiveOrder.lastIndex
            }

            CatalogOrderItem(
                key = entry.key,
                disableKey = entry.disableKey,
                catalogName = displayName,
                addonName = entry.addonName,
                typeLabel = entry.typeLabel,
                isDisabled = entry.disableKey in disabledKeys ||
                    (entry.legacyDisableKey != null && entry.legacyDisableKey in disabledKeys),
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown
            )
        }
    }

    /**
     * Ensures collections are positioned at addon block boundaries.
     * If a collection ended up between two catalogs of the same addon,
     * push it to the end of that addon's block.
     */
    private fun normalizeCollectionPositions(
        order: List<String>,
        availableMap: Map<String, CatalogOrderEntry>
    ): List<String> {
        val result = order.toMutableList()
        var changed = true
        while (changed) {
            changed = false
            var i = 0
            while (i < result.size) {
                val key = result[i]
                if (!key.startsWith("collection_")) {
                    i++
                    continue
                }
                val prevAddon = findAddonNameBefore(result, i, availableMap)
                val nextAddon = findAddonNameAfter(result, i, availableMap)
                if (prevAddon != null && nextAddon != null && prevAddon == nextAddon) {
                    result.removeAt(i)
                    var insertPos = i
                    while (insertPos < result.size &&
                        !result[insertPos].startsWith("collection_") &&
                        availableMap[result[insertPos]]?.addonName == prevAddon
                    ) {
                        insertPos++
                    }
                    result.add(insertPos, key)
                    if (insertPos != i) changed = true
                    i++
                } else {
                    i++
                }
            }
        }
        return result
    }

    private fun findAddonNameBefore(
        order: List<String>,
        index: Int,
        availableMap: Map<String, CatalogOrderEntry>
    ): String? {
        for (j in index - 1 downTo 0) {
            if (!order[j].startsWith("collection_")) {
                return availableMap[order[j]]?.addonName
            }
        }
        return null
    }

    private fun findAddonNameAfter(
        order: List<String>,
        index: Int,
        availableMap: Map<String, CatalogOrderEntry>
    ): String? {
        for (j in index + 1 until order.size) {
            if (!order[j].startsWith("collection_")) {
                return availableMap[order[j]]?.addonName
            }
        }
        return null
    }

    private fun buildDefaultCatalogEntries(addons: List<Addon>): List<CatalogOrderEntry> {
        val entries = mutableListOf<CatalogOrderEntry>()
        val seenKeys = mutableSetOf<String>()

        addons.forEach { addon ->
            addon.catalogs
                .filterNot { it.isSearchOnlyCatalog() }
                .forEach { catalog ->
                    val key = catalogKey(
                        addonId = addon.id,
                        type = catalog.apiType,
                        catalogId = catalog.id
                    )
                    if (seenKeys.add(key)) {
                        entries.add(
                            CatalogOrderEntry(
                                key = key,
                                disableKey = homeCatalogKey(
                                    addonId = addon.id,
                                    type = catalog.apiType,
                                    catalogId = catalog.id
                                ),
                                legacyDisableKey = homeLegacyDisabledCatalogKey(
                                    addonBaseUrl = addon.baseUrl,
                                    type = catalog.apiType,
                                    catalogId = catalog.id,
                                    catalogName = catalog.name
                                ),
                                catalogName = catalog.name,
                                addonName = addon.displayName,
                                typeLabel = catalog.apiType
                            )
                        )
                    }
                }
        }

        return entries
    }

    private fun catalogKey(addonId: String, type: String, catalogId: String): String {
        return homeCatalogKey(addonId, type, catalogId)
    }
}

data class CatalogOrderUiState(
    val isLoading: Boolean = true,
    val items: List<CatalogOrderItem> = emptyList(),
    val followAddonsOrder: Boolean = false
)

data class CatalogOrderItem(
    val key: String,
    val disableKey: String,
    val catalogName: String,
    val addonName: String,
    val typeLabel: String,
    val isDisabled: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean
)

private data class CatalogOrderEntry(
    val key: String,
    val disableKey: String,
    val legacyDisableKey: String? = null,
    val catalogName: String,
    val addonName: String,
    val typeLabel: String
)
