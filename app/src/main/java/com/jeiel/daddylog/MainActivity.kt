package com.jeiel.daddylog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jeiel.daddylog.ui.MainAppContainer
import com.jeiel.daddylog.ui.ScalpViewModel
import com.jeiel.daddylog.ui.theme.DaddyLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaddyLogTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: ScalpViewModel = viewModel()
                    MainAppContainer(viewModel = viewModel)
                }
            }
        }
    }
}
