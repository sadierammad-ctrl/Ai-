package com.example.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.animations.GalaxyVideoBackground
import com.example.ui.components.BabyBottomNavBar
import com.example.ui.components.PhoneConfirmationDialog
import com.example.ui.components.Routes
import com.example.ui.components.babyBottomTabs
import com.example.ui.screens.AlwaysReadyScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.LiveAiScreen
import com.example.ui.screens.LiveWallpaperPreviewScreen
import com.example.ui.screens.PhoneControlScreen
import com.example.ui.screens.PrivacySecurityScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.BabyAiTheme
import com.example.ui.theme.GlassCardDark
import com.example.viewmodel.BabyAiViewModel

@Composable
fun BabyAiApp(
    viewModel: BabyAiViewModel,
    initialRoute: String = Routes.HOME
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissionStatus()
        if (granted) {
            viewModel.startVoiceRecognition()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionStatus()
    }

    val requestMicAndListen: () -> Unit = {
        if (uiState.permissionStatus.hasMicrophone) {
            viewModel.startVoiceRecognition()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val requestMicOnly: () -> Unit = {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val requestNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.phoneExecutor.openAppNotificationSettings()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        val err = uiState.errorMessage
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showMainNav = currentRoute in listOf(
        Routes.HOME, Routes.CHAT, Routes.VOICE, Routes.HISTORY, Routes.SETTINGS
    )

    BabyAiTheme(themePreset = uiState.settings.themePreset) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Persistent Full-Screen Futuristic Galaxy Video Background across ALL screens
            GalaxyVideoBackground(
                orbState = uiState.orbState,
                animationsEnabled = uiState.settings.animationsEnabled,
                batterySavingMode = uiState.settings.batterySavingMode
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 840.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavRail && showMainNav) {
                        NavigationRail(
                            containerColor = GlassCardDark.copy(alpha = 0.85f),
                            modifier = Modifier
                                .fillMaxHeight()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                        ) {
                            babyBottomTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            if (!useNavRail && showMainNav) {
                                BabyBottomNavBar(
                                    currentRoute = currentRoute,
                                    onNavigate = ::navigateTopTab
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = initialRoute,
                            enterTransition = { fadeIn(animationSpec = tween(220)) },
                            exitTransition = { fadeOut(animationSpec = tween(180)) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable(Routes.HOME) {
                                HomeDashboardScreen(
                                    uiState = uiState,
                                    rmsLevel = rmsLevel,
                                    recentConversations = conversations,
                                    onRequestMicPermission = requestMicOnly,
                                    onMicTap = requestMicAndListen,
                                    onSubmitText = { text ->
                                        viewModel.sendUserMessage(text, speakReply = false)
                                        navigateTopTab(Routes.CHAT)
                                    },
                                    onStartConversation = {
                                        viewModel.startNewConversation()
                                        navigateTopTab(Routes.CHAT)
                                    },
                                    onOpenVoiceAssistant = { navigateTopTab(Routes.VOICE) },
                                    onOpenLiveAi = { navController.navigate(Routes.LIVE_AI) },
                                    onOpenPhoneControl = { navController.navigate(Routes.PHONE_CONTROL) },
                                    onOpenAlwaysReady = { navController.navigate(Routes.ALWAYS_READY) },
                                    onOpenWallpaper = { navController.navigate(Routes.WALLPAPER) },
                                    onOpenConversation = { id ->
                                        viewModel.openConversation(id)
                                        navigateTopTab(Routes.CHAT)
                                    },
                                    onEmergencyStop = viewModel::triggerEmergencyStop
                                )
                            }

                            composable(Routes.CHAT) {
                                ChatScreen(
                                    uiState = uiState,
                                    messages = activeMessages,
                                    onSendMessage = { viewModel.sendUserMessage(it, speakReply = false) },
                                    onVoiceInput = requestMicAndListen,
                                    onRetryLast = viewModel::retryLastUserMessage,
                                    onSpeakMessage = { viewModel.speakText(it) },
                                    onNewConversation = viewModel::startNewConversation,
                                    onClearConversation = viewModel::clearActiveConversation,
                                    onEmergencyStop = viewModel::triggerEmergencyStop
                                )
                            }

                            composable(Routes.VOICE) {
                                VoiceAssistantScreen(
                                    uiState = uiState,
                                    isListening = isListening,
                                    isSpeaking = isSpeaking,
                                    rmsLevel = rmsLevel,
                                    partialTranscript = partialTranscript,
                                    onStartListening = requestMicAndListen,
                                    onStopListening = viewModel.voiceController::stopListening,
                                    onInterruptAndListen = viewModel::interruptAndListen,
                                    onStopSpeaking = viewModel::stopSpeaking,
                                    onRepeatResponse = viewModel::repeatLastResponse,
                                    onGreetUser = viewModel::greetVoiceAssistant,
                                    onOpenLiveAi = { navController.navigate(Routes.LIVE_AI) },
                                    onLanguageChange = viewModel::setLanguage,
                                    onGenderChange = viewModel::setVoiceGender,
                                    onSpeedChange = viewModel::setVoiceSpeed,
                                    onVolumeChange = viewModel::setSpeechVolume,
                                    onEmergencyStop = viewModel::triggerEmergencyStop
                                )
                            }

                            composable(Routes.LIVE_AI) {
                                LiveAiScreen(
                                    uiState = uiState,
                                    isListening = isListening,
                                    isSpeaking = isSpeaking,
                                    rmsLevel = rmsLevel,
                                    onStartSession = {
                                        if (uiState.permissionStatus.hasMicrophone) {
                                            viewModel.startLiveAiSession()
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    onToggleMute = viewModel::toggleLiveMute,
                                    onInterrupt = viewModel::interruptAndListen,
                                    onEndSession = viewModel::endLiveAiSession,
                                    onBack = {
                                        viewModel.endLiveAiSession()
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(Routes.HISTORY) {
                                HistoryScreen(
                                    conversations = conversations,
                                    searchQuery = uiState.historySearchQuery,
                                    onSearchChange = viewModel::updateHistorySearch,
                                    onOpenConversation = { id ->
                                        viewModel.openConversation(id)
                                        navigateTopTab(Routes.CHAT)
                                    },
                                    onDeleteConversation = viewModel::deleteConversation,
                                    onClearAllHistory = viewModel::clearAllHistory
                                )
                            }

                            composable(Routes.PHONE_CONTROL) {
                                PhoneControlScreen(
                                    uiState = uiState,
                                    onBack = { navController.popBackStack() },
                                    onRequestMicPermission = requestMicOnly,
                                    onRequestNotificationPermission = requestNotifications,
                                    onOpenAccessibilitySettings = viewModel.phoneExecutor::openAccessibilitySettings,
                                    onOpenNotificationListenerSettings = viewModel.phoneExecutor::openNotificationListenerSettings,
                                    onOpenBatterySettings = viewModel.phoneExecutor::openBatteryOptimizationSettings,
                                    onOpenKeyboardSettings = viewModel.phoneExecutor::openInputMethodSettings,
                                    onToggleWakeWord = { enabled ->
                                        if (enabled && !uiState.permissionStatus.hasMicrophone) {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            viewModel.toggleWakeWord(enabled)
                                        }
                                    },
                                    onExecuteAction = viewModel::executeDirectPhoneAction,
                                    onEmergencyStop = viewModel::triggerEmergencyStop
                                )
                            }

                            composable(Routes.SETTINGS) {
                                SettingsScreen(
                                    uiState = uiState,
                                    availableVoices = availableVoices,
                                    onAiModelChange = viewModel::setAiModel,
                                    onLanguageChange = viewModel::setLanguage,
                                    onVoiceSelected = viewModel::setSelectedVoiceName,
                                    onVoiceGenderChange = viewModel::setVoiceGender,
                                    onVoiceSpeedChange = viewModel::setVoiceSpeed,
                                    onSpeechVolumeChange = viewModel::setSpeechVolume,
                                    onToggleWakeWord = { enabled ->
                                        if (enabled && !uiState.permissionStatus.hasMicrophone) {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            viewModel.toggleWakeWord(enabled)
                                        }
                                    },
                                    onToggleAnimations = viewModel::setAnimationsEnabled,
                                    onToggleBatterySaving = viewModel::setBatterySavingMode,
                                    onThemePresetChange = viewModel::setThemePreset,
                                    onRequestMicPermission = requestMicOnly,
                                    onRequestNotificationPermission = requestNotifications,
                                    onOpenAccessibilitySettings = viewModel.phoneExecutor::openAccessibilitySettings,
                                    onOpenPrivacyScreen = { navController.navigate(Routes.PRIVACY) },
                                    onOpenHistoryScreen = { navigateTopTab(Routes.HISTORY) },
                                    onOpenWallpaperScreen = { navController.navigate(Routes.WALLPAPER) },
                                    onOpenKeyboardSettings = viewModel.phoneExecutor::openInputMethodSettings,
                                    onOpenAlwaysReady = { navController.navigate(Routes.ALWAYS_READY) }
                                )
                            }

                            composable(Routes.ALWAYS_READY) {
                                AlwaysReadyScreen(
                                    uiState = uiState,
                                    onBack = { navController.popBackStack() },
                                    onToggleAlwaysReady = { enabled ->
                                        if (enabled && !uiState.permissionStatus.hasMicrophone) {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            viewModel.toggleWakeWord(enabled)
                                        }
                                    },
                                    onToggleLowPower = viewModel::setLowPowerWakeDetection,
                                    onToggleStartOnBoot = viewModel::setStartOnBootEnabled,
                                    onToggleVoiceChime = viewModel::setWakeWordVoiceChime,
                                    onAskBabyNow = {
                                        if (!uiState.permissionStatus.hasMicrophone) {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        } else {
                                            viewModel.askBabyViaWakeService()
                                        }
                                    },
                                    onRefreshDiagnostics = viewModel::refreshPermissionStatus,
                                    onRequestMicPermission = requestMicOnly,
                                    onRequestNotificationPermission = requestNotifications,
                                    onOpenBatterySettings = viewModel.phoneExecutor::openBatteryOptimizationSettings,
                                    onEmergencyStop = viewModel::triggerEmergencyStop
                                )
                            }

                            composable(Routes.PRIVACY) {
                                PrivacySecurityScreen(
                                    onBack = { navController.popBackStack() },
                                    onEmergencyStop = viewModel::triggerEmergencyStop
                                )
                            }

                            composable(Routes.WALLPAPER) {
                                LiveWallpaperPreviewScreen(
                                    animationsEnabled = uiState.settings.animationsEnabled,
                                    batterySavingMode = uiState.settings.batterySavingMode,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }

            uiState.pendingConfirmation?.let { pending ->
                PhoneConfirmationDialog(
                    pending = pending,
                    onConfirm = viewModel::executeConfirmedAction,
                    onDismiss = viewModel::dismissPendingConfirmation
                )
            }
        }
    }
}
