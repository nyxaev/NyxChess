package com.nyx.chess.presentation

import com.nyx.chess.domain.engine.ChessEngine
import com.nyx.chess.domain.model.*
import com.nyx.chess.ui.theme.BoardTheme
import com.nyx.chess.ui.theme.ThemeMode

enum class TimeControl(val label: String, val baseMs: Long, val incMs: Long) {
    UNLIMITED("Ilimitado", 0, 0),
    BULLET("Bullet 1+0", 60_000, 0),
    BLITZ("Blitz 5+0", 300_000, 0),
    FISCHER("Fischer 10+5", 600_000, 5_000);
    val enabled: Boolean get() = baseMs > 0
}

data class GameSettings(
    val boardTheme: BoardTheme = BoardTheme.WOOD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val timeControl: TimeControl = TimeControl.UNLIMITED,
    val autoFlip: Boolean = false,
    val sound: Boolean = true,
    val haptics: Boolean = true
)

data class MoveRecord(val move: Move, val san: String, val side: Side)

data class GameUiState(
    val inGame: Boolean = false,
    val settings: GameSettings = GameSettings(),
    val position: Position = ChessEngine.initial(),
    val records: List<MoveRecord> = emptyList(),
    val selected: Int? = null,
    val targets: Set<Int> = emptySet(),
    val lastMove: Pair<Int, Int>? = null,
    val status: GameStatus = GameStatus.Ongoing,
    val pendingPromotion: Pair<Int, Int>? = null,
    /** Pares (origem, destino) a animar neste lance (inclui a torre no roque). */
    val anims: List<Pair<Int, Int>> = emptyList(),
    /** Peça capturada que "desvanece" na casa indicada. */
    val ghost: Pair<Int, Piece>? = null,
    val animTick: Int = 0,
    val whiteMs: Long = 0,
    val blackMs: Long = 0
) {
    val checkSquare: Int?
        get() = if (status is GameStatus.Check || status is GameStatus.Checkmate)
            ChessEngine.kingSquare(position.squares, position.turn).takeIf { it >= 0 } else null

    /** Peças que [side] capturou (ordenadas por valor). */
    fun capturedBy(side: Side): List<Piece> =
        records.mapNotNull { it.move.captured }.filter { it.side == side.opp }.sortedByDescending { it.type.value }
}

sealed interface GameEvent {
    data object Select : GameEvent
    data object Move : GameEvent
    data object Capture : GameEvent
    data object Check : GameEvent
    data object GameOver : GameEvent
}
