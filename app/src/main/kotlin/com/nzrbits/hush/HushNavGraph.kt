package com.nzrbits.hush

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nzrbits.hush.core.common.navigation.HushRoutes
import com.nzrbits.hush.feature.apps.AppActionsNavigation
import com.nzrbits.hush.feature.apps.DrawerScreen
import com.nzrbits.hush.feature.apps.FavoritesScreen
import com.nzrbits.hush.feature.apps.HiddenAppsScreen
import com.nzrbits.hush.feature.home.HomeNavigation
import com.nzrbits.hush.feature.home.HomeScreen
import com.nzrbits.hush.feature.notifications.ui.NotificationLogScreen
import com.nzrbits.hush.feature.notifications.ui.NotificationRuleEditScreen
import com.nzrbits.hush.feature.notifications.ui.NotificationRulesScreen
import com.nzrbits.hush.feature.settings.AboutScreen
import com.nzrbits.hush.feature.settings.AppearanceScreen
import com.nzrbits.hush.feature.settings.CozySettingsScreen
import com.nzrbits.hush.feature.settings.FaqScreen
import com.nzrbits.hush.feature.settings.GesturesScreen
import com.nzrbits.hush.feature.settings.HomeSettingsScreen
import com.nzrbits.hush.feature.settings.OnboardingScreen
import com.nzrbits.hush.feature.settings.PermissionsScreen
import com.nzrbits.hush.feature.settings.PrivacyScreen
import com.nzrbits.hush.feature.settings.SettingsNavigation
import com.nzrbits.hush.feature.settings.SettingsScreen
import com.nzrbits.hush.feature.wellbeing.ui.BlockAppScreen
import com.nzrbits.hush.feature.wellbeing.ui.LimitsScreen
import com.nzrbits.hush.feature.wellbeing.ui.ScheduleEditScreen
import com.nzrbits.hush.feature.wellbeing.ui.SchedulesScreen
import com.nzrbits.hush.feature.wellbeing.ui.ScreenTimeScreen
import com.nzrbits.hush.feature.wellbeing.ui.ShortVideoScreen
import com.nzrbits.hush.feature.wellbeing.ui.WellbeingHubScreen

