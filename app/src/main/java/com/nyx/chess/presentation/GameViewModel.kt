package com.nyx.chess.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyx.chess.domain.engine.ChessEngine
import com.nyx.chess.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val undoStack = ArrayDeque<GameUiState>()
    private var clockJob: Job? = null

    // ---------- Menu / ciclo de partida ----------
    fun updateSettings(f: (GameSettings) -> GameSettings) = _state.update { it.copy(settings = f(it.settings)) }

    fun startGame() {
        undoStack.clear()
        val s = _state.value.settings
        _state.value = GameUiState(inGame = true, settings = s, whiteMs = s.timeControl.baseMs, blackMs = s.timeControl.baseMs)
        startClock()
    }

    fun restart() = startGame()

    fun backToMenu() {
        clockJob?.cancel()
        _state.update { it.copy(inGame = false) }
    }

    // ---------- Interação ----------
    fun onSquareClick(sq: Int) {
        val s = _state.value
        if (s.status.isOver || s.pendingPromotion != null) return
        val piece = s.position.squares[sq]
        when {
            s.selected != null && sq in s.targets -> commitMove(s.selected, sq, animate = true)
            piece?.side == s.position.turn && s.selected != sq -> {
                val targets = ChessEngine.legalMoves(s.position).filter { it.from == sq }.map { it.to }.toSet()
                _state.value = s.copy(selected = sq, targets = targets)
                _events.tryEmit(GameEvent.Select)
            }
            else -> _state.value = s.copy(selected = null, targets = emptySet())
        }
    }

    /** Chamado ao soltar uma peça arrastada (já "encaixada" visualmente, sem reanimar). */
    fun onDrop(from: Int, to: Int) = commitMove(from, to, animate = false)

    private fun commitMove(from: Int, to: Int, animate: Boolean) {
        val s = _state.value
        val candidates = ChessEngine.legalMoves(s.position).filter { it.from == from && it.to == to }
        when {
            candidates.isEmpty() -> return
            candidates.size > 1 -> _state.value = s.copy(pendingPromotion = from to to)
            else -> execute(candidates.first(), animate)
        }
    }

    fun choosePromotion(type: PieceType) {
        val s = _state.value
        val (f, t) = s.pendingPromotion ?: return
        val m = ChessEngine.legalMoves(s.position).first { it.from == f && it.to == t && it.promotion == type }
        execute(m, animate = true)
    }

    fun cancelPromotion() = _state.update { it.copy(pendingPromotion = null, selected = null, targets = emptySet()) }

    private fun execute(m: Move, animate: Boolean) {
        val s = _state.value
        undoStack.addLast(s)
        val san = ChessEngine.san(s.position, m)
        val newPos = ChessEngine.apply(s.position, m)
        val status = ChessEngine.status(newPos)
        val inc = if (s.settings.timeControl.enabled) s.settings.timeControl.incMs else 0L
        val mover = s.position.turn
        val anims = buildList<Pair<Int, Int>> {
            if (animate) {
                add(m.from to m.to)
                if (m.isCastle) {
                    val row = m.from / 8
                    if (m.to % 8 == 6) add((row * 8 + 7) to (row * 8 + 5)) else add((row * 8) to (row * 8 + 3))
                }
            }
        }
        val ghostSq = if (m.isEnPassant) (m.from / 8) * 8 + m.to % 8 else m.to
        _state.value = s.copy(
            position = newPos,
            records = s.records + MoveRecord(m, san, mover),
            selected = null, targets = emptySet(), pendingPromotion = null,
            lastMove = m.from to m.to,
            status = status,
            anims = anims,
            ghost = m.captured?.let { ghostSq to it },
            animTick = s.animTick + 1,
            whiteMs = if (mover == Side.WHITE) s.whiteMs + inc else s.whiteMs,
            blackMs = if (mover == Side.BLACK) s.blackMs + inc else s.blackMs
        )
        _events.tryEmit(
            when {
                status.isOver -> GameEvent.GameOver
                status is GameStatus.Check -> GameEvent.Check
                m.captured != null -> GameEvent.Capture
                else -> GameEvent.Move
            }
        )
    }

    fun undo() {
        val cur = _state.value
        val prev = undoStack.removeLastOrNull() ?: return
        val keepPrevClock = cur.status is GameStatus.Timeout
        _state.value = prev.copy(
            settings = cur.settings, anims = emptyList(), ghost = null,
            selected = null, targets = emptySet(), pendingPromotion = null,
            whiteMs = if (keepPrevClock) prev.whiteMs else cur.whiteMs,
            blackMs = if (keepPrevClock) prev.blackMs else cur.blackMs
        )
        _events.tryEmit(GameEvent.Move)
    }

    // ---------- Relógio ----------
    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(100)
                val now = SystemClock.elapsedRealtime()
                val dt = now - last
                last = now
                val s = _state.value
                if (!s.inGame || s.status.isOver || !s.settings.timeControl.enabled || s.pendingPromotion != null) continue
                val white = s.position.turn == Side.WHITE
                val left = (if (white) s.whiteMs else s.blackMs) - dt
                if (left <= 0) {
                    val z = if (white) s.copy(whiteMs = 0) else s.copy(blackMs = 0)
                    _state.value = z.copy(status = GameStatus.Timeout(s.position.turn.opp), selected = null, targets = emptySet())
                    _events.tryEmit(GameEvent.GameOver)
                } else {
                    _state.value = if (white) s.copy(whiteMs = left) else s.copy(blackMs = left)
                }
            }
        }
    }

    override fun onCleared() { clockJob?.cancel() }
}
