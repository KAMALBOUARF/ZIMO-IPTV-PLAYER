package com.lecteur.iptv

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lecteur.iptv.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val config: ServerConfig? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val group: String? = null,
    val playing: Channel? = null,
    val groups: List<String> = emptyList(),
    val visible: List<Channel> = emptyList(),
) {
    val channels: List<Channel> get() = visible
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val repo = PlaylistRepository(app)
    private var all: List<Channel> = emptyList()
    private var filterJob: Job? = null

    var ui by mutableStateOf(UiState(config = prefs.load()))
        private set

    init { if (ui.config != null) refresh() }

    fun saveConfig(c: ServerConfig) {
        prefs.save(c)
        ui = ui.copy(config = c, error = null)
        refresh()
    }

    fun refresh() {
        val cfg = ui.config ?: return
        viewModelScope.launch {
            ui = ui.copy(loading = true, error = null)
            val result = repo.load(cfg)
            result.onSuccess { list ->
                all = list
                val groups = withContext(Dispatchers.Default) { list.map { it.group }.distinct().sorted() }
                ui = ui.copy(loading = false, groups = groups)
                applyFilter(immediate = true)
            }.onFailure {
                ui = ui.copy(loading = false, error = it.message ?: "Erreur de connexion")
            }
        }
    }

    private fun applyFilter(immediate: Boolean = false) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            if (!immediate) delay(250)
            val q = ui.query
            val g = ui.group
            val list = withContext(Dispatchers.Default) {
                all.filter {
                    (g == null || it.group == g) &&
                        (q.isBlank() || it.name.contains(q, ignoreCase = true))
                }
            }
            ui = ui.copy(visible = list)
        }
    }

    fun logout() {
        prefs.clear(); repo.clearCache()
        all = emptyList()
        ui = UiState()
    }

    fun setQuery(q: String) { ui = ui.copy(query = q); applyFilter() }
    fun setGroup(g: String?) { ui = ui.copy(group = g); applyFilter(immediate = true) }
    fun play(c: Channel) { prefs.lastChannelUrl = c.url; ui = ui.copy(playing = c) }
    fun stop() { ui = ui.copy(playing = null) }

    fun step(delta: Int) {
        val list = ui.visible
        val cur = ui.playing ?: return
        if (list.isEmpty()) return
        val i = list.indexOfFirst { it.id == cur.id }.let { if (it < 0) 0 else it }
        play(list[(i + delta + list.size) % list.size])
    }
}
