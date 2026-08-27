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
import androidx.compose.material.icons.filled.*
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
import com.example.viewmodel.AppScreen

@Composable
fun ClassSelectionScreen(
    notes: List<NoteEntity>,
    onSelectClass: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val classes = listOf(
        Triple("IX", "Class 9th", "Secondary Foundation - Science, Maths, Social Science, IT 402"),
        Triple("X", "Class 10th", "CBSE Board Exam - Complete Chapter Notes & PYQ Bank"),
        Triple("XI", "Class 11th", "Senior Secondary - Physics, Chemistry, Maths, CS"),
        Triple("XII", "Class 12th", "Board Preparation - Comprehensive Study Repository")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SiteBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    text = "Select Your Class",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite
                )
                Text(
                    text = "CBSE & OAV Curated Study Materials",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(classes) { (classNum, label, desc) ->
                val count = notes.count { it.classLevel.equals(classNum, ignoreCase = true) }
                ClassCardLarge(
                    classNum = classNum,
                    label = label,
                    desc = desc,
                    noteCount = count,
                    onClick = { onSelectClass(classNum) }
                )
            }
        }
    }
}

@Composable
fun ClassCardLarge(
    classNum: String,
    label: String,
    desc: String,
    noteCount: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .height(180.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyanSoftBg)
                    .border(1.dp, CardBorderActive, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = classNum,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = CyanPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(EmeraldSoftBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$noteCount Note(s)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 2,
                lineHeight = 13.sp
            )
        }

        Text(
            text = "Browse Subjects ➔",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyanPrimary
        )
    }
}
