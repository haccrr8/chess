package com.example.chess.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.AIDifficulty
import com.example.chess.model.GameMode
import com.example.chess.model.PlayerColor
import com.example.chess.ui.theme.BgDarkEnd
import com.example.chess.ui.theme.BgDarkMid
import com.example.chess.ui.theme.BgDarkStart
import com.example.chess.ui.theme.NeonCyan
import com.example.chess.ui.theme.NeonGreenDark
import com.example.chess.ui.theme.NeonPink
import com.example.chess.ui.theme.NeonPurple
import com.example.chess.viewmodel.ChessUiState
import com.example.chess.viewmodel.ChessViewModel
import com.example.chess.viewmodel.ConfirmDialogType

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
            .padding(horizontal = 10.dp, vertical = 6.dp)
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
                isAiThinking = uiState.isAiThinking,
                isMusicPlaying = uiState.isMusicPlaying,
                onHint = { viewModel.requestHint() },
                onOpenHistory = { viewModel.openMoveHistory() },
                onOpenStats = { viewModel.openStats() },
                onOpenSettings = { viewModel.openSettings() },
                onToggleMusic = { viewModel.toggleMusic() },
                onOpenInstagram = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.instagram.com/madxninja_?stkn=d3g4MmZnY2xidm9o")
                    )
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Evaluation / Advantage balance bar
            AdvantageBar(
                whiteScore = uiState.whiteMaterial,
                blackScore = uiState.blackMaterial
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Top Player Bar
            val topIsWhite = uiState.isBoardFlipped
            val topColor = if (topIsWhite) PlayerColor.WHITE else PlayerColor.BLACK
            val topName = when {
                uiState.gameMode == GameMode.PVP -> if (topIsWhite) "Player 1 (White)" else "Player 2 (Black)"
                uiState.playerColorVsAi == topColor -> "Mad X Ninja"
                else -> "Ninja AI (${uiState.aiDifficulty.label})"
            }
            val topAdvantage = if (topIsWhite) {
                (uiState.whiteMaterial - uiState.blackMaterial).coerceAtLeast(0)
            } else {
                (uiState.blackMaterial - uiState.whiteMaterial).coerceAtLeast(0)
            }

            PlayerBar(
                avatarText = if (topIsWhite) "WK" else "AI",
                avatarBg = if (topIsWhite) Brush.linearGradient(listOf(Color(0xFFFF4E50), Color(0xFFF9D423))) else NeonCyan,
                avatarTextColor = if (topIsWhite) Color.White else Color.Black,
                playerName = topName,
                materialAdvantage = topAdvantage,
                capturedPieces = if (topIsWhite) uiState.capturedByWhite else uiState.capturedByBlack,
                capturedColor = if (topIsWhite) NeonCyan else NeonPink,
                timeLeftSeconds = if (topIsWhite) uiState.whiteTimeLeft else uiState.blackTimeLeft,
                timeControl = uiState.timeControl,
                isActiveTurn = uiState.turn == topColor,
                isInCheck = if (topIsWhite) uiState.isWhiteCheck else uiState.isBlackCheck,
                timerFillColor = if (topIsWhite) NeonCyan else NeonPink,
                modifier = Modifier.testTag("top_player_bar")
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Chess Board
            ChessBoardView(
                uiState = uiState,
                onSquareClick = { r, c -> viewModel.onSquareClicked(r, c) }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Move Ticker & Quick Flip
            MoveTickerView(
                text = uiState.moveTicker,
                onFlip = { viewModel.flipBoard() }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Bottom Player Bar
            val bottomIsWhite = !uiState.isBoardFlipped
            val bottomColor = if (bottomIsWhite) PlayerColor.WHITE else PlayerColor.BLACK
            val bottomName = when {
                uiState.gameMode == GameMode.PVP -> if (bottomIsWhite) "Player 1 (White)" else "Player 2 (Black)"
                uiState.playerColorVsAi == bottomColor -> "Mad X Ninja"
                else -> "Ninja AI (${uiState.aiDifficulty.label})"
            }
            val bottomAdvantage = if (bottomIsWhite) {
                (uiState.whiteMaterial - uiState.blackMaterial).coerceAtLeast(0)
            } else {
                (uiState.blackMaterial - uiState.whiteMaterial).coerceAtLeast(0)
            }

            PlayerBar(
                avatarText = if (bottomIsWhite) "WK" else "AI",
                avatarBg = if (bottomIsWhite) Brush.linearGradient(listOf(Color(0xFFFF4E50), Color(0xFFF9D423))) else NeonPink,
                avatarTextColor = Color.White,
                playerName = bottomName,
                materialAdvantage = bottomAdvantage,
                capturedPieces = if (bottomIsWhite) uiState.capturedByWhite else uiState.capturedByBlack,
                capturedColor = if (bottomIsWhite) NeonCyan else NeonPink,
                timeLeftSeconds = if (bottomIsWhite) uiState.whiteTimeLeft else uiState.blackTimeLeft,
                timeControl = uiState.timeControl,
                isActiveTurn = uiState.turn == bottomColor,
                isInCheck = if (bottomIsWhite) uiState.isWhiteCheck else uiState.isBlackCheck,
                timerFillColor = if (bottomIsWhite) NeonCyan else NeonPink,
                modifier = Modifier.testTag("bottom_player_bar")
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Footer Action Controls
            FooterControls(
                gameMode = uiState.gameMode,
                aiDifficulty = uiState.aiDifficulty,
                onModeChange = { viewModel.setGameMode(it) },
                onUndo = { viewModel.undoMove() },
                onHint = { viewModel.requestHint() },
                onFlip = { viewModel.flipBoard() },
                onDraw = { viewModel.requestDraw() },
                onResign = { viewModel.requestResign() },
                onReset = { viewModel.resetGame() }
            )
        }

        // Winner Modal Dialog
        if (uiState.showWinnerModal) {
            WinnerModalDialog(
                title = uiState.modalTitle,
                desc = uiState.modalDesc,
                onPlayAgain = { viewModel.dismissWinnerModal() },
                onViewHistory = {
                    viewModel.dismissWinnerModal()
                    viewModel.openMoveHistory()
                }
            )
        }

        // Pawn Promotion Modal Dialog
        if (uiState.showPromotionModal) {
            PromotionModalDialog(
                isWhite = uiState.turn == PlayerColor.WHITE,
                onSelectPiece = { viewModel.onPromotionSelected(it) }
            )
        }

        // Draw / Resign Confirmation Dialog
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

        // Move History & PGN / FEN Dialog
        if (uiState.showHistoryModal) {
            MoveHistoryDialog(
                moveHistory = uiState.moveHistory,
                fenString = uiState.fenString,
                pgnString = viewModel.generatePgn(),
                onDismiss = { viewModel.closeMoveHistory() }
            )
        }

        // Settings Dialog
        if (uiState.showSettingsModal) {
            SettingsDialog(
                gameMode = uiState.gameMode,
                aiDifficulty = uiState.aiDifficulty,
                timeControl = uiState.timeControl,
                boardTheme = uiState.boardTheme,
                playerColorVsAi = uiState.playerColorVsAi,
                isSoundFxEnabled = uiState.isSoundFxEnabled,
                isHapticsEnabled = uiState.isHapticsEnabled,
                isMusicPlaying = uiState.isMusicPlaying,
                onDifficultyChange = { viewModel.setAiDifficulty(it) },
                onTimeControlChange = { viewModel.setTimeControl(it) },
                onThemeChange = { viewModel.setBoardTheme(it) },
                onPlayerColorChange = { viewModel.setPlayerColorVsAi(it) },
                onToggleSoundFx = { viewModel.toggleSoundFx() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onToggleMusic = { viewModel.toggleMusic() },
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // Stats Dialog
        if (uiState.showStatsModal) {
            StatsDialog(
                aiWins = uiState.aiWins,
                aiLosses = uiState.aiLosses,
                totalDraws = uiState.totalDraws,
                totalGames = uiState.totalGames,
                onResetStats = { viewModel.resetStats() },
                onDismiss = { viewModel.closeStats() }
            )
        }
    }
}

@Composable
fun HeaderBar(
    isAiThinking: Boolean,
    isMusicPlaying: Boolean,
    onHint: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
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
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "🥷 NINJA CHESS",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        listOf(NeonPink, NeonPurple, NeonCyan)
                    ),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            )

            if (isAiThinking) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x3300F2FE))
                        .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text("AI 🤔", fontSize = 9.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hint Button
            HeaderIconButton(icon = "💡", onClick = onHint)

            // History Button
            HeaderIconButton(icon = "📜", onClick = onOpenHistory)

            // Stats Button
            HeaderIconButton(icon = "📊", onClick = onOpenStats)

            // Settings Button
            HeaderIconButton(icon = "⚙️", onClick = onOpenSettings)

            // Music Button
            HeaderIconButton(
                icon = if (isMusicPlaying) "🎵" else "🔇",
                onClick = onToggleMusic,
                bg = Brush.linearGradient(listOf(NeonPink, NeonPurple))
            )

            // Instagram Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
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
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "madxninja_",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HeaderIconButton(
    icon: String,
    onClick: () -> Unit,
    bg: Brush = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF)))
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, fontSize = 12.sp)
    }
}

