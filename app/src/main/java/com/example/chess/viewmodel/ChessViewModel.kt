package com.example.chess.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.audio.SoundManager
import com.example.chess.engine.Board
import com.example.chess.engine.ChessEngine
import com.example.chess.model.CastleRights
import com.example.chess.model.ChessPieces
import com.example.chess.model.GameMode
import com.example.chess.model.PlayerColor
import com.example.chess.model.Position
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class ChessUiState(
    val board: Board = ChessEngine.createInitialBoard(),
    val turn: PlayerColor = PlayerColor.WHITE,
    val gameMode: GameMode = GameMode.VS_AI,
    val selectedSquare: Position? = null,
    val validMovesForSelected: List<Position> = emptyList(),
    val capturedByWhite: List<Char> = emptyList(),
    val capturedByBlack: List<Char> = emptyList(),
    val whiteTimeLeft: Int = 300,
    val blackTimeLeft: Int = 300,
    val isGameOver: Boolean = false,
    val isWhiteCheck: Boolean = false,
    val isBlackCheck: Boolean = false,
    val moveTicker: String = "🎮 Match Started — White's Turn",
    val showWinnerModal: Boolean = false,
    val modalTitle: String = "",
    val modalDesc: String = "",
    val showPromotionModal: Boolean = false,
    val isMusicPlaying: Boolean = false,
    val confirmDialogType: ConfirmDialogType? = null
)

enum class ConfirmDialogType {
    DRAW, RESIGN
}

