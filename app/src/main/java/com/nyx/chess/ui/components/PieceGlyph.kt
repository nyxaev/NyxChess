package com.nyx.chess.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.nyx.chess.domain.model.Piece
import com.nyx.chess.domain.model.Side
import com.nyx.chess.ui.theme.BoardPalette

/** Peça vetorial (glifo Unicode com contorno): escala perfeita em qualquer densidade. */
@Composable
fun PieceGlyph(piece: Piece, palette: BoardPalette, size: Dp, modifier: Modifier = Modifier) {
    val fontSp = with(LocalDensity.current) { (size * 0.84f).toSp() }
    val strokePx = with(LocalDensity.current) { (size * 0.035f).toPx() }
    val white = piece.side == Side.WHITE
    val fill = if (white) palette.whiteFill else palette.blackFill
    val stroke = if (white) palette.whiteStroke else palette.blackStroke
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Text(piece.type.glyph, style = TextStyle(color = stroke, fontSize = fontSp,
            drawStyle = Stroke(width = strokePx * 2, join = StrokeJoin.Round)))
        Text(piece.type.glyph, style = TextStyle(color = fill, fontSize = fontSp))
    }
}
