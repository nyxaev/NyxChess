package com.nyx.chess.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyx.chess.presentation.*
import com.nyx.chess.ui.theme.BoardTheme
import com.nyx.chess.ui.theme.ThemeMode

@Composable
fun MenuScreen(settings: GameSettings, vm: GameViewModel) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("♞ Xadrez", fontSize = 40.sp, fontWeight = FontWeight.Black)
        Text("Pass-and-Play para 2 jogadores", color = MaterialTheme.colorScheme.outline)

        Section("Tempo de jogo") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeControl.entries.forEach { tc ->
                    FilterChip(settings.timeControl == tc, { vm.updateSettings { it.copy(timeControl = tc) } }, { Text(tc.label) })
                }
            }
        }
        Section("Tabuleiro") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BoardTheme.entries.forEach { bt ->
                    val sel = settings.boardTheme == bt
                    Card(
                        Modifier.width(132.dp).clickable { vm.updateSettings { it.copy(boardTheme = bt) } },
                        border = BorderStroke(if (sel) 3.dp else 1.dp,
                            if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Column(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))) {
                                repeat(4) { r -> Row(Modifier.weight(1f)) { repeat(4) { c ->
                                    Box(Modifier.weight(1f).fillMaxHeight()
                                        .background(if ((r + c) % 2 == 0) bt.palette.light else bt.palette.dark))
                                } } }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(bt.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        Section("Aparência do app") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { m ->
                    FilterChip(settings.themeMode == m, { vm.updateSettings { it.copy(themeMode = m) } }, { Text(m.label) })
                }
            }
        }
        Section("Opções") {
            SwitchRow("Girar o tabuleiro a cada jogada", settings.autoFlip) { v -> vm.updateSettings { it.copy(autoFlip = v) } }
            SwitchRow("Efeitos sonoros", settings.sound) { v -> vm.updateSettings { it.copy(sound = v) } }
            SwitchRow("Vibração (háptico)", settings.haptics) { v -> vm.updateSettings { it.copy(haptics = v) } }
        }
        Button(vm::startGame, Modifier.fillMaxWidth().height(56.dp)) { Text("Jogar", fontSize = 18.sp) }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChange)
    }
}
