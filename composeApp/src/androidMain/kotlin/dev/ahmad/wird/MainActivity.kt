package dev.ahmad.wird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.ahmad.wird.ui.WirdApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15 and later draw edge to edge at this target SDK whatever the app asks;
        // asking on every version lays the app out the same way everywhere.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { WirdApp() }
    }
}
