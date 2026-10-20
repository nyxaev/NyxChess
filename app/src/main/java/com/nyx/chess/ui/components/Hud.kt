package com.nyx.chess.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyx.chess.domain.model.*
import com.nyx.chess.presentation.GameUiState
import com.nyx.chess.presentation.MoveRecord
import com.nyx.chess.ui.theme.BoardPalette

fun formatTime(ms: Long): String {
    val s = (ms + 999) / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

fun sideName(s: Side) = if (s == Side.WHITE) "Brancas" else "Pretas"

@Composable
fun PlayerBar(side: Side, state: GameUiState, palette: BoardPalette, modifier: Modifier = Modifier) {
    val active = state.position.turn == side && !state.status.isOver
    val bg by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, label = "bg")
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape)
            .background(if (side == Side.WHITE) palette.whiteFill else palette.blackFill))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(sideName(side), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            val caps = state.capturedBy(side)
            val adv = caps.sumOf { it.type.value } - state.capturedBy(side.opp).sumOf { it.type.value }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 22.dp)) {
                Text(caps.joinToString("") { it.type.glyph }, fontSize = 16.sp, maxLines = 1,
                    color = if (side == Side.WHITE) palette.blackFill.copy(alpha = 1f) else palette.whiteFill,
                    modifier = Modifier.weight(1f, fill = false))
                if (adv > 0) Text("  +$adv", style = MaterialTheme.typography.labelMedium)
            }
        }
        if (state.settings.timeControl.enabled) {
            val ms = if (side == Side.WHITE) state.whiteMs else state.blackMs
            Text(formatTime(ms), fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = if (ms < 10_000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun StatusStrip(state: GameUiState, modifier: Modifier = Modifier) {
    val turn = sideName(state.position.turn)
    val text = when (val s = state.status) {
        GameStatus.Ongoing -> "Vez das $turn"
        GameStatus.Check -> "Xeque! Vez das $turn"
        is GameStatus.Checkmate -> "Xeque-mate! ${sideName(s.winner)} vencem"
        GameStatus.Stalemate -> "Empate por afogamento"
        is GameStatus.Draw -> "Empate"
        is GameStatus.Timeout -> "Tempo esgotado! ${sideName(s.winner)} vencem"
    }
    val danger = state.status is GameStatus.Check || state.status is GameStatus.Checkmate
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AnimatedContent(text, label = "status") { t ->
            Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun MoveHistory(records: List<MoveRecord>, modifier: Modifier = Modifier) {
    val list = rememberLazyListState()
    val pairs = records.chunked(2)
    LaunchedEffect(pairs.size) { if (pairs.isNotEmpty()) list.animateScrollToItem(pairs.lastIndex) }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        if (pairs.isEmpty()) Text("Histórico de jogadas", Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        else LazyRow(state = list, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(pairs) { idx, p ->
                Text(buildString { append("${idx + 1}. ${p[0].san}"); if (p.size > 1) append("  ${p[1].san}") },
                    fontFamily = FontFamily.Monospace, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}
