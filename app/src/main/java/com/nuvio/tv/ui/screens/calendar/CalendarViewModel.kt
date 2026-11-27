package com.nuvio.tv.ui.screens.calendar

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.calendar.CALENDAR_DEFAULT_WINDOW_DAYS
import com.nuvio.tv.core.calendar.CalendarRefreshReason
import com.nuvio.tv.core.calendar.CalendarSectionKind
import com.nuvio.tv.core.calendar.CalendarSettings
import com.nuvio.tv.core.calendar.CalendarState
import com.nuvio.tv.core.calendar.CalendarTitleRecord
import com.nuvio.tv.core.calendar.CalendarTitleType
import com.nuvio.tv.core.calendar.CalendarWatchTarget
import com.nuvio.tv.core.calendar.buildCalendarSections
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CalendarViewMode { WEEK, LIST, MONTH }

enum class CalendarPhase { LOADING, BUILDING, NOTHING_FOLLOWED, ALL_QUIET, UNAVAILABLE, READY }

@Immutable
data class CalendarUiState(
    val phase: CalendarPhase = CalendarPhase.LOADING,
    val remaining: Int = 0,
    val zone: ZoneId? = null,
    val now: Instant = Instant.EPOCH,
    val today: LocalDate? = null,
    val windowDays: Int = CALENDAR_DEFAULT_WINDOW_DAYS,
    val list: CalendarListModel = CalendarListModel(),
    val movieReleasesLoading: Boolean = false,
    val movieReleasesUnavailable: Boolean = false,
    val days: Map<LocalDate, List<CalendarRow>> = emptyMap(),
    val byTitle: Map<String, List<CalendarRow>> = emptyMap(),
    val brazilMovieReleases: Boolean = true,
    val refreshFailed: Boolean = false,
    val profileId: Int? = null,
    val filter: CalendarFilter = CalendarFilter.ALL,
    val availableFilters: Set<CalendarFilter> = CalendarFilter.entries.take(3).toSet()
)

@Immutable
data class CalendarFocus(val row: CalendarRow, val details: CalendarRowDetails? = null)

/** Nulls stand for today, read when the month is drawn so the box's current date and zone are used. */
@Immutable
data class CalendarMonthState(val month: YearMonth? = null, val focusedDay: LocalDate? = null) {
    fun focusedDay(today: LocalDate): LocalDate = focusedDay ?: today
    fun month(today: LocalDate): YearMonth = month ?: YearMonth.from(focusedDay(today))
}

private data class CalendarInputs(
    val state: CalendarState,
    val settings: CalendarSettings?,
    val zone: ZoneId,
    val now: Instant,
    val movieReleases: MovieReleasesState,
    val owner: Int?,
    val filter: CalendarFilter = CalendarFilter.ALL
)

private data class MovieReleasesState(
    val titles: List<CalendarTitleRecord> = emptyList(),
    val loading: Boolean = false,
    val unavailable: Boolean = false
)

