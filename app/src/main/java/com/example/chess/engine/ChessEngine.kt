package com.example.chess.engine

import com.example.chess.model.CastleRights
import com.example.chess.model.ChessPieces
import com.example.chess.model.Move
import com.example.chess.model.PlayerColor
import com.example.chess.model.Position
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias Board = Array<CharArray>

object ChessEngine {

    fun createInitialBoard(): Board {
        return arrayOf(
            charArrayOf('r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'),
            charArrayOf('p', 'p', 'p', 'p', 'p', 'p', 'p', 'p'),
            charArrayOf(' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '),
            charArrayOf(' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '),
            charArrayOf(' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '),
            charArrayOf(' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '),
            charArrayOf('P', 'P', 'P', 'P', 'P', 'P', 'P', 'P'),
            charArrayOf('R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R')
        )
    }

    fun cloneBoard(board: Board): Board {
        return Array(8) { r -> board[r].copyOf() }
    }

    fun findKing(color: PlayerColor, board: Board): Position? {
        val kingChar = if (color == PlayerColor.WHITE) 'K' else 'k'
        for (r in 0..7) {
            for (c in 0..7) {
                if (board[r][c] == kingChar) return Position(r, c)
            }
        }
        return null
    }

    fun isSquareAttacked(target: Position, attackerColor: PlayerColor, board: Board): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece == ChessPieces.EMPTY) continue
                if (ChessPieces.colorOf(piece) == attackerColor) {
                    val rawMoves = getRawMoves(Position(r, c), board, includeSpecial = false)
                    if (rawMoves.any { it.to == target }) return true
                }
            }
        }
        return false
    }

    fun isKingInCheck(color: PlayerColor, board: Board): Boolean {
        val kingPos = findKing(color, board) ?: return false
        val attackerColor = color.opposite()
        return isSquareAttacked(kingPos, attackerColor, board)
    }

    fun getRawMoves(
        pos: Position,
        board: Board,
        includeSpecial: Boolean = true,
        castleRights: CastleRights? = null,
        enPassantTarget: Position? = null
    ): List<Move> {
        val r = pos.row
        val c = pos.col
        val piece = board[r][c]
        if (piece == ChessPieces.EMPTY) return emptyList()

        val isWhite = ChessPieces.isWhite(piece)
        val type = piece.lowercaseChar()
        val moves = mutableListOf<Move>()

        fun isEmpty(tr: Int, tc: Int) = board[tr][tc] == ChessPieces.EMPTY
        fun isEnemy(tr: Int, tc: Int): Boolean {
            val target = board[tr][tc]
            if (target == ChessPieces.EMPTY) return false
            return if (isWhite) ChessPieces.isBlack(target) else ChessPieces.isWhite(target)
        }

        when (type) {
            'p' -> {
                val dir = if (isWhite) -1 else 1
                val startRow = if (isWhite) 6 else 1

                // 1 step forward
                val nextR = r + dir
                if (nextR in 0..7 && isEmpty(nextR, c)) {
                    moves.add(Move(pos, Position(nextR, c)))
                    // 2 steps forward from start
                    val next2R = r + 2 * dir
                    if (r == startRow && next2R in 0..7 && isEmpty(next2R, c)) {
                        moves.add(Move(pos, Position(next2R, c)))
                    }
                }

                // Diagonal captures
                for (dc in listOf(-1, 1)) {
                    val tc = c + dc
                    if (tc in 0..7 && nextR in 0..7) {
                        if (isEnemy(nextR, tc)) {
                            moves.add(Move(pos, Position(nextR, tc)))
                        }
                        if (includeSpecial && enPassantTarget != null &&
                            enPassantTarget.row == nextR && enPassantTarget.col == tc
                        ) {
                            moves.add(Move(pos, Position(nextR, tc)))
                        }
                    }
                }
            }

            'n' -> {
                val knightDeltas = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                for ((dr, dc) in knightDeltas) {
                    val tr = r + dr
                    val tc = c + dc
                    if (tr in 0..7 && tc in 0..7 && (isEmpty(tr, tc) || isEnemy(tr, tc))) {
                        moves.add(Move(pos, Position(tr, tc)))
                    }
                }
            }

            'r', 'q' -> {
                val orthoDeltas = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                for ((dr, dc) in orthoDeltas) {
                    var tr = r + dr
                    var tc = c + dc
                    while (tr in 0..7 && tc in 0..7) {
                        if (isEmpty(tr, tc)) {
                            moves.add(Move(pos, Position(tr, tc)))
                        } else {
                            if (isEnemy(tr, tc)) moves.add(Move(pos, Position(tr, tc)))
                            break
                        }
                        tr += dr
                        tc += dc
                    }
                }
            }

            'b' -> {
                // Diagonals handled below for bishop
            }

            'k' -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val tr = r + dr
                        val tc = c + dc
                        if (tr in 0..7 && tc in 0..7 && (isEmpty(tr, tc) || isEnemy(tr, tc))) {
                            moves.add(Move(pos, Position(tr, tc)))
                        }
                    }
                }

                // Castling
                if (includeSpecial && castleRights != null) {
                    val colorKey = if (isWhite) PlayerColor.WHITE else PlayerColor.BLACK
                    val enemyColor = colorKey.opposite()
                    if (!isKingInCheck(colorKey, board)) {
                        val kingSideRights = if (isWhite) castleRights.whiteKingSide else castleRights.blackKingSide
                        val queenSideRights = if (isWhite) castleRights.whiteQueenSide else castleRights.blackQueenSide

                        // King-side castling (col 6)
                        if (kingSideRights && isEmpty(r, 5) && isEmpty(r, 6) &&
                            !isSquareAttacked(Position(r, 5), enemyColor, board) &&
                            !isSquareAttacked(Position(r, 6), enemyColor, board)
                        ) {
                            moves.add(Move(pos, Position(r, 6)))
                        }

                        // Queen-side castling (col 2)
                        if (queenSideRights && isEmpty(r, 1) && isEmpty(r, 2) && isEmpty(r, 3) &&
                            !isSquareAttacked(Position(r, 2), enemyColor, board) &&
                            !isSquareAttacked(Position(r, 3), enemyColor, board)
                        ) {
                            moves.add(Move(pos, Position(r, 2)))
                        }
                    }
                }
            }
        }

        // Bishop or Queen diagonal rays
        if (type == 'b' || type == 'q') {
            val diagDeltas = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
            for ((dr, dc) in diagDeltas) {
                var tr = r + dr
                var tc = c + dc
                while (tr in 0..7 && tc in 0..7) {
                    if (isEmpty(tr, tc)) {
                        moves.add(Move(pos, Position(tr, tc)))
                    } else {
                        if (isEnemy(tr, tc)) moves.add(Move(pos, Position(tr, tc)))
                        break
                    }
                    tr += dr
                    tc += dc
                }
            }
        }

        return moves
    }

    fun getStrictLegalMoves(
        pos: Position,
        board: Board,
        castleRights: CastleRights,
        enPassantTarget: Position?
    ): List<Move> {
        val rawMoves = getRawMoves(pos, board, includeSpecial = true, castleRights, enPassantTarget)
        val piece = board[pos.row][pos.col]
        val color = ChessPieces.colorOf(piece) ?: return emptyList()

        return rawMoves.filter { move ->
            val tempBoard = cloneBoard(board)
            tempBoard[move.to.row][move.to.col] = tempBoard[pos.row][pos.col]
            tempBoard[pos.row][pos.col] = ChessPieces.EMPTY
            !isKingInCheck(color, tempBoard)
        }
    }

    fun hasAnyValidMoves(
        color: PlayerColor,
        board: Board,
        castleRights: CastleRights,
        enPassantTarget: Position?
    ): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != ChessPieces.EMPTY && ChessPieces.colorOf(piece) == color) {
                    val moves = getStrictLegalMoves(Position(r, c), board, castleRights, enPassantTarget)
                    if (moves.isNotEmpty()) return true
                }
            }
        }
        return false
    }

    fun getAllLegalMoves(
        color: PlayerColor,
        board: Board,
        castleRights: CastleRights,
        enPassantTarget: Position?
    ): List<Move> {
        val allMoves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != ChessPieces.EMPTY && ChessPieces.colorOf(piece) == color) {
                    allMoves.addAll(getStrictLegalMoves(Position(r, c), board, castleRights, enPassantTarget))
                }
            }
        }
        return allMoves
    }

    fun evaluateBoard(board: Board): Int {
        var total = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != ChessPieces.EMPTY) {
                    total += ChessPieces.values[piece] ?: 0
                }
            }
        }
        return total
    }

    private fun minimax(
        board: Board,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        isMaximizing: Boolean,
        castleRights: CastleRights,
        enPassantTarget: Position?
    ): Int {
        if (depth == 0) return evaluateBoard(board)

        var alpha = alphaInit
        var beta = betaInit
        val color = if (isMaximizing) PlayerColor.BLACK else PlayerColor.WHITE
        val allMoves = getAllLegalMoves(color, board, castleRights, enPassantTarget)

        if (allMoves.isEmpty()) {
            if (isKingInCheck(color, board)) {
                return if (isMaximizing) -9999 else 9999
            }
            return 0
        }

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in allMoves) {
                val tempBoard = cloneBoard(board)
                tempBoard[move.to.row][move.to.col] = tempBoard[move.from.row][move.from.col]
                tempBoard[move.from.row][move.from.col] = ChessPieces.EMPTY

                val eval = minimax(tempBoard, depth - 1, alpha, beta, false, castleRights, null)
                maxEval = max(maxEval, eval)
                alpha = max(alpha, eval)
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in allMoves) {
                val tempBoard = cloneBoard(board)
                tempBoard[move.to.row][move.to.col] = tempBoard[move.from.row][move.from.col]
                tempBoard[move.from.row][move.from.col] = ChessPieces.EMPTY

                val eval = minimax(tempBoard, depth - 1, alpha, beta, true, castleRights, null)
                minEval = min(minEval, eval)
                beta = min(beta, eval)
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    fun findBestMoveForBlack(
        board: Board,
        castleRights: CastleRights,
        enPassantTarget: Position?
    ): Move? {
        var bestMove: Move? = null
        var bestValue = Int.MIN_VALUE
        val allMoves = getAllLegalMoves(PlayerColor.BLACK, board, castleRights, enPassantTarget)

        for (move in allMoves) {
            val tempBoard = cloneBoard(board)
            tempBoard[move.to.row][move.to.col] = tempBoard[move.from.row][move.from.col]
            tempBoard[move.from.row][move.from.col] = ChessPieces.EMPTY

            val boardValue = minimax(tempBoard, 2, Int.MIN_VALUE, Int.MAX_VALUE, false, castleRights, null)
            if (boardValue > bestValue) {
                bestValue = boardValue
                bestMove = move
            }
        }
        return bestMove
    }
}
