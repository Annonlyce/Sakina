package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SleepTimerMode
import com.example.data.model.SleepTimerState
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldSubtle
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteRaised
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MutedText
import com.example.ui.theme.PrimaryText
import com.example.util.formatPlayerTimeArabic

enum class SakinaTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    RECITERS("القرّاء", Icons.Rounded.Headphones, "tab_reciters"),
    WIRD("الورد", Icons.AutoMirrored.Rounded.MenuBook, "tab_wird"),
    ADHKAR("الأذكار", Icons.Rounded.WbSunny, "tab_adhkar"),
    DOWNLOADS("التحميلات", Icons.Rounded.Download, "tab_downloads")
}

@Composable
fun SakinaTopBar(
    sleepTimerState: SleepTimerState,
    onSleepTimerClick: () -> Unit,
    onJourneyClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CharcoalBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Right side in RTL: crescent mark + app name "سكينة"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sakina_crescent),
                contentDescription = "شعار سكينة",
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "سكينة",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
        }

        // Left side in RTL: Sleep Timer, Journey, About
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val timerLabel = when {
                !sleepTimerState.isActive -> "مؤقّت"
                sleepTimerState.mode == SleepTimerMode.END_OF_SURAH -> "نهاية السورة"
                else -> formatPlayerTimeArabic(sleepTimerState.remainingSeconds * 1000L)
            }

            Row(
                modifier = Modifier
                    .height(42.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .background(if (sleepTimerState.isActive) EmeraldSubtle else GraphiteSurface)
                    .border(
                        1.dp,
                        if (sleepTimerState.isActive) EmeraldAccent.copy(alpha = 0.6f) else GraphiteBorder,
                        RoundedCornerShape(50)
                    )
                    .clickable(onClick = onSleepTimerClick)
                    .padding(horizontal = 12.dp)
                    .testTag("sleep_timer_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (sleepTimerState.isActive) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = "مؤقّت النوم",
                    tint = if (sleepTimerState.isActive) EmeraldAccent else PrimaryText,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = timerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (sleepTimerState.isActive) EmeraldAccent else PrimaryText
                )
            }

            IconButton(
                onClick = onJourneyClick,
                modifier = Modifier
                    .size(42.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(GraphiteSurface)
                    .border(1.dp, GraphiteBorder, CircleShape)
                    .testTag("journey_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = "رحلتي",
                    tint = PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onAboutClick,
                modifier = Modifier
                    .size(42.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(GraphiteSurface)
                    .border(1.dp, GraphiteBorder, CircleShape)
                    .testTag("about_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "عن التطبيق",
                    tint = PrimaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SakinaBottomBar(
    selectedTab: SakinaTab,
    showWirdDot: Boolean,
    showAdhkarDot: Boolean,
    onTabSelected: (SakinaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = GraphiteSurface,
        contentColor = PrimaryText,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        SakinaTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            val showDot = (tab == SakinaTab.WIRD && showWirdDot) ||
                (tab == SakinaTab.ADHKAR && showAdhkarDot)

            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.testTag(tab.testTag),
                icon = {
                    Box {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            modifier = Modifier.size(24.dp)
                        )
                        if (showDot) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-2).dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldAccent)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EmeraldAccent,
                    selectedTextColor = EmeraldAccent,
                    unselectedIconColor = MutedText,
                    unselectedTextColor = MutedText,
                    indicatorColor = EmeraldSubtle
                )
            )
        }
    }
}
