package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.NoteEntity
import com.example.ui.theme.*
import com.example.viewmodel.AdminState
import com.example.viewmodel.AppScreen

@Composable
fun HomeScreen(
    notes: List<NoteEntity>,
    adminState: AdminState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onToggleBookmark: (NoteEntity) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SiteBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Top Hero Badges & Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Banner Image (if present)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hero_banner),
                        contentDescription = "OAV Knowledge Hub Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, SiteBgDarker.copy(alpha = 0.85f))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "OAV BIBINA",
                            color = CyanPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "• Knowledge & Games Hub",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Feature Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeatureBadge(label = "📚 Notes", color = CyanPrimary)
                    FeatureBadge(label = "♟️ Chess", color = AmberAccent)
                    FeatureBadge(label = "💡 Learning", color = EmeraldSuccess)
                    FeatureBadge(label = "🤖 AI Buddy", color = CyanGlow)
                }

                // Title Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Welcome to the",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                    Text(
                        text = "Knowledge Hub",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Everything you need to excel. Select a portal below to begin.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Stat Info Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InfoPill(
                        value = "4",
                        label = "Classes",
                        color = CyanPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.ClassSelection) }
                    )
                    InfoPill(
                        value = "${notes.size}+",
                        label = "Notes",
                        color = EmeraldSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.ClassSelection) }
                    )
                    InfoPill(
                        value = "Live",
                        label = "Chess",
                        color = AmberAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppScreen.Chess) }
                    )
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input"),
                    placeholder = {
                        Text(
                            text = "Search notes by title, chapter or subject...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CyanPrimary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }
        }

        // Main Navigation Cards Grid
        item {
            Text(
                text = "EXPLORE HUB PORTALS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Study Notes Card (Primary gateway to Academic Classes)
                CustomPortalCard(
                    title = "Study Notes",
                    subtitle = "Classes IX - XII CBSE Curriculum Repository",
                    iconEmoji = "📚",
                    actionText = "Select Class ➔",
                    accentColor = CyanPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("notes_portal_card"),
                    onClick = { onNavigate(AppScreen.ClassSelection) }
                )

                // 2. Ritik K Chess Card
                CustomPortalCard(
                    title = "Ritik K Chess",
                    subtitle = "Play Live Multiplayer & AI Match",
                    iconEmoji = "♟️",
                    actionText = "Play Game ➔",
                    accentColor = AmberAccent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chess_portal_card"),
                    onClick = { onNavigate(AppScreen.Chess) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3. AI Study Buddy Card
                CustomPortalCard(
                    title = "Study Buddy AI",
                    subtitle = "Instant CBSE Concept & Formula Q&A",
                    iconEmoji = "🤖",
                    actionText = "Ask AI ➔",
                    accentColor = CyanGlow,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_portal_card"),
                    onClick = { onNavigate(AppScreen.StudyBuddy) }
                )

                // 4. Bookmarks Card
                CustomPortalCard(
                    title = "Bookmarks",
                    subtitle = "Saved Chapter Notes for Quick Revision",
                    iconEmoji = "🔖",
                    actionText = "View Saved ➔",
                    accentColor = EmeraldSuccess,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bookmarks_portal_card"),
                    onClick = { onNavigate(AppScreen.Bookmarks) }
                )
            }
        }

        item {
            // Admin Portal Card
            CustomPortalCard(
                title = if (adminState.isLoggedIn) "Admin Portal" else "Faculty & Admin Portal",
                subtitle = if (adminState.isLoggedIn) "Upload new PDF notes and manage existing materials" else "Sign in to upload new study materials and syllabus PDFs",
                iconEmoji = "📂",
                actionText = if (adminState.isLoggedIn) "Open Admin Portal ➔" else "Faculty Sign In ➔",
                accentColor = if (adminState.isLoggedIn) EmeraldSuccess else CyanPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_portal_card"),
                onClick = { onNavigate(AppScreen.AdminPortal) }
            )
        }

        // When search is active, show matching results directly on home
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "SEARCH RESULTS (${notes.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
            }

            if (notes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notes found matching '$searchQuery'",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    NoteCardItem(
                        note = note,
                        onOpenNote = { onNavigate(AppScreen.PdfViewer(note)) },
                        onToggleBookmark = { onToggleBookmark(note) }
                    )
                }
            }
        } else {
            // Hub info card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🏛️", fontSize = 18.sp)
                            Text(
                                text = "OAV Knowledge Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextWhite
                            )
                        }
                        Text(
                            text = "Tap on 'Study Notes' to choose your class (IX, X, XI, XII), pick your subject, and access comprehensive chapter notes and solutions.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun InfoPill(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}

@Composable
fun CustomPortalCard(
    title: String,
    subtitle: String,
    iconEmoji: String,
    actionText: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(148.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SiteBgDarker)
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = iconEmoji, fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextWhite,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    lineHeight = 13.sp
                )
            }
        }

        Text(
            text = actionText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
fun ClassQuickCard(
    romanNum: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = romanNum,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = CyanPrimary
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}

@Composable
fun NoteCardItem(
    note: NoteEntity,
    onOpenNote: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable { onOpenNote() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CyanSoftBg)
                .border(1.dp, CardBorderActive, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (note.fileUrl != null) "📄" else "📝",
                fontSize = 20.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyanSoftBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Class ${note.classLevel}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary
                    )
                }
                Text(
                    text = "• ${note.subject}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = note.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                maxLines = 2
            )
        }

        IconButton(
            onClick = onToggleBookmark,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (note.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Bookmark",
                tint = if (note.isBookmarked) AmberAccent else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
