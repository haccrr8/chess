package com.example.chess.model

enum class PlayerColor {
    WHITE, BLACK;

    fun opposite(): PlayerColor = if (this == WHITE) BLACK else WHITE
}

enum class GameMode {
    VS_AI, PVP
}

enum class AIDifficulty(val label: String, val title: String, val depth: Int) {
    NOVICE("Novice", "Genin (Easy)", 1),
    ADEPT("Adept", "Chunin (Medium)", 2),
    GRANDMASTER("Grandmaster", "Kage (Master)", 3)
}

enum class TimeControl(val label: String, val seconds: Int) {
    BLITZ("Blitz 3m", 180),
    RAPID("Rapid 5m", 300),
    CLASSICAL("Classical 10m", 600),
    UNLIMITED("Unlimited", 0)
}

enum class BoardTheme(
    val themeName: String,
    val lightSquareHex: Long,
    val darkSquareHex: Long,
    val accentHex: Long,
    val secondaryHex: Long
) {
    CYBER_NEON("Cyber Neon", 0xFF2D3250, 0xFF424769, 0xFF00F2FE, 0xFFFF007F),
    SHADOW_NINJA("Shadow Ninja", 0xFF202026, 0xFF141418, 0xFFFF2A55, 0xFFFFB300),
    EMERALD_DOJO("Emerald Dojo", 0xFF1E3A2F, 0xFF0D231B, 0xFF00E676, 0xFFFFD700),
    MIDNIGHT_BLUE("Midnight Blue", 0xFF1E293B, 0xFF0F172A, 0xFF38BDF8, 0xFFA855F7)
}

data class MoveLogItem(
    val moveNumber: Int,
    val whiteMove: String,
    val blackMove: String? = null
)

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
