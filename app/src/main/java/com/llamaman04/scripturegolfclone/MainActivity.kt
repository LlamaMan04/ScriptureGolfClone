package com.llamaman04.scripturegolfclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.llamaman04.scripturegolfclone.ui.GameScreen
import com.llamaman04.scripturegolfclone.ui.theme.ScriptureGolfCloneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScriptureGolfCloneTheme {
                GameScreen(viewModel = viewModel())
            }
        }
    }
}
