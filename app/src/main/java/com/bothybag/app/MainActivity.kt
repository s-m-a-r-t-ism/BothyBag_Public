package com.bothybag.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bothybag.app.navigation.AppNavigation
import com.bothybag.app.ui.theme.BothyBagTheme
import com.bothybag.app.util.appVersionName
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure OSMDroid before any MapView is created
        Configuration.getInstance().apply {
            userAgentValue = "BothyBag/${appVersionName()}"
            osmdroidTileCache = cacheDir
        }

        enableEdgeToEdge()

        setContent {
            BothyBagTheme {
                AppNavigation()
            }
        }
    }
}
