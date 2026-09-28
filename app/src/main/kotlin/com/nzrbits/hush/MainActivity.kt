package com.nzrbits.hush

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            // Null until DataStore has answered once, so neither the onboarding nor the wrong
            // theme flashes before the stored settings are known.
            val current: HushSettings? by settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val loaded = current
            if (loaded == null) {
                Box(Modifier.fillMaxSize().background(Color.Black))
                return@setContent
            }
            val navController = rememberNavController()
            val homeTick by homePressed.collectAsStateWithLifecycle()
            // Decided once per activity instance; later changes navigate instead of rebuilding the graph.
            val startDestination = remember { if (loaded.onboardingDone) HushRoutes.HOME else HushRoutes.ONBOARDING }
            HushTheme(appearance = loaded.appearance) {
                HushNavGraph(
                    navController = navController,
                    startDestination = startDestination,
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
