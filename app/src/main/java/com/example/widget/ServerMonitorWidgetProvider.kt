package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import kotlin.random.Random

class ServerMonitorWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Build the update configuration details for all user instances on the Home screen
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.cyber_widget_layout)

        // Setup mock servers list of names matching the cyberpunk universe
        val servers = listOf(
            Pair("NEOTOKYO_CORE-1", "192.168.42.10"),
            Pair("NEXUS_MATRIXGATE", "149.201.2.99"),
            Pair("SHADOW_MAIN_DB", "10.0.9.44"),
            Pair("CHIPSIDE_GATEWAY", "127.0.0.1")
        )
        val selectedServer = servers[Random.nextInt(servers.size)]

        val cpuNum = Random.nextInt(12, 94)
        val ramNum = Random.nextInt(32, 88)

        // Create cyberpunk active visual bar indicator: e.g. "||||......"
        val cpuBarsCount = (cpuNum / 10).coerceAtLeast(1)
        val cpuBars = "I".repeat(cpuBarsCount) + ".".repeat(10 - cpuBarsCount)

        val ramBarsCount = (ramNum / 10).coerceAtLeast(1)
        val ramBars = "I".repeat(ramBarsCount) + ".".repeat(10 - ramBarsCount)

        // Bind text details
        views.setTextViewText(R.id.widget_server_name, selectedServer.first)
        views.setTextViewText(R.id.widget_host, "IP: ${selectedServer.second}")
        views.setTextViewText(R.id.widget_cpu_text, "$cpuBars $cpuNum%")
        views.setTextViewText(R.id.widget_ram_text, "$ramBars $ramNum%")

        // Tapping on the Widget launches the Main SSH client deck dashboard
        val clickIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // System update trigger execution
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
