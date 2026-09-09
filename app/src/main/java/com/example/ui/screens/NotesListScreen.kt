package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NoteEntity
import com.example.ui.theme.*
import com.example.viewmodel.AdminState

@Composable
fun NotesListScreen(
    classLevel: String,
    subject: String,
    notes: List<NoteEntity>,
    adminState: AdminState,
    onOpenNote: (NoteEntity) -> Unit,
    onToggleBookmark: (NoteEntity) -> Unit,
    onDeleteNote: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    var filterText by remember { mutableStateOf("") }

    val subjectNotes = notes.filter {
        it.classLevel.equals(classLevel, ignoreCase = true) &&
        it.subject.equals(subject, ignoreCase = true) &&
        (filterText.isBlank() || it.title.contains(filterText, ignoreCase = true))
    }

    // Delete Confirmation Dialog
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = {
                Text(
                    "Are you sure you want to delete '${noteToDelete?.title}'?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        noteToDelete?.id?.let { onDeleteNote(it) }
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                ) {
                    Text("Delete Permanently", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SiteBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBg)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanPrimary
                    )
                }
                Column {
                    Text(
                        text = "Class $classLevel ➔ $subject",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        maxLines = 1
                    )
                    Text(
                        text = "${subjectNotes.size} Chapters & Study Notes Available",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Search in subject
        OutlinedTextField(
            value = filterText,
            onValueChange = { filterText = it },
            placeholder = { Text("Filter chapters in $subject...", fontSize = 12.sp, color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanPrimary, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg,
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            singleLine = true
        )

        if (subjectNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📝", fontSize = 38.sp)
                    Text(
                        text = "No notes found for '$subject' matching query.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(subjectNotes, key = { _, note -> note.id }) { index, note ->
                    ChapterNoteCard(
                        chapterIndex = index + 1,
                        note = note,
                        isAdmin = adminState.isLoggedIn,
                        onOpenNote = { onOpenNote(note) },
                        onToggleBookmark = { onToggleBookmark(note) },
                        onShare = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "OAV Hub Study Notes: ${note.title} (Class ${note.classLevel} ${note.subject})\n${note.fileUrl ?: "Available on OAV Hub app"}"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                        },
                        onDeleteClick = { noteToDelete = note }
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterNoteCard(
    chapterIndex: Int,
    note: NoteEntity,
    isAdmin: Boolean,
    onOpenNote: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onOpenNote() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyanSoftBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Chapter #$chapterIndex",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onToggleBookmark, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (note.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (note.isBookmarked) AmberAccent else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isAdmin) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = RoseDanger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Note Title
        Text(
            text = note.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            lineHeight = 18.sp
        )

        HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

        // Action CTA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (note.fileUrl != null) "📄 Cloud PDF Attached" else "📝 In-App Study Notes",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Text(
                text = "Open PDF / Study ➔",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanPrimary
            )
        }
    }
}
