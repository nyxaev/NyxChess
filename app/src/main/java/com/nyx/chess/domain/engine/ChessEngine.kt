package com.nyx.chess.domain.engine

import com.nyx.chess.domain.model.*
import com.nyx.chess.domain.model.PieceType.*
import com.nyx.chess.domain.model.Side.*
import kotlin.math.abs

/** Motor de regras puro (sem Android): movimentos legais, xeque, mate, afogamento, roque, en passant, SAN. */
object ChessEngine {
    private val KNIGHT_D = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
    private val KING_D = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
    private val DIAG = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
    private val ORTHO = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    fun initial(): Position {
        val back = listOf(ROOK, KNIGHT, BISHOP, QUEEN, KING, BISHOP, KNIGHT, ROOK)
        val sq = MutableList<Piece?>(64) { null }
        for (c in 0..7) {
            sq[c] = Piece(back[c], BLACK); sq[8 + c] = Piece(PAWN, BLACK)
            sq[48 + c] = Piece(PAWN, WHITE); sq[56 + c] = Piece(back[c], WHITE)
        }
        return Position(sq, WHITE, Castling(), null, 0)
    }

    fun kingSquare(sq: List<Piece?>, side: Side): Int = sq.indexOfFirst { it?.type == KING && it.side == side }

    fun isAttacked(sq: List<Piece?>, target: Int, by: Side): Boolean {
        val r = target / 8
        val c = target % 8
        val pr = if (by == WHITE) r + 1 else r - 1
        for (dc in intArrayOf(-1, 1)) {
            val pc = c + dc
            if (pr in 0..7 && pc in 0..7) {
                val p = sq[pr * 8 + pc]
                if (p != null && p.side == by && p.type == PAWN) return true
            }
        }
        for ((dr, dc) in KNIGHT_D) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0..7 && nc in 0..7) {
                val p = sq[nr * 8 + nc]
                if (p != null && p.side == by && p.type == KNIGHT) return true
            }
        }
        for ((dr, dc) in KING_D) {
            val nr = r + dr; val nc = c + dc
            if (nr in 0..7 && nc in 0..7) {
                val p = sq[nr * 8 + nc]
                if (p != null && p.side == by && p.type == KING) return true
            }
        }
        for (dirs in listOf(DIAG to BISHOP, ORTHO to ROOK)) {
            for ((dr, dc) in dirs.first) {
                var nr = r + dr; var nc = c + dc
                while (nr in 0..7 && nc in 0..7) {
                    val p = sq[nr * 8 + nc]
                    if (p != null) {
                        if (p.side == by && (p.type == dirs.second || p.type == QUEEN)) return true
                        break
                    }
                    nr += dr; nc += dc
                }
            }
        }
        return false
    }

    private fun steps(sq: List<Piece?>, i: Int, me: Side, deltas: List<Pair<Int, Int>>, out: MutableList<Move>) {
        val r = i / 8; val c = i % 8
        for ((dr, dc) in deltas) {
            val nr = r + dr; val nc = c + dc
            if (nr !in 0..7 || nc !in 0..7) continue
            val t = nr * 8 + nc
            val tp = sq[t]
            if (tp == null) out.add(Move(i, t)) else if (tp.side != me) out.add(Move(i, t, captured = tp))
        }
    }

    private fun slide(sq: List<Piece?>, i: Int, me: Side, dirs: List<Pair<Int, Int>>, out: MutableList<Move>) {
        val r = i / 8; val c = i % 8
        for ((dr, dc) in dirs) {
            var nr = r + dr; var nc = c + dc
            while (nr in 0..7 && nc in 0..7) {
                val t = nr * 8 + nc
                val tp = sq[t]
                if (tp == null) out.add(Move(i, t)) else {
                    if (tp.side != me) out.add(Move(i, t, captured = tp))
                    break
                }
                nr += dr; nc += dc
            }
        }
    }

    private fun addPawn(out: MutableList<Move>, from: Int, to: Int, cap: Piece?, promo: Boolean) {
        if (promo) listOf(QUEEN, ROOK, BISHOP, KNIGHT).forEach { out.add(Move(from, to, it, cap)) }
        else out.add(Move(from, to, null, cap))
    }

    private fun pseudo(pos: Position): List<Move> {
        val out = ArrayList<Move>(48)
        val sq = pos.squares
        val me = pos.turn
        for (i in 0..63) {
            val p = sq[i] ?: continue
            if (p.side != me) continue
            val r = i / 8; val c = i % 8
            when (p.type) {
                PAWN -> {
                    val dir = if (me == WHITE) -1 else 1
                    val startRow = if (me == WHITE) 6 else 1
                    val promoRow = if (me == WHITE) 0 else 7
                    val r1 = r + dir
                    if (r1 in 0..7) {
                        if (sq[r1 * 8 + c] == null) {
                            addPawn(out, i, r1 * 8 + c, null, r1 == promoRow)
                            if (r == startRow && sq[(r + 2 * dir) * 8 + c] == null) out.add(Move(i, (r + 2 * dir) * 8 + c))
                        }
                        for (dc in intArrayOf(-1, 1)) {
                            val nc = c + dc
                            if (nc !in 0..7) continue
                            val t = r1 * 8 + nc
                            val tp = sq[t]
                            if (tp != null && tp.side != me) addPawn(out, i, t, tp, r1 == promoRow)
                            else if (tp == null && t == pos.enPassant)
                                out.add(Move(i, t, captured = Piece(PAWN, me.opp), isEnPassant = true))
                        }
                    }
                }
                KNIGHT -> steps(sq, i, me, KNIGHT_D, out)
                BISHOP -> slide(sq, i, me, DIAG, out)
                ROOK -> slide(sq, i, me, ORTHO, out)
                QUEEN -> slide(sq, i, me, DIAG + ORTHO, out)
                KING -> {
                    steps(sq, i, me, KING_D, out)
                    val row = if (me == WHITE) 7 else 0
                    val h = row * 8
                    if (i == h + 4 && !isAttacked(sq, i, me.opp)) {
                        val ks = if (me == WHITE) pos.castling.wk else pos.castling.bk
                        val qs = if (me == WHITE) pos.castling.wq else pos.castling.bq
                        val rook = Piece(ROOK, me)
                        if (ks && sq[h + 5] == null && sq[h + 6] == null && sq[h + 7] == rook &&
                            !isAttacked(sq, h + 5, me.opp) && !isAttacked(sq, h + 6, me.opp)
                        ) out.add(Move(i, h + 6, isCastle = true))
                        if (qs && sq[h + 3] == null && sq[h + 2] == null && sq[h + 1] == null && sq[h] == rook &&
                            !isAttacked(sq, h + 3, me.opp) && !isAttacked(sq, h + 2, me.opp)
                        ) out.add(Move(i, h + 2, isCastle = true))
                    }
                }
            }
        }
        return out
    }

    fun apply(pos: Position, m: Move): Position {
        val sq = pos.squares.toMutableList()
        val p = sq[m.from]!!
        sq[m.from] = null
        if (m.isEnPassant) sq[(m.from / 8) * 8 + m.to % 8] = null
        sq[m.to] = if (m.promotion != null) Piece(m.promotion, p.side) else p
        if (m.isCastle) {
            val row = m.from / 8
            if (m.to % 8 == 6) { sq[row * 8 + 5] = sq[row * 8 + 7]; sq[row * 8 + 7] = null }
            else { sq[row * 8 + 3] = sq[row * 8]; sq[row * 8] = null }
        }
        var c = pos.castling
        fun touch(s: Int) {
            c = when (s) {
                60 -> c.copy(wk = false, wq = false)
                4 -> c.copy(bk = false, bq = false)
                63 -> c.copy(wk = false)
                56 -> c.copy(wq = false)
                7 -> c.copy(bk = false)
                0 -> c.copy(bq = false)
                else -> c
            }
        }
        touch(m.from); touch(m.to)
        val ep = if (p.type == PAWN && abs(m.to - m.from) == 16) (m.from + m.to) / 2 else null
        val half = if (p.type == PAWN || m.captured != null) 0 else pos.halfmove + 1
        return Position(sq, pos.turn.opp, c, ep, half)
    }

    fun legalMoves(pos: Position): List<Move> = pseudo(pos).filter { m ->
        val n = apply(pos, m)
        val k = kingSquare(n.squares, pos.turn)
        k < 0 || !isAttacked(n.squares, k, pos.turn.opp)
    }

    fun inCheck(pos: Position): Boolean {
        val k = kingSquare(pos.squares, pos.turn)
        return k >= 0 && isAttacked(pos.squares, k, pos.turn.opp)
    }

    private fun insufficient(pos: Position): Boolean {
        val rest = pos.squares.filterNotNull().filter { it.type != KING }
        return rest.isEmpty() || (rest.size == 1 && (rest[0].type == BISHOP || rest[0].type == KNIGHT))
    }

    fun status(pos: Position): GameStatus {
        val legal = legalMoves(pos)
        val chk = inCheck(pos)
        return when {
            legal.isEmpty() && chk -> GameStatus.Checkmate(pos.turn.opp)
            legal.isEmpty() -> GameStatus.Stalemate
            pos.halfmove >= 100 -> GameStatus.Draw(DrawReason.FIFTY_MOVES)
            insufficient(pos) -> GameStatus.Draw(DrawReason.INSUFFICIENT_MATERIAL)
            chk -> GameStatus.Check
            else -> GameStatus.Ongoing
        }
    }

    private fun file(s: Int) = ('a' + s % 8).toString()
    fun name(s: Int) = file(s) + (8 - s / 8)

    /** Notação algébrica padrão (SAN) do lance [m] jogado em [pos]. */
    fun san(pos: Position, m: Move): String {
        val p = pos.squares[m.from]!!
        val base = when {
            m.isCastle -> if (m.to % 8 == 6) "O-O" else "O-O-O"
            p.type == PAWN -> (if (m.captured != null) file(m.from) + "x" else "") + name(m.to) +
                (m.promotion?.let { "=" + it.letter } ?: "")
            else -> {
                val others = legalMoves(pos).filter {
                    it.to == m.to && it.from != m.from && pos.squares[it.from]?.type == p.type
                }
                val dis = when {
                    others.isEmpty() -> ""
                    others.none { it.from % 8 == m.from % 8 } -> file(m.from)
                    others.none { it.from / 8 == m.from / 8 } -> (8 - m.from / 8).toString()
                    else -> name(m.from)
                }
                p.type.letter + dis + (if (m.captured != null) "x" else "") + name(m.to)
            }
        }
        return base + when (status(apply(pos, m))) {
            is GameStatus.Checkmate -> "#"
            GameStatus.Check -> "+"
            else -> ""
        }
    }
}