internal fun calendarPhase(state: CalendarState, settingsLoaded: Boolean, list: CalendarListModel): CalendarPhase = when {
    !state.loaded || !settingsLoaded -> CalendarPhase.LOADING
    !list.isEmpty -> CalendarPhase.READY
    state.refreshing -> CalendarPhase.BUILDING
    state.refreshFailed -> CalendarPhase.UNAVAILABLE
    state.watched == null -> CalendarPhase.BUILDING
    state.titles.isEmpty() -> CalendarPhase.NOTHING_FOLLOWED
    else -> CalendarPhase.ALL_QUIET
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel internal constructor(
    private val data: CalendarScreenData,
    computeDispatcher: CoroutineDispatcher,
    tickMillis: Long
) : ViewModel() {

    @Inject
    constructor(data: CalendarScreenData) : this(data, Dispatchers.Default, TICK_MS)

    private val ticks = flow {
        while (true) {
            emit(data.now())
            delay(tickMillis)
        }
    }

    val activeProfileId: StateFlow<Int?> = data.activeProfileId ?: MutableStateFlow(null)
    private val _filter = MutableStateFlow(CalendarFilter.fromPreference(ownedSettings()?.filter))
    val filter: StateFlow<CalendarFilter> = _filter.asStateFlow()

    private val movieReleases = MutableStateFlow(MovieReleasesState())
    private var movieReleasesJob: Job? = null

    val uiState: StateFlow<CalendarUiState> =
        combine(data.state, activeProfileId) { state, owner -> state to owner }
            .let { ownedState ->
                combine(ownedState, data.settings, data.zones, ticks, movieReleases) { (state, owner), settings, zone, now, releases ->
                    CalendarInputs(state, settings, zone, now, releases, owner)
                }
            }
            .combine(filter) { inputs, selected -> inputs.copy(filter = selected) }
            .mapLatest(::build)
            .distinctUntilChanged()
            .flowOn(computeDispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), CalendarUiState())

    private val _viewMode = MutableStateFlow(ownedSettings()?.viewMode.toCalendarViewMode())
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    private val _month = MutableStateFlow(CalendarMonthState())
    val month: StateFlow<CalendarMonthState> = _month.asStateFlow()

    /** The first day of the week on show; null for this week. */
    private val _weekStart = MutableStateFlow<LocalDate?>(null)
    val weekStart: StateFlow<LocalDate?> = _weekStart.asStateFlow()

    private val _focus = MutableStateFlow<CalendarFocus?>(null)
    val focus: StateFlow<CalendarFocus?> = _focus.asStateFlow()

    /** Set when OK on a month day asks the list to open on that day; the screen clears it once it has focused there. */
    private val _pendingListKey = MutableStateFlow<String?>(null)
    val pendingListKey: StateFlow<String?> = _pendingListKey.asStateFlow()

    var lastFocusedListKey: String? = null
        private set

    private var detailsJob: Job? = null
    private var titleOpened = false
    private var selectedDate: LocalDate? = null
    private var screenVisible = false
    private var preferenceOwner = activeProfileId.value
    private var loadedPreferences = ownedSettings()

    private fun ownedSettings(): CalendarSettings? = data.settings.value?.takeIf {
        activeProfileId.value == null || it.profileId == activeProfileId.value
    }

    init {
        viewModelScope.launch {
            combine(activeProfileId, data.settings) { owner, settings -> owner to settings }
                .collect { (owner, settings) ->
                    if (owner != preferenceOwner) {
                        preferenceOwner = owner
                        loadedPreferences = null
                        _filter.value = CalendarFilter.ALL
                        _viewMode.value = CalendarViewMode.WEEK
                        titleOpened = false
                        clearFocus()
                        showToday()
                        cancelMovieReleases()
                        movieReleases.value = MovieReleasesState()
                    }
                    if (settings == null || (owner != null && settings.profileId != owner)) return@collect
                    val previous = loadedPreferences
                    loadedPreferences = settings
                    if (previous == null || previous.viewMode != settings.viewMode) {
                        _viewMode.value = settings.viewMode.toCalendarViewMode()
                    }
                    if (previous == null || previous.filter != settings.filter) {
                        _filter.value = CalendarFilter.fromPreference(settings.filter)
                    }
                    if (!settings.brazilMovieReleases) {
                        cancelMovieReleases()
                        movieReleases.value = MovieReleasesState()
                    } else if (screenVisible && previous?.brazilMovieReleases != true) {
                        loadMovieReleases()
                    }
                }
        }
        viewModelScope.launch {
            data.openAtJustAired.collectLatest { open ->
                if (!open) return@collectLatest
                val state = uiState.first { it.phase != CalendarPhase.LOADING && it.phase != CalendarPhase.BUILDING }
                data.consumeOpenAtJustAired()
                if (state.phase != CalendarPhase.READY) return@collectLatest
                _pendingListKey.value = state.list.firstRowKeyIn(CalendarSectionKind.JUST_AIRED) ?: state.list.startRowKey
                _viewMode.value = CalendarViewMode.LIST
            }
        }
    }

    /** Each time the page comes into view; the page is kept while you are elsewhere, so this is not done once. */
    fun onScreenShown() {
        screenVisible = true
        if (!titleOpened) showToday()
        titleOpened = false
        data.requestRefresh(CalendarRefreshReason.CALENDAR_OPENED)
        loadMovieReleases()
    }

    fun onScreenHidden() {
        screenVisible = false
        cancelMovieReleases()
    }

    fun retryCalendar() {
        data.requestRefresh(CalendarRefreshReason.MANUAL_RETRY)
        loadMovieReleases()
    }

    fun loadMovieReleases() {
        if (movieReleasesJob?.isActive == true) return
        val owner = activeProfileId.value
        movieReleasesJob = viewModelScope.launch {
            if (data.settings.first { it != null && (owner == null || it.profileId == owner) }?.brazilMovieReleases != true) {
                movieReleases.value = MovieReleasesState()
                return@launch
            }
            val today = data.now().atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate()
            movieReleases.value = movieReleases.value.copy(loading = true, unavailable = false)
            try {
                val titles = data.upcomingBrazilianMovies(today)
                if (activeProfileId.value == owner) movieReleases.value = MovieReleasesState(titles = titles)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (activeProfileId.value == owner) {
                    movieReleases.value = movieReleases.value.copy(loading = false, unavailable = true)
                }
            }
        }
    }

    fun cancelMovieReleases() {
        movieReleasesJob?.cancel()
        movieReleasesJob = null
    }

    fun setWindowDays(days: Int) {
        viewModelScope.launch { data.setWindowDays(days) }
    }

    /** Back from a title page keeps the month where it was; any other way in starts on today. */
    fun onTitleOpened(row: CalendarRow? = null) {
        titleOpened = true
        if (row != null && _viewMode.value == CalendarViewMode.LIST) lastFocusedListKey = row.key
    }

    /** The menu entry chosen while the Calendar is already open. */
    fun showToday() {
        selectedDate = null
        _month.value = CalendarMonthState()
        _weekStart.value = null
        _pendingListKey.value = null
        _pendingWeekDay.value = null
        lastFocusedListKey = null
    }

    fun selectToday() {
        showToday()
        uiState.value.today?.let { today ->
            when (_viewMode.value) {
                CalendarViewMode.LIST -> openDayInList(today)
                CalendarViewMode.WEEK -> _pendingWeekDay.value = today
                CalendarViewMode.MONTH -> focusDay(today)
            }
        }
    }

    fun shiftWeek(current: LocalDate, weeks: Long) {
        current.plusWeeks(weeks).let { next ->
            selectedDate = next
            _weekStart.value = next
        }
    }

    fun setFilter(filter: CalendarFilter) {
        if (ownedSettings() == null || _filter.value == filter) return
        val owner = activeProfileId.value
        _filter.value = filter
        if (_focus.value?.row?.let { !filter.keeps(it) } == true) clearFocus()
        _pendingListKey.value = null
        lastFocusedListKey = null
        viewModelScope.launch { data.setFilter(owner, filter) }
    }

    suspend fun watchTarget(row: CalendarRow): CalendarWatchTarget? {
        val owner = activeProfileId.value
        return try {
            data.watchTarget(row).takeIf { activeProfileId.value == owner }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun build(inputs: CalendarInputs): CalendarUiState {
        val (rawState, rawSettings, zone, now, releases, owner, activeFilter) = inputs
        val state = rawState.takeIf { owner == null || it.profileId == owner }
            ?: CalendarState(profileId = owner)
        val settings = rawSettings?.takeIf { owner == null || it.profileId == owner }
        val bells = settings?.bellOn.orEmpty()
        val windowDays = settings?.windowDays ?: CALENDAR_DEFAULT_WINDOW_DAYS
        val today = now.atZone(zone).toLocalDate()
        val personalMovies = state.titles.asSequence().filter { it.type == CalendarTitleType.MOVIE }
            .mapNotNull { title -> title.ids.tmdb?.let { it to title } }.toMap()
        val endDate = today.plusDays(windowDays.toLong() - 1)
        val hidden = settings?.hiddenTitleKeys.orEmpty()
        val movieReleasesEnabled = settings?.brazilMovieReleases == true
        val upcoming = releases.titles.takeIf { movieReleasesEnabled }.orEmpty().filter { release ->
            hidden.none(release::matchesKey) &&
                release.entries.any { it.localDate(zone)?.let { date -> date in today..endDate } == true }
        }
        val upcomingById = upcoming.associateBy { it.ids.tmdb }
        val personalWithDates = state.titles.map { title ->
            val release = upcomingById[title.ids.tmdb]
            if (title.type != CalendarTitleType.MOVIE || release == null || title.entries.any { entry ->
                    entry.localDate(zone)?.let { it in today..endDate } == true
                }) title
            else title.copy(sources = title.sources + (
                com.nuvio.tv.core.calendar.CalendarSource.TMDB to
                    (title.sources[com.nuvio.tv.core.calendar.CalendarSource.TMDB].orEmpty() + release.entries)
            ))
        }
        val titles = personalWithDates + upcoming.filter { it.ids.tmdb !in personalMovies }
        val list = if (state.loaded && settings != null) {
            buildCalendarList(buildCalendarSections(titles, now, zone, windowDays), now, zone, bells, state.watched)
        } else CalendarListModel()
        val allDays = if (state.loaded && settings != null) buildCalendarDays(titles, now, zone, bells, state.watched) else emptyMap()
        val rows = list.entries.asSequence().filterIsInstance<CalendarRowEntry>().map { it.row } +
            allDays.values.asSequence().flatten()
        val filters = CalendarFilter.entries.take(3).toMutableSet()
        val missingFilters = CalendarFilter.entries.drop(3).toMutableSet()
        for (row in rows) {
            val found = missingFilters.filter { it.keeps(row) }
            filters.addAll(found)
            missingFilters.removeAll(found)
            if (missingFilters.isEmpty()) break
        }
        filters.add(activeFilter)
        val days = if (activeFilter == CalendarFilter.ALL) allDays
            else allDays.mapValues { (_, rows) -> rows.filter(activeFilter::keeps) }.filterValues { it.isNotEmpty() }
        return CalendarUiState(
            phase = calendarPhase(state, settings != null, list),
            remaining = state.remaining,
            zone = zone,
            now = now.truncatedTo(java.time.temporal.ChronoUnit.MINUTES),
            today = today,
            movieReleasesLoading = movieReleasesEnabled && releases.loading,
            movieReleasesUnavailable = movieReleasesEnabled && releases.unavailable,
            brazilMovieReleases = movieReleasesEnabled,
            windowDays = windowDays,
            list = list.filtered(activeFilter),
            filter = activeFilter,
            availableFilters = filters,
            refreshFailed = state.refreshFailed,
            profileId = owner,
            days = days,
            byTitle = calendarRowsByTitle(days)
        )
    }

    fun setViewMode(mode: CalendarViewMode, firstDay: DayOfWeek = DayOfWeek.MONDAY) {
        if (ownedSettings() == null) return
        if (_viewMode.value == mode && ownedSettings()?.viewMode == mode.name) return
        val owner = activeProfileId.value
        val date = selectedDate
        if (date != null && _viewMode.value != mode) when (mode) {
            CalendarViewMode.MONTH -> focusDay(date)
            CalendarViewMode.WEEK -> {
                _weekStart.value = calendarWeekStart(date, firstDay)
                _pendingWeekDay.value = date
            }
            CalendarViewMode.LIST -> _pendingListKey.value = uiState.value.list.rowKeyFor(date)
        }
        _viewMode.value = mode
        // Only a header choice is remembered; jumps into a day keep the view the user picked.
        viewModelScope.launch { data.setViewMode(owner, mode) }
    }

    fun focusDay(date: LocalDate) {
        selectedDate = date
        _month.value = CalendarMonthState(YearMonth.from(date), date)
    }

    /** OK on a month day: back to the list, on that day or the next one with something on it. */
    fun openDayInList(date: LocalDate) {
        selectedDate = date
        _pendingListKey.value = uiState.value.list.rowKeyFor(date)
        _viewMode.value = CalendarViewMode.LIST
    }

    /** Set when OK on a month day opens the Week on it; the Week clears it once it has focused that day. */
    private val _pendingWeekDay = MutableStateFlow<LocalDate?>(null)
    val pendingWeekDay: StateFlow<LocalDate?> = _pendingWeekDay.asStateFlow()

    /** Open the requested week with the same filter, including a selected day with no matches. */
    fun openDayInWeek(date: LocalDate, weekStart: LocalDate) {
        selectedDate = date
        _weekStart.value = weekStart
        _pendingWeekDay.value = date
        _viewMode.value = CalendarViewMode.WEEK
    }

    fun consumePendingWeekDay() {
        _pendingWeekDay.value = null
    }

    fun consumePendingListKey() {
        _pendingListKey.value = null
    }

    fun onRowFocused(row: CalendarRow, fromList: Boolean, trackDate: Boolean = true) {
        if (trackDate && _viewMode.value != CalendarViewMode.MONTH) row.date?.let { selectedDate = it }
        if (fromList) lastFocusedListKey = row.key
        if (_focus.value?.row?.key == row.key && _focus.value?.row == row) return
        _focus.value = CalendarFocus(row)
        detailsJob?.cancel()
        val owner = activeProfileId.value
        detailsJob = viewModelScope.launch {
            delay(DETAILS_DELAY_MS)
            val cached = runCatching { data.details(row) }.getOrNull()
            if (activeProfileId.value != owner || _focus.value?.row?.key != row.key) return@launch
            _focus.value = CalendarFocus(row, cached)
            if (cached?.complete == true && rowContentIds(row).none { it.startsWith("tmdb:") }) return@launch
            delay(DETAILS_LOOKUP_DELAY_MS - DETAILS_DELAY_MS)
            val fetched = runCatching { data.details(row, fetch = true) }.getOrNull() ?: return@launch
            if (activeProfileId.value == owner && _focus.value?.row?.key == row.key) {
                _focus.value = CalendarFocus(row, fetched)
            }
        }
    }

    fun clearFocus() {
        detailsJob?.cancel()
        _focus.value = null
    }

    fun toggleBell(row: CalendarRow) {
        if (!row.status.hasBell) return
        viewModelScope.launch { data.setBell(row.titleKey, row.status != CalendarRowStatus.BELL_ON) }
    }

    fun hide(row: CalendarRow) {
        if (_focus.value?.row?.titleKey == row.titleKey) _focus.value = null
        if (_viewMode.value == CalendarViewMode.LIST) {
            _pendingListKey.value = uiState.value.list.neighbourOf(row)
        }
        viewModelScope.launch { data.setHidden(row.titleKey, hidden = true, name = row.name) }
    }

    private companion object {
        const val TICK_MS = 60_000L
        const val DETAILS_DELAY_MS = 120L
        const val DETAILS_LOOKUP_DELAY_MS = 400L
        const val STOP_AFTER_MS = 5_000L
    }
}

/** The remembered view, Week when none was picked or the name is unknown. */
internal fun String?.toCalendarViewMode(): CalendarViewMode =
    this?.let { name -> runCatching { CalendarViewMode.valueOf(name) }.getOrNull() } ?: CalendarViewMode.WEEK
