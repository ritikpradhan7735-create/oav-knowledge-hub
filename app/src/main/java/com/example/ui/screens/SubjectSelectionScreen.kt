package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NoteEntity
import com.example.ui.theme.*
import com.example.viewmodel.AdminState
import com.example.viewmodel.AppScreen

@Composable
fun SubjectSelectionScreen(
    classLevel: String,
    notes: List<NoteEntity>,
    adminState: AdminState,
    onSelectSubject: (String) -> Unit,
    onSelectOtherClass: (String) -> Unit,
    onOpenUploadModal: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val classNotes = notes.filter { it.classLevel.equals(classLevel, ignoreCase = true) }
    val grouped = classNotes.groupBy { it.subject }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SiteBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                        text = "Class $classLevel Subjects",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite
                    )
                    Text(
                        text = "${classNotes.size} Total Chapter Notes Available",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            if (adminState.isLoggedIn) {
                Button(
                    onClick = onOpenUploadModal,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Note", tint = SiteBgDarker, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Upload", color = SiteBgDarker, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Quick Class Switcher Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("IX", "X", "XI", "XII").forEach { cls ->
                val isCurrent = cls.equals(classLevel, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCurrent) CyanPrimary else CardBg)
                        .border(1.dp, if (isCurrent) CyanPrimary else CardBorder, RoundedCornerShape(10.dp))
                        .clickable { if (!isCurrent) onSelectOtherClass(cls) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Class $cls",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) SiteBgDarker else TextSecondary
                    )
                }
            }
        }

        if (grouped.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "📂", fontSize = 42.sp)
                    Text(
                        text = "No subjects or notes found for Class $classLevel yet.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (adminState.isLoggedIn) {
                        Button(
                            onClick = onOpenUploadModal,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text(text = "+ Upload First Note for Class $classLevel", color = SiteBgDarker, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(grouped.keys.toList()) { subject ->
                    val noteCount = grouped[subject]?.size ?: 0
                    SubjectCard(
                        subjectName = subject,
                        noteCount = noteCount,
                        onClick = { onSelectSubject(subject) }
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectCard(
    subjectName: String,
    noteCount: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(136.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyanSoftBg)
                    .border(1.dp, CardBorderActive, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📁", fontSize = 18.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyanSoftBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$noteCount Note(s)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
            }
        }

        Column {
            Text(
                text = subjectName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                maxLines = 1
            )
            Text(
                text = "Click to view chapters ➔",
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}
