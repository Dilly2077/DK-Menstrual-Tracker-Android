package com.dkcycle.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal enum class LunaraWidgetKind { STATUS, CALENDAR }

internal fun requestLunaraWidget(context: Context, kind: LunaraWidgetKind): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    val provider = when (kind) {
        LunaraWidgetKind.STATUS -> ComponentName(context, CycleStatusWidgetProvider::class.java)
        LunaraWidgetKind.CALENDAR -> ComponentName(context, CalendarWidgetProvider::class.java)
    }
    return manager.requestPinAppWidget(provider, null, null)
}

internal object LunaraWidgetUpdater {
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val status = manager.getAppWidgetIds(ComponentName(appContext, CycleStatusWidgetProvider::class.java))
        if (status.isNotEmpty()) CycleStatusWidgetProvider.update(appContext, manager, status)
        val calendars = manager.getAppWidgetIds(ComponentName(appContext, CalendarWidgetProvider::class.java))
        if (calendars.isNotEmpty()) CalendarWidgetProvider.update(appContext, manager, calendars)
    }
}

private fun launchIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class CycleStatusWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        update(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val logs = LocalStore(context).loadLogs()
            val analysis = CycleEngine.analyse(logs.values.toList())
            val click = launchIntent(context)
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_cycle_status)
                views.setTextViewText(R.id.widget_phase, analysis.phase)
                views.setTextViewText(
                    R.id.widget_cycle_day,
                    analysis.cycleDay?.let { "Cycle day $it" } ?: "Learning your cycle"
                )
                views.setTextViewText(
                    R.id.widget_next_period,
                    analysis.nextPeriodStart?.let { "Next period · ${it.format(shortDate())}" } ?: "Next period · learning"
                )
                views.setTextViewText(
                    R.id.widget_confidence,
                    if (analysis.nextPeriodStart == null) "Log a period start to begin" else "${analysis.confidence} confidence"
                )
                views.setOnClickPendingIntent(R.id.widget_status_root, click)
                manager.updateAppWidget(id, views)
            }
        }
    }
}

class CalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        update(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        private val dayIds = intArrayOf(
            R.id.widget_day_1,
            R.id.widget_day_2,
            R.id.widget_day_3,
            R.id.widget_day_4,
            R.id.widget_day_5,
            R.id.widget_day_6,
            R.id.widget_day_7
        )

        fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val logs = LocalStore(context).loadLogs()
            val analysis = CycleEngine.analyse(logs.values.toList())
            val month = YearMonth.now()
            val first = month.atDay(1)
            val offset = first.dayOfWeek.value - 1
            val dates = List<LocalDate?>(offset) { null } + (1..month.lengthOfMonth()).map(month::atDay)
            val padded = dates + List((42 - dates.size).coerceAtLeast(0)) { null }
            val click = launchIntent(context)

            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_calendar)
                views.setTextViewText(
                    R.id.widget_month,
                    "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"
                )
                views.removeAllViews(R.id.widget_calendar_rows)
                padded.chunked(7).take(6).forEach { week ->
                    val row = RemoteViews(context.packageName, R.layout.widget_calendar_row)
                    week.forEachIndexed { index, date ->
                        val viewId = dayIds[index]
                        if (date == null) {
                            row.setTextViewText(viewId, "")
                            row.setInt(viewId, "setBackgroundResource", R.drawable.widget_day_clear)
                        } else {
                            row.setTextViewText(viewId, date.dayOfMonth.toString())
                            val style = dayStyle(date, logs[date], analysis)
                            row.setInt(viewId, "setBackgroundResource", style.background)
                            row.setTextColor(viewId, style.textColor)
                        }
                    }
                    views.addView(R.id.widget_calendar_rows, row)
                }
                views.setTextViewText(
                    R.id.widget_calendar_footer,
                    analysis.nextPeriodStart?.let { "Next period ${it.format(shortDate())}" } ?: "Learning your cycle"
                )
                views.setOnClickPendingIntent(R.id.widget_calendar_root, click)
                manager.updateAppWidget(id, views)
            }
        }

        private data class DayStyle(val background: Int, val textColor: Int)

        private fun dayStyle(date: LocalDate, log: DailyLog?, analysis: CycleAnalysis): DayStyle {
            val logged = log?.flow != null && log.flow != FlowIntensity.NONE
            val predicted = analysis.nextPeriodStart != null && analysis.predictedPeriodEnd != null &&
                !date.isBefore(analysis.nextPeriodStart) && !date.isAfter(analysis.predictedPeriodEnd)
            val ovulation = date == analysis.estimatedOvulation
            val fertile = analysis.fertileStart != null && analysis.fertileEnd != null &&
                !date.isBefore(analysis.fertileStart) && !date.isAfter(analysis.fertileEnd) && !ovulation
            val luteal = analysis.lutealStart != null && analysis.lutealEnd != null &&
                !date.isBefore(analysis.lutealStart) && !date.isAfter(analysis.lutealEnd)

            return when {
                logged -> DayStyle(R.drawable.widget_day_logged, AndroidColor.WHITE)
                predicted -> DayStyle(R.drawable.widget_day_predicted, AndroidColor.rgb(52, 31, 40))
                ovulation -> DayStyle(R.drawable.widget_day_ovulation, AndroidColor.WHITE)
                fertile -> DayStyle(R.drawable.widget_day_fertile, AndroidColor.rgb(52, 31, 40))
                luteal -> DayStyle(R.drawable.widget_day_luteal, AndroidColor.rgb(52, 31, 40))
                else -> DayStyle(R.drawable.widget_day_clear, AndroidColor.rgb(52, 31, 40))
            }
        }
    }
}
