package com.example.chess.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.model.AIDifficulty
import com.example.chess.model.BoardTheme
import com.example.chess.model.ChessPieces
import com.example.chess.model.GameMode
import com.example.chess.model.MoveLogItem
import com.example.chess.model.PlayerColor
import com.example.chess.model.TimeControl
import com.example.chess.ui.theme.NeonCyan
import com.example.chess.ui.theme.NeonGreen
import com.example.chess.ui.theme.NeonPink
import com.example.chess.ui.theme.NeonPurple
import com.example.chess.viewmodel.ConfirmDialogType

@Composable
fun WinnerModalDialog(
    title: String,
    desc: String,
    onPlayAgain: () -> Unit,
    onViewHistory: () -> Unit
) {
    Dialog(onDismissRequest = onPlayAgain) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1F1D36), Color(0xFF0F0C20))
                    )
                )
                .border(2.dp, NeonCyan, RoundedCornerShape(18.dp))
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "👑",
                    fontSize = 46.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = title,
                    color = NeonPink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = desc,
                    color = Color(0xFFDCD6F7),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onViewHistory,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0x66FFFFFF))
                    ) {
                        Text("📜 History", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PLAY AGAIN", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
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

    Dialog(onDismissRequest = { /* Force selection */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E1E2F), Color(0xFF0F0E17))
                    )
                )
                .border(2.dp, NeonPink, RoundedCornerShape(18.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "PAWN PROMOTION",
                    color = NeonCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = "Select a piece to promote your pawn:",
                    color = Color(0xFFCCCCCC),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    choices.forEach { choice ->
                        val symbol = ChessPieces.symbols[choice] ?: "$choice"
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x22FFFFFF))
                                .border(1.5.dp, if (isWhite) NeonCyan else NeonPink, RoundedCornerShape(12.dp))
                                .clickable { onSelectPiece(choice) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = symbol,
                                color = if (isWhite) NeonCyan else NeonPink,
                                fontSize = 32.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoveHistoryDialog(
    moveHistory: List<MoveLogItem>,
    fenString: String,
    pgnString: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF181824))
                .border(1.5.dp, NeonCyan, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📜 MOVE NOTATION (${moveHistory.size} Turns)",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable moves list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33000000))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    if (moveHistory.isEmpty()) {
                        Text(
                            text = "No moves made yet.",
                            color = Color(0xFF888888),
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(moveHistory) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.moveNumber}.",
                                        color = Color(0xFF8888AA),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Text(
                                        text = item.whiteMove,
                                        color = NeonCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.blackMove ?: "",
                                        color = NeonPink,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FEN Section
                Text(
                    text = "Current Position (FEN):",
                    color = Color(0xFFAAAAAA),
                    fontSize = 10.sp
                )
                Text(
                    text = fenString,
                    color = Color(0xFFEEEEEE),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Copy FEN, Copy PGN, Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(fenString))
                            Toast.makeText(context, "FEN copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("Copy FEN", color = Color.White, fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(pgnString))
                            Toast.makeText(context, "PGN copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("Copy PGN", color = Color.White, fontSize = 10.sp)
                    }

                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, pgnString)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Ninja Chess PGN"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("Share", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    gameMode: GameMode,
    aiDifficulty: AIDifficulty,
    timeControl: TimeControl,
    boardTheme: BoardTheme,
    playerColorVsAi: PlayerColor,
    isSoundFxEnabled: Boolean,
    isHapticsEnabled: Boolean,
    isMusicPlaying: Boolean,
    onDifficultyChange: (AIDifficulty) -> Unit,
    onTimeControlChange: (TimeControl) -> Unit,
    onThemeChange: (BoardTheme) -> Unit,
    onPlayerColorChange: (PlayerColor) -> Unit,
    onToggleSoundFx: () -> Unit,
    onToggleHaptics: () -> Unit,
    onToggleMusic: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF181824))
                .border(1.5.dp, NeonPurple, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ GAME SETTINGS",
                        color = NeonCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // AI Difficulty
                    item {
                        Text("AI Difficulty (Vs AI Mode)", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AIDifficulty.entries.forEach { diff ->
                                val selected = diff == aiDifficulty
                                OutlinedButton(
                                    onClick = { onDifficultyChange(diff) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (selected) NeonCyan else Color(0x33FFFFFF)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) Color(0x3300F2FE) else Color.Transparent
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                                ) {
                                    Text(
                                        diff.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) NeonCyan else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Play As (White or Black)
                    item {
                        Text("Play As (Vs AI)", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isWhite = playerColorVsAi == PlayerColor.WHITE
                            OutlinedButton(
                                onClick = { onPlayerColorChange(PlayerColor.WHITE) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isWhite) NeonCyan else Color(0x33FFFFFF)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isWhite) Color(0x3300F2FE) else Color.Transparent
                                )
                            ) {
                                Text("⚪ White (First)", fontSize = 10.sp, color = if (isWhite) NeonCyan else Color.White)
                            }

                            OutlinedButton(
                                onClick = { onPlayerColorChange(PlayerColor.BLACK) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (!isWhite) NeonPink else Color(0x33FFFFFF)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!isWhite) Color(0x33FF007F) else Color.Transparent
                                )
                            ) {
                                Text("⚫ Black (Second)", fontSize = 10.sp, color = if (!isWhite) NeonPink else Color.White)
                            }
                        }
                    }

                    // Time Control
                    item {
                        Text("Time Control (Per Player)", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TimeControl.entries.forEach { tc ->
                                val selected = tc == timeControl
                                OutlinedButton(
                                    onClick = { onTimeControlChange(tc) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (selected) NeonGreen else Color(0x33FFFFFF)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) Color(0x3338EF7D) else Color.Transparent
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                                ) {
                                    Text(
                                        tc.label,
                                        fontSize = 9.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) NeonGreen else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Themes
                    item {
                        Text("Board Theme", color = Color(0xFFAAAAAA), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BoardTheme.entries.forEach { theme ->
                                val selected = theme == boardTheme
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(theme.darkSquareHex))
                                        .border(
                                            2.dp,
                                            if (selected) Color(theme.accentHex) else Color(0x26FFFFFF),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { onThemeChange(theme) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        theme.themeName.split(" ").first(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Toggles (Sound FX, Haptics, Music)
                    item {
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Sound FX
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔊 Sound Effects", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = isSoundFxEnabled,
                                onCheckedChange = { onToggleSoundFx() },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                            )
                        }

                        // Haptics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📳 Haptic Feedback (Vibration)", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = isHapticsEnabled,
                                onCheckedChange = { onToggleHaptics() },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonPink)
                            )
                        }

                        // Music
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎵 Ryuk Background Music", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = isMusicPlaying,
                                onCheckedChange = { onToggleMusic() },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonPurple)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsDialog(
    aiWins: Int,
    aiLosses: Int,
    totalDraws: Int,
    totalGames: Int,
    onResetStats: () -> Unit,
    onDismiss: () -> Unit
) {
    var showResetConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF181824))
                .border(1.5.dp, NeonGreen, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 NINJA RECORDS",
                        color = NeonGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val winRate = if (totalGames > 0) (aiWins * 100 / totalGames) else 0

                // Stat Cards Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Matches", "$totalGames", Color.White, Modifier.weight(1f))
                    StatCard("Win Rate", "$winRate%", NeonGreen, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("AI Wins", "$aiWins", NeonCyan, Modifier.weight(1f))
                    StatCard("AI Losses", "$aiLosses", NeonPink, Modifier.weight(1f))
                    StatCard("Draws", "$totalDraws", Color(0xFFF9D423), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0x33FF007F)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPink)
                ) {
                    Text("Clear Records", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Records?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to reset all game statistics to zero?", color = Color(0xFFCCCCCC)) },
            containerColor = Color(0xFF1F1D36),
            confirmButton = {
                Button(
                    onClick = {
                        onResetStats()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black)
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun StatCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x33000000))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color(0xFFAAAAAA), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
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