@Composable
fun MoveTickerView(
    text: String,
    onFlip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x4D000000))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = NeonCyan,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "🔄 Flip",
            color = Color(0xFFAAAAAA),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onFlip() }
                .padding(start = 6.dp)
        )
    }
}

@Composable
fun FooterControls(
    gameMode: GameMode,
    aiDifficulty: AIDifficulty,
    onModeChange: (GameMode) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onFlip: () -> Unit,
    onDraw: () -> Unit,
    onResign: () -> Unit,
    onReset: () -> Unit
) {
    var modeDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x80000000))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mode Selector
        Box {
            OutlinedButton(
                onClick = { modeDropdownExpanded = true },
                modifier = Modifier
                    .height(32.dp)
                    .width(76.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0x1AFFFFFF),
                    contentColor = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                val label = if (gameMode == GameMode.VS_AI) "Vs AI ▾" else "2P ▾"
                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            DropdownMenu(
                expanded = modeDropdownExpanded,
                onDismissRequest = { modeDropdownExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Vs AI Mode", fontSize = 12.sp) },
                    onClick = {
                        onModeChange(GameMode.VS_AI)
                        modeDropdownExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Pass & Play (2P)", fontSize = 12.sp) },
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
                .height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF), contentColor = Color.White),
            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("↩ Undo", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
        }

        // Hint
        Button(
            onClick = onHint,
            modifier = Modifier
                .weight(0.9f)
                .height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF), contentColor = Color.White),
            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("💡 Hint", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
        }

        // Draw
        Button(
            onClick = onDraw,
            modifier = Modifier
                .weight(0.9f)
                .height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF), contentColor = Color.White),
            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("🤝 Draw", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
        }

        // Resign
        Button(
            onClick = onResign,
            modifier = Modifier
                .weight(0.9f)
                .height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF), contentColor = Color.White),
            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("🏳️ Resign", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
        }

        // Reset
        Button(
            onClick = onReset,
            modifier = Modifier
                .weight(0.95f)
                .height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreenDark,
                contentColor = Color.Black
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("↻ Reset", fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
