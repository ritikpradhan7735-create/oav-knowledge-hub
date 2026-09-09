package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.chess.*
import com.example.ui.theme.*

@Composable
fun ChessScreen(
    chessState: ChessGameState,
    onSquareClicked: (Int, Int) -> Unit,
    onResetGame: () -> Unit,
    onSetMode: (GameMode) -> Unit,
    onToggleFlip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Interactive Board, 1: Live Server (Ritik K Chess)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SiteBg)
    ) {
        // Top Bar
        Surface(
            color = SiteBgDarker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBg)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Ritik K Chess",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Play AI / 2P / Live Multiplayer Server",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onToggleFlip,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Board",
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onResetGame,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBg)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Game",
                            tint = AmberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Mode Switcher Tab (Local Engine vs Live Server)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 0) CyanPrimary else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♟️ Interactive Board",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selectedTab == 0) SiteBgDarker else TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedTab == 1) CyanPrimary else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌐 Live Server Match",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selectedTab == 1) SiteBgDarker else TextSecondary
                )
            }
        }

        if (selectedTab == 1) {
            // Live Server Match WebView (pointing to Ritik K Chess on https://r-pradhan.onrender.com)
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = WebViewClient()
                            loadUrl("https://r-pradhan.onrender.com")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Interactive Board UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Game Difficulty Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DifficultyChip("Casual AI", chessState.gameMode == GameMode.VS_AI_EASY, Modifier.weight(1f)) {
                        onSetMode(GameMode.VS_AI_EASY)
                    }
                    DifficultyChip("Tactical AI", chessState.gameMode == GameMode.VS_AI_MEDIUM, Modifier.weight(1f)) {
                        onSetMode(GameMode.VS_AI_MEDIUM)
                    }
                    DifficultyChip("Master AI", chessState.gameMode == GameMode.VS_AI_HARD, Modifier.weight(1f)) {
                        onSetMode(GameMode.VS_AI_HARD)
                    }
                    DifficultyChip("2 Players", chessState.gameMode == GameMode.PASS_AND_PLAY, Modifier.weight(1f)) {
                        onSetMode(GameMode.PASS_AND_PLAY)
                    }
                }

                // Top Player Status / Captured Pieces
                PlayerStatusCard(
                    playerColor = PieceColor.BLACK,
                    isCurrentTurn = chessState.turn == PieceColor.BLACK,
                    timeSeconds = chessState.blackTimeSeconds,
                    capturedPieces = chessState.capturedByWhite,
                    isAi = chessState.gameMode != GameMode.PASS_AND_PLAY
                )

                // The 8x8 Chessboard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ChessSquareDark)
                        .border(2.dp, CardBorderActive, RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val rowRange = if (chessState.isFlipped) (7 downTo 0) else (0..7)
                        val colRange = if (chessState.isFlipped) (7 downTo 0) else (0..7)

                        for (r in rowRange) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                for (c in colRange) {
                                    val isLight = (r + c) % 2 == 0
                                    val isSelected = chessState.selectedPos?.row == r && chessState.selectedPos?.col == c
                                    val isValidMove = chessState.validMoves.any { it.row == r && it.col == c }
                                    val piece = chessState.board[r][c]

                                    ChessSquare(
                                        row = r,
                                        col = c,
                                        piece = piece,
                                        isLight = isLight,
                                        isSelected = isSelected,
                                        isValidMove = isValidMove,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        onClick = { onSquareClicked(r, c) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Player Status / Captured Pieces
                PlayerStatusCard(
                    playerColor = PieceColor.WHITE,
                    isCurrentTurn = chessState.turn == PieceColor.WHITE,
                    timeSeconds = chessState.whiteTimeSeconds,
                    capturedPieces = chessState.capturedByBlack,
                    isAi = false
                )

                // Status Banner / Checkmate Notification
                if (chessState.gameStatus != GameStatus.IN_PROGRESS) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (chessState.gameStatus == GameStatus.CHECKMATE) RoseDanger.copy(alpha = 0.2f)
                                else AmberAccent.copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (chessState.gameStatus == GameStatus.CHECKMATE) RoseDanger else AmberAccent,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (chessState.gameStatus) {
                                GameStatus.CHECK -> "⚠️ CHECK! King is under attack!"
                                GameStatus.CHECKMATE -> "🏆 CHECKMATE! ${chessState.winner} Wins!"
                                GameStatus.STALEMATE -> "🤝 STALEMATE! Game is drawn."
                                else -> "Game status: ${chessState.gameStatus}"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (chessState.gameStatus == GameStatus.CHECKMATE) RoseDanger else AmberAccent
                        )
                    }
                }

                // Move History Log
                if (chessState.moveHistory.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(chessState.moveHistory) { idx, move ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CardBg)
                                    .border(0.5.dp, CardBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${idx + 1}. ${move.notation}",
                                    fontSize = 10.sp,
                                    color = TextLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }
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
    piece: ChessPiece?,
    isLight: Boolean,
    isSelected: Boolean,
    isValidMove: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> ChessSquareSelected
        isLight -> ChessSquareLight
        else -> ChessSquareDark
    }

    Box(
        modifier = modifier
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (piece != null) {
            Text(
                text = piece.symbol,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (piece.color == PieceColor.WHITE) ChessPieceWhite else ChessPieceBlack,
                textAlign = TextAlign.Center
            )
        }

        // Legal Move Dot or Ring
        if (isValidMove) {
            if (piece != null) {
                // Ring for capture
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, RoseDanger, CircleShape)
                )
            } else {
                // Dot for empty square
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(CyanPrimary)
                )
            }
        }
    }
}

@Composable
fun PlayerStatusCard(
    playerColor: PieceColor,
    isCurrentTurn: Boolean,
    timeSeconds: Int,
    capturedPieces: List<ChessPiece>,
    isAi: Boolean
) {
    val minutes = timeSeconds / 60
    val seconds = timeSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentTurn) CyanSoftBg else CardBg)
            .border(1.dp, if (isCurrentTurn) CyanPrimary else CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (playerColor == PieceColor.WHITE) ChessPieceWhite else ChessPieceBlack)
                    .border(1.dp, CardBorder, CircleShape)
            )

            Text(
                text = if (playerColor == PieceColor.WHITE) "White (You)" else if (isAi) "Black (AI)" else "Black",
                fontSize = 12.sp,
                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrentTurn) CyanPrimary else TextWhite
            )

            // Captured symbols
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                capturedPieces.take(6).forEach { p ->
                    Text(text = p.symbol, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        Text(
            text = timeFormatted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCurrentTurn) CyanPrimary else TextSecondary
        )
    }
}

@Composable
fun DifficultyChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CyanPrimary else CardBg)
            .border(1.dp, if (isSelected) CyanPrimary else CardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) SiteBgDarker else TextSecondary
        )
    }
}
