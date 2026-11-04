package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DebtBootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DebtBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot or package update detected. Rescheduling all debt reminders.")
            DebtReminderScheduler.createNotificationChannel(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val allDebts = db.appDao().getAllDebtsList()
                    DebtReminderScheduler.rescheduleAllReminders(context, allDebts)
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling reminders on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
