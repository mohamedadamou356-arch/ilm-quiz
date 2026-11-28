package com.ilmquiz.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {

    private lateinit var web: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demande les autorisations utiles : micro (récitation) et photos (photo de profil).
        val wanted = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) {
            wanted.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            wanted.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val missing = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 1)
        }

        web = WebView(this)
        setContentView(web)

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.webViewClient = WebViewClient()
        web.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback
                val intent = fileChooserParams?.createIntent() ?: return false
                startActivityForResult(intent, FILE_REQUEST)
                return true
            }
        }
        web.addJavascriptInterface(Bridge(this), "IlmAndroid")
        web.loadUrl(SITE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST) {
            val result = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            fileCallback?.onReceiveValue(result)
            fileCallback = null
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    companion object {
        const val SITE = "https://mohamedadamou356-arch.github.io/ilm-quiz/"
        const val FILE_REQUEST = 1001
        const val CHANNEL_ID = "ilm"
    }
}

/** Pont JavaScript : le site envoie les points, le niveau, la série et demande les notifications. */
class Bridge(private val ctx: Context) {

    @JavascriptInterface
    fun setPoints(points: Int, plan: String, nick: String) {
        val prefs = ctx.getSharedPreferences("ilm", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("points", points)
            .putString("plan", plan)
            .putString("nick", nick)
            .apply()
        refreshWidgets()
    }

    /** Niveau, XP, XP du niveau suivant et série de jours (pour le widget). */
    @JavascriptInterface
    fun setDetails(level: Int, xp: Int, nextXp: Int, streak: Int) {
        val prefs = ctx.getSharedPreferences("ilm", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("level", level)
            .putInt("xp", xp)
            .putInt("nextXp", nextXp)
            .putInt("streak", streak)
            .apply()
        refreshWidgets()
    }

    private fun refreshWidgets() {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, IlmWidget::class.java))
        IlmWidget.updateAll(ctx, mgr, ids)
    }

    /** Vrai si le téléphone autorise les notifications de l'application. */
    @JavascriptInterface
    fun notifEnabled(): Boolean {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val permOk = Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permOk && nm.areNotificationsEnabled()
    }

    /** Demande l'autorisation des notifications (Android 13 et plus). */
    @JavascriptInterface
    fun askNotif() {
        val act = ctx as? Activity ?: return
        act.runOnUiThread {
            if (Build.VERSION.SDK_INT >= 33) {
                act.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
            }
        }
    }

    /** Affiche une notification sur le téléphone. */
    @JavascriptInterface
    fun notify(title: String, body: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(MainActivity.CHANNEL_ID, "Ilm Quiz", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(
            ctx, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val note = Notification.Builder(ctx, MainActivity.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % 100000).toInt(), note)
    }
}
