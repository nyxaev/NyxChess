package com.nyx.chess.ui.theme

import androidx.compose.ui.graphics.Color

data class BoardPalette(
    val light: Color, val dark: Color,
    val lastMove: Color, val selected: Color, val target: Color, val check: Color,
    val whiteFill: Color, val whiteStroke: Color, val blackFill: Color, val blackStroke: Color,
    val frame: Color
)

enum class BoardTheme(val label: String, val palette: BoardPalette) {
    WOOD("Madeira Clássica", BoardPalette(
        Color(0xFFF0D9B5), Color(0xFFB58863), Color(0x66FFE066), Color(0x8890C040), Color(0x66000000), Color(0xFFE53935),
        Color(0xFFFFF8E7), Color(0xFF3B2A1A), Color(0xFF2B1B10), Color(0xFFE8D5B0), Color(0xFF5D4037))),
    MARBLE("Mármore Minimalista", BoardPalette(
        Color(0xFFEDEFF2), Color(0xFF9AA3AF), Color(0x6642A5F5), Color(0x8866BB6A), Color(0x55263238), Color(0xFFE53935),
        Color(0xFFFFFFFF), Color(0xFF2D3748), Color(0xFF1A202C), Color(0xFFCBD5E0), Color(0xFF6B7280))),
    NEON("Neon / Dark Modern", BoardPalette(
        Color(0xFF232750), Color(0xFF141733), Color(0x66FF4DDB), Color(0x8800E5FF), Color(0x6600E5FF), Color(0xFFFF1744),
        Color(0xFF7DF9FF), Color(0xFF00838F), Color(0xFFFF6EE7), Color(0xFF8E0070), Color(0xFF00E5FF)))
}
