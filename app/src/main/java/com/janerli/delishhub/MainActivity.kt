package com.janerli.delishhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.janerli.delishhub.core.navigation.AppNavGraph
import com.janerli.delishhub.core.ui.theme.DelishHubTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DelishHubTheme {
                AppNavGraph()
            }
        }
    }
}
