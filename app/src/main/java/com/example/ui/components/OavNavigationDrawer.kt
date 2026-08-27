package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.AdminState
import com.example.viewmodel.AppScreen

@Composable
fun OavDrawerContent(
    currentScreen: AppScreen,
    adminState: AdminState,
    onNavigate: (AppScreen) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(290.dp),
        color = SiteBgDarker
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Brand + Menu Items
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header Brand
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(CyanDark, CyanPrimary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "RK",
                                color = SiteBgDarker,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                        Column {
                            Text(
                                text = "OAV Hub",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = TextWhite
                            )
                            Text(
                                text = "Bibina Knowledge Hub",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onCloseDrawer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Drawer",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = CardBorder, thickness = 1.dp)

                // Navigation Items List
                Text(
                    text = "ACADEMIC PORTAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                DrawerItem(
                    title = "Home Dashboard",
                    subtitle = "Main portal overview",
                    icon = Icons.Default.Dashboard,
                    isSelected = currentScreen is AppScreen.Home,
                    onClick = {
                        onNavigate(AppScreen.Home)
                        onCloseDrawer()
                    }
                )

                DrawerItem(
                    title = "Class IX Notes",
                    subtitle = "Grade 9 CBSE Repository",
                    badge = "IX",
                    isSelected = currentScreen is AppScreen.SubjectSelection && (currentScreen as AppScreen.SubjectSelection).classLevel == "IX",
                    onClick = {
                        onNavigate(AppScreen.SubjectSelection("IX"))
                        onCloseDrawer()
                    }
                )

                DrawerItem(
                    title = "Class X Notes",
                    subtitle = "Grade 10 CBSE Board",
                    badge = "X",
                    isSelected = currentScreen is AppScreen.SubjectSelection && (currentScreen as AppScreen.SubjectSelection).classLevel == "X",
                    onClick = {
                        onNavigate(AppScreen.SubjectSelection("X"))
                        onCloseDrawer()
                    }
                )

                DrawerItem(
                    title = "Class XI Notes",
                    subtitle = "Grade 11 Higher Secondary",
                    badge = "XI",
                    isSelected = currentScreen is AppScreen.SubjectSelection && (currentScreen as AppScreen.SubjectSelection).classLevel == "XI",
                    onClick = {
                        onNavigate(AppScreen.SubjectSelection("XI"))
                        onCloseDrawer()
                    }
                )

                DrawerItem(
                    title = "Class XII Notes",
                    subtitle = "Grade 12 Board & Competitive",
                    badge = "XII",
                    isSelected = currentScreen is AppScreen.SubjectSelection && (currentScreen as AppScreen.SubjectSelection).classLevel == "XII",
                    onClick = {
                        onNavigate(AppScreen.SubjectSelection("XII"))
                        onCloseDrawer()
                    }
                )

                HorizontalDivider(color = CardBorder, thickness = 1.dp)

                Text(
                    text = "INTERACTIVE & TOOLS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                DrawerItem(
                    title = "♟️ Ritik K Chess",
                    subtitle = "Play AI / 2-Player / Live Match",
                    icon = Icons.Default.SportsEsports,
                    isSelected = currentScreen is AppScreen.Chess,
                    highlightColor = CyanPrimary,
                    onClick = {
                        onNavigate(AppScreen.Chess)
                        onCloseDrawer()
                    }
                )

                DrawerItem(
                    title = "🤖 Study Buddy AI",
                    subtitle = "Smart CBSE Curriculum Assistant",
                    icon = Icons.Default.SmartToy,
                    isSelected = currentScreen is AppScreen.StudyBuddy,
                    highlightColor = AmberAccent,
                    onClick = {
                        onNavigate(AppScreen.StudyBuddy)
                        onCloseDrawer()
                    }
                )

                DrawerItem(
                    title = "🔖 Bookmarked Notes",
                    subtitle = "Quick offline access",
                    icon = Icons.Default.Bookmark,
                    isSelected = currentScreen is AppScreen.Bookmarks,
                    onClick = {
                        onNavigate(AppScreen.Bookmarks)
                        onCloseDrawer()
                    }
                )

                HorizontalDivider(color = CardBorder, thickness = 1.dp)

                Text(
                    text = "ADMINISTRATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                DrawerItem(
                    title = if (adminState.isLoggedIn) "📂 Admin Portal (Active)" else "🔐 Admin Login",
                    subtitle = if (adminState.isLoggedIn) "Upload & delete notes" else "Faculty & teacher sign in",
                    icon = Icons.Default.AdminPanelSettings,
                    isSelected = currentScreen is AppScreen.AdminPortal,
                    highlightColor = if (adminState.isLoggedIn) EmeraldSuccess else AmberAccent,
                    onClick = {
                        onNavigate(AppScreen.AdminPortal)
                        onCloseDrawer()
                    }
                )
            }

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "© 2026 OAV Bibina Knowledge Hub",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Designed by Ritik Pradhan",
                    fontSize = 11.sp,
                    color = CyanPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    badge: String? = null,
    isSelected: Boolean = false,
    highlightColor: androidx.compose.ui.graphics.Color = CyanPrimary,
    onClick: () -> Unit
) {
    val bg = if (isSelected) CyanSoftBg else CardBg.copy(alpha = 0.5f)
    val border = if (isSelected) highlightColor else CardBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) highlightColor else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        } else if (badge != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyanSoftBg)
                    .border(1.dp, CardBorderActive, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) TextWhite else TextLight
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}
