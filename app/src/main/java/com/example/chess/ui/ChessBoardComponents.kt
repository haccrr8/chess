package com.example.chess.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.ChessPieces
import com.example.chess.model.Position
import com.example.chess.model.TimeControl
import com.example.chess.ui.theme.CoordText
import com.example.chess.ui.theme.InCheckRed
import com.example.chess.ui.theme.NeonCyan
import com.example.chess.ui.theme.NeonGreen
import com.example.chess.ui.theme.NeonPink
import com.example.chess.ui.theme.NeonPurple
import com.example.chess.ui.theme.SurfaceBorderActive
import com.example.chess.viewmodel.ChessUiState

@Composable
fun ChessBoardView(
    uiState: ChessUiState,
    onSquareClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "boardAnimations")
    val checkPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val hintPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintPulse"
    )

    val theme = uiState.boardTheme
    val lightSquareColor = Color(theme.lightSquareHex)
    val darkSquareColor = Color(theme.darkSquareHex)
    val accentColor = Color(theme.accentHex)

    val rowIndices = if (uiState.isBoardFlipped) (7 downTo 0).toList() else (0..7).toList()
    val colIndices = if (uiState.isBoardFlipped) (7 downTo 0).toList() else (0..7).toList()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val boardSize = minOf(maxWidth, maxHeight)

        Box(
            modifier = Modifier
                .size(boardSize)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(accentColor, NeonPurple, Color(0xFF1B1B3A))
                    )
                )
                .padding(3.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                for ((displayRowIdx, r) in rowIndices.withIndex()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for ((displayColIdx, c) in colIndices.withIndex()) {
                            val isLight = (r + c) % 2 == 0
                            val squarePos = Position(r, c)
                            val piece = uiState.board[r][c]
                            val isSelected = uiState.selectedSquare == squarePos
                            val isValidTarget = uiState.validMovesForSelected.contains(squarePos)

                            val isKingInCheck = (piece == 'K' && uiState.isWhiteCheck) ||
                                    (piece == 'k' && uiState.isBlackCheck)

                            val isHintSource = uiState.hintMove?.from == squarePos
                            val isHintTarget = uiState.hintMove?.to == squarePos

                            ChessSquare(
                                row = r,
                                col = c,
                                displayRow = displayRowIdx,
                                displayCol = displayColIdx,
                                isLight = isLight,
                                lightColor = lightSquareColor,
                                darkColor = darkSquareColor,
                                accentColor = accentColor,
                                piece = piece,
                                isSelected = isSelected,
                                isValidTarget = isValidTarget,
                                isKingInCheck = isKingInCheck,
                                isHintSource = isHintSource,
                                isHintTarget = isHintTarget,
                                checkPulseAlpha = checkPulseAlpha,
                                hintPulseAlpha = hintPulseAlpha,
                                onClick = { onSquareClick(r, c) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChessSquare(
    row: Int,
    col: Int,
    displayRow: Int,
    displayCol: Int,
    isLight: Boolean,
    lightColor: Color,
    darkColor: Color,
    accentColor: Color,
    piece: Char,
    isSelected: Boolean,
    isValidTarget: Boolean,
    isKingInCheck: Boolean,
    isHintSource: Boolean,
    isHintTarget: Boolean,
    checkPulseAlpha: Float,
    hintPulseAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = if (isLight) lightColor else darkColor
    val bgColor = when {
        isKingInCheck -> InCheckRed.copy(alpha = checkPulseAlpha)
        isSelected -> accentColor.copy(alpha = 0.85f)
        isHintSource -> Color(0xFFFFD700).copy(alpha = 0.45f * hintPulseAlpha)
        isHintTarget -> Color(0xFFFFD700).copy(alpha = 0.35f * hintPulseAlpha)
        else -> baseColor
    }

    val borderModifier = when {
        isSelected -> Modifier.border(2.dp, Color.White)
        isHintSource || isHintTarget -> Modifier.border(2.dp, Color(0xFFFFD700).copy(alpha = hintPulseAlpha))
        else -> Modifier
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(bgColor)
            .then(borderModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("square_${row}_${col}"),
        contentAlignment = Alignment.Center
    ) {
        // Rank label on leftmost rendered column
        if (displayCol == 0) {
            Text(
                text = "${8 - row}",
                color = CoordText.copy(alpha = 0.65f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 1.dp)
            )
        }

        // File label on bottommost rendered row
        if (displayRow == 7) {
            val fileChar = ('a'.code + col).toChar()
            Text(
                text = "$fileChar",
                color = CoordText.copy(alpha = 0.65f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 1.dp)
            )
        }

        // Piece symbol
        if (piece != ChessPieces.EMPTY) {
            val isWhite = ChessPieces.isWhite(piece)
            val pieceColor = if (isWhite) NeonCyan else NeonPink
            val symbol = ChessPieces.symbols[piece] ?: "$piece"

            Text(
                text = symbol,
                color = pieceColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Valid move highlight
        if (isValidTarget) {
            if (piece == ChessPieces.EMPTY) {
                // Empty square: Center Dot
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.85f))
                        .border(1.dp, Color.White, CircleShape)
                )
            } else {
                // Capture target: Hollow capture ring
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, NeonCyan, CircleShape)
                )
            }
        }

        // Hint icon on target square
        if (isHintTarget && piece == ChessPieces.EMPTY) {
            Text("💡", fontSize = 10.sp)
        }
    }
}

@Composable
fun AdvantageBar(
    whiteScore: Int,
    blackScore: Int,
    modifier: Modifier = Modifier
) {
    val total = (whiteScore + blackScore).coerceAtLeast(1)
    val whiteRatio = (whiteScore.toFloat() / total.toFloat()).coerceIn(0.1f, 0.9f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.5.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(NeonPink)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = whiteRatio)
                .background(NeonCyan)
        )
    }
}

@Composable
fun PlayerBar(
    avatarText: String,
    avatarBg: Any,
    avatarTextColor: Color,
    playerName: String,
    materialAdvantage: Int,
    capturedPieces: List<Char>,
    capturedColor: Color,
    timeLeftSeconds: Int,
    timeControl: TimeControl,
    isActiveTurn: Boolean,
    isInCheck: Boolean,
    timerFillColor: Color,
    modifier: Modifier = Modifier
) {
    val borderModifier = if (isActiveTurn) {
        Modifier.border(1.5.dp, SurfaceBorderActive, RoundedCornerShape(8.dp))
    } else {
        Modifier.border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(8.dp))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x73000000))
            .then(borderModifier)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .then(
                        if (avatarBg is Brush) Modifier.background(avatarBg)
                        else Modifier.background(avatarBg as Color)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarText,
                    color = avatarTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            // Info
            Column(verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = playerName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    // Advantage score badge (e.g. +3)
                    if (materialAdvantage > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3300F2FE))
                                .border(0.5.dp, NeonCyan, RoundedCornerShape(4.dp))
                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                        ) {
                            Text(
                                text = "+$materialAdvantage",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Captured pieces
                if (capturedPieces.isNotEmpty()) {
                    Text(
                        text = capturedPieces.joinToString("") { ChessPieces.symbols[it] ?: "$it" },
                        color = capturedColor,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Right side: Check badge & Timer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // CHECK badge
            AnimatedVisibility(
                visible = isInCheck,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(InCheckRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "CHECK",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Circular Timer
            MiniCircularTimer(
                timeLeftSeconds = timeLeftSeconds,
                timeControl = timeControl,
                fillColor = timerFillColor
            )
        }
    }
}

@Composable
fun MiniCircularTimer(
    timeLeftSeconds: Int,
    timeControl: TimeControl,
    fillColor: Color
) {
    if (timeControl == TimeControl.UNLIMITED) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x26FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "∞",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    val totalTime = timeControl.seconds.toFloat().coerceAtLeast(1f)
    val progress = (timeLeftSeconds / totalTime).coerceIn(0f, 1f)

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier.size(34.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val strokeWidth = 3.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Background ring
            drawCircle(
                color = Color(0x26FFFFFF),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc
            drawArc(
                color = fillColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = timeFormatted,
            color = Color.White,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
