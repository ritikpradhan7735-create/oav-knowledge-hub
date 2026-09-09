package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NoteEntity
import com.example.ui.theme.*
import com.example.viewmodel.AdminState

@Composable
fun AdminScreen(
    adminState: AdminState,
    allNotes: List<NoteEntity>,
    isUploading: Boolean,
    uploadMessage: String?,
    onLogin: (String, String) -> Boolean,
    onLogout: () -> Unit,
    onUploadNote: (String, String, String, String?) -> Unit,
    onDeleteNote: (String) -> Unit,
    onClearUploadMessage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    // Upload form states
    var selectedClass by remember { mutableStateOf("IX") }
    var subjectInput by remember { mutableStateOf("Science") }
    var titleInput by remember { mutableStateOf("") }
    var pdfUrlInput by remember { mutableStateOf("") }

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
                            text = "Admin Portal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                        Text(
                            text = if (adminState.isLoggedIn) "Logged in as ${adminState.username}" else "Faculty & Staff Sign In",
                            fontSize = 10.sp,
                            color = if (adminState.isLoggedIn) EmeraldSuccess else TextSecondary
                        )
                    }
                }

                if (adminState.isLoggedIn) {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = RoseDanger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        HorizontalDivider(color = CardBorder, thickness = 1.dp)

        if (!adminState.isLoggedIn) {
            // Login Form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBg)
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyanSoftBg)
                            .border(1.dp, CardBorderActive, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Login",
                            tint = CyanPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Administrator Sign In",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite
                    )

                    Text(
                        text = "Sign in to upload CBSE study notes and manage existing files.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username", color = TextSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_username_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SiteBgDarker,
                            unfocusedContainerColor = SiteBgDarker,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password", color = TextSecondary) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SiteBgDarker,
                            unfocusedContainerColor = SiteBgDarker,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        singleLine = true
                    )

                    if (loginError) {
                        Text(
                            text = "Please enter valid credentials.",
                            color = RoseDanger,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            val success = onLogin(usernameInput, passwordInput)
                            if (!success) loginError = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_login_submit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Login to Portal",
                            color = SiteBgDarker,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // Logged in: Management & Upload Form
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Upload status message
                if (uploadMessage != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldSoftBg)
                                .border(1.dp, EmeraldSuccess, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uploadMessage,
                                    color = EmeraldSuccess,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = onClearUploadMessage, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = EmeraldSuccess)
                                }
                            }
                        }
                    }
                }

                // Upload Form Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "📤", fontSize = 18.sp)
                            Text(
                                text = "Upload New Study Note",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                        }

                        // Class Selector
                        Text("Select Class:", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("IX", "X", "XI", "XII").forEach { cls ->
                                val isSelected = selectedClass == cls
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) CyanPrimary else SiteBgDarker)
                                        .clickable { selectedClass = cls }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Class $cls",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSelected) SiteBgDarker else TextSecondary
                                    )
                                }
                            }
                        }

                        // Subject Input
                        OutlinedTextField(
                            value = subjectInput,
                            onValueChange = { subjectInput = it },
                            label = { Text("Subject (e.g. Science, Mathematics, IT 402)", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SiteBgDarker,
                                unfocusedContainerColor = SiteBgDarker,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        // Title Input
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Chapter Title (e.g. Newton Laws of Motion)", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SiteBgDarker,
                                unfocusedContainerColor = SiteBgDarker,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        // PDF URL input
                        OutlinedTextField(
                            value = pdfUrlInput,
                            onValueChange = { pdfUrlInput = it },
                            label = { Text("PDF Document Link / Cloudinary URL (Optional)", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SiteBgDarker,
                                unfocusedContainerColor = SiteBgDarker,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (titleInput.isNotBlank() && subjectInput.isNotBlank()) {
                                    onUploadNote(
                                        selectedClass,
                                        subjectInput.trim(),
                                        titleInput.trim(),
                                        pdfUrlInput.trim().ifEmpty { null }
                                    )
                                    titleInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isUploading && titleInput.isNotBlank()
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SiteBgDarker)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading Note...", color = SiteBgDarker, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Upload Chapter Note", color = SiteBgDarker, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Manage All Notes Section
                item {
                    Text(
                        text = "MANAGE REPOSITORY NOTES (${allNotes.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                items(allNotes, key = { it.id }) { note ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Class ${note.classLevel} • ${note.subject}",
                                fontSize = 10.sp,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = note.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = { onDeleteNote(note.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
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
        }
    }
}
