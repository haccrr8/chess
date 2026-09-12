package com.example.chess.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.model.ChessPieces
import com.example.chess.model.GameMode
import com.example.chess.model.PlayerColor
import com.example.chess.model.Position
import com.example.chess.viewmodel.ConfirmDialogType
import com.example.chess.ui.theme.BgDarkEnd
import com.example.chess.ui.theme.BgDarkMid
import com.example.chess.ui.theme.BgDarkStart
import com.example.chess.ui.theme.CoordText
import com.example.chess.ui.theme.InCheckRed
import com.example.chess.ui.theme.NeonCyan
import com.example.chess.ui.theme.NeonGreen
import com.example.chess.ui.theme.NeonGreenDark
import com.example.chess.ui.theme.NeonPink
import com.example.chess.ui.theme.NeonPurple
import com.example.chess.ui.theme.SquareDark
import com.example.chess.ui.theme.SquareLight
import com.example.chess.ui.theme.SurfaceBorderActive
import com.example.chess.viewmodel.ChessUiState
import com.example.chess.viewmodel.ChessViewModel

@Composable
fun ChessScreen(
    viewModel: ChessViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(BgDarkStart, BgDarkMid, BgDarkEnd),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 2000f)
                )
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            HeaderBar(
                isMusicPlaying = uiState.isMusicPlaying,
                onToggleMusic = { viewModel.toggleMusic() },
                onOpenInstagram = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.instagram.com/madxninja_?stkn=d3g4MmZnY2xidm9o")
                    )
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Black Player Bar (Top)
            PlayerBar(
                avatarText = "PRO",
                avatarBg = NeonCyan,
                avatarTextColor = Color.Black,
                playerName = if (uiState.gameMode == GameMode.VS_AI) "Ninja Pro AI 🥷" else "Player 2 (Black)",
                capturedPieces = uiState.capturedByBlack,
                capturedColor = NeonPink,
                timeLeftSeconds = uiState.blackTimeLeft,
                isActiveTurn = uiState.turn == PlayerColor.BLACK,
                isInCheck = uiState.isBlackCheck,
                timerFillColor = NeonPink,
                modifier = Modifier.testTag("black_player_bar")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Chess Board
            ChessBoardView(
                uiState = uiState,
                onSquareClick = { r, c -> viewModel.onSquareClicked(r, c) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Move Ticker
            MoveTickerView(text = uiState.moveTicker)

            Spacer(modifier = Modifier.height(4.dp))

            // White Player Bar (Bottom)
            PlayerBar(
                avatarText = "NK",
                avatarBg = Brush.linearGradient(listOf(Color(0xFFFF4E50), Color(0xFFF9D423))),
                avatarTextColor = Color.White,
                playerName = "Mad X Ninja",
                capturedPieces = uiState.capturedByWhite,
                capturedColor = NeonCyan,
                timeLeftSeconds = uiState.whiteTimeLeft,
                isActiveTurn = uiState.turn == PlayerColor.WHITE,
                isInCheck = uiState.isWhiteCheck,
                timerFillColor = NeonCyan,
                modifier = Modifier.testTag("white_player_bar")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Footer Action Controls
            FooterControls(
                gameMode = uiState.gameMode,
                onModeChange = { viewModel.setGameMode(it) },
                onUndo = { viewModel.undoMove() },
                onDraw = { viewModel.requestDraw() },
                onResign = { viewModel.requestResign() },
                onReset = { viewModel.resetGame() }
            )
        }

        // Modals and Dialogs
        if (uiState.showWinnerModal) {
            WinnerModalDialog(
                title = uiState.modalTitle,
                desc = uiState.modalDesc,
                onPlayAgain = { viewModel.dismissWinnerModal() }
            )
        }

        if (uiState.showPromotionModal) {
            PromotionModalDialog(
                isWhite = uiState.turn == PlayerColor.WHITE,
                onSelectPiece = { viewModel.onPromotionSelected(it) }
            )
        }

        uiState.confirmDialogType?.let { dialogType ->
            ConfirmActionDialog(
                type = dialogType,
                onConfirm = {
                    if (dialogType == ConfirmDialogType.DRAW) viewModel.confirmDraw()
                    else viewModel.confirmResign()
                },
                onDismiss = { viewModel.dismissConfirmDialog() }
            )
        }
    }
}

@Composable
fun HeaderBar(
    isMusicPlaying: Boolean,
    onToggleMusic: () -> Unit,
    onOpenInstagram: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🥷 NINJA CHESS",
            style = TextStyle(
                brush = Brush.linearGradient(
                    listOf(NeonPink, NeonPurple, NeonCyan)
                ),
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Instagram Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFF09433),
                                Color(0xFFE6683C),
                                Color(0xFFDC2743),
                                Color(0xFFCC2366),
                                Color(0xFFBC1888)
                            )
                        )
                    )
                    .clickable { onOpenInstagram() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "madxninja_",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Music Toggle Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonPink, NeonPurple)
                        )
                    )
                    .clickable { onToggleMusic() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMusicPlaying) "🎵" else "🔇",
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PlayerBar(
    avatarText: String,
    avatarBg: Any,
    avatarTextColor: Color,
    playerName: String,
    capturedPieces: List<Char>,
    capturedColor: Color,
    timeLeftSeconds: Int,
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
                Text(
                    text = playerName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

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

        // CHECK badge
        AnimatedVisibility(
            visible = isInCheck,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonPink)
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
            fillColor = timerFillColor
        )
    }
}

