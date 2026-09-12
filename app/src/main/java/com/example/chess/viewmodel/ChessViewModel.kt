package com.example.chess.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.audio.SoundManager
import com.example.chess.engine.Board
import com.example.chess.engine.ChessEngine
import com.example.chess.model.AIDifficulty
import com.example.chess.model.BoardTheme
import com.example.chess.model.CastleRights
import com.example.chess.model.ChessPieces
import com.example.chess.model.GameMode
import com.example.chess.model.Move
import com.example.chess.model.MoveLogItem
import com.example.chess.model.PlayerColor
import com.example.chess.model.Position
import com.example.chess.model.TimeControl
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
    val aiDifficulty: AIDifficulty = AIDifficulty.ADEPT,
    val timeControl: TimeControl = TimeControl.RAPID,
    val boardTheme: BoardTheme = BoardTheme.CYBER_NEON,
    val isBoardFlipped: Boolean = false,
    val playerColorVsAi: PlayerColor = PlayerColor.WHITE,
    val selectedSquare: Position? = null,
    val validMovesForSelected: List<Position> = emptyList(),
    val hintMove: Move? = null,
    val capturedByWhite: List<Char> = emptyList(),
    val capturedByBlack: List<Char> = emptyList(),
    val whiteMaterial: Int = 39,
    val blackMaterial: Int = 39,
    val whiteTimeLeft: Int = 300,
    val blackTimeLeft: Int = 300,
    val isGameOver: Boolean = false,
    val isWhiteCheck: Boolean = false,
    val isBlackCheck: Boolean = false,
    val moveTicker: String = "🎮 Match Started — White's Turn",
    val moveHistory: List<MoveLogItem> = emptyList(),
    val showWinnerModal: Boolean = false,
    val modalTitle: String = "",
    val modalDesc: String = "",
    val showPromotionModal: Boolean = false,
    val isMusicPlaying: Boolean = false,
    val isSoundFxEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val confirmDialogType: ConfirmDialogType? = null,
    val showHistoryModal: Boolean = false,
    val showSettingsModal: Boolean = false,
    val showStatsModal: Boolean = false,
    val aiWins: Int = 0,
    val aiLosses: Int = 0,
    val totalDraws: Int = 0,
    val totalGames: Int = 0,
    val fenString: String = "",
    val isAiThinking: Boolean = false
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
    val ticker: String,
    val moveHistory: List<MoveLogItem>,
    val halfMoveClock: Int
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    val soundManager = SoundManager(application.applicationContext)
    private val prefs = application.getSharedPreferences("ninja_chess_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    private var castleRights = CastleRights()
    private var enPassantTarget: Position? = null
    private var halfMoveClock: Int = 0
    private val positionCounts = mutableMapOf<String, Int>()
    private val history = mutableListOf<HistorySnapshot>()

    private var timerJob: Job? = null
    private var aiJob: Job? = null
    private var pendingPromotionMove: Pair<Position, Position>? = null

    init {
        loadPreferences()
        startTimer()
    }

    private fun loadPreferences() {
        val themeOrdinal = prefs.getInt("theme", BoardTheme.CYBER_NEON.ordinal)
        val theme = BoardTheme.entries.getOrElse(themeOrdinal) { BoardTheme.CYBER_NEON }
        val diffOrdinal = prefs.getInt("diff", AIDifficulty.ADEPT.ordinal)
        val diff = AIDifficulty.entries.getOrElse(diffOrdinal) { AIDifficulty.ADEPT }
        val timeOrdinal = prefs.getInt("time_ctrl", TimeControl.RAPID.ordinal)
        val tc = TimeControl.entries.getOrElse(timeOrdinal) { TimeControl.RAPID }
        val soundFx = prefs.getBoolean("sound_fx", true)
        val haptics = prefs.getBoolean("haptics", true)

        soundManager.isSoundFxEnabled = soundFx
        soundManager.isHapticsEnabled = haptics

        val wins = prefs.getInt("stats_wins", 0)
        val losses = prefs.getInt("stats_losses", 0)
        val draws = prefs.getInt("stats_draws", 0)
        val total = prefs.getInt("stats_total", 0)

        val initialFen = ChessEngine.generateFen(
            _uiState.value.board,
            PlayerColor.WHITE,
            castleRights,
            enPassantTarget
        )

        _uiState.update {
            it.copy(
                boardTheme = theme,
                aiDifficulty = diff,
                timeControl = tc,
                whiteTimeLeft = tc.seconds,
                blackTimeLeft = tc.seconds,
                isSoundFxEnabled = soundFx,
                isHapticsEnabled = haptics,
                aiWins = wins,
                aiLosses = losses,
                totalDraws = draws,
                totalGames = total,
                fenString = initialFen
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (!state.isGameOver && state.timeControl != TimeControl.UNLIMITED) {
                    if (state.turn == PlayerColor.WHITE && state.whiteTimeLeft > 0) {
                        val newTime = state.whiteTimeLeft - 1
                        _uiState.update { it.copy(whiteTimeLeft = newTime) }
                        if (newTime == 0) {
                            handleGameOver(isWhiteWin = false, isDraw = false, "TIME OUT!", "Black Wins on Time! ⏱️")
                        }
                    } else if (state.turn == PlayerColor.BLACK && state.blackTimeLeft > 0) {
                        val newTime = state.blackTimeLeft - 1
                        _uiState.update { it.copy(blackTimeLeft = newTime) }
                        if (newTime == 0) {
                            handleGameOver(isWhiteWin = true, isDraw = false, "TIME OUT!", "White Wins on Time! ⏱️")
                        }
                    }
                }
            }
        }
    }

    fun onSquareClicked(r: Int, c: Int) {
        val state = _uiState.value
        if (state.isGameOver || state.isAiThinking) return

        // If playing vs AI and it's AI turn, reject input
        if (state.gameMode == GameMode.VS_AI) {
            val aiColor = state.playerColorVsAi.opposite()
            if (state.turn == aiColor) return
        }

        val clickedPos = Position(r, c)
        val selected = state.selectedSquare

        if (selected != null) {
            val isTarget = state.validMovesForSelected.contains(clickedPos)
            if (isTarget) {
                attemptMove(selected, clickedPos)
                return
            }
        }

        // Clear hint if any
        if (state.hintMove != null) {
            _uiState.update { it.copy(hintMove = null) }
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

        val isHumanTurn = if (_uiState.value.gameMode == GameMode.VS_AI) {
            _uiState.value.turn == _uiState.value.playerColorVsAi
        } else {
            true
        }

        if ((isWhitePawn && isHumanTurn) || (isBlackPawn && isHumanTurn)) {
            pendingPromotionMove = from to to
            _uiState.update {
                it.copy(
                    selectedSquare = null,
                    validMovesForSelected = emptyList(),
                    hintMove = null,
                    showPromotionModal = true
                )
            }
        } else if (isBlackPawn || isWhitePawn) {
            executeMove(from, to, if (piece == 'P') 'Q' else 'q')
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
                ticker = state.moveTicker,
                moveHistory = state.moveHistory.toList(),
                halfMoveClock = halfMoveClock
            )
        )

        val newCapturedWhite = state.capturedByWhite.toMutableList()
        val newCapturedBlack = state.capturedByBlack.toMutableList()
        var isCapture = false

        if (targetPiece != ChessPieces.EMPTY) {
            if (state.turn == PlayerColor.WHITE) newCapturedWhite.add(targetPiece)
            else newCapturedBlack.add(targetPiece)
            isCapture = true
        }

        // Update 50-move rule counter
        if (piece.lowercaseChar() == 'p' || isCapture) {
            halfMoveClock = 0
        } else {
            halfMoveClock++
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

        // Update Castle Rights when king or rook moves
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

        // Corner rook capture also revokes castling rights
        if (to.row == 7 && to.col == 7) castleRights.whiteKingSide = false
        if (to.row == 7 && to.col == 0) castleRights.whiteQueenSide = false
        if (to.row == 0 && to.col == 7) castleRights.blackKingSide = false
        if (to.row == 0 && to.col == 0) castleRights.blackQueenSide = false

        val nextTurn = state.turn.opposite()
        val whiteInCheck = ChessEngine.isKingInCheck(PlayerColor.WHITE, board)
        val blackInCheck = ChessEngine.isKingInCheck(PlayerColor.BLACK, board)
        val isOpponentInCheck = if (nextTurn == PlayerColor.WHITE) whiteInCheck else blackInCheck

        val hasMoves = ChessEngine.hasAnyValidMoves(nextTurn, board, castleRights, enPassantTarget)
        val isOpponentCheckmated = !hasMoves && isOpponentInCheck
        val isOpponentStalemated = !hasMoves && !isOpponentInCheck

        // Play sound & vibration
        if (isOpponentInCheck) {
            soundManager.playCheckSound()
            soundManager.vibrateCheck()
        } else if (isCapture) {
            soundManager.playCaptureSound()
            soundManager.vibrateCapture()
        } else {
            soundManager.playMoveSound()
            soundManager.vibrateMove()
        }

        // Generate SAN notation
        val san = ChessEngine.generateSan(
            from = from,
            to = to,
            piece = piece,
            isCapture = isCapture,
            isCheck = isOpponentInCheck,
            isCheckmate = isOpponentCheckmated,
            promotionPiece = promotionPiece
        )

        // Update Move History
        val currentHistory = state.moveHistory.toMutableList()
        if (state.turn == PlayerColor.WHITE) {
            val moveNum = currentHistory.size + 1
            currentHistory.add(MoveLogItem(moveNumber = moveNum, whiteMove = san))
        } else {
            if (currentHistory.isNotEmpty()) {
                val lastItem = currentHistory.last()
                currentHistory[currentHistory.lastIndex] = lastItem.copy(blackMove = san)
            } else {
                currentHistory.add(MoveLogItem(moveNumber = 1, whiteMove = "...", blackMove = san))
            }
        }

        val files = arrayOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h')
        val pieceSymbol = ChessPieces.symbols[piece] ?: "$piece"
        val ticker = "⚡ Last: $pieceSymbol ${files[from.col]}${8 - from.row}➔${files[to.col]}${8 - to.row} ($san)"

        val (whiteMat, blackMat) = ChessEngine.calculateMaterialScores(board)
        val newFen = ChessEngine.generateFen(
            board = board,
            turn = nextTurn,
            castleRights = castleRights,
            enPassantTarget = enPassantTarget,
            fullMoves = (currentHistory.size + 1)
        )

        _uiState.update {
            it.copy(
                board = board,
                turn = nextTurn,
                selectedSquare = null,
                validMovesForSelected = emptyList(),
                hintMove = null,
                capturedByWhite = newCapturedWhite,
                capturedByBlack = newCapturedBlack,
                whiteMaterial = whiteMat,
                blackMaterial = blackMat,
                moveTicker = ticker,
                moveHistory = currentHistory,
                isWhiteCheck = whiteInCheck,
                isBlackCheck = blackInCheck,
                fenString = newFen,
                isAiThinking = false
            )
        }

        // Check for Game Over: Checkmate or Stalemate
        if (!hasMoves) {
            if (isOpponentInCheck) {
                val isWhiteWinner = state.turn == PlayerColor.WHITE
                val winnerDesc = if (state.gameMode == GameMode.VS_AI) {
                    val userWon = (isWhiteWinner && state.playerColorVsAi == PlayerColor.WHITE) ||
                            (!isWhiteWinner && state.playerColorVsAi == PlayerColor.BLACK)
                    if (userWon) "You defeated the Ninja AI! 👑" else "Ninja AI emerged victorious! 🥷"
                } else {
                    if (isWhiteWinner) "White Won The Match! 🏆" else "Black Won The Match! 🏆"
                }
                handleGameOver(isWhiteWin = isWhiteWinner, isDraw = false, "CHECKMATE!", winnerDesc)
            } else {
                handleGameOver(isWhiteWin = false, isDraw = true, "STALEMATE!", "The Game Ended In A Draw 🤝")
            }
            return
        }

        // Check for Draw by Insufficient Material
        if (ChessEngine.isDrawByInsufficientMaterial(board)) {
            handleGameOver(isWhiteWin = false, isDraw = true, "DRAW!", "Draw by Insufficient Material 🤝")
            return
        }

        // Check for Draw by Threefold Repetition
        val posKey = ChessEngine.getPositionKey(board, nextTurn, castleRights, enPassantTarget)
        val repCount = (positionCounts[posKey] ?: 0) + 1
        positionCounts[posKey] = repCount
        if (repCount >= 3) {
            handleGameOver(isWhiteWin = false, isDraw = true, "DRAW!", "Draw by Threefold Repetition 🤝")
            return
        }

        // Check for Draw by 50-Move Rule (100 half-moves without capture or pawn advance)
        if (halfMoveClock >= 100) {
            handleGameOver(isWhiteWin = false, isDraw = true, "DRAW!", "Draw by 50-Move Rule 🤝")
            return
        }

        // Trigger AI Move if needed
        if (state.gameMode == GameMode.VS_AI) {
            val aiColor = state.playerColorVsAi.opposite()
            if (nextTurn == aiColor) {
                triggerAIMove(aiColor)
            }
        }
    }

    private fun triggerAIMove(aiColor: PlayerColor) {
        aiJob?.cancel()
        _uiState.update { it.copy(isAiThinking = true) }
        aiJob = viewModelScope.launch {
            delay(400)
            if (_uiState.value.isGameOver) {
                _uiState.update { it.copy(isAiThinking = false) }
                return@launch
            }
            val diff = _uiState.value.aiDifficulty
            val bestMove = ChessEngine.findBestMove(
                color = aiColor,
                board = _uiState.value.board,
                castleRights = castleRights,
                enPassantTarget = enPassantTarget,
                depth = diff.depth,
                isNoviceRandom = diff == AIDifficulty.NOVICE
            )
            _uiState.update { it.copy(isAiThinking = false) }
            if (bestMove != null) {
                val piece = _uiState.value.board[bestMove.from.row][bestMove.from.col]
                val isPawnPromotion = (piece == 'p' && bestMove.to.row == 7) || (piece == 'P' && bestMove.to.row == 0)
                val promoPiece = if (isPawnPromotion) (if (piece == 'P') 'Q' else 'q') else null
                executeMove(bestMove.from, bestMove.to, promoPiece)
            }
        }
    }

    fun requestHint() {
        val state = _uiState.value
        if (state.isGameOver || state.isAiThinking) return
        val best = ChessEngine.findBestMove(
            color = state.turn,
            board = state.board,
            castleRights = castleRights,
            enPassantTarget = enPassantTarget,
            depth = 2
        )
        if (best != null) {
            _uiState.update {
                it.copy(
                    hintMove = best,
                    moveTicker = "💡 Sensei Hint: ${best.from.toAlgebraic()} ➔ ${best.to.toAlgebraic()}"
                )
            }
        }
    }

    fun flipBoard() {
        _uiState.update { it.copy(isBoardFlipped = !it.isBoardFlipped) }
    }

    private fun handleGameOver(isWhiteWin: Boolean, isDraw: Boolean, title: String, desc: String) {
        val state = _uiState.value
        var newWins = state.aiWins
        var newLosses = state.aiLosses
        var newDraws = state.totalDraws
        val newTotal = state.totalGames + 1

        if (state.gameMode == GameMode.VS_AI) {
            if (isDraw) {
                newDraws++
            } else {
                val playerWon = (isWhiteWin && state.playerColorVsAi == PlayerColor.WHITE) ||
                        (!isWhiteWin && state.playerColorVsAi == PlayerColor.BLACK)
                if (playerWon) newWins++ else newLosses++
            }
        } else {
            if (isDraw) newDraws++
        }

        prefs.edit()
            .putInt("stats_wins", newWins)
            .putInt("stats_losses", newLosses)
            .putInt("stats_draws", newDraws)
            .putInt("stats_total", newTotal)
            .apply()

        _uiState.update {
            it.copy(
                isGameOver = true,
                showWinnerModal = true,
                modalTitle = title,
                modalDesc = desc,
                aiWins = newWins,
                aiLosses = newLosses,
                totalDraws = newDraws,
                totalGames = newTotal,
                isAiThinking = false
            )
        }
    }

    fun undoMove() {
        if (_uiState.value.isGameOver || _uiState.value.isAiThinking) return
        if (history.isEmpty()) return

        // In VS_AI, if it's player's turn, undoing means undoing AI move AND player's move
        val stepsToUndo = if (_uiState.value.gameMode == GameMode.VS_AI && history.size >= 2) {
            2
        } else {
            1
        }

        repeat(stepsToUndo) {
            if (history.isNotEmpty()) {
                val snapshot = history.removeAt(history.lastIndex)
                castleRights = snapshot.castleRights
                enPassantTarget = snapshot.enPassantTarget
                halfMoveClock = snapshot.halfMoveClock
                val posKey = ChessEngine.getPositionKey(snapshot.board, snapshot.turn, snapshot.castleRights, snapshot.enPassantTarget)
                val curr = positionCounts[posKey] ?: 1
                if (curr <= 1) positionCounts.remove(posKey) else positionCounts[posKey] = curr - 1
                val whiteInCheck = ChessEngine.isKingInCheck(PlayerColor.WHITE, snapshot.board)
                val blackInCheck = ChessEngine.isKingInCheck(PlayerColor.BLACK, snapshot.board)
                val (whiteMat, blackMat) = ChessEngine.calculateMaterialScores(snapshot.board)
                val fen = ChessEngine.generateFen(
                    snapshot.board,
                    snapshot.turn,
                    snapshot.castleRights,
                    snapshot.enPassantTarget
                )

                _uiState.update {
                    it.copy(
                        board = snapshot.board,
                        turn = snapshot.turn,
                        selectedSquare = null,
                        validMovesForSelected = emptyList(),
                        hintMove = null,
                        capturedByWhite = snapshot.capturedByWhite,
                        capturedByBlack = snapshot.capturedByBlack,
                        whiteMaterial = whiteMat,
                        blackMaterial = blackMat,
                        moveTicker = snapshot.ticker,
                        moveHistory = snapshot.moveHistory,
                        isWhiteCheck = whiteInCheck,
                        isBlackCheck = blackInCheck,
                        fenString = fen
                    )
                }
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
        handleGameOver(isWhiteWin = false, isDraw = true, "DRAW AGREED", "Match Drawn By Mutual Agreement 🤝")
    }

    fun confirmResign() {
        val state = _uiState.value
        _uiState.update { it.copy(confirmDialogType = null) }
        val isWhiteResigned = state.turn == PlayerColor.WHITE
        handleGameOver(
            isWhiteWin = !isWhiteResigned,
            isDraw = false,
            "GAME OVER",
            if (isWhiteResigned) "White Resigned! 🏳️" else "Black Resigned! 🏳️"
        )
    }

    fun resetGame() {
        aiJob?.cancel()
        pendingPromotionMove = null
        history.clear()
        castleRights = CastleRights()
        enPassantTarget = null
        halfMoveClock = 0
        positionCounts.clear()

        val initBoard = ChessEngine.createInitialBoard()
        val (wMat, bMat) = ChessEngine.calculateMaterialScores(initBoard)
        val initialFen = ChessEngine.generateFen(initBoard, PlayerColor.WHITE, castleRights, enPassantTarget)

        _uiState.update {
            it.copy(
                board = initBoard,
                turn = PlayerColor.WHITE,
                selectedSquare = null,
                validMovesForSelected = emptyList(),
                hintMove = null,
                capturedByWhite = emptyList(),
                capturedByBlack = emptyList(),
                whiteMaterial = wMat,
                blackMaterial = bMat,
                whiteTimeLeft = it.timeControl.seconds,
                blackTimeLeft = it.timeControl.seconds,
                isGameOver = false,
                isWhiteCheck = false,
                isBlackCheck = false,
                moveTicker = "🎮 Match Started — White's Turn",
                moveHistory = emptyList(),
                showWinnerModal = false,
                fenString = initialFen,
                isAiThinking = false
            )
        }

        // If AI plays White, trigger AI move
        if (_uiState.value.gameMode == GameMode.VS_AI && _uiState.value.playerColorVsAi == PlayerColor.BLACK) {
            triggerAIMove(PlayerColor.WHITE)
        }
    }

    fun setGameMode(mode: GameMode) {
        if (_uiState.value.gameMode != mode) {
            _uiState.update { it.copy(gameMode = mode) }
            resetGame()
        }
    }

    fun setAiDifficulty(diff: AIDifficulty) {
        prefs.edit().putInt("diff", diff.ordinal).apply()
        _uiState.update { it.copy(aiDifficulty = diff) }
    }

    fun setTimeControl(tc: TimeControl) {
        prefs.edit().putInt("time_ctrl", tc.ordinal).apply()
        _uiState.update {
            it.copy(
                timeControl = tc,
                whiteTimeLeft = tc.seconds,
                blackTimeLeft = tc.seconds
            )
        }
    }

    fun setBoardTheme(theme: BoardTheme) {
        prefs.edit().putInt("theme", theme.ordinal).apply()
        _uiState.update { it.copy(boardTheme = theme) }
    }

    fun setPlayerColorVsAi(color: PlayerColor) {
        _uiState.update {
            it.copy(
                playerColorVsAi = color,
                isBoardFlipped = (color == PlayerColor.BLACK)
            )
        }
        resetGame()
    }

    fun toggleSoundFx() {
        val next = !_uiState.value.isSoundFxEnabled
        soundManager.isSoundFxEnabled = next
        prefs.edit().putBoolean("sound_fx", next).apply()
        _uiState.update { it.copy(isSoundFxEnabled = next) }
    }

    fun toggleHaptics() {
        val next = !_uiState.value.isHapticsEnabled
        soundManager.isHapticsEnabled = next
        prefs.edit().putBoolean("haptics", next).apply()
        _uiState.update { it.copy(isHapticsEnabled = next) }
    }

    fun toggleMusic() {
        val playing = soundManager.toggleRyukMusic()
        _uiState.update { it.copy(isMusicPlaying = playing) }
    }

    fun openMoveHistory() {
        _uiState.update { it.copy(showHistoryModal = true) }
    }

    fun closeMoveHistory() {
        _uiState.update { it.copy(showHistoryModal = false) }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettingsModal = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettingsModal = false) }
    }

    fun openStats() {
        _uiState.update { it.copy(showStatsModal = true) }
    }

    fun closeStats() {
        _uiState.update { it.copy(showStatsModal = false) }
    }

    fun resetStats() {
        prefs.edit()
            .putInt("stats_wins", 0)
            .putInt("stats_losses", 0)
            .putInt("stats_draws", 0)
            .putInt("stats_total", 0)
            .apply()
        _uiState.update {
            it.copy(aiWins = 0, aiLosses = 0, totalDraws = 0, totalGames = 0)
        }
    }

    fun generatePgn(): String {
        val state = _uiState.value
        val sb = StringBuilder()
        sb.append("[Event \"Ninja Chess Match\"]\n")
        sb.append("[Site \"Android Ninja Arena\"]\n")
        sb.append("[White \"${if (state.playerColorVsAi == PlayerColor.WHITE) "Mad X Ninja" else "Ninja AI"}\"]\n")
        sb.append("[Black \"${if (state.playerColorVsAi == PlayerColor.BLACK) "Mad X Ninja" else "Ninja AI"}\"]\n")
        sb.append("[Result \"${if (state.isGameOver) "*" else "*"}\"]\n\n")

        for (item in state.moveHistory) {
            sb.append("${item.moveNumber}. ${item.whiteMove} ")
            if (item.blackMove != null) {
                sb.append("${item.blackMove} ")
            }
        }
        return sb.toString().trim()
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
