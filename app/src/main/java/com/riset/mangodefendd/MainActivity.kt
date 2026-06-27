package com.riset.mangodefendd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.riset.mangodefendd.ui.theme.MangoDefenddTheme
import dagger.hilt.android.AndroidEntryPoint
import com.riset.mangodefendd.navigation.AppNavHost

/**
 * MainActivity now delegates navigation to `AppNavHost` to keep the activity minimal.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MangoDefenddTheme {
                AppNavHost()
            }
        }
    }
}

