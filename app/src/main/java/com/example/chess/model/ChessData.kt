package com.example.chess.model

enum class PlayerColor {
    WHITE, BLACK;

    fun opposite(): PlayerColor = if (this == WHITE) BLACK else WHITE
}

enum class GameMode {
    VS_AI, PVP
}

data class Position(val row: Int, val col: Int) {
    fun toAlgebraic(): String {
        val file = ('a'.code + col).toChar()
        val rank = 8 - row
        return "$file$rank"
    }
}

data class Move(
    val from: Position,
    val to: Position,
    val promotionPiece: Char? = null
)

data class CastleRights(
    var whiteKingSide: Boolean = true,
    var whiteQueenSide: Boolean = true,
    var blackKingSide: Boolean = true,
    var blackQueenSide: Boolean = true
) {
    fun copy(): CastleRights = CastleRights(
        whiteKingSide,
        whiteQueenSide,
        blackKingSide,
        blackQueenSide
    )
}

object ChessPieces {
    const val EMPTY: Char = ' '

    val symbols = mapOf(
        'r' to "♜", 'n' to "♞", 'b' to "♝", 'q' to "♛", 'k' to "♚", 'p' to "♟",
        'R' to "♖", 'N' to "♘", 'B' to "♗", 'Q' to "♕", 'K' to "♔", 'P' to "♙"
    )

    val values = mapOf(
        'p' to 100, 'n' to 320, 'b' to 330, 'r' to 500, 'q' to 900, 'k' to 20000,
        'P' to -100, 'N' to -320, 'B' to -330, 'R' to -500, 'Q' to -900, 'K' to -20000
    )

    fun isWhite(piece: Char): Boolean = piece in 'A'..'Z'
    fun isBlack(piece: Char): Boolean = piece in 'a'..'z'

    fun colorOf(piece: Char): PlayerColor? = when {
        isWhite(piece) -> PlayerColor.WHITE
        isBlack(piece) -> PlayerColor.BLACK
        else -> null
    }
}
