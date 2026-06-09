package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.ui.ssh.SshClientHelper
import com.example.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerMonitorWidgetProvider : AppWidgetProvider() {

    private val sshHelper = SshClientHelper()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.Default)
        
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val profiles = withContext(Dispatchers.IO) {
                    db.serverProfileDao().getAllProfilesSync()
                }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.cyber_widget_layout)

                    if (profiles.isEmpty()) {
                        views.setTextViewText(R.id.widget_server_name, "NO SAVED HOSTS")
                        views.setTextViewText(R.id.widget_host, "Add profiles in CoreShell")
                        views.setTextViewText(R.id.widget_cpu_text, ".......... N/A")
                        views.setTextViewText(R.id.widget_ram_text, ".......... N/A")
                    } else {
                        // Use the first (most recent) server profile
                        val profile = profiles.first()
                        views.setTextViewText(R.id.widget_server_name, profile.name.uppercase())
                        views.setTextViewText(R.id.widget_host, "IP: ${profile.host}")

                        try {
                            val decryptedPassword = SecurityUtils.decrypt(profile.password)
                            val privateKey = profile.rsaKeyId?.let { keyId ->
                                withContext(Dispatchers.IO) {
                                    db.rsaKeyPairDao().getKeyPairById(keyId)?.privateKey
                                }
                            }

                            // Perform quick connection and telemetry check
                            withContext(Dispatchers.IO) {
                                val session = sshHelper.connect(profile.copy(password = decryptedPassword), privateKey)
                                try {
                                    val telemetry = sshHelper.fetchTelemetry(session, profile.id)
                                    val cpuNum = (telemetry.cpuUsage * 100).toInt().coerceIn(0, 100)
                                    val ramNum = (telemetry.ramUsage * 100).toInt().coerceIn(0, 100)

                                    val cpuBarsCount = (cpuNum / 10).coerceIn(1, 10)
                                    val cpuBars = "I".repeat(cpuBarsCount) + ".".repeat(10 - cpuBarsCount)

                                    val ramBarsCount = (ramNum / 10).coerceIn(1, 10)
                                    val ramBars = "I".repeat(ramBarsCount) + ".".repeat(10 - ramBarsCount)

                                    views.setTextViewText(R.id.widget_cpu_text, "$cpuBars $cpuNum%")
                                    views.setTextViewText(R.id.widget_ram_text, "$ramBars $ramNum%")
                                } finally {
                                    session.disconnect()
                                }
                            }
                        } catch (e: Exception) {
                            views.setTextViewText(R.id.widget_cpu_text, ".......... OFFLINE")
                            views.setTextViewText(R.id.widget_ram_text, ".......... OFFLINE")
                        }
                    }

                    // Tapping on the Widget launches the Main client deck
                    val clickIntent = Intent(context, MainActivity::class.java)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
