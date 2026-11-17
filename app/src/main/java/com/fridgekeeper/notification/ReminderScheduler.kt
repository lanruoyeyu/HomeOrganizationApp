package com.fridgekeeper.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fridgekeeper.FridgeApp
import com.fridgekeeper.MainActivity
import com.fridgekeeper.R
import com.fridgekeeper.model.CheckinPeriod
import com.fridgekeeper.model.CouponItemUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/** 每天固定时间检查临期优惠券和未打卡项目 */
object ReminderScheduler {

    private const val REQUEST_CODE = 1001
    private const val REMIND_HOUR = 9

    fun schedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextTriggerMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent(context)
            )
        }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun nextTriggerMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMIND_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis
    }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? FridgeApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val coupons = app.welfareRepository.getAllCoupons()
                val tasks = app.welfareRepository.getAllTasks()

                val expiring = coupons.filter { coupon ->
                    !coupon.isUsed &&
                        CouponItemUiState.remainingDays(coupon.validUntil, now) in 1..3
                }
                val pendingCheckins = tasks.filter { task ->
                    val last = task.lastCheckinAt ?: 0L
                    last < CheckinPeriod.startOf(task.cycle, now)
                }

                if (expiring.isEmpty() && pendingCheckins.isEmpty()) return@launch

                val lines = buildList {
                    if (expiring.isNotEmpty()) {
                        val value = expiring.sumOf { it.amount }
                        add("${expiring.size} 张优惠券 3 天内到期，共 ¥$value")
                    }
                    if (pendingCheckins.isNotEmpty()) {
                        add("${pendingCheckins.size} 项打卡待完成")
                    }
                }
                notify(context, lines.joinToString("；"))
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun notify(context: Context, message: String) {
        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "福利提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "优惠券到期与打卡提醒" }
            manager.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("福利提醒")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    private companion object {
        const val CHANNEL_ID = "welfare_reminder"
        const val NOTIFICATION_ID = 2001
    }
}
