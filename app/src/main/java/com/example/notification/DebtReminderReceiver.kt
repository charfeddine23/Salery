package com.example.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.notification.DebtReminderScheduler.CHANNEL_ID
import com.example.notification.DebtReminderScheduler.EXTRA_DEBT_ID
import com.example.notification.DebtReminderScheduler.EXTRA_IS_OWED_BY_ME
import com.example.notification.DebtReminderScheduler.EXTRA_PERSON_NAME
import com.example.notification.DebtReminderScheduler.EXTRA_REMAINING_AMOUNT
import com.example.ui.i18n.formatDzd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DebtReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DebtReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val debtId = intent.getLongExtra(EXTRA_DEBT_ID, -1L)
        val personName = intent.getStringExtra(EXTRA_PERSON_NAME) ?: "Contact"
        val remainingAmount = intent.getDoubleExtra(EXTRA_REMAINING_AMOUNT, 0.0)
        val isOwedByMe = intent.getBooleanExtra(EXTRA_IS_OWED_BY_ME, true)

        Log.d(TAG, "Debt reminder received for ID: $debtId, person: $personName, amount: $remainingAmount")

        // Make sure notification channel exists
        DebtReminderScheduler.createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted, skipping notification")
                return
            }
        }

        // Verify with database that the debt has not been paid off in the meantime
        CoroutineScope(Dispatchers.IO).launch {
            if (debtId > 0) {
                val db = AppDatabase.getDatabase(context)
                val debt = db.appDao().getDebtById(debtId)
                if (debt != null && (debt.isPaid || debt.remainingAmount <= 0.001)) {
                    Log.d(TAG, "Debt $debtId was already paid or settled. Notification suppressed.")
                    return@launch
                }
            }

            postNotification(context, debtId, personName, remainingAmount, isOwedByMe)
        }
    }

    private fun postNotification(
        context: Context,
        debtId: Long,
        personName: String,
        amount: Double,
        isOwedByMe: Boolean
    ) {
        val formattedAmount = formatDzd(amount, "DZD")

        val title = if (isOwedByMe) {
            "Debt Due Tomorrow: $formattedAmount"
        } else {
            "Receivable Due Tomorrow: $formattedAmount"
        }

        val message = if (isOwedByMe) {
            "Reminder: Your payment of $formattedAmount to $personName is due in 24 hours."
        } else {
            "Reminder: Payment of $formattedAmount from $personName is expected tomorrow."
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "DEBTS")
            putExtra("HIGHLIGHT_DEBT_ID", debtId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (debtId and 0x7FFFFFFF).toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.sp_brand_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationId = (debtId and 0x7FFFFFFF).toInt().coerceAtLeast(1001)
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "Successfully posted debt reminder notification for ID: $debtId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while showing notification", e)
        }
    }
}
