package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.update.AppUpdateManager
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
            Log.d(TAG, "Boot or package update detected ($action). Rescheduling debt reminders & cleaning updates.")
            try {
                DebtReminderScheduler.createNotificationChannel(context)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed creating notification channel", t)
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                        AppUpdateManager.cleanupObsoleteUpdates(context)
                    }
                    val db = AppDatabase.getDatabase(context)
                    val allDebts = db.appDao().getAllDebtsList()
                    DebtReminderScheduler.rescheduleAllReminders(context, allDebts)
                } catch (t: Throwable) {
                    Log.e(TAG, "Error handling boot/package-replace broadcast", t)
                } finally {
                    try {
                        pendingResult.finish()
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }
}
