package com.ilmquiz.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Widget d'accueil : affiche les points et le plan, un appui ouvre l'application. */
class IlmWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAll(context, manager, appWidgetIds)
    }

    companion object {
        private fun planLabel(plan: String): String = when (plan) {
            "free" -> "Gratuit"
            "mid" -> "Moyen"
            "prem" -> "Premium"
            "sup" -> "Niveau supérieur"
            else -> "Ouvre Ilm Quiz"
        }

        fun updateAll(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
            if (ids.isEmpty()) return
            val prefs = ctx.getSharedPreferences("ilm", Context.MODE_PRIVATE)
            val points = prefs.getInt("points", 0)
            val plan = prefs.getString("plan", "") ?: ""

            val views = RemoteViews(ctx.packageName, R.layout.widget)
            views.setTextViewText(R.id.wPoints, "⭐ $points")
            views.setTextViewText(R.id.wPlan, planLabel(plan))

            val open = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pending = PendingIntent.getActivity(
                ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.wRoot, pending)

            for (id in ids) manager.updateAppWidget(id, views)
        }
    }
}
