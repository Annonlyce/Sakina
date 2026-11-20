package com.example.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.SakinaApp
import com.example.data.model.SleepTimerMode
import com.example.data.model.SleepTimerState
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class SakinaPlaybackService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private var timerJob: Job? = null
    private var trackingJob: Job? = null

    private var pendingStatsReciterId: Int = -1
    private var pendingStatsReciterName: String = ""
    private var pendingStatsSurah: Int = -1
    private var pendingStatsSeconds: Long = 0L

    private var activeTimerMode: SleepTimerMode = SleepTimerMode.NONE
    private var timerTargetRealtimeMs: Long = 0L
    private var timerTargetEpochMs: Long = 0L
    private var endOfSurahTargetItemId: String? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        ensurePlaybackChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(15_000L)
            .setSeekForwardIncrementMs(15_000L)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        exoPlayer.addListener(playerListener)
        player = exoPlayer

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(SessionCallback())
            .build()

        startListeningTracker()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = player
        if (currentPlayer == null || !currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        flushPendingStatsSync()
        saveCurrentPosition()
        timerJob?.cancel()
        trackingJob?.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_SET_TIMER_DURATION, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SET_TIMER_CLOCK, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SET_TIMER_END_OF_SURAH, Bundle.EMPTY))
                .add(SessionCommand(ACTION_CANCEL_TIMER, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_SET_TIMER_DURATION -> {
                    val minutes = args.getInt(EXTRA_MINUTES, 15).coerceAtLeast(1)
                    startDurationTimer(minutes * 60_000L, SleepTimerMode.DURATION, 0L)
                }
                ACTION_SET_TIMER_CLOCK -> {
                    val targetEpoch = args.getLong(EXTRA_TARGET_EPOCH, 0L)
                    val deltaMs = (targetEpoch - System.currentTimeMillis()).coerceAtLeast(1_000L)
                    startDurationTimer(deltaMs, SleepTimerMode.CLOCK_TIME, targetEpoch)
                }
                ACTION_SET_TIMER_END_OF_SURAH -> {
                    startEndOfSurahTimer()
                }
                ACTION_CANCEL_TIMER -> {
                    cancelSleepTimer(resetVolume = true)
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) {
                flushPendingStatsSync()
                saveCurrentPosition()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            flushPendingStatsSync()
            if (activeTimerMode == SleepTimerMode.END_OF_SURAH &&
                endOfSurahTargetItemId != null &&
                mediaItem?.mediaId != endOfSurahTargetItemId
            ) {
                // Surah changed while waiting for end of current surah -> pause at start of next item
                player?.pause()
                player?.seekTo(0L)
                finishSleepTimer()
            }
            saveCurrentMetadataAndPosition(mediaItem)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && activeTimerMode == SleepTimerMode.END_OF_SURAH) {
                finishSleepTimer()
            }
        }
    }

    private fun startDurationTimer(
        durationMs: Long,
        mode: SleepTimerMode,
        targetEpoch: Long
    ) {
        timerJob?.cancel()
        player?.volume = 1f
        activeTimerMode = mode
        timerTargetRealtimeMs = SystemClock.elapsedRealtime() + durationMs
        timerTargetEpochMs = targetEpoch
        endOfSurahTargetItemId = null

        timerJob = serviceScope.launch {
            while (isActive) {
                val remainingMs = timerTargetRealtimeMs - SystemClock.elapsedRealtime()
                if (remainingMs <= 0L) {
                    player?.pause()
                    finishSleepTimer()
                    break
                }
                if (remainingMs <= FADE_OUT_MS) {
                    val fraction = (remainingMs.toFloat() / FADE_OUT_MS.toFloat()).coerceIn(0f, 1f)
                    player?.volume = fraction
                } else if (player?.volume != 1f) {
                    player?.volume = 1f
                }

                _sleepTimerState.value = SleepTimerState(
                    mode = mode,
                    remainingSeconds = (remainingMs + 999L) / 1000L,
                    targetEpochMs = targetEpoch,
                    isActive = true
                )
                delay(250L)
            }
        }
    }

    private fun startEndOfSurahTimer() {
        timerJob?.cancel()
        val currentPlayer = player
        val currentItem = currentPlayer?.currentMediaItem
        if (currentPlayer == null || currentItem == null) {
            return
        }
        currentPlayer.volume = 1f
        activeTimerMode = SleepTimerMode.END_OF_SURAH
        endOfSurahTargetItemId = currentItem.mediaId
        timerTargetRealtimeMs = 0L
        timerTargetEpochMs = 0L

        timerJob = serviceScope.launch {
            while (isActive) {
                val p = player ?: break
                val duration = p.duration
                val position = p.currentPosition
                val remainingMs = if (duration > 0L && duration != C.TIME_UNSET) {
                    (duration - position).coerceAtLeast(0L)
                } else {
                    -1L
                }

                if (remainingMs in 0L..FADE_OUT_MS && p.isPlaying) {
                    val fraction = (remainingMs.toFloat() / FADE_OUT_MS.toFloat()).coerceIn(0f, 1f)
                    p.volume = fraction
                } else if (p.volume != 1f) {
                    p.volume = 1f
                }

                _sleepTimerState.value = SleepTimerState(
                    mode = SleepTimerMode.END_OF_SURAH,
                    remainingSeconds = if (remainingMs >= 0L) (remainingMs + 999L) / 1000L else 0L,
                    targetEpochMs = 0L,
                    isActive = true
                )
                delay(250L)
            }
        }
    }

    private fun finishSleepTimer() {
        cancelSleepTimer(resetVolume = true)
        _timerEndedEvents.tryEmit("انتهى المؤقّت")
    }

    private fun cancelSleepTimer(resetVolume: Boolean) {
        timerJob?.cancel()
        timerJob = null
        activeTimerMode = SleepTimerMode.NONE
        timerTargetRealtimeMs = 0L
        timerTargetEpochMs = 0L
        endOfSurahTargetItemId = null
        if (resetVolume) {
            player?.volume = 1f
        }
        _sleepTimerState.value = SleepTimerState()
    }

    private fun startListeningTracker() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            var posSaveCounter = 0
            while (isActive) {
                delay(1000L)
                val p = player ?: continue
                if (p.isPlaying) {
                    val extras = p.currentMediaItem?.mediaMetadata?.extras
                    val reciterId = extras?.getInt(EXTRA_RECITER_ID, -1) ?: -1
                    val reciterName = extras?.getString(EXTRA_RECITER_NAME).orEmpty()
                    val surahNumber = extras?.getInt(EXTRA_SURAH_NUMBER, -1) ?: -1

                    if (reciterId > 0 && surahNumber in 1..114) {
                        if (pendingStatsReciterId == reciterId && pendingStatsSurah == surahNumber) {
                            pendingStatsSeconds += 1L
                        } else {
                            flushPendingStatsSync()
                            pendingStatsReciterId = reciterId
                            pendingStatsReciterName = reciterName
                            pendingStatsSurah = surahNumber
                            pendingStatsSeconds = 1L
                        }
                        if (pendingStatsSeconds >= 5L) {
                            flushPendingStatsSync()
                        }
                    }
                    posSaveCounter++
                    if (posSaveCounter >= 5) {
                        posSaveCounter = 0
                        saveCurrentPosition()
                    }
                }
            }
        }
    }

    private fun flushPendingStatsSync() {
        val rId = pendingStatsReciterId
        val rName = pendingStatsReciterName
        val surah = pendingStatsSurah
        val secs = pendingStatsSeconds
        if (rId > 0 && surah in 1..114 && secs > 0L) {
            pendingStatsSeconds = 0L
            val app = application as? SakinaApp ?: return
            app.container.appScope.launch {
                app.container.journeyRepository.recordListening(rId, rName, surah, secs)
            }
        }
    }

    private fun saveCurrentPosition() {
        val p = player ?: return
        val pos = p.currentPosition.coerceAtLeast(0L)
        val app = application as? SakinaApp ?: return
        app.container.appScope.launch {
            app.container.preferences.savePlaybackPosition(pos)
        }
    }

    private fun saveCurrentMetadataAndPosition(mediaItem: MediaItem?) {
        val extras = mediaItem?.mediaMetadata?.extras ?: return
        val reciterId = extras.getInt(EXTRA_RECITER_ID, -1)
        val surahNumber = extras.getInt(EXTRA_SURAH_NUMBER, -1)
        if (reciterId <= 0 || surahNumber !in 1..114) return
        val app = application as? SakinaApp ?: return
        val p = player ?: return
        val currentPos = p.currentPosition.coerceAtLeast(0L)
        val reciterName = extras.getString(EXTRA_RECITER_NAME).orEmpty()
        val moshafId = extras.getInt(EXTRA_MOSHAF_ID, -1)
        val moshafName = extras.getString(EXTRA_MOSHAF_NAME).orEmpty()
        app.container.appScope.launch {
            val existing = app.container.preferences.getLastPlayed()
            if (existing != null) {
                app.container.preferences.saveLastPlayed(
                    existing.copy(
                        reciterId = reciterId,
                        reciterName = reciterName,
                        moshafId = if (moshafId >= 0) moshafId else existing.moshafId,
                        moshafName = moshafName,
                        surahNumber = surahNumber,
                        positionMs = currentPos
                    )
                )
            }
        }
    }

    private fun ensurePlaybackChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(PLAYBACK_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                getString(R.string.notification_channel_playback_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_playback_desc)
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "sakina_playback"
        private const val FADE_OUT_MS = 20_000L

        const val ACTION_SET_TIMER_DURATION = "com.example.sakina.SET_TIMER_DURATION"
        const val ACTION_SET_TIMER_CLOCK = "com.example.sakina.SET_TIMER_CLOCK"
        const val ACTION_SET_TIMER_END_OF_SURAH = "com.example.sakina.SET_TIMER_END_OF_SURAH"
        const val ACTION_CANCEL_TIMER = "com.example.sakina.CANCEL_TIMER"

        const val EXTRA_MINUTES = "minutes"
        const val EXTRA_TARGET_EPOCH = "target_epoch"

        const val EXTRA_RECITER_ID = "reciter_id"
        const val EXTRA_RECITER_NAME = "reciter_name"
        const val EXTRA_MOSHAF_ID = "moshaf_id"
        const val EXTRA_MOSHAF_NAME = "moshaf_name"
        const val EXTRA_SURAH_NUMBER = "surah_number"

        private val _sleepTimerState = MutableStateFlow(SleepTimerState())
        val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

        private val _timerEndedEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
        val timerEndedEvents: SharedFlow<String> = _timerEndedEvents.asSharedFlow()

        fun createArtworkBytes(context: Context): ByteArray? {
            return try {
                val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
                val size = 256
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                bitmap.recycle()
                stream.toByteArray()
            } catch (_: Throwable) {
                null
            }
        }
    }
}
