package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.OtherRecitersSheet
import com.example.ui.components.SakinaBottomBar
import com.example.ui.components.SakinaTab
import com.example.ui.components.SakinaTopBar
import com.example.ui.components.SleepTimerBottomSheet
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AdhkarScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.JourneyScreen
import com.example.ui.screens.ReciterDetailScreen
import com.example.ui.screens.RecitersScreen
import com.example.ui.screens.WirdScreen
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.SakinaTheme
import com.example.ui.viewmodel.AdhkarViewModel
import com.example.ui.viewmodel.DownloadsViewModel
import com.example.ui.viewmodel.JourneyViewModel
import com.example.ui.viewmodel.RecitersViewModel
import com.example.ui.viewmodel.SakinaViewModelFactory
import com.example.ui.viewmodel.WirdViewModel
import com.example.util.ShareCardGenerator
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object RecitersRoute

@Serializable
object WirdRoute

@Serializable
object AdhkarRoute

@Serializable
object DownloadsRoute

@Serializable
data class ReciterDetailRoute(val reciterId: Int)

@Serializable
object JourneyRoute

@Serializable
object AboutRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        val app = application as SakinaApp
        app.container.playerController.initialize()

        setContent {
            SakinaTheme {
                SakinaRoot(app = app)
            }
        }
    }
}

