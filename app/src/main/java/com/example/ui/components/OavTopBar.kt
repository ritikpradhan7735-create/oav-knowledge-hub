package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.AdminState
import com.example.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OavTopBar(
    adminState: AdminState,
    onMenuClick: () -> Unit,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAdminClick: () -> Unit,
    currentScreen: AppScreen,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SiteBgDarker,
            titleContentColor = TextWhite,
            navigationIconContentColor = TextLight,
            actionIconContentColor = TextLight
        ),
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = CyanPrimary
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onHomeClick() }
                    .padding(vertical = 4.dp, horizontal = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CyanDark, CyanPrimary)
                            )
                        )
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RK",
                        color = SiteBgDarker,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
                Column {
                    Text(
                        text = "OAV HUB",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = TextWhite,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Knowledge & Games Hub",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        actions = {
            // Home button
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier.testTag("home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Go to Home",
                    tint = if (currentScreen is AppScreen.Home) CyanPrimary else TextSecondary
                )
            }

            // Admin button / indicator
            IconButton(
                onClick = onAdminClick,
                modifier = Modifier.testTag("admin_button")
            ) {
                Icon(
                    imageVector = if (adminState.isLoggedIn) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                    contentDescription = "Admin Portal",
                    tint = if (adminState.isLoggedIn) EmeraldSuccess else AmberAccent
                )
            }
        }
    )
}
