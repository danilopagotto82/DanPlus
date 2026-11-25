package com.nuvio.tv.ui.v2.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.Brush
import com.nuvio.tv.ui.components.DeviceSystemClock
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.v2.appearance.LocalV2Appearance
import com.nuvio.tv.ui.v2.components.V2Wordmark
import com.nuvio.tv.ui.v2.components.nuvioV2Focus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.tv.material3.Text
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.LocalSidebarExpanded
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.v2.components.AvatarModel
import com.nuvio.tv.ui.v2.components.NuvioAvatar

val LocalV2TopChrome = staticCompositionLocalOf { false }

/** Alternative chrome around the same navigation controller and destination graph. */
@Composable
@OptIn(ExperimentalTvMaterial3Api::class)
fun V2TopNavigation(
    navigationContent: @Composable (Boolean) -> Unit,
    currentRoute: String?,
    rootRoutes: Set<String>,
    items: List<DrawerItem>,
    selectedRoute: String?,
    profile: AvatarModel?,
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    onExit: () -> Unit
) {
    val visible = currentRoute in rootRoutes
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val contentFocus = remember { FocusRequester() }
    val settings = items.firstOrNull { it.route == Screen.Settings.route }
    val menuItems = remember(items) { items.filterNot { it.route == Screen.Settings.route } }
    val routes = items.map { it.route }
    val routeFocus = remember(routes) { routes.associateWith { FocusRequester() } }
    var lastRoute by remember { mutableStateOf(selectedRoute) }
    var navFocused by remember { mutableStateOf(false) }
    var returnToContent by remember { mutableStateOf(false) }
    val entryFocus = routeFocus[lastRoute] ?: routeFocus[selectedRoute] ?: routeFocus.values.firstOrNull() ?: contentFocus

    // Destination BackHandlers are composed later and retain priority for sheets/settings.
    BackHandler(enabled = visible) {
        if (navFocused) onExit() else runCatching { entryFocus.requestFocus() }
    }
    LaunchedEffect(returnToContent, currentRoute) {
        if (returnToContent) {
            repeat(6) {
                withFrameNanos { }
                if (runCatching { contentFocus.requestFocus() }.getOrDefault(false)) {
                    returnToContent = false
                    return@LaunchedEffect
                }
            }
            returnToContent = false
        }
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(top = if (visible) 54.dp else 0.dp)
            .focusRequester(contentFocus).focusRestorer()
            .focusProperties {
                this.onExit = {
                    if (visible && requestedFocusDirection == FocusDirection.Up) entryFocus.requestFocus()
                }
            }.focusGroup()) {
            CompositionLocalProvider(LocalContentFocusRequester provides contentFocus,
                LocalSidebarExpanded provides navFocused,
                LocalV2TopChrome provides visible) {
                navigationContent(true)
            }
        }
        if (visible) {
            val transparency = LocalV2Appearance.current?.glassTransparencyPercent ?: 60
            val centerAlpha = signatureGlassAlpha(transparency)
            val capsule = RoundedCornerShape(50)
            val settingsFocus = settings?.let { routeFocus[it.route] }
            val avatarFocus = remember { FocusRequester() }
            val lastMenuFocus = menuItems.lastOrNull()?.let { routeFocus[it.route] }
            val activate: (String) -> Unit = { route ->
                onNavigate(route)
                returnToContent = true
            }
            Row(
                modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 36.dp, vertical = 8.dp)
                    .fillMaxWidth().onFocusChanged { navFocused = it.hasFocus }.focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                V2Wordmark(modifier = Modifier.width(100.dp).height(30.dp))
                LazyRow(
                    modifier = Modifier.weight(1f)
                        .background(Brush.verticalGradient(listOf(
                            Color(0xFF202329).copy(alpha = centerAlpha),
                            Color(0xFF090B10).copy(alpha = centerAlpha)
                        )), capsule)
                        .border(1.dp, Color.White.copy(alpha = centerAlpha * .18f), capsule),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
                ) {
                    items(menuItems, key = { it.route }) { item ->
                        SignatureRouteButton(
                            item = item,
                            selected = item.route == selectedRoute,
                            modifier = Modifier.focusRequester(routeFocus.getValue(item.route)).focusProperties {
                                down = contentFocus
                                if (item == menuItems.lastOrNull()) {
                                    val next = settingsFocus ?: if (profile != null) avatarFocus else FocusRequester.Default
                                    if (rtl) left = next else right = next
                                }
                            },
                            onFocused = { lastRoute = item.route },
                            onClick = { activate(item.route) }
                        )
                    }
                }
                DeviceSystemClock()
                if (settings != null || profile != null) {
                    val controlAlpha = centerAlpha * .10f
                    Row(
                        modifier = Modifier.background(Color.Black.copy(alpha = controlAlpha), capsule)
                            .border(1.dp, Color.White.copy(alpha = controlAlpha * .25f), capsule)
                            .padding(horizontal = 3.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (settings != null && settingsFocus != null) {
                            SignatureRouteButton(
                                item = settings,
                                selected = settings.route == selectedRoute,
                                modifier = Modifier.focusRequester(settingsFocus).focusProperties {
                                    down = contentFocus
                                    val previous = lastMenuFocus ?: FocusRequester.Default
                                    if (rtl) {
                                        right = previous
                                        if (profile != null) left = avatarFocus
                                    } else {
                                        left = previous
                                        if (profile != null) right = avatarFocus
                                    }
                                },
                                onFocused = { lastRoute = settings.route },
                                onClick = { activate(settings.route) },
                                compact = true
                            )
                        }
                        if (profile != null) {
                            var focused by remember { mutableStateOf(false) }
                            Button(
                                onClick = onSwitchProfile,
                                modifier = Modifier.focusRequester(avatarFocus).focusProperties {
                                    down = contentFocus
                                    val previous = settingsFocus ?: lastMenuFocus ?: FocusRequester.Default
                                    if (rtl) right = previous else left = previous
                                }.onFocusChanged { focused = it.isFocused }.nuvioV2Focus(focused, capsule),
                                shape = ButtonDefaults.shape(capsule),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                scale = ButtonDefaults.scale(focusedScale = 1f),
                                colors = signatureButtonColors(false)
                            ) {
                                NuvioAvatar(profile, 26.dp, description = profile.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun signatureGlassAlpha(transparencyPercent: Int): Float =
    (100 - transparencyPercent.coerceIn(0, 100)) / 100f

@Composable
@OptIn(ExperimentalTvMaterial3Api::class)
private fun SignatureRouteButton(
    item: DrawerItem,
    selected: Boolean,
    modifier: Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(50)
    var focused by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 36.dp).onFocusChanged {
            focused = it.isFocused
            if (focused) onFocused()
        }.nuvioV2Focus(focused, shape).semantics { this.selected = selected },
        shape = ButtonDefaults.shape(shape),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        contentPadding = PaddingValues(horizontal = if (compact) 10.dp else 9.dp, vertical = 5.dp),
        colors = signatureButtonColors(selected)
    ) {
        when {
            item.icon != null -> androidx.tv.material3.Icon(item.icon, if (compact) item.label else null, Modifier.size(15.dp))
            item.iconRes != null -> androidx.tv.material3.Icon(
                com.nuvio.tv.ui.util.rememberRawSvgPainter(item.iconRes, 15.dp),
                if (compact) item.label else null, Modifier.size(15.dp)
            )
        }
        if (!compact || (item.icon == null && item.iconRes == null)) {
            if (item.icon != null || item.iconRes != null) Spacer(Modifier.width(5.dp))
            Text(
                item.label, modifier = Modifier.widthIn(max = 128.dp),
                style = androidx.tv.material3.MaterialTheme.typography.labelMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
@OptIn(ExperimentalTvMaterial3Api::class)
private fun signatureButtonColors(selected: Boolean) = ButtonDefaults.colors(
    containerColor = if (selected) Color.White.copy(alpha = .17f) else Color.Transparent,
    focusedContainerColor = Color(0xFF3C414B),
    contentColor = if (selected) Color.White else NuvioTheme.colors.TextSecondary,
    focusedContentColor = Color.White
)