@Composable
private fun SakinaRoot(app: SakinaApp) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val container = app.container
    val factory = remember { SakinaViewModelFactory(container) }

    val recitersVm: RecitersViewModel = viewModel(factory = factory)
    val wirdVm: WirdViewModel = viewModel(factory = factory)
    val adhkarVm: AdhkarViewModel = viewModel(factory = factory)
    val downloadsVm: DownloadsViewModel = viewModel(factory = factory)
    val journeyVm: JourneyViewModel = viewModel(factory = factory)

    val recitersState by recitersVm.uiState.collectAsStateWithLifecycle()
    val wirdState by wirdVm.uiState.collectAsStateWithLifecycle()
    val adhkarState by adhkarVm.uiState.collectAsStateWithLifecycle()
    val downloadsState by downloadsVm.uiState.collectAsStateWithLifecycle()
    val journeyState by journeyVm.uiState.collectAsStateWithLifecycle()

    val playerState by container.playerController.uiState.collectAsStateWithLifecycle()
    val sleepTimerState by container.playerController.sleepTimerState.collectAsStateWithLifecycle()
    val activeDownloads by recitersVm.activeDownloads.collectAsStateWithLifecycle()
    val completedDownloads by recitersVm.downloads.collectAsStateWithLifecycle()
    val pendingDeleteConfirmIds by recitersVm.pendingDeleteConfirmIds.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentTab: SakinaTab? = when {
        currentDestination?.hasRoute<RecitersRoute>() == true -> SakinaTab.RECITERS
        currentDestination?.hasRoute<WirdRoute>() == true -> SakinaTab.WIRD
        currentDestination?.hasRoute<AdhkarRoute>() == true -> SakinaTab.ADHKAR
        currentDestination?.hasRoute<DownloadsRoute>() == true -> SakinaTab.DOWNLOADS
        else -> null
    }
    val isMainTab = currentTab != null
    val isReciterDetail = currentDestination?.hasRoute<ReciterDetailRoute>() == true

    var showFullPlayer by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var otherRecitersSurah by remember { mutableStateOf<Int?>(null) }

    var recitersScrollTick by remember { mutableIntStateOf(0) }
    var wirdScrollTick by remember { mutableIntStateOf(0) }
    var adhkarScrollTick by remember { mutableIntStateOf(0) }
    var downloadsScrollTick by remember { mutableIntStateOf(0) }

    var askedNotificationPermission by remember { mutableStateOf(false) }
    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun requestNotificationPermissionIfNeeded() {
        if (!askedNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                askedNotificationPermission = true
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        container.downloadRepository.errorEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(Unit) {
        container.playerController.playerErrors.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(Unit) {
        container.playerController.timerEndedEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    fun navigateToTab(tab: SakinaTab) {
        if (tab == currentTab) {
            when (tab) {
                SakinaTab.RECITERS -> recitersScrollTick++
                SakinaTab.WIRD -> wirdScrollTick++
                SakinaTab.ADHKAR -> adhkarScrollTick++
                SakinaTab.DOWNLOADS -> downloadsScrollTick++
            }
            return
        }
        val route: Any = when (tab) {
            SakinaTab.RECITERS -> RecitersRoute
            SakinaTab.WIRD -> WirdRoute
            SakinaTab.ADHKAR -> AdhkarRoute
            SakinaTab.DOWNLOADS -> DownloadsRoute
        }
        navController.navigate(route) {
            popUpTo<RecitersRoute> { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBg),
        containerColor = CharcoalBg,
        topBar = {
            if (isMainTab) {
                SakinaTopBar(
                    sleepTimerState = sleepTimerState,
                    onSleepTimerClick = { showSleepTimerSheet = true },
                    onJourneyClick = { navController.navigate(JourneyRoute) },
                    onAboutClick = { navController.navigate(AboutRoute) }
                )
            }
        },
        bottomBar = {
            if (isMainTab || isReciterDetail) {
                Column {
                    MiniPlayerBar(
                        state = playerState,
                        onTogglePlayPause = {
                            requestNotificationPermissionIfNeeded()
                            container.playerController.togglePlayPause()
                        },
                        onExpand = { showFullPlayer = true },
                        modifier = if (!isMainTab) Modifier.navigationBarsPadding() else Modifier
                    )
                    if (isMainTab && currentTab != null) {
                        SakinaBottomBar(
                            selectedTab = currentTab,
                            showWirdDot = !wirdState.isTodayGoalReached,
                            showAdhkarDot = adhkarState.isCurrentTimeSetUnfinished,
                            onTabSelected = { navigateToTab(it) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = RecitersRoute
            ) {
                composable<RecitersRoute> {
                    RecitersScreen(
                        state = recitersState,
                        scrollToTopTick = recitersScrollTick,
                        onSearchQueryChange = recitersVm::onSearchQueryChange,
                        onFilterChange = recitersVm::onFilterChange,
                        onReciterClick = { reciter ->
                            navController.navigate(ReciterDetailRoute(reciter.id))
                        },
                        onToggleFavorite = recitersVm::toggleFavorite,
                        onRetryNetwork = recitersVm::refreshReciters,
                        onOpenAdhkarSet = { set ->
                            adhkarVm.selectSet(set)
                            navigateToTab(SakinaTab.ADHKAR)
                        },
                        onPlaySuggestedSurah = { reciter, moshaf, surahNum ->
                            requestNotificationPermissionIfNeeded()
                            container.playerController.playSurah(reciter, moshaf, surahNum)
                        }
                    )
                }

                composable<ReciterDetailRoute> { entry ->
                    val args = entry.toRoute<ReciterDetailRoute>()
                    val reciter = recitersVm.reciterById(args.reciterId)
                    if (reciter != null) {
                        ReciterDetailScreen(
                            reciter = reciter,
                            isFavorite = reciter.id in recitersState.favoriteIds,
                            playerState = playerState,
                            downloads = completedDownloads,
                            activeDownloads = activeDownloads,
                            pendingDeleteConfirmIds = pendingDeleteConfirmIds,
                            filterSurahs = recitersVm::filterSurahs,
                            onBack = { navController.popBackStack() },
                            onToggleFavorite = { recitersVm.toggleFavorite(reciter.id) },
                            onPlaySurah = { moshaf, surahNum ->
                                requestNotificationPermissionIfNeeded()
                                container.playerController.playSurah(reciter, moshaf, surahNum)
                            },
                            onDownloadAction = { moshaf, surahNum ->
                                requestNotificationPermissionIfNeeded()
                                recitersVm.onDownloadIconTapped(reciter, moshaf, surahNum)
                            },
                            onOpenOtherReciters = { surahNum ->
                                otherRecitersSurah = surahNum
                            }
                        )
                    }
                }

                composable<WirdRoute> {
                    WirdScreen(
                        state = wirdState,
                        events = wirdVm.events,
                        scrollToTopTick = wirdScrollTick,
                        onNextAyah = wirdVm::onNextAyah,
                        onPreviousAyah = wirdVm::onPreviousAyah,
                        onCycleFontSize = wirdVm::cycleFontSize,
                        onCycleDailyGoal = wirdVm::cycleDailyGoal,
                        onJumpToPosition = wirdVm::jumpTo
                    )
                }

                composable<AdhkarRoute> {
                    AdhkarScreen(
                        state = adhkarState,
                        hapticEvents = adhkarVm.hapticEvents,
                        scrollToTopTick = adhkarScrollTick,
                        onSelectSet = adhkarVm::selectSet,
                        onDhikrTap = adhkarVm::onDhikrTapped
                    )
                }

                composable<DownloadsRoute> {
                    DownloadsScreen(
                        state = downloadsState,
                        playerState = playerState,
                        scrollToTopTick = downloadsScrollTick,
                        onPlayGroup = { group, startSurah ->
                            requestNotificationPermissionIfNeeded()
                            container.playerController.playDownloadedGroup(group.items, startSurah)
                        },
                        onCancelActive = downloadsVm::cancelActiveDownload,
                        onDeleteItemClick = downloadsVm::onDeleteDownloadClicked,
                        onDeleteAllClick = downloadsVm::onDeleteAllClicked,
                        onBrowseReciters = { navigateToTab(SakinaTab.RECITERS) }
                    )
                }

                composable<JourneyRoute> {
                    JourneyScreen(
                        state = journeyState,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<AboutRoute> {
                    AboutScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showFullPlayer && playerState.hasActiveItem) {
        val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
        FullPlayerSheet(
            state = playerState,
            sleepTimerState = sleepTimerState,
            isFavoriteReciter = playerState.reciterId in recitersState.favoriteIds,
            onDismiss = { showFullPlayer = false },
            onTogglePlayPause = {
                requestNotificationPermissionIfNeeded()
                container.playerController.togglePlayPause()
            },
            onSeekTo = container.playerController::seekTo,
            onSeekBack15 = container.playerController::seekBack15,
            onSeekForward15 = container.playerController::seekForward15,
            onSkipPrevious = container.playerController::skipPrevious,
            onSkipNext = container.playerController::skipNext,
            onCycleSpeed = {
                val idx = speeds.indexOfFirst { kotlin.math.abs(it - playerState.playbackSpeed) < 0.05f }
                val next = speeds[(idx + 1) % speeds.size]
                container.playerController.setPlaybackSpeed(next)
            },
            onToggleRepeat = container.playerController::toggleRepeatCurrent,
            onToggleFavorite = {
                if (playerState.reciterId > 0) {
                    recitersVm.toggleFavorite(playerState.reciterId)
                }
            },
            onShare = {
                scope.launch {
                    runCatching {
                        ShareCardGenerator.shareSurahCard(
                            context = context,
                            surahNumber = playerState.surahNumber,
                            reciterName = playerState.reciterName,
                            moshafName = playerState.moshafName
                        )
                    }
                }
            },
            onOpenSleepTimer = { showSleepTimerSheet = true },
            onOpenOtherReciters = { otherRecitersSurah = playerState.surahNumber }
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            state = sleepTimerState,
            hasActiveSurah = playerState.hasActiveItem,
            onDismiss = { showSleepTimerSheet = false },
            onSelectMinutes = { mins ->
                requestNotificationPermissionIfNeeded()
                container.playerController.setSleepTimerMinutes(mins)
            },
            onSelectClockEpoch = { epoch ->
                requestNotificationPermissionIfNeeded()
                container.playerController.setSleepTimerAtEpoch(epoch)
            },
            onSelectEndOfSurah = {
                requestNotificationPermissionIfNeeded()
                container.playerController.setSleepTimerEndOfSurah()
            },
            onCancelTimer = container.playerController::cancelSleepTimer
        )
    }

    otherRecitersSurah?.let { surahNum ->
        val matchingReciters = remember(surahNum, recitersState.allReciters) {
            recitersVm.otherRecitersForSurah(surahNum)
        }
        OtherRecitersSheet(
            surahNumber = surahNum,
            currentReciterId = playerState.reciterId,
            reciters = matchingReciters,
            onDismiss = { otherRecitersSurah = null },
            onSelectReciter = { reciter, moshaf ->
                requestNotificationPermissionIfNeeded()
                container.playerController.playSurah(reciter, moshaf, surahNum)
            }
        )
    }
}
