package com.example.iptvplayer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.iptvplayer.databinding.ActivityMainBinding
import com.example.iptvplayer.databinding.DialogSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs
    private val adapter = ChannelAdapter { openPlayer(it) }
    private var all: List<Channel> = emptyList()
    private var selectedGroup = ALL

    private class Outcome(val channels: List<Channel>, val error: String?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)

        b.toolbar.inflateMenu(R.menu.main)
        b.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_settings -> { showSettings(); true }
                R.id.action_refresh -> { loadPlaylist(); true }
                else -> false
            }
        }

        b.recycler.layoutManager = LinearLayoutManager(this)
        b.recycler.adapter = adapter
        b.search.doAfterTextChanged { applyFilter() }
        b.groupSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedGroup = p?.getItemAtPosition(pos) as? String ?: ALL
                applyFilter()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        if (prefs.playlistUrl() == null) showSettings() else loadPlaylist()
    }

    private fun showSettings() {
        val d = DialogSettingsBinding.inflate(layoutInflater)
        d.inputUrl.setText(prefs.serverUrl)
        d.inputUser.setText(prefs.username)
        d.inputPass.setText(prefs.password)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setView(d.root)
            .setPositiveButton(R.string.save) { _, _ ->
                prefs.serverUrl = d.inputUrl.text?.toString().orEmpty()
                prefs.username = d.inputUser.text?.toString().orEmpty()
                prefs.password = d.inputPass.text?.toString().orEmpty()
                loadPlaylist()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadPlaylist() {
        val url = prefs.playlistUrl() ?: run { showSettings(); return }
        val cacheFile = File(cacheDir, "playlist.m3u")
        b.progress.isVisible = true
        b.empty.isVisible = false

        lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                try {
                    val text = download(url)
                    val list = M3uParser.parse(text)
                    if (list.isNotEmpty()) cacheFile.writeText(text)
                    Outcome(list, null)
                } catch (e: Exception) {
                    val cached =
                        if (cacheFile.exists()) M3uParser.parse(cacheFile.readText()) else emptyList()
                    Outcome(cached, e.message ?: e.javaClass.simpleName)
                }
            }
            b.progress.isVisible = false
            all = outcome.channels
            outcome.error?.let {
                Toast.makeText(this@MainActivity, getString(R.string.load_error, it), Toast.LENGTH_LONG).show()
            }

            val groups = listOf(ALL) + all.map { it.group }.distinct().sorted()
            selectedGroup = ALL
            b.groupSpinner.adapter = ArrayAdapter(
                this@MainActivity, android.R.layout.simple_spinner_dropdown_item, groups
            )
            applyFilter()
            b.empty.isVisible = all.isEmpty()
        }
    }

    private fun applyFilter() {
        val q = b.search.text?.toString()?.trim().orEmpty()
        val filtered = all.filter {
            (selectedGroup == ALL || it.group == selectedGroup) &&
                (q.isEmpty() || it.name.contains(q, ignoreCase = true))
        }
        adapter.submitList(filtered)
        b.count.text = getString(R.string.channels_count, filtered.size)
    }

    private fun download(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) IPTVPlayer/1.0")
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun openPlayer(c: Channel) {
        val list = adapter.currentList
        PlaylistHolder.channels = list
        PlaylistHolder.index = list.indexOf(c).coerceAtLeast(0)
        startActivity(Intent(this, PlayerActivity::class.java))
    }

    companion object {
        private const val ALL = "Tous"
    }
}
