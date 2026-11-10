package com.lecteur.iptv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

const val USER_AGENT = "VLC/3.0.18 LibVLC/3.0.18"

private val AGENTS = listOf(
    USER_AGENT,
    "okhttp/4.12.0",
    "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120.0 Safari/537.36",
    "IPTVSmarters/1.0",
)

class PlaylistRepository(context: Context) {
    private val cache = File(context.filesDir, "playlist.m3u")
    private val tmp = File(context.filesDir, "playlist.tmp")
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun load(config: ServerConfig): Result<List<Channel>> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                download(config.playlistUrl())
            } catch (e: Exception) {
                if (!cache.exists() || cache.length() == 0L) throw e
            }
            val channels = cache.bufferedReader().use { M3uParser.parse(it) }
            if (channels.isEmpty()) error("Aucune chaîne trouvée. Réponse du serveur : ${head()}")
            channels
        }
    }

    private fun head(): String = cache.bufferedReader().use {
        val buf = CharArray(150)
        val n = it.read(buf)
        if (n > 0) String(buf, 0, n) else ""
    }

    private fun download(url: String) {
        var last = "Erreur inconnue"
        for (agent in AGENTS) {
            try {
                val req = Request.Builder().url(url).header("User-Agent", agent).build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body
                        if (body != null) {
                            tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                            if (tmp.length() > 0) {
                                cache.delete()
                                tmp.renameTo(cache)
                                return
                            }
                        }
                        last = "Réponse vide"
                    } else {
                        last = "HTTP ${resp.code} : ${resp.peekBody(150).string()}"
                    }
                }
            } catch (e: Exception) {
                last = e.message ?: e.javaClass.simpleName
            }
        }
        error(last)
    }

    fun clearCache() { cache.delete() }
}