private data class HistorySnapshot(
    val board: Board,
    val turn: PlayerColor,
    val castleRights: CastleRights,
    val enPassantTarget: Position?,
    val capturedByWhite: List<Char>,
    val capturedByBlack: List<Char>,
    val ticker: String
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    val soundManager = SoundManager(application.applicationContext)

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    private var castleRights = CastleRights()
    private var enPassantTarget: Position? = null
    private val history = mutableListOf<HistorySnapshot>()

    private var timerJob: Job? = null
    private var aiJob: Job? = null
    private var pendingPromotionMove: Pair<Position, Position>? = null

    init {
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (!state.isGameOver) {
                    if (state.turn == PlayerColor.WHITE && state.whiteTimeLeft > 0) {
                        val newTime = state.whiteTimeLeft - 1
                        _uiState.update { it.copy(whiteTimeLeft = newTime) }
                        if (newTime == 0) {
                            endGame("TIME OUT!", "Black Wins on Time! ⏱️")
                        }
                    } else if (state.turn == PlayerColor.BLACK && state.blackTimeLeft > 0) {
                        val newTime = state.blackTimeLeft - 1
                        _uiState.update { it.copy(blackTimeLeft = newTime) }
                        if (newTime == 0) {
                            endGame("TIME OUT!", "White Wins on Time! ⏱️")
                        }
                    }
                }
            }
        }
    }

    fun onSquareClicked(r: Int, c: Int) {
        val state = _uiState.value
        if (state.isGameOver) return
        if (state.gameMode == GameMode.VS_AI && state.turn == PlayerColor.BLACK) return

        val clickedPos = Position(r, c)
        val selected = state.selectedSquare

        if (selected != null) {
            val isTarget = state.validMovesForSelected.contains(clickedPos)
            if (isTarget) {
                attemptMove(selected, clickedPos)
                return
            }
        }

        // Select own piece
        val piece = state.board[r][c]
        if (piece != ChessPieces.EMPTY && ChessPieces.colorOf(piece) == state.turn) {
            val validMoves = ChessEngine.getStrictLegalMoves(clickedPos, state.board, castleRights, enPassantTarget)
                .map { it.to }
            _uiState.update {
                it.copy(
                    selectedSquare = clickedPos,
                    validMovesForSelected = validMoves
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedSquare = null,
                    validMovesForSelected = emptyList()
                )
            }
        }
    }

    private fun attemptMove(from: Position, to: Position) {
        val board = _uiState.value.board
        val piece = board[from.row][from.col]
        val isWhitePawn = piece == 'P' && to.row == 0
        val isBlackPawn = piece == 'p' && to.row == 7

        if (isWhitePawn || (isBlackPawn && _uiState.value.gameMode == GameMode.PVP)) {
            pendingPromotionMove = from to to
            _uiState.update {
                it.copy(
                    selectedSquare = null,
                    validMovesForSelected = emptyList(),
                    showPromotionModal = true
                )
            }
        } else if (isBlackPawn) {
            executeMove(from, to, 'q')
        } else {
            executeMove(from, to, null)
        }
    }

    fun onPromotionSelected(chosenPiece: Char) {
        val move = pendingPromotionMove ?: return
        pendingPromotionMove = null
        _uiState.update { it.copy(showPromotionModal = false) }
        executeMove(move.first, move.second, chosenPiece)
    }

    private fun executeMove(from: Position, to: Position, promotionPiece: Char?) {
        val state = _uiState.value
        val board = ChessEngine.cloneBoard(state.board)
        val piece = board[from.row][from.col]
        val targetPiece = board[to.row][to.col]

        // Save history snapshot
        history.add(
            HistorySnapshot(
                board = ChessEngine.cloneBoard(state.board),
                turn = state.turn,
                castleRights = castleRights.copy(),
                enPassantTarget = enPassantTarget,
                capturedByWhite = state.capturedByWhite.toList(),
                capturedByBlack = state.capturedByBlack.toList(),
                ticker = state.moveTicker
            )
        )

        var newCapturedWhite = state.capturedByWhite.toMutableList()
        var newCapturedBlack = state.capturedByBlack.toMutableList()
        var isCapture = false

        if (targetPiece != ChessPieces.EMPTY) {
            if (state.turn == PlayerColor.WHITE) newCapturedWhite.add(targetPiece)
            else newCapturedBlack.add(targetPiece)
            isCapture = true
        }

        // Handle Castling King movement
        if (piece.lowercaseChar() == 'k' && abs(from.col - to.col) == 2) {
            if (to.col == 6) {
                // King-side
                board[to.row][5] = board[to.row][7]
                board[to.row][7] = ChessPieces.EMPTY
            } else if (to.col == 2) {
                // Queen-side
                board[to.row][3] = board[to.row][0]
                board[to.row][0] = ChessPieces.EMPTY
            }
        }

        // Handle En Passant
        if (piece.lowercaseChar() == 'p' && enPassantTarget != null && to == enPassantTarget) {
            val capRow = if (piece == 'P') to.row + 1 else to.row - 1
            val epPiece = board[capRow][to.col]
            if (state.turn == PlayerColor.WHITE) newCapturedWhite.add(epPiece)
            else newCapturedBlack.add(epPiece)
            board[capRow][to.col] = ChessPieces.EMPTY
            isCapture = true
        }

        // Update en passant target for next turn
        enPassantTarget = if (piece.lowercaseChar() == 'p' && abs(from.row - to.row) == 2) {
            Position((from.row + to.row) / 2, from.col)
        } else {
            null
        }

        // Move piece
        board[to.row][to.col] = promotionPiece ?: piece
        board[from.row][from.col] = ChessPieces.EMPTY

        // Play sound
        if (isCapture) soundManager.playCaptureSound()
        else soundManager.playMoveSound()

        // Update Castle Rights
        if (piece == 'K') {
            castleRights.whiteKingSide = false
            castleRights.whiteQueenSide = false
        }
        if (piece == 'k') {
            castleRights.blackKingSide = false
            castleRights.blackQueenSide = false
        }
        if (from.row == 7 && from.col == 7) castleRights.whiteKingSide = false
        if (from.row == 7 && from.col == 0) castleRights.whiteQueenSide = false
        if (from.row == 0 && from.col == 7) castleRights.blackKingSide = false
        if (from.row == 0 && from.col == 0) castleRights.blackQueenSide = false

        // Last move ticker text
        val files = arrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h')
        val pieceSymbol = ChessPieces.symbols[piece] ?: "$piece"
        val ticker = "⚡ Last Move: $pieceSymbol ${files[from.col]}${8 - from.row} ➔ ${files[to.col]}${8 - to.row}"

        val nextTurn = state.turn.opposite()
        val whiteInCheck = ChessEngine.isKingInCheck(PlayerColor.WHITE, board)
        val blackInCheck = ChessEngine.isKingInCheck(PlayerColor.BLACK, board)

        _uiState.update {
            it.copy(
                board = board,
                turn = nextTurn,
                selectedSquare = null,
                validMovesForSelected = emptyList(),
                capturedByWhite = newCapturedWhite,
                capturedByBlack = newCapturedBlack,
                moveTicker = ticker,
                isWhiteCheck = whiteInCheck,
                isBlackCheck = blackInCheck
            )
        }

        // Check for Game Over (Checkmate or Stalemate)
        val hasMoves = ChessEngine.hasAnyValidMoves(nextTurn, board, castleRights, enPassantTarget)
        if (!hasMoves) {
            val isCheck = if (nextTurn == PlayerColor.WHITE) whiteInCheck else blackInCheck
            if (isCheck) {
                val winnerName = if (nextTurn == PlayerColor.WHITE) {
                    if (state.gameMode == GameMode.VS_AI) "Ninja Pro AI Won!" else "Black Won The Match!"
                } else {
                    "MadXNinja Won The Match!"
                }
                endGame("CHECKMATE!", winnerName)
            } else {
                endGame("STALEMATE!", "The Game Ended In A Draw 🤝")
            }
            return
        }

        // Trigger AI Move if needed
        if (state.gameMode == GameMode.VS_AI && nextTurn == PlayerColor.BLACK) {
            triggerAIMove()
        }
    }

    private fun triggerAIMove() {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            delay(300)
            if (_uiState.value.isGameOver) return@launch
            val bestMove = ChessEngine.findBestMoveForBlack(
                _uiState.value.board,
                castleRights,
                enPassantTarget
            )
            if (bestMove != null) {
                val piece = _uiState.value.board[bestMove.from.row][bestMove.from.col]
                val isPawnPromotion = piece == 'p' && bestMove.to.row == 7
                executeMove(bestMove.from, bestMove.to, if (isPawnPromotion) 'q' else null)
            }
        }
    }

    private fun endGame(title: String, desc: String) {
        _uiState.update {
            it.copy(
                isGameOver = true,
                showWinnerModal = true,
                modalTitle = title,
                modalDesc = desc
            )
        }
    }

    fun undoMove() {
        if (_uiState.value.isGameOver) return
        if (history.isNotEmpty()) {
            val snapshot = history.removeAt(history.lastIndex)
            castleRights = snapshot.castleRights
            enPassantTarget = snapshot.enPassantTarget
            val whiteInCheck = ChessEngine.isKingInCheck(PlayerColor.WHITE, snapshot.board)
            val blackInCheck = ChessEngine.isKingInCheck(PlayerColor.BLACK, snapshot.board)

            _uiState.update {
                it.copy(
                    board = snapshot.board,
                    turn = snapshot.turn,
                    selectedSquare = null,
                    validMovesForSelected = emptyList(),
                    capturedByWhite = snapshot.capturedByWhite,
                    capturedByBlack = snapshot.capturedByBlack,
                    moveTicker = snapshot.ticker,
                    isWhiteCheck = whiteInCheck,
                    isBlackCheck = blackInCheck
                )
            }
        }
    }

    fun requestDraw() {
        if (_uiState.value.isGameOver) return
        _uiState.update { it.copy(confirmDialogType = ConfirmDialogType.DRAW) }
    }

    fun requestResign() {
        if (_uiState.value.isGameOver) return
        _uiState.update { it.copy(confirmDialogType = ConfirmDialogType.RESIGN) }
    }

    fun dismissConfirmDialog() {
        _uiState.update { it.copy(confirmDialogType = null) }
    }

    fun confirmDraw() {
        _uiState.update { it.copy(confirmDialogType = null) }
        endGame("DRAW OFFERED", "Match Drawn By Mutual Agreement 🤝")
    }

    fun confirmResign() {
        _uiState.update { it.copy(confirmDialogType = null) }
        endGame("GAME OVER", "You Resigned. Ninja AI Wins! 🏆")
    }

    fun resetGame() {
        aiJob?.cancel()
        pendingPromotionMove = null
        history.clear()
        castleRights = CastleRights()
        enPassantTarget = null

        _uiState.update {
            ChessUiState(
                gameMode = it.gameMode,
                isMusicPlaying = it.isMusicPlaying
            )
        }
    }

    fun setGameMode(mode: GameMode) {
        if (_uiState.value.gameMode != mode) {
            _uiState.update { it.copy(gameMode = mode) }
            resetGame()
        }
    }

    fun toggleMusic() {
        val playing = soundManager.toggleRyukMusic()
        _uiState.update { it.copy(isMusicPlaying = playing) }
    }

    fun dismissWinnerModal() {
        _uiState.update { it.copy(showWinnerModal = false) }
        resetGame()
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
