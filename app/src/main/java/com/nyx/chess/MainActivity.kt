package com.nyx.chess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nyx.chess.presentation.GameViewModel
import com.nyx.chess.ui.screens.GameScreen
import com.nyx.chess.ui.screens.MenuScreen
import com.nyx.chess.ui.theme.ChessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: GameViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            ChessTheme(state.settings.themeMode) {
                Surface(Modifier.fillMaxSize()) {
                    if (state.inGame) {
                        BackHandler { vm.backToMenu() }
                        GameScreen(state, vm)
                    } else MenuScreen(state.settings, vm)
                }
            }
        }
    }
}
