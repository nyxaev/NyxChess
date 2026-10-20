package com.nyx.chess

import com.nyx.chess.domain.engine.ChessEngine
import com.nyx.chess.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ChessEngineTest {
    private fun sq(n: String) = (8 - (n[1] - '0')) * 8 + (n[0] - 'a')
    private fun perft(p: Position, d: Int): Int =
        if (d == 0) 1 else ChessEngine.legalMoves(p).sumOf { perft(ChessEngine.apply(p, it), d - 1) }

    private fun play(p: Position, vararg mv: String): Position {
        var pos = p
        for (m in mv) {
            val move = ChessEngine.legalMoves(pos).first { it.from == sq(m.substring(0, 2)) && it.to == sq(m.substring(2, 4)) }
            pos = ChessEngine.apply(pos, move)
        }
        return pos
    }

    @Test fun perftFromStart() {
        val p = ChessEngine.initial()
        assertEquals(20, perft(p, 1)); assertEquals(400, perft(p, 2)); assertEquals(8902, perft(p, 3))
    }

    @Test fun foolsMate() {
        val p = play(ChessEngine.initial(), "f2f3", "e7e5", "g2g4", "d8h4")
        assertEquals(GameStatus.Checkmate(Side.BLACK), ChessEngine.status(p))
    }

    @Test fun enPassantAndCastling() {
        val p = play(ChessEngine.initial(), "e2e4", "a7a6", "e4e5", "d7d5")
        assertTrue(ChessEngine.legalMoves(p).any { it.isEnPassant })
        val c = play(ChessEngine.initial(), "e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6")
        assertTrue(ChessEngine.legalMoves(c).any { it.isCastle })
    }
}