@Composable
fun MiniCircularTimer(
    timeLeftSeconds: Int,
    fillColor: Color
) {
    val totalTime = 300f
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

@Composable
fun ChessBoardView(
    uiState: ChessUiState,
    onSquareClick: (Int, Int) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "checkPulse")
    val checkPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    BoxWithConstraints(
        modifier = Modifier
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
                        listOf(NeonPink, NeonCyan, NeonPurple)
                    )
                )
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                for (r in 0..7) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (c in 0..7) {
                            val isLight = (r + c) % 2 == 0
                            val squarePos = Position(r, c)
                            val piece = uiState.board[r][c]
                            val isSelected = uiState.selectedSquare == squarePos
                            val isValidTarget = uiState.validMovesForSelected.contains(squarePos)

                            val isKingInCheck = (piece == 'K' && uiState.isWhiteCheck) ||
                                    (piece == 'p' && false) ||
                                    (piece == 'k' && uiState.isBlackCheck)

                            ChessSquare(
                                row = r,
                                col = c,
                                isLight = isLight,
                                piece = piece,
                                isSelected = isSelected,
                                isValidTarget = isValidTarget,
                                isKingInCheck = isKingInCheck,
                                checkPulseAlpha = checkPulseAlpha,
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
    isLight: Boolean,
    piece: Char,
    isSelected: Boolean,
    isValidTarget: Boolean,
    isKingInCheck: Boolean,
    checkPulseAlpha: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isKingInCheck -> InCheckRed.copy(alpha = checkPulseAlpha)
        isSelected -> NeonPink
        isLight -> SquareLight
        else -> SquareDark
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("square_${row}_${col}"),
        contentAlignment = Alignment.Center
    ) {
        // Rank label on leftmost column (col 0)
        if (col == 0) {
            Text(
                text = "${8 - row}",
                color = CoordText.copy(alpha = 0.6f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 1.dp)
            )
        }

        // File label on bottom row (row 7)
        if (row == 7) {
            val fileChar = ('a'.code + col).toChar()
            Text(
                text = "$fileChar",
                color = CoordText.copy(alpha = 0.6f),
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

        // Target highlight dot
        if (isValidTarget) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun MoveTickerView(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x4D000000))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun FooterControls(
    gameMode: GameMode,
    onModeChange: (GameMode) -> Unit,
    onUndo: () -> Unit,
    onDraw: () -> Unit,
    onResign: () -> Unit,
    onReset: () -> Unit
) {
    var modeDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x80000000))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mode Selector
        Box {
            OutlinedButton(
                onClick = { modeDropdownExpanded = true },
                modifier = Modifier
                    .height(34.dp)
                    .width(82.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0x1AFFFFFF),
                    contentColor = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (gameMode == GameMode.VS_AI) "Vs AI ▾" else "2P ▾",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            DropdownMenu(
                expanded = modeDropdownExpanded,
                onDismissRequest = { modeDropdownExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Vs AI", fontSize = 12.sp) },
                    onClick = {
                        onModeChange(GameMode.VS_AI)
                        modeDropdownExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("2 Player", fontSize = 12.sp) },
                    onClick = {
                        onModeChange(GameMode.PVP)
                        modeDropdownExpanded = false
                    }
                )
            }
        }

        // Undo
        Button(
            onClick = onUndo,
            modifier = Modifier
                .weight(1f)
                .height(34.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x1AFFFFFF),
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 0.dp)
        ) {
            Text("↩ Undo", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        // Draw
        Button(
            onClick = onDraw,
            modifier = Modifier
                .weight(1f)
                .height(34.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x1AFFFFFF),
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 0.dp)
        ) {
            Text("🤝 Draw", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        // Resign
        Button(
            onClick = onResign,
            modifier = Modifier
                .weight(1f)
                .height(34.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x1AFFFFFF),
                contentColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 0.dp)
        ) {
            Text("🏳️ Resign", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }

        // Reset
        Button(
            onClick = onReset,
            modifier = Modifier
                .weight(1.1f)
                .height(34.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreenDark,
                contentColor = Color.Black
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 0.dp)
        ) {
            Text("↻ Reset", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun WinnerModalDialog(
    title: String,
    desc: String,
    onPlayAgain: () -> Unit
) {
    Dialog(onDismissRequest = onPlayAgain) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1E1E2F), Color(0xFF111111))
                    )
                )
                .border(2.dp, NeonCyan, RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "👑",
                    fontSize = 44.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = title,
                    color = NeonPink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = desc,
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                Button(
                    onClick = onPlayAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PLAY AGAIN",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PromotionModalDialog(
    isWhite: Boolean,
    onSelectPiece: (Char) -> Unit
) {
    val choices = if (isWhite) {
        listOf('Q', 'R', 'B', 'N')
    } else {
        listOf('q', 'r', 'b', 'n')
    }

    Dialog(onDismissRequest = { /* Must choose */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1E1E2F), Color(0xFF111111))
                    )
                )
                .border(2.dp, NeonPink, RoundedCornerShape(16.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "PROMOTE PAWN",
                    color = NeonCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = "Choose a piece to promote your pawn:",
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    choices.forEach { choice ->
                        val symbol = ChessPieces.symbols[choice] ?: "$choice"
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                .clickable { onSelectPiece(choice) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol,
                                color = if (isWhite) NeonCyan else NeonPink,
                                fontSize = 28.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmActionDialog(
    type: ConfirmDialogType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = if (type == ConfirmDialogType.DRAW) "Offer Draw?" else "Resign match?"
    val message = if (type == ConfirmDialogType.DRAW) {
        "Are you sure you want to offer a draw?"
    } else {
        "Are you sure you want to resign the game?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = Color.White) },
        text = { Text(message, color = Color(0xFFCCCCCC)) },
        containerColor = Color(0xFF1E1E2F),
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == ConfirmDialogType.DRAW) NeonCyan else NeonPink,
                    contentColor = Color.Black
                )
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}
