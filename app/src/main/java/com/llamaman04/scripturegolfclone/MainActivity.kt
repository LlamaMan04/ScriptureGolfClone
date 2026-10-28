package com.llamaman04.scripturegolfclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.llamaman04.scripturegolfclone.data.AssetScriptureCatalogDataSource
import com.llamaman04.scripturegolfclone.data.CatalogScriptureRepository
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.llamaman04.scripturegolfclone.ui.GameScreen
import com.llamaman04.scripturegolfclone.ui.GameViewModelFactory
import com.llamaman04.scripturegolfclone.ui.theme.ScriptureGolfCloneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScriptureGolfCloneTheme {
                val context = LocalContext.current
                val repository = remember(context.applicationContext) {
                    CatalogScriptureRepository(
                        AssetScriptureCatalogDataSource(context.applicationContext.assets)
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(viewModel = viewModel(factory = GameViewModelFactory(repository)))
                }
            }
        }
    }
}
