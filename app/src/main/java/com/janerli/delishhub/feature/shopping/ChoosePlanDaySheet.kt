package com.janerli.delishhub.feature.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private data class DayChipUi(
    val label: String,
    val date: LocalDate
)

@Composable
fun ChoosePlanDaySheet(
    onPick: (epochDay: Long) -> Unit,
    onClose: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM") }
    val today = remember { LocalDate.now() }
    val monday = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }

    val days = remember(monday) {
        val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        (0..6).map { i -> DayChipUi(labels[i], monday.plusDays(i.toLong())) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Импорт из плана", style = MaterialTheme.typography.titleMedium)
        Text("Выбери день недели:", style = MaterialTheme.typography.bodyMedium)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(days) { d ->
                Button(onClick = { onPick(d.date.toEpochDay()) }) {
                    Text("${d.label} ${d.date.format(dateFormatter)}")
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClose
        ) {
            Text("Закрыть")
        }
    }
}
