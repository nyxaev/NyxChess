package com.nyx.chess.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nyx.chess.presentation.GameUiState
import com.nyx.chess.ui.theme.BoardPalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ChessBoard(
    state: GameUiState,
    palette: BoardPalette,
    flipped: Boolean,
    onTap: (Int) -> Unit,
    onDrop: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val density = LocalDensity.current
        val sizePx = with(density) { maxWidth.toPx() }
        val sq = sizePx / 8f
        val sqDp = maxWidth / 8
        val scope = rememberCoroutineScope()
        val cur by rememberUpdatedState(state)

        // Rotação suave do tabuleiro (pass-and-play); peças contra-rotacionam para ficarem em pé.
        val angle by animateFloatAsState(if (flipped) 180f else 0f, tween(650, easing = FastOutSlowInEasing), label = "rot")
        val pulse by rememberInfiniteTransition(label = "pulse")
            .animateFloat(0.35f, 0.9f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "p")

        var dragFrom by remember { mutableStateOf<Int?>(null) }
        var dragPos by remember { mutableStateOf(Offset.Zero) }

        fun squareAt(o: Offset): Int =
            (o.y / sq).toInt().coerceIn(0, 7) * 8 + (o.x / sq).toInt().coerceIn(0, 7)
        fun centerOf(s: Int) = Offset((s % 8 + 0.5f) * sq, (s / 8 + 0.5f) * sq)

        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { rotationZ = angle }
                .shadow(10.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(sq) { detectTapGestures { onTap(squareAt(it)) } }
                .pointerInput(sq) {
                    detectDragGestures(
                        onDragStart = { off ->
                            val s = squareAt(off)
                            val p = cur.position.squares[s]
                            if (p != null && p.side == cur.position.turn && !cur.status.isOver && cur.pendingPromotion == null) {
                                dragFrom = s; dragPos = off
                                if (cur.selected != s) onTap(s)
                            }
                        },
                        onDrag = { change, amount -> change.consume(); if (dragFrom != null) dragPos += amount },
                        onDragEnd = {
                            val from = dragFrom
                            if (from != null) {
                                val to = squareAt(dragPos)
                                val start = dragPos
                                scope.launch {
                                    val ok = to in cur.targets
                                    animate(Offset.VectorConverter, start, centerOf(if (ok) to else from),
                                        animationSpec = spring(0.7f, Spring.StiffnessMedium)) { v, _ -> dragPos = v }
                                    if (ok) onDrop(from, to)
                                    dragFrom = null
                                }
                            }
                        },
                        onDragCancel = { dragFrom = null }
                    )
                }
        ) {
            // Casas + destaques
            Canvas(Modifier.fillMaxSize()) {
                for (i in 0..63) {
                    val r = i / 8; val c = i % 8
                    val tl = Offset(c * sq, r * sq)
                    drawRect(if ((r + c) % 2 == 0) palette.light else palette.dark, tl, Size(sq, sq))
                    cur.lastMove?.let { if (i == it.first || i == it.second) drawRect(palette.lastMove, tl, Size(sq, sq)) }
                    if (i == cur.selected) drawRect(palette.selected, tl, Size(sq, sq))
                    if (i == cur.checkSquare) drawRect(
                        Brush.radialGradient(
                            listOf(palette.check.copy(alpha = pulse), palette.check.copy(alpha = 0f)),
                            center = centerOf(i), radius = sq * 0.75f
                        ), tl, Size(sq, sq)
                    )
                    if (i in cur.targets) {
                        if (cur.position.squares[i] == null)
                            drawCircle(palette.target, sq * 0.16f, centerOf(i))
                        else drawRoundRect(palette.target, tl + Offset(sq * 0.06f, sq * 0.06f), Size(sq * 0.88f, sq * 0.88f),
                            CornerRadius(sq * 0.4f), Stroke(sq * 0.08f))
                    }
                }
            }
            // Coordenadas
            for (k in 0..7) {
                val lbl = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                    color = if (k % 2 == 0) palette.dark else palette.light)
                Text(('a' + k).toString(), style = lbl.copy(color = if ((7 + k) % 2 == 0) palette.dark else palette.light),
                    modifier = Modifier.offset { IntOffset((k * sq + sq * 0.8f).roundToInt(), (7 * sq + sq * 0.74f).roundToInt()) }
                        .graphicsLayer { rotationZ = angle })
                Text((8 - k).toString(), style = lbl.copy(color = if (k % 2 == 0) palette.dark else palette.light),
                    modifier = Modifier.offset { IntOffset((sq * 0.06f).roundToInt(), (k * sq + sq * 0.04f).roundToInt()) }
                        .graphicsLayer { rotationZ = angle })
            }
            // Peça capturada desvanecendo
            cur.ghost?.let { (gs, gp) ->
                key(cur.animTick) {
                    val a = remember { Animatable(1f) }
                    LaunchedEffect(Unit) { a.animateTo(0f, tween(260)) }
                    PieceGlyph(gp, palette, sqDp, Modifier
                        .offset { IntOffset(((gs % 8) * sq).roundToInt(), ((gs / 8) * sq).roundToInt()) }
                        .graphicsLayer { alpha = a.value; scaleX = 0.6f + 0.4f * a.value; scaleY = 0.6f + 0.4f * a.value; rotationZ = angle }
                        .zIndex(1f))
                }
            }
            // Peças
            for (i in 0..63) {
                val piece = cur.position.squares[i] ?: continue
                if (i == dragFrom) continue
                val anim = cur.anims.firstOrNull { it.second == i }
                key(i) {
                    val a = remember(cur.animTick) { Animatable(if (anim != null) 0f else 1f) }
                    LaunchedEffect(cur.animTick) {
                        if (anim != null) a.animateTo(1f, spring(0.78f, Spring.StiffnessMediumLow))
                    }
                    val dx = if (anim != null) ((anim.first % 8) - (i % 8)) * sq else 0f
                    val dy = if (anim != null) ((anim.first / 8) - (i / 8)) * sq else 0f
                    PieceGlyph(piece, palette, sqDp, Modifier
                        .offset { IntOffset(((i % 8) * sq).roundToInt(), ((i / 8) * sq).roundToInt()) }
                        .graphicsLayer {
                            translationX = dx * (1f - a.value); translationY = dy * (1f - a.value); rotationZ = angle
                        }
                        .zIndex(if (anim != null) 3f else 2f))
                }
            }
            // Peça sendo arrastada (levemente ampliada)
            dragFrom?.let { f ->
                val dp = cur.position.squares[f] ?: cur.position.squares[cur.lastMove?.second ?: f]
                if (dp != null) PieceGlyph(dp, palette, sqDp, Modifier
                    .offset { IntOffset((dragPos.x - sq / 2).roundToInt(), (dragPos.y - sq / 2).roundToInt()) }
                    .graphicsLayer { scaleX = 1.18f; scaleY = 1.18f; rotationZ = angle }
                    .zIndex(10f))
            }
        }
    }
}