@Composable
fun HushNavGraph(navController: NavHostController, startDestination: String, homeTick: Int) {
    LaunchedEffect(homeTick) {
        if (homeTick > 0) navController.popBackStack(HushRoutes.HOME, inclusive = false)
    }
    val back: () -> Unit = { navController.popBackStack() }
    val go: (String) -> Unit = { route -> navController.navigate(route) { launchSingleTop = true } }
    val openPermissions: () -> Unit = { go(HushRoutes.SETTINGS_PERMISSIONS) }

    val appActions = remember {
        AppActionsNavigation(
            onBlockApp = { pkg -> go(HushRoutes.blockApp(pkg)) },
            onSetLimit = { pkg -> go("${HushRoutes.LIMITS}?packageName=$pkg") },
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(HushRoutes.ONBOARDING) {
            OnboardingScreen(onDone = {
                navController.navigate(HushRoutes.HOME) { popUpTo(HushRoutes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(HushRoutes.HOME) {
            HomeScreen(
                HomeNavigation(
                    openDrawer = { go(HushRoutes.DRAWER) },
                    openSettings = { go(HushRoutes.SETTINGS) },
                    openWellbeing = { go(HushRoutes.WELLBEING) },
                    openNotificationLog = { go(HushRoutes.NOTIFICATION_LOG) },
                    openPermissions = openPermissions,
                    appActions = appActions,
                ),
            )
        }
        composable(HushRoutes.DRAWER) { DrawerScreen(navigation = appActions, onClose = back, onOpenSettings = { go(HushRoutes.SETTINGS) }) }

        composable(HushRoutes.WELLBEING) {
            WellbeingHubScreen(
                onBack = back,
                onOpenSchedules = { go(HushRoutes.SCHEDULES) },
                onOpenLimits = { go(HushRoutes.LIMITS) },
                onOpenScreenTime = { go(HushRoutes.SCREEN_TIME) },
                onOpenShortVideo = { go(HushRoutes.SHORT_VIDEO) },
                onOpenNotificationRules = { go(HushRoutes.NOTIFICATION_LOG) },
                onOpenPermissions = openPermissions,
            )
        }
        composable(HushRoutes.BLOCK_APP) { BlockAppScreen(onBack = back, onOpenPermissions = openPermissions) }
        composable(HushRoutes.SCHEDULES) { SchedulesScreen(onBack = back, onEdit = { id -> go(HushRoutes.scheduleEdit(id)) }) }
        composable(HushRoutes.SCHEDULE_EDIT) { ScheduleEditScreen(onBack = back) }
        composable("${HushRoutes.LIMITS}?packageName={packageName}") { LimitsScreen(onBack = back, onOpenPermissions = openPermissions) }
        composable(HushRoutes.SCREEN_TIME) { ScreenTimeScreen(onBack = back, onOpenPermissions = openPermissions) }
        composable(HushRoutes.SHORT_VIDEO) { ShortVideoScreen(onBack = back, onOpenPermissions = openPermissions) }

        composable(HushRoutes.NOTIFICATION_LOG) {
            NotificationLogScreen(onBack = back, onOpenRules = { go(HushRoutes.NOTIFICATION_RULES) }, onOpenPermissions = openPermissions)
        }
        composable(HushRoutes.NOTIFICATION_RULES) { NotificationRulesScreen(onBack = back, onEdit = { id -> go(HushRoutes.notificationRuleEdit(id)) }) }
        composable(HushRoutes.NOTIFICATION_RULE_EDIT) { NotificationRuleEditScreen(onBack = back) }

        composable(HushRoutes.SETTINGS) {
            SettingsScreen(
                SettingsNavigation(
                    back = back,
                    limits = { go(HushRoutes.LIMITS) },
                    blocking = { go(HushRoutes.WELLBEING) },
                    schedules = { go(HushRoutes.SCHEDULES) },
                    screenTime = { go(HushRoutes.SCREEN_TIME) },
                    notificationRules = { go(HushRoutes.NOTIFICATION_RULES) },
                    notificationLog = { go(HushRoutes.NOTIFICATION_LOG) },
                    shortVideo = { go(HushRoutes.SHORT_VIDEO) },
                    home = { go(HushRoutes.SETTINGS_HOME) },
                    favorites = { go(HushRoutes.SETTINGS_FAVORITES) },
                    hidden = { go(HushRoutes.SETTINGS_HIDDEN) },
                    appearance = { go(HushRoutes.SETTINGS_APPEARANCE) },
                    gestures = { go(HushRoutes.SETTINGS_GESTURES) },
                    cozy = { go(HushRoutes.SETTINGS_COZY) },
                    permissions = openPermissions,
                    about = { go(HushRoutes.SETTINGS_ABOUT) },
                    privacy = { go(HushRoutes.SETTINGS_PRIVACY) },
                    faq = { go(HushRoutes.SETTINGS_FAQ) },
                ),
            )
        }
        composable(HushRoutes.SETTINGS_HOME) { HomeSettingsScreen(onBack = back) }
        composable(HushRoutes.SETTINGS_FAVORITES) { FavoritesScreen(onBack = back) }
        composable(HushRoutes.SETTINGS_HIDDEN) { HiddenAppsScreen(onBack = back) }
        composable(HushRoutes.SETTINGS_APPEARANCE) { AppearanceScreen(onBack = back) }
        composable(HushRoutes.SETTINGS_GESTURES) { GesturesScreen(onBack = back) }
        composable(HushRoutes.SETTINGS_COZY) { CozySettingsScreen(onBack = back) }
        composable(HushRoutes.SETTINGS_PERMISSIONS) { PermissionsScreen(onBack = back) }
        composable(HushRoutes.SETTINGS_ABOUT) { AboutScreen(onBack = back) }
        composable(HushRoutes.SETTINGS_PRIVACY) { PrivacyScreen(onBack = back) }
        composable(HushRoutes.SETTINGS_FAQ) { FaqScreen(onBack = back) }
    }
}
