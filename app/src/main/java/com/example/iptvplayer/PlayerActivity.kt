package com.example.iptvplayer

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.iptvplayer.databinding.ActivityPlayerBinding

@OptIn(UnstableApi::class)
class PlayerActivity : AppCompatActivity() {

    private lateinit var b: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideTitle = Runnable { b.title.isVisible = false }

    private var channels: List<Channel> = emptyList()
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(b.root)

        channels = PlaylistHolder.channels
        index = PlaylistHolder.index
        if (channels.isEmpty()) {
            finish()
            return
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        b.error.setOnClickListener {
            b.error.isVisible = false
            player?.prepare()
            player?.playWhenReady = true
        }

        val tv = isTv()
        b.btnNext.setOnClickListener { zap(1) }
        b.btnPrev.setOnClickListener { zap(-1) }
        b.playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility ->
                if (!tv) b.zapButtons.isVisible = visibility == View.VISIBLE
            }
        )

        hideSystemUi()
        showTitle()
    }

    override fun onStart() {
        super.onStart()
        if (channels.isNotEmpty()) initPlayer()
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (channels.isNotEmpty()) handler.removeCallbacks(hideTitle)
    }

    private fun initPlayer() {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android) IPTVPlayer/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val p = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .build()

        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                b.progress.isVisible = state == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                b.progress.isVisible = false
                b.error.text = getString(R.string.playback_error, error.errorCodeName)
                b.error.isVisible = true
            }
        })

        b.playerView.player = p
        player = p
        playCurrent()
    }

    private fun playCurrent() {
        val p = player ?: return
        p.setMediaItem(MediaItem.fromUri(channels[index].url))
        p.prepare()
        p.playWhenReady = true
    }

    /** delta = +1 : chaîne suivante, -1 : chaîne précédente (boucle sur la liste). */
    private fun zap(delta: Int) {
        index = (index + delta + channels.size) % channels.size
        PlaylistHolder.index = index
        b.error.isVisible = false
        playCurrent()
        showTitle()
    }

    private fun showTitle() {
        val c = channels[index]
        b.title.text = "${index + 1}/${channels.size}   ${c.name}"
        b.title.isVisible = true
        handler.removeCallbacks(hideTitle)
        handler.postDelayed(hideTitle, 4000)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (channels.isNotEmpty() && event.action == KeyEvent.ACTION_DOWN) {
            val controllerShown = b.playerView.isControllerFullyVisible
            when (event.keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> { zap(1); return true }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> { zap(-1); return true }
                KeyEvent.KEYCODE_DPAD_UP -> if (!controllerShown) { zap(1); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> if (!controllerShown) { zap(-1); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isTv(): Boolean {
        val m = getSystemService(UI_MODE_SERVICE) as UiModeManager
        return m.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, b.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
