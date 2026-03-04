package com.example.trackerapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DayChip(val dayNum: String, val dayName: String, val selected: Boolean)

private enum class LimitOption(val label: String, val minutes: Int?) {
    MIN_30("30 min", 30),
    HOUR_1("1 hr", 60),
    HOUR_2("2 hr", 120),
    CUSTOM("Custom", null)
}

@Composable
fun HomeScreen(
    name: String = "Oluwasegun",

    // ✅ Option B inputs (MainActivity owns these)
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,

    // Display label (MainActivity can pass same as currentMonth formatted)
    monthLabel: String,

    stepsToday: Int = 0,
    goalSteps: Int = 10_000,
    streakDays: Int = 5,
    sittingLabel: String = "0m",
    caloriesLabel: String = "0 Kcal",
    kmLabel: String = "0.0",
    inactiveLabel: String = "0m",
    progressToLimit: Float = 0f,

    status: String = "Sitting",

    limitMinutes: Int = 120,
    onLimitMinutesChange: (Int) -> Unit = {},

    onStandUpNow: () -> Unit = {},
    onBell: () -> Unit = {}
) {
    val visibleDates = remember(selectedDate) { build5DayStrip(selectedDate) }

    val days = remember(visibleDates, selectedDate) {
        visibleDates.map { d ->
            DayChip(
                dayNum = d.dayOfMonth.toString().padStart(2, '0'),
                dayName = d.dayOfWeek.name.take(3).lowercase()
                    .replaceFirstChar { it.uppercase() },
                selected = d == selectedDate
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderRow(name = name, onBell = onBell)

            MonthRow(
                monthLabel = monthLabel,
                onPrev = onPrevMonth,
                onNext = onNextMonth
            )

            CalendarRow(
                days = days,
                onDayClick = { index ->
                    val newDate = visibleDates[index]
                    onDateSelected(newDate)
                }
            )

            StepsRingCard(stepsToday = stepsToday, goalSteps = goalSteps)

            StreakBanner(streakDays = streakDays)

            StatsRow(sitting = sittingLabel, calories = caloriesLabel, km = kmLabel)

            InactivityCard(
                inactiveLabel = inactiveLabel,
                progressToLimit = progressToLimit,
                limitMinutes = limitMinutes,
                status = status,
                onLimitMinutesChange = onLimitMinutesChange,
                onStandUpNow = onStandUpNow
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeaderRow(name: String, onBell: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hello, 👋",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(
            onClick = onBell,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
        }
    }
}

@Composable
private fun MonthRow(monthLabel: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onPrev,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev") }

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = onNext,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next") }
    }
}

@Composable
private fun CalendarRow(days: List<DayChip>, onDayClick: (index: Int) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(days.size) { i ->
            val d = days[i]
            val shape = RoundedCornerShape(18.dp)

            val bg =
                if (d.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val text =
                if (d.selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

            Column(
                modifier = Modifier
                    .width(66.dp)
                    .height(90.dp)
                    .clip(shape)
                    .background(bg)
                    .clickable { onDayClick(i) }
                    .padding(10.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(d.dayNum, fontWeight = FontWeight.Bold, color = text)
                Spacer(Modifier.height(6.dp))
                Text(d.dayName, color = text)
            }
        }
    }
}

@Composable
private fun StepsRingCard(stepsToday: Int, goalSteps: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        ProgressRing(
            steps = stepsToday,
            goal = goalSteps,
            modifier = Modifier.size(260.dp)
        )
    }
}

@Composable
private fun StreakBanner(streakDays: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔥", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "$streakDays-day movement streak",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatsRow(sitting: String, calories: String, km: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard("Sitting Time", sitting, Modifier.weight(1f))
        StatCard("Calories", calories, Modifier.weight(1f))
        StatCard("Kilometers", km, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(88.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InactivityCard(
    inactiveLabel: String,
    progressToLimit: Float,
    limitMinutes: Int,
    status: String,
    onLimitMinutesChange: (Int) -> Unit,
    onStandUpNow: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }

    val limitLabel = when (limitMinutes) {
        30 -> "30 min Limit"
        60 -> "1 hr Limit"
        120 -> "2 hr Limit"
        else -> "${limitMinutes} min Limit"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "You've been inactive for $inactiveLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (status == "Sitting") "Nudge: Circulation is slowing down" else "Nice — you’re moving 👍🏽",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (status == "Sitting") "🪑" else "🚶‍♂️", textAlign = TextAlign.Center)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Current Status", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))

                AssistChip(onClick = { }, label = { Text(status) })

                Spacer(Modifier.width(10.dp))

                Box {
                    TextButton(onClick = { menuOpen = true }) {
                        Text(limitLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        LimitOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = {
                                    menuOpen = false
                                    if (opt.minutes != null) onLimitMinutesChange(opt.minutes)
                                    else {
                                        showCustomDialog = true
                                        customText = ""
                                    }
                                }
                            )
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progressToLimit.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(10.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Button(
                onClick = onStandUpNow,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (status == "Sitting") "Stand up now" else "Keep moving",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom limit (minutes)") },
            text = {
                OutlinedTextField(
                    value = customText,
                    onValueChange = { input -> customText = input.filter { it.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("e.g., 45") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = customText.toIntOrNull()
                    if (v != null && v >= 1) {
                        onLimitMinutesChange(v)
                        showCustomDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") } }
        )
    }
}

private fun build5DayStrip(selected: LocalDate): List<LocalDate> =
    listOf(
        selected.minusDays(2),
        selected.minusDays(1),
        selected,
        selected.plusDays(1),
        selected.plusDays(2)
    )