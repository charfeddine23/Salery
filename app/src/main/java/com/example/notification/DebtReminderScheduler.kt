package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.DebtEntity
import com.example.data.DebtType
import java.util.Calendar

object DebtReminderScheduler {
    const val CHANNEL_ID = "debt_due_reminders"
    const val ACTION_DEBT_REMINDER = "com.example.notification.ACTION_DEBT_REMINDER"

    const val EXTRA_DEBT_ID = "extra_debt_id"
    const val EXTRA_PERSON_NAME = "extra_person_name"
    const val EXTRA_REMAINING_AMOUNT = "extra_remaining_amount"
    const val EXTRA_IS_OWED_BY_ME = "extra_is_owed_by_me"
    const val EXTRA_DUE_DATE_MILLIS = "extra_due_date_millis"

    private const val TAG = "DebtReminderScheduler"
    private const val TWENTY_FOUR_HOURS_MILLIS = 24 * 60 * 60 * 1000L

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Debt Due Date Reminders"
            val descriptionText = "Sends notifications 24 hours before a debt payment due date"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun calculateEffectiveDueDate(debt: DebtEntity): Long {
        return if (debt.isRecurringMonthlyInstallment) {
            val calendar = Calendar.getInstance()
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            if (currentDay >= debt.installmentDayOfMonth) {
                calendar.add(Calendar.MONTH, 1)
            }
            val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, debt.installmentDayOfMonth.coerceAtMost(maxDay))
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        } else {
            debt.dueDateMillis
        }
    }

    fun scheduleDebtReminder(context: Context, debt: DebtEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // If debt is settled or paid off, cancel any existing reminder
        if (debt.isPaid || debt.remainingAmount <= 0.001) {
            cancelDebtReminder(context, debt.id)
            return
        }

        val effectiveDueDate = calculateEffectiveDueDate(debt)
        val reminderTime = effectiveDueDate - TWENTY_FOUR_HOURS_MILLIS
        val now = System.currentTimeMillis()

        if (reminderTime <= now) {
            Log.d(TAG, "Reminder time is already in the past for debt ${debt.id}")
            return
        }

        val intent = Intent(context, DebtReminderReceiver::class.java).apply {
            action = ACTION_DEBT_REMINDER
            putExtra(EXTRA_DEBT_ID, debt.id)
            putExtra(EXTRA_PERSON_NAME, debt.personName)
            putExtra(EXTRA_REMAINING_AMOUNT, debt.remainingAmount)
            putExtra(EXTRA_IS_OWED_BY_ME, debt.type == DebtType.OWED_BY_ME)
            putExtra(EXTRA_DUE_DATE_MILLIS, effectiveDueDate)
        }

        val requestCode = (debt.id and 0x7FFFFFFF).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled 24h debt reminder for debt ${debt.id} at $reminderTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule debt reminder", e)
        }
    }

    fun cancelDebtReminder(context: Context, debtId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DebtReminderReceiver::class.java).apply {
            action = ACTION_DEBT_REMINDER
        }
        val requestCode = (debtId and 0x7FFFFFFF).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled debt reminder for debt $debtId")
        }
    }

    fun rescheduleAllReminders(context: Context, debts: List<DebtEntity>) {
        createNotificationChannel(context)
        for (debt in debts) {
            if (!debt.isPaid && debt.remainingAmount > 0.001) {
                scheduleDebtReminder(context, debt)
            } else {
                cancelDebtReminder(context, debt.id)
            }
        }
    }
}
