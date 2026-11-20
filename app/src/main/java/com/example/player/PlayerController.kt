package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.local.LastPlayedRecord
import com.example.data.local.SakinaPreferences
import com.example.data.local.db.DownloadEntity
import com.example.data.model.Moshaf
import com.example.data.model.Reciter
import com.example.data.model.SleepTimerState
import com.example.data.repository.DownloadRepository
import com.example.util.SurahMetadata
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PlayerUiState(
    val hasActiveItem: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val reciterId: Int = -1,
    val reciterName: String = "",
    val moshafId: Int = -1,
    val moshafName: String = "",
    val serverUrl: String = "",
    val surahList: List<Int> = emptyList(),
    val surahNumber: Int = 1,
    val surahName: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val repeatCurrent: Boolean = false,
    val isDownloadsQueue: Boolean = false
)

class PlayerController(
    private val context: Context,
    private val preferences: SakinaPreferences,
    private val downloadRepository: DownloadRepository,
    private val appScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val sleepTimerState: StateFlow<SleepTimerState> = SakinaPlaybackService.sleepTimerState
    val timerEndedEvents: SharedFlow<String> = SakinaPlaybackService.timerEndedEvents

    private val _playerErrors = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val playerErrors: SharedFlow<String> = _playerErrors.asSharedFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null
    private var restoredInitialSession = false

    private val artworkBytes: ByteArray? by lazy {
        SakinaPlaybackService.createArtworkBytes(context)
    }

    fun initialize() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, SakinaPlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller
                controller.addListener(controllerListener)
                startPositionTicker()
                if (controller.mediaItemCount > 0) {
                    syncFromController(controller)
                } else {
                    restoreLastPlayedIfNeeded()
                }
            } catch (_: Throwable) {
            }
        }, context.mainExecutor)
    }

    private fun restoreLastPlayedIfNeeded() {
        if (restoredInitialSession) return
        restoredInitialSession = true
        appScope.launch(Dispatchers.Main) {
            val last = withContext(Dispatchers.IO) { preferences.getLastPlayed() } ?: return@launch
            val controller = mediaController ?: return@launch
            if (controller.mediaItemCount > 0) return@launch

            val surahList = if (last.isDownloadsQueue) {
                val parts = last.downloadsGroupKey.split("_")
                if (parts.size == 2) {
                    val rId = parts[0].toIntOrNull() ?: last.reciterId
                    val mId = parts[1].toIntOrNull() ?: last.moshafId
                    downloadRepository.getGroupDownloads(rId, mId).map { it.surahNumber }
                        .ifEmpty { listOf(last.surahNumber) }
                } else {
                    listOf(last.surahNumber)
                }
            } else {
                last.surahListCsv.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 1..114 }
                    .ifEmpty { (1..114).toList() }
            }

            val moshaf = Moshaf(
                id = last.moshafId,
                name = last.moshafName,
                server = last.serverUrl,
                surahList = surahList
            )
            val reciter = Reciter(
                id = last.reciterId,
                name = last.reciterName,
                letter = last.reciterName.firstOrNull()?.toString() ?: "ق",
                moshafs = listOf(moshaf)
            )
            preparePlaylist(
                reciter = reciter,
                moshaf = moshaf,
                availableSurahs = surahList,
                startSurah = last.surahNumber,
                startPositionMs = last.positionMs,
                autoPlay = false,
                speed = last.playbackSpeed,
                repeatCurrent = last.repeatCurrent,
                isDownloadsQueue = last.isDownloadsQueue,
                downloadsGroupKey = last.downloadsGroupKey
            )
        }
    }

    fun playSurah(
        reciter: Reciter,
        moshaf: Moshaf,
        surahNumber: Int,
        startPositionMs: Long = 0L
    ) {
        appScope.launch(Dispatchers.Main) {
            val current = _uiState.value
            val speed = current.playbackSpeed
            val repeat = current.repeatCurrent
            preparePlaylist(
                reciter = reciter,
                moshaf = moshaf,
                availableSurahs = moshaf.availableSurahs(),
                startSurah = surahNumber,
                startPositionMs = startPositionMs,
                autoPlay = true,
                speed = speed,
                repeatCurrent = repeat,
                isDownloadsQueue = false,
                downloadsGroupKey = ""
            )
        }
    }

    fun playDownloadedGroup(downloads: List<DownloadEntity>, startSurahNumber: Int? = null) {
        if (downloads.isEmpty()) return
        val sorted = downloads.sortedBy { it.surahNumber }
        val first = sorted.first()
        val surahNumbers = sorted.map { it.surahNumber }
        val targetSurah = startSurahNumber ?: first.surahNumber

        val moshaf = Moshaf(
            id = first.moshafId,
            name = first.moshafName,
            server = first.serverUrl,
            surahList = surahNumbers
        )
        val reciter = Reciter(
            id = first.reciterId,
            name = first.reciterName,
            letter = first.reciterName.firstOrNull()?.toString() ?: "ق",
            moshafs = listOf(moshaf)
        )
        appScope.launch(Dispatchers.Main) {
            val current = _uiState.value
            preparePlaylist(
                reciter = reciter,
                moshaf = moshaf,
                availableSurahs = surahNumbers,
                startSurah = targetSurah,
                startPositionMs = 0L,
                autoPlay = true,
                speed = current.playbackSpeed,
                repeatCurrent = current.repeatCurrent,
                isDownloadsQueue = true,
                downloadsGroupKey = first.groupKey
            )
        }
    }

    private suspend fun preparePlaylist(
        reciter: Reciter,
        moshaf: Moshaf,
        availableSurahs: List<Int>,
        startSurah: Int,
        startPositionMs: Long,
        autoPlay: Boolean,
        speed: Float,
        repeatCurrent: Boolean,
        isDownloadsQueue: Boolean,
        downloadsGroupKey: String
    ) {
        val controller = mediaController ?: run {
            initialize()
            return
        }

        val mediaItems = withContext(Dispatchers.IO) {
            availableSurahs.map { surahNum ->
                val localFile: File? = downloadRepository.findLocalFileIfDownloaded(
                    reciterId = reciter.id,
                    moshafId = moshaf.id,
                    surahNumber = surahNum
                )
                val uri = if (localFile != null) {
                    Uri.fromFile(localFile)
                } else {
                    Uri.parse(moshaf.audioUrlForSurah(surahNum))
                }
                val surahName = SurahMetadata.byNumber(surahNum).name
                val extras = Bundle().apply {
                    putInt(SakinaPlaybackService.EXTRA_RECITER_ID, reciter.id)
                    putString(SakinaPlaybackService.EXTRA_RECITER_NAME, reciter.name)
                    putInt(SakinaPlaybackService.EXTRA_MOSHAF_ID, moshaf.id)
                    putString(SakinaPlaybackService.EXTRA_MOSHAF_NAME, moshaf.name)
                    putInt(SakinaPlaybackService.EXTRA_SURAH_NUMBER, surahNum)
                }
                val metaBuilder = MediaMetadata.Builder()
                    .setTitle("سورة $surahName")
                    .setArtist(reciter.name)
                    .setAlbumTitle(moshaf.name)
                    .setExtras(extras)
                artworkBytes?.let { bytes ->
                    metaBuilder.setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
                MediaItem.Builder()
                    .setMediaId("${reciter.id}_${moshaf.id}_$surahNum")
                    .setUri(uri)
                    .setMediaMetadata(metaBuilder.build())
                    .build()
            }
        }

        val startIndex = availableSurahs.indexOf(startSurah).coerceAtLeast(0)

        _uiState.update {
            it.copy(
                hasActiveItem = true,
                reciterId = reciter.id,
                reciterName = reciter.name,
                moshafId = moshaf.id,
                moshafName = moshaf.name,
                serverUrl = moshaf.normalizedServer(),
                surahList = availableSurahs,
                surahNumber = availableSurahs.getOrElse(startIndex) { startSurah },
                surahName = SurahMetadata.byNumber(availableSurahs.getOrElse(startIndex) { startSurah }).name,
                positionMs = startPositionMs,
                playbackSpeed = speed,
                repeatCurrent = repeatCurrent,
                isDownloadsQueue = isDownloadsQueue
            )
        }

        preferences.saveLastPlayed(
            LastPlayedRecord(
                reciterId = reciter.id,
                reciterName = reciter.name,
                moshafId = moshaf.id,
                moshafName = moshaf.name,
                serverUrl = moshaf.normalizedServer(),
                surahListCsv = availableSurahs.joinToString(","),
                surahNumber = availableSurahs.getOrElse(startIndex) { startSurah },
                positionMs = startPositionMs,
                playbackSpeed = speed,
                repeatCurrent = repeatCurrent,
                isDownloadsQueue = isDownloadsQueue,
                downloadsGroupKey = downloadsGroupKey
            )
        )

        withContext(Dispatchers.Main) {
            controller.setMediaItems(mediaItems, startIndex, startPositionMs)
            controller.setPlaybackSpeed(speed)
            controller.repeatMode = if (repeatCurrent) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            controller.prepare()
            controller.playWhenReady = autoPlay
        }
    }

    fun togglePlayPause() {
        val c = mediaController ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            c.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs.coerceAtLeast(0L))
        _uiState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun seekBack15() {
        mediaController?.seekBack()
    }

    fun seekForward15() {
        mediaController?.seekForward()
    }

    fun skipPrevious() {
        val c = mediaController ?: return
        if (c.hasPreviousMediaItem()) {
            c.seekToPreviousMediaItem()
        } else {
            c.seekTo(0L)
        }
    }

    fun skipNext() {
        val c = mediaController ?: return
        if (c.hasNextMediaItem()) {
            c.seekToNextMediaItem()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val c = mediaController ?: return
        c.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
        appScope.launch {
            preferences.savePlaybackOptions(speed, _uiState.value.repeatCurrent)
        }
    }

    fun toggleRepeatCurrent() {
        val c = mediaController ?: return
        val next = !_uiState.value.repeatCurrent
        c.repeatMode = if (next) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _uiState.update { it.copy(repeatCurrent = next) }
        appScope.launch {
            preferences.savePlaybackOptions(_uiState.value.playbackSpeed, next)
        }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        val c = mediaController ?: return
        val args = Bundle().apply { putInt(SakinaPlaybackService.EXTRA_MINUTES, minutes) }
        c.sendCustomCommand(SessionCommand(SakinaPlaybackService.ACTION_SET_TIMER_DURATION, Bundle.EMPTY), args)
    }

    fun setSleepTimerAtEpoch(targetEpochMs: Long) {
        val c = mediaController ?: return
        val args = Bundle().apply { putLong(SakinaPlaybackService.EXTRA_TARGET_EPOCH, targetEpochMs) }
        c.sendCustomCommand(SessionCommand(SakinaPlaybackService.ACTION_SET_TIMER_CLOCK, Bundle.EMPTY), args)
    }

    fun setSleepTimerEndOfSurah() {
        val c = mediaController ?: return
        c.sendCustomCommand(SessionCommand(SakinaPlaybackService.ACTION_SET_TIMER_END_OF_SURAH, Bundle.EMPTY), Bundle.EMPTY)
    }

    fun cancelSleepTimer() {
        val c = mediaController ?: return
        c.sendCustomCommand(SessionCommand(SakinaPlaybackService.ACTION_CANCEL_TIMER, Bundle.EMPTY), Bundle.EMPTY)
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = appScope.launch(Dispatchers.Main) {
            while (isActive) {
                val c = mediaController
                if (c != null && c.mediaItemCount > 0) {
                    val dur = c.duration.let { if (it > 0L) it else 0L }
                    val pos = c.currentPosition.coerceAtLeast(0L)
                    _uiState.update {
                        it.copy(
                            positionMs = pos,
                            durationMs = dur,
                            isPlaying = c.isPlaying,
                            isBuffering = c.playbackState == Player.STATE_BUFFERING
                        )
                    }
                }
                delay(400L)
            }
        }
    }

    private fun syncFromController(c: MediaController) {
        val item = c.currentMediaItem
        val extras = item?.mediaMetadata?.extras
        val surahNum = extras?.getInt(SakinaPlaybackService.EXTRA_SURAH_NUMBER, -1) ?: -1
        val reciterId = extras?.getInt(SakinaPlaybackService.EXTRA_RECITER_ID, -1) ?: -1
        val reciterName = extras?.getString(SakinaPlaybackService.EXTRA_RECITER_NAME).orEmpty()
        val moshafId = extras?.getInt(SakinaPlaybackService.EXTRA_MOSHAF_ID, -1) ?: -1
        val moshafName = extras?.getString(SakinaPlaybackService.EXTRA_MOSHAF_NAME).orEmpty()

        if (surahNum in 1..114 && reciterId > 0) {
            _uiState.update {
                it.copy(
                    hasActiveItem = true,
                    isPlaying = c.isPlaying,
                    isBuffering = c.playbackState == Player.STATE_BUFFERING,
                    reciterId = reciterId,
                    reciterName = reciterName,
                    moshafId = moshafId,
                    moshafName = moshafName,
                    surahNumber = surahNum,
                    surahName = SurahMetadata.byNumber(surahNum).name,
                    positionMs = c.currentPosition.coerceAtLeast(0L),
                    durationMs = c.duration.let { d -> if (d > 0L) d else 0L },
                    repeatCurrent = c.repeatMode == Player.REPEAT_MODE_ONE,
                    playbackSpeed = c.playbackParameters.speed
                )
            }
        }
    }

    private val controllerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            mediaController?.let { syncFromController(it) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            mediaController?.let { syncFromController(it) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaController?.let { syncFromController(it) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playerErrors.tryEmit("تعذّر تشغيل التلاوة. تحقق من الاتصال بالإنترنت")
        }
    }
}
