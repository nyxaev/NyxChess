package com.nyx.chess.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyx.chess.domain.model.*
import com.nyx.chess.presentation.*
import com.nyx.chess.ui.components.*
import com.nyx.chess.ui.theme.BoardTheme
import com.nyx.chess.util.SoundPlayer

@Composable
fun GameScreen(state: GameUiState, vm: GameViewModel) {
    val haptic = LocalHapticFeedback.current
    val sound = remember { SoundPlayer() }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    val latest by rememberUpdatedState(state)

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            if (latest.settings.haptics) haptic.performHapticFeedback(
                if (e == GameEvent.Select || e == GameEvent.Move) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
            if (latest.settings.sound) sound.play(e)
        }
    }

    val palette = state.settings.boardTheme.palette
    val flipped = state.settings.autoFlip && state.position.turn == Side.BLACK
    val top = if (flipped) Side.WHITE else Side.BLACK
    var showRestart by remember { mutableStateOf(false) }
    var resultDismissed by remember(state.status) { mutableStateOf(false) }

    val board: @Composable (Modifier) -> Unit = { m ->
        ChessBoard(state, palette, flipped, vm::onSquareClick, vm::onDrop, m)
    }
    val controls: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton({ vm.undo() }, Modifier.weight(1f), enabled = state.records.isNotEmpty()) { Text("↶ Desfazer") }
            FilledTonalButton({ showRestart = true }, Modifier.weight(1f)) { Text("⟳ Reiniciar") }
            FilledTonalButton({
                val all = BoardTheme.entries
                val next = all[(state.settings.boardTheme.ordinal + 1) % all.size]
                vm.updateSettings { it.copy(boardTheme = next) }
            }, Modifier.weight(1f)) { Text("◧ Tema") }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
        if (maxWidth > maxHeight) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                board(Modifier.fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerBar(top, state, palette)
                    StatusStrip(state, Modifier.weight(1f))
                    MoveHistory(state.records)
                    controls()
                    PlayerBar(top.opp, state, palette)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlayerBar(top, state, palette)
                board(Modifier.fillMaxWidth())
                PlayerBar(top.opp, state, palette)
                StatusStrip(state, Modifier.padding(vertical = 2.dp))
                MoveHistory(state.records)
                Spacer(Modifier.weight(1f))
                controls()
                TextButton({ vm.backToMenu() }, Modifier.align(Alignment.CenterHorizontally)) { Text("Menu principal") }
            }
        }
    }

    // Promoção de peão
    if (state.pendingPromotion != null) {
        val side = state.position.turn
        AlertDialog(
            onDismissRequest = vm::cancelPromotion,
            title = { Text("Promover peão") },
            text = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { t ->
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.clickable { vm.choosePromotion(t) }) {
                            Box(Modifier.padding(4.dp)) { PieceGlyph(Piece(t, side), palette, 52.dp) }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(vm::cancelPromotion) { Text("Cancelar") } }
        )
    }

    // Fim de jogo
    if (state.status.isOver && !resultDismissed) {
        val msg = when (val s = state.status) {
            is GameStatus.Checkmate -> "Xeque-mate! ${sideName(s.winner)} vencem."
            GameStatus.Stalemate -> "Empate por afogamento (rei sem lances legais)."
            is GameStatus.Draw -> if (s.reason == DrawReason.FIFTY_MOVES) "Empate pela regra dos 50 lances." else "Empate por material insuficiente."
            is GameStatus.Timeout -> "Tempo esgotado. ${sideName(s.winner)} vencem."
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { resultDismissed = true },
            title = { Text("Fim de jogo") }, text = { Text(msg, fontSize = 16.sp) },
            confirmButton = { Button(vm::restart) { Text("Nova partida") } },
            dismissButton = { TextButton({ resultDismissed = true }) { Text("Ver tabuleiro") } }
        )
    }

    if (showRestart) AlertDialog(
        onDismissRequest = { showRestart = false },
        title = { Text("Reiniciar partida?") }, text = { Text("O progresso atual será perdido.") },
        confirmButton = { Button({ showRestart = false; vm.restart() }) { Text("Reiniciar") } },
        dismissButton = { TextButton({ showRestart = false }) { Text("Cancelar") } }
    )
}
