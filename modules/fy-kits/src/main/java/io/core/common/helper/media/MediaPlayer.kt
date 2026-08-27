package io.core.common.helper.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
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
import java.io.IOException

// 精简状态管理
sealed class PlayerState {
    object Idle : PlayerState()
    object Preparing : PlayerState()
    data class Ready(val duration: Int) : PlayerState()
    data class Playing(val progress: Int) : PlayerState()
    data class Paused(val progress: Int) : PlayerState()
    data class Completed(val duration: Int) : PlayerState()
    data class Error(val error: Throwable) : PlayerState()
}

// 精简事件类型
sealed class PlayerEvent {
    data class ProgressChanged(val progress: Int, val duration: Int) : PlayerEvent()
    data class BufferingUpdate(val percent: Int) : PlayerEvent()
}

interface MediaPlayerController {
    val playerState: StateFlow<PlayerState>
    val playerEvents: SharedFlow<PlayerEvent>

    fun prepare(uri: Uri)
    fun playPause()
    fun seekTo(positionMs: Int)
    fun stop()
    fun release()
}

class FlowMediaPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) : MediaPlayerController, MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnBufferingUpdateListener {

    // 状态管理
    private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
    override val playerState: StateFlow<PlayerState> = _state.asStateFlow()

    // 事件管理
    private val _events = MutableSharedFlow<PlayerEvent>()
    override val playerEvents: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    override fun prepare(uri: Uri) {
        cleanupPlayer()
        _state.value = PlayerState.Preparing

        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            setOnPreparedListener(this@FlowMediaPlayer)
            setOnCompletionListener(this@FlowMediaPlayer)
            setOnErrorListener(this@FlowMediaPlayer)
            setOnBufferingUpdateListener(this@FlowMediaPlayer)
            prepareAsync()
        }
    }

    override fun playPause() {
        when (val state = _state.value) {
            is PlayerState.Ready -> startPlayback(0)
            is PlayerState.Playing -> pausePlayback()
            is PlayerState.Paused -> resumePlayback()
            else -> handleError(IllegalStateException("Cannot start from ${state::class.simpleName} state"))
        }
    }

    private fun startPlayback(startPosition: Int = 0) {
        mediaPlayer?.let { mp ->
            mp.seekTo(startPosition)
            mp.start()
            startProgressUpdates()
            _state.value = PlayerState.Playing(mp.currentPosition)
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        mediaPlayer?.currentPosition?.let { pos ->
            _state.value = PlayerState.Paused(pos)
        }
        progressJob?.cancel()
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        startProgressUpdates()
        (playerState.value as? PlayerState.Paused)?.let {
            _state.value = PlayerState.Playing(it.progress)
        }
    }

    override fun seekTo(positionMs: Int) {
        when (val state = _state.value) {
            is PlayerState.Playing, is PlayerState.Paused -> {
                mediaPlayer?.seekTo(positionMs)
                _state.value = when (state) {
                    is PlayerState.Playing -> state.copy(progress = positionMs)
                    is PlayerState.Paused -> state.copy(progress = positionMs)
                    else -> state
                }
            }

            is PlayerState.Ready -> startPlayback(positionMs) // 新增准备完成后的跳转
            is PlayerState.Completed -> startPlayback(positionMs) // 新增播放完成后的跳转
            else -> Unit
        }
    }

    override fun stop() {
        mediaPlayer?.let { mp ->
            synchronized(this) {
                mp.stop()
                cleanupPlayer()
                _state.value = PlayerState.Idle
            }
        }
    }

    override fun release() {
        stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // MediaPlayer 回调
    override fun onPrepared(mp: MediaPlayer) {
        _state.value = PlayerState.Ready(mp.duration)
    }

    override fun onCompletion(mp: MediaPlayer) {
        _state.value = PlayerState.Completed(mp.duration)
        progressJob?.cancel()
    }

    override fun onBufferingUpdate(mp: MediaPlayer?, percent: Int) {
        scope.launch { _events.emit(PlayerEvent.BufferingUpdate(percent)) }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        val error = when (what) {
            MediaPlayer.MEDIA_ERROR_IO -> IOException("I/O error")
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> IOException("Timeout error")
            else -> IllegalStateException("Media error ($what, $extra)")
        }
        _state.value = PlayerState.Error(error)
        return true
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.takeIf { it.isPlaying }?.let { mp ->
                    val pos = mp.currentPosition
                    val dur = mp.duration
                    _state.update {
                        if (it is PlayerState.Playing) it.copy(progress = pos) else it
                    }
                    _events.emit(PlayerEvent.ProgressChanged(pos, dur))
                }
                delay(1000)
            }
        }
    }

    private fun cleanupPlayer() {
        progressJob?.cancel()
        mediaPlayer?.reset()
    }

    private fun handleError(error: Throwable) = scope.launch {
        _state.value = PlayerState.Error(error)
        release()
    }

}