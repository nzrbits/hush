package com.nzrbits.hush

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.nzrbits.hush.core.common.model.HushSettings
import com.nzrbits.hush.core.common.navigation.HushRoutes
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsRepository

    /** Set when the HOME button is pressed while the activity is already on top. */
    private val homePressed = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val current by settings.settings.collectAsStateWithLifecycle(initialValue = HushSettings())
            val navController = rememberNavController()
            val homeTick by homePressed.collectAsStateWithLifecycle()
            HushTheme(appearance = current.appearance) {
                HushNavGraph(
                    navController = navController,
                    startDestination = if (current.onboardingDone) HushRoutes.HOME else HushRoutes.ONBOARDING,
                    homeTick = homeTick,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // HOME pressed while Hush is on a sub screen: pop back to the home screen.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePressed.value = homePressed.value + 1
    }
}
