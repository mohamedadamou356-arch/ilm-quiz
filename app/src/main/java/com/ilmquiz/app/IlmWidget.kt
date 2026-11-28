package com.ilmquiz.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Widget d'accueil : logo, points, niveau avec barre de progression, série et bouton Jouer. */
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
            val level = prefs.getInt("level", 1)
            val xp = prefs.getInt("xp", 0)
            val nextXp = prefs.getInt("nextXp", 100)
            val streak = prefs.getInt("streak", 0)

            // Progression vers le niveau suivant, en pourcentage
            val base = (level - 1) * (level - 1) * 100
            val span = (nextXp - base).coerceAtLeast(1)
            val percent = ((xp - base) * 100 / span).coerceIn(0, 100)

            val views = RemoteViews(ctx.packageName, R.layout.widget)
            views.setTextViewText(R.id.wPoints, "⭐ $points")
            views.setTextViewText(R.id.wPlan, planLabel(plan))
            views.setTextViewText(R.id.wLevel, "Niveau $level · $xp / $nextXp XP")
            views.setProgressBar(R.id.wBar, 100, percent, false)
            views.setTextViewText(R.id.wStreak, if (streak > 0) "🔥 $streak j" else "")
            views.setTextViewText(R.id.wButton, "Jouer")

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
