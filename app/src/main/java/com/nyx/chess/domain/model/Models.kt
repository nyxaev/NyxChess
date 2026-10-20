package com.nyx.chess.domain.model

enum class Side {
    WHITE, BLACK;
    val opp: Side get() = if (this == WHITE) BLACK else WHITE
}

/** Glifos preenchidos (U+265A..F) + seletor de texto (FE0E) para nunca virarem emoji. */
enum class PieceType(val letter: String, val glyph: String, val value: Int) {
    KING("K", "\u265A\uFE0E", 0),
    QUEEN("Q", "\u265B\uFE0E", 9),
    ROOK("R", "\u265C\uFE0E", 5),
    BISHOP("B", "\u265D\uFE0E", 3),
    KNIGHT("N", "\u265E\uFE0E", 3),
    PAWN("", "\u265F\uFE0E", 1)
}

data class Piece(val type: PieceType, val side: Side)

data class Castling(val wk: Boolean = true, val wq: Boolean = true, val bk: Boolean = true, val bq: Boolean = true)

/** Casas indexadas de 0..63: linha*8+coluna, linha 0 = rank 8 (topo), coluna 0 = coluna a. */
data class Move(
    val from: Int,
    val to: Int,
    val promotion: PieceType? = null,
    val captured: Piece? = null,
    val isEnPassant: Boolean = false,
    val isCastle: Boolean = false
)

data class Position(
    val squares: List<Piece?>,
    val turn: Side,
    val castling: Castling,
    val enPassant: Int?,
    val halfmove: Int
)

enum class DrawReason { FIFTY_MOVES, INSUFFICIENT_MATERIAL }

sealed interface GameStatus {
    data object Ongoing : GameStatus
    data object Check : GameStatus
    data class Checkmate(val winner: Side) : GameStatus
    data object Stalemate : GameStatus
    data class Draw(val reason: DrawReason) : GameStatus
    data class Timeout(val winner: Side) : GameStatus

    val isOver: Boolean
        get() = this is Checkmate || this is Stalemate || this is Draw || this is Timeout
}
