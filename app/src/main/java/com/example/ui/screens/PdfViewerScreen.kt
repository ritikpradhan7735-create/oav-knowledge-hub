package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.NoteEntity
import com.example.ui.theme.*

@Composable
fun PdfViewerScreen(
    note: NoteEntity,
    onToggleBookmark: (NoteEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isWebViewLoading by remember { mutableStateOf(true) }
    var showStudyMode by remember { mutableStateOf(note.fileUrl == null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SiteBg)
    ) {
        // Top App Bar for Document
        Surface(
            color = SiteBgDarker,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
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
                            text = note.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1
                        )
                        Text(
                            text = "Class ${note.classLevel} • ${note.subject}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (note.fileUrl != null) {
                        IconButton(
                            onClick = { showStudyMode = !showStudyMode }
                        ) {
                            Icon(
                                imageVector = if (showStudyMode) Icons.Default.PictureAsPdf else Icons.Default.MenuBook,
                                contentDescription = "Toggle View Mode",
                                tint = CyanPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(note.fileUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback to browser
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in Browser",
                                tint = CyanPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = { onToggleBookmark(note) }
                    ) {
                        Icon(
                            imageVector = if (note.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (note.isBookmarked) AmberAccent else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "OAV Hub Study Material: ${note.title} (Class ${note.classLevel})\n${note.fileUrl ?: ""}"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Document"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = CardBorder, thickness = 1.dp)

        // Document Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SiteBg)
        ) {
            if (note.fileUrl != null && !showStudyMode) {
                // Cloud PDF WebView Reader (Google Docs Viewer proxy for fast smooth mobile rendering)
                val embedUrl = if (note.fileUrl.endsWith(".pdf", ignoreCase = true)) {
                    "https://docs.google.com/gview?embedded=true&url=${Uri.encode(note.fileUrl)}"
                } else {
                    note.fileUrl
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.domStorageEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isWebViewLoading = false
                                }
                            }
                            loadUrl(embedUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isWebViewLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(color = CyanPrimary)
                            Text(
                                text = "Loading document...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                // Study Overview & Chapter Notes Mode
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyanSoftBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Class ${note.classLevel}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanPrimary
                                )
                            }
                            Text(
                                text = "• ${note.subject}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextLight
                            )
                        }

                        Text(
                            text = note.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                    }

                    // Key Concepts & Highlights
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "💡 Quick Chapter Highlights",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )

                        Text(
                            text = "• Aligned with latest CBSE & OAVS (Odisha Adarsha Vidyalaya) curriculum standards.\n" +
                                   "• Includes conceptual summaries, critical definitions, solved illustrations, and important exam revision points.\n" +
                                   "• Prepared for comprehensive classroom understanding and quick board exam revision.",
                            fontSize = 13.sp,
                            color = TextLight,
                            lineHeight = 20.sp
                        )

                        if (note.fileUrl != null) {
                            Button(
                                onClick = { showStudyMode = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = SiteBgDarker)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Full PDF in In-App Reader", color = SiteBgDarker, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Study Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (note.fileUrl != null) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(note.fileUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary)
                            ) {
                                Text("External App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { onToggleBookmark(note) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (note.isBookmarked) AmberAccent else CardBg
                            )
                        ) {
                            Text(
                                text = if (note.isBookmarked) "★ Bookmarked" else "☆ Bookmark",
                                color = if (note.isBookmarked) SiteBgDarker else TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
