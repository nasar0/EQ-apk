package com.surgical.eq.htcne76.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surgical.eq.htcne76.ui.components.UpdateNotificationBanner
import com.surgical.eq.htcne76.ui.theme.DarkBorder
import com.surgical.eq.htcne76.ui.theme.DarkSurface
import com.surgical.eq.htcne76.ui.theme.DarkTextPrimary
import com.surgical.eq.htcne76.ui.theme.DarkTextSecondary
import com.surgical.eq.htcne76.ui.theme.ElectricBlue
import com.surgical.eq.htcne76.ui.theme.EmeraldActive
import com.surgical.eq.htcne76.ui.theme.NeonCyan
import com.surgical.eq.htcne76.ui.theme.OledBlack
import com.surgical.eq.htcne76.ui.viewmodel.EqViewModel
import com.surgical.eq.htcne76.updater.AppUpdater
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: EqViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val updateInfo by AppUpdater.updateInfoFlow.collectAsState()
    val isDownloading by AppUpdater.isDownloading.collectAsState()
    val downloadProgress by AppUpdater.downloadProgress.collectAsState()

    LaunchedEffect(Unit) {
        AppUpdater.checkForUpdates(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = OledBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OledBlack)
                    .border(0.5.dp, DarkBorder)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Logo",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SURGICAL AUDIO DSP",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.5.sp,
                            color = DarkTextPrimary
                        )
                        Text(
                            text = "HTC NE76 Edition • Auto CI/CD",
                            fontSize = 8.5.sp,
                            color = DarkTextSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                AppUpdater.checkForUpdates(context)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .border(0.7.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(5.dp)
                                .background(EmeraldActive, RoundedCornerShape(2.5.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "HTC NE76",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTextPrimary
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = OledBlack,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(56.dp)
                    .border(0.5.dp, DarkBorder)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ecualizador",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Ecualizador",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonCyan,
                        selectedTextColor = NeonCyan,
                        unselectedIconColor = DarkTextSecondary,
                        unselectedTextColor = DarkTextSecondary,
                        indicatorColor = Color.Transparent
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "ANC Lab",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "ANC Lab",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ElectricBlue,
                        selectedTextColor = ElectricBlue,
                        unselectedIconColor = DarkTextSecondary,
                        unselectedTextColor = DarkTextSecondary,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            UpdateNotificationBanner(
                updateInfo = updateInfo,
                isDownloading = isDownloading,
                downloadProgress = downloadProgress,
                onDismiss = { AppUpdater.dismissUpdate() },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> EqualizerScreen(viewModel = viewModel, uiState = uiState)
                    1 -> NoiseControlScreen(viewModel = viewModel, uiState = uiState)
                }
            }
        }
    }
}
