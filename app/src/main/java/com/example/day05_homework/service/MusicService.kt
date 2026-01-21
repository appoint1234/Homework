package com.example.day05_homework.service

import android.app.Service
import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFocusRequest
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.day05_homework.R
import com.example.day05_homework.model.Track


import kotlin.math.sqrt

class MusicService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID = "music_playback"
        const val NOTIF_ID = 1001

        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_NEXT = "action_next"
        const val ACTION_STOP = "action_stop"
    }

    // -------- Binder: Activity 绑定后直接调用 service 控制 ----------
    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
    private val binder = MusicBinder()

    // -------- 播放器 ----------
    private var player: MediaPlayer? = null
    private var prepared = false

    val playlist = listOf(
        Track("A", "Me", R.raw.music1),
        Track("B", "Me", R.raw.music2),
        Track("C", "Me", R.raw.music3),
    )
    private var index = 0

    // -------- 音频焦点 ----------
    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> setVolume(0.2f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                setVolume(1.0f)
                // 不强制自动播放，你也可以选择 resume()
            }
        }
    }

    // -------- 摇一摇 ----------
    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var lastShakeTime = 0L

    // 经验阈值：12~16 比较合适
    private val shakeThreshold = 13.5
    private val shakeCooldownMs = 900L

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        createNotificationChannel()
        registerShakeSensor()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> next()
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                // 第一次启动 service：确保前台
                startForeground(NOTIF_ID, buildNotification())
            }
        }
        return START_STICKY
    }

    // ------------------ 对外控制 API（Activity 调用） ------------------

    fun togglePlayPause() {
        if (isPlaying()) pause() else play()
    }

    fun play() {
        if (!requestFocus()) return

        val p = player
        if (p == null) {
            prepareAndPlay(index)
        } else {
            if (prepared) {
                p.start()
                updateNotification()
            } else {
                // 还没准备好就忽略
            }
        }
    }

    fun pause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                updateNotification()
            }
        }
        abandonFocus()
    }

    fun next() {
        index = (index + 1) % playlist.size
        prepareAndPlay(index)
    }

    fun seekTo(ms: Int) {
        if (prepared) player?.seekTo(ms)
    }

    fun duration(): Int = if (prepared) (player?.duration ?: 0) else 0
    fun position(): Int = if (prepared) (player?.currentPosition ?: 0) else 0
    fun currentTrack(): Track = playlist[index]
    fun isPlaying(): Boolean = player?.isPlaying == true

    // ------------------ 播放实现 ------------------

    private fun prepareAndPlay(i: Int) {
        releasePlayer()

        if (!requestFocus()) return

        val track = playlist[i]

        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setOnPreparedListener {
                prepared = true
                start()
                updateNotification()
            }

            setOnCompletionListener {
                // 自动下一曲
                next()
            }

            setOnErrorListener { _, _, _ ->
                prepared = false
                updateNotification()
                true
            }

            try {
                val afd = resources.openRawResourceFd(track.resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepared = false
                prepareAsync()
                updateNotification()
            } catch (e: Exception) {
                prepared = false
            }
        }
    }

    private fun setVolume(v: Float) {
        player?.setVolume(v, v)
    }

    private fun releasePlayer() {
        prepared = false
        player?.release()
        player = null
    }

    // ------------------ 前台通知 ------------------

    private fun buildNotification(): Notification {
        val playPauseAction = if (isPlaying()) "暂停" else "播放"
        val playPauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val t = currentTrack()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note) // 你自己放个小图标
            .setContentTitle(t.title)
            .setContentText("${t.artist}  •  ${if (isPlaying()) "Playing" else "Paused"}")
            .setOngoing(isPlaying())
            .addAction(0, playPauseAction, playPauseIntent)
            .addAction(0, "下一曲", nextIntent)
            .addAction(0, "停止", stopIntent)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
        // 前台服务必须持续有通知，保险起见保持前台
        startForeground(NOTIF_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
    }

    // ------------------ 音频焦点 ------------------

    private fun requestFocus(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .build()
            focusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }


    private fun abandonFocus() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }


    // ------------------ 摇一摇 ------------------

    private fun registerShakeSensor() {
        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun unregisterShakeSensor() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val z = event.values[2].toDouble()
        val g = sqrt(x * x + y * y + z * z) // 简单强度

        val now = System.currentTimeMillis()
        if (g > shakeThreshold && now - lastShakeTime > shakeCooldownMs) {
            lastShakeTime = now
            next()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        unregisterShakeSensor()
        pause()
        releasePlayer()
        super.onDestroy()
    }
}