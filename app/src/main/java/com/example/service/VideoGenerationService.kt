package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.generator.VideoGenerator
import com.example.ui.ReelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VideoGenerationService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var notificationManager: NotificationManager
    private val channelId = "video_generation_channel"
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val surah = intent.getIntExtra("surah", 1)
        val startAyah = intent.getIntExtra("startAyah", 1)
        val endAyah = intent.getIntExtra("endAyah", 5)
        val reciterId = intent.getStringExtra("reciterId") ?: "ar.alafasy"
        val showTranslation = intent.getBooleanExtra("showTranslation", true)
        val pexelsApiKey = intent.getStringExtra("pexelsApiKey") ?: ""

        scope.launch {
            val settingsManager = com.example.settings.SettingsManager(this@VideoGenerationService)
            val language = settingsManager.language.first()
            val isArabic = language == "ar"

            // 1. Show immediate Foreground Service Notification
            startForegroundServiceState(startAyah, endAyah, isArabic)

            try {
                _serviceState.value = ReelState.Loading(
                    if (isArabic) "جاري بدء المعالجة وإنشاء القنوات السينمائية..." else "Initializing video filters...",
                    0f
                )
                val videoGenerator = VideoGenerator()

                videoGenerator.generateReel(
                    context = this@VideoGenerationService,
                    surah = surah,
                    startAyah = startAyah,
                    endAyah = endAyah,
                    reciterId = reciterId,
                    showTranslation = showTranslation,
                    pexelsApiKey = pexelsApiKey,
                    onProgress = { msg, progress ->
                        _serviceState.value = ReelState.Loading(msg, progress)
                        updateNotificationProgress(msg, progress, isArabic)
                    },
                    onComplete = { uri ->
                        _serviceState.value = ReelState.Success(uri)
                        showCompleteNotification(uri, isArabic)
                        stopForeground(true)
                        stopSelf()
                    },
                    onError = { err ->
                        _serviceState.value = ReelState.Error(err)
                        showErrorNotification(err, isArabic)
                        stopForeground(true)
                        stopSelf()
                    }
                )
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: "Unknown error occurred"
                _serviceState.value = ReelState.Error(errMsg)
                showErrorNotification(errMsg, isArabic)
                stopForeground(true)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceState(startAyah: Int, endAyah: Int, isArabic: Boolean) {
        val title = if (isArabic) "جاري تصميم مقطع ريلز القرآن..." else "Designing Quran Reel..."
        val desc = if (isArabic) "جاري معالجة الآيات من $startAyah إلى $endAyah" else "Processing verses $startAyah to $endAyah"

        // Intent to open MainActivity when clicking notification
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPendingIntent = PendingIntent.getActivity(
            this, 10, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(desc)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(appPendingIntent)
            .setProgress(100, 0, false)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun updateNotificationProgress(message: String, progress: Float, isArabic: Boolean) {
        val title = if (isArabic) "جاري تصميم المقطع السينمائي..." else "Reel render active..."
        
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPendingIntent = PendingIntent.getActivity(
            this, 10, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(appPendingIntent)
            .setProgress(100, (progress * 100).toInt(), false)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun showCompleteNotification(uri: Uri, isArabic: Boolean) {
        val title = if (isArabic) "تم تصميم المقطع بنجاح! 🎉" else "Reel rendering complete! 🎉"
        val desc = if (isArabic) "اضغط لعرض المقطع ومشاركته في الأستوديو" else "Tap to view and share from your gallery"

        // Build intent to play video
        val playIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val playPendingIntent = PendingIntent.getActivity(
            this, 11, playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(desc)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(playPendingIntent)
            .build()

        notificationManager.notify(notificationId + 1, notification)
    }

    private fun showErrorNotification(error: String, isArabic: Boolean) {
        val title = if (isArabic) "فشل تصميم مقطع الفيديو" else "Reel design failed"

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val appPendingIntent = PendingIntent.getActivity(
            this, 12, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(error)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(appPendingIntent)
            .build()

        notificationManager.notify(notificationId + 2, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "إنشاء ريلز القرآن",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "قناة إشعارات جاري معالجة فيديو ريلز القرآن الكريم"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {
        private val _serviceState = MutableStateFlow<ReelState>(ReelState.Idle)
        val serviceState: StateFlow<ReelState> = _serviceState

        fun clearState() {
            _serviceState.value = ReelState.Idle
        }
    }
}
