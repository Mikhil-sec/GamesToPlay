package com.mikhilnaika.continueapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mikhilnaika.continueapp.core.design.ContinueColors
import com.mikhilnaika.continueapp.core.design.ContinueSpacing
import com.mikhilnaika.continueapp.core.design.ContinueTextStyles
import com.mikhilnaika.continueapp.core.util.CalendarDates

/** Which of the two dates a picker is currently open for. */
private enum class DateField { STARTED, CLEARED }

/**
 * The started/cleared date editor — the whole of "let me log a game I finished before I had
 * this app", and the fix for a closed-test request that the pile could only ever describe the
 * present.
 *
 * Used from two places, deliberately the same component in both: DISCOVER's **LOG AS CLEARED**
 * (a game that was never in the pile) and PILE's **EDIT DATES** (one that is). A backlog app
 * whose history starts on install day is a worse record of your gaming than the memory it was
 * supposed to replace.
 *
 * Both dates are optional and independently clearable. The only rule enforced is the one that
 * would produce nonsense downstream — a clear date before the start date, which would make
 * "time in the pile" negative on the Credits Roll and in STATS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDatesDialog(
    gameName: String,
    initialStartedAt: Long?,
    initialFinishedAt: Long?,
    onDismiss: () -> Unit,
    onSave: (startedAt: Long?, finishedAt: Long?) -> Unit,
    title: String = "WHEN DID YOU PLAY IT?",
    confirmLabel: String = "SAVE",
    /** Copy explaining what saving will do, when it does more than write two dates. */
    supporting: String? = null,
) {
    var startedAt by remember { mutableStateOf(initialStartedAt) }
    var finishedAt by remember { mutableStateOf(initialFinishedAt) }
    var picking by remember { mutableStateOf<DateField?>(null) }

    val outOfOrder = startedAt != null && finishedAt != null && finishedAt!! < startedAt!!

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = gameName,
                    style = ContinueTextStyles.titleM,
                    color = ContinueColors.TextPrimary,
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = ContinueTextStyles.label,
                        color = ContinueColors.TextTertiary,
                        modifier = Modifier.padding(top = ContinueSpacing.XS.dp),
                    )
                }
                DateRow(
                    label = "STARTED",
                    value = startedAt,
                    onPick = { picking = DateField.STARTED },
                    onClear = { startedAt = null },
                )
                DateRow(
                    label = "CLEARED",
                    value = finishedAt,
                    onPick = { picking = DateField.CLEARED },
                    onClear = { finishedAt = null },
                )
                if (outOfOrder) {
                    Text(
                        text = "You can't have cleared it before you started it.",
                        style = ContinueTextStyles.label,
                        color = ContinueColors.AccentHot,
                        modifier = Modifier.padding(top = ContinueSpacing.SM.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(startedAt, finishedAt) }, enabled = !outOfOrder) {
                Text(confirmLabel, color = if (outOfOrder) ContinueColors.TextTertiary else ContinueColors.AccentCoin)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
        containerColor = ContinueColors.SurfaceRaised,
        textContentColor = ContinueColors.TextSecondary,
        titleContentColor = ContinueColors.TextPrimary,
    )

    picking?.let { field ->
        val current = if (field == DateField.STARTED) startedAt else finishedAt
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = current?.let { CalendarDates.localInstantToPickedDay(it) },
            // Nobody has finished a game tomorrow. Blocking it in the picker beats validating
            // it afterwards, which is an error message for a mistake the UI allowed.
            selectableDates = PastOnly,
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { utc ->
                            val local = CalendarDates.pickedDayToLocalNoon(utc)
                            if (field == DateField.STARTED) startedAt = local else finishedAt = local
                        }
                        picking = null
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("CANCEL") } },
            colors = DatePickerDefaults.colors(containerColor = ContinueColors.SurfaceRaised),
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(containerColor = ContinueColors.SurfaceRaised),
            )
        }
    }
}

/** Today and earlier only. */
@OptIn(ExperimentalMaterial3Api::class)
private object PastOnly : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= CalendarDates.localInstantToPickedDay(System.currentTimeMillis())

    override fun isSelectableYear(year: Int): Boolean {
        val thisYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        // 1970 rather than "the last few years": the point of this dialog is a back catalogue.
        return year in 1970..thisYear
    }
}

@Composable
private fun DateRow(label: String, value: Long?, onPick: () -> Unit, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = ContinueSpacing.MD.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = ContinueTextStyles.label, color = ContinueColors.TextTertiary)
            Text(
                text = value?.let { CalendarDates.format(it) } ?: "NOT SET",
                style = ContinueTextStyles.body,
                color = if (value != null) ContinueColors.TextPrimary else ContinueColors.TextTertiary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ContinueColors.SurfaceCabinet)
                    .clickable(onClick = onPick)
                    .padding(horizontal = ContinueSpacing.MD.dp, vertical = ContinueSpacing.SM.dp),
            )
        }
        if (value != null) {
            TextButton(onClick = onClear) {
                Text("CLEAR", style = ContinueTextStyles.label, color = ContinueColors.TextTertiary)
            }
        }
    }
}
