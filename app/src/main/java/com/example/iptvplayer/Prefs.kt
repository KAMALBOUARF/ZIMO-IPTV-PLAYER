package com.example.iptvplayer

import android.content.Context
import android.net.Uri

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("iptv", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = sp.getString("url", "") ?: ""
        set(v) = sp.edit().putString("url", v.trim()).apply()

    var username: String
        get() = sp.getString("user", "") ?: ""
        set(v) = sp.edit().putString("user", v.trim()).apply()

    var password: String
        get() = sp.getString("pass", "") ?: ""
        set(v) = sp.edit().putString("pass", v.trim()).apply()

    /**
     * - Si un utilisateur est saisi : mode Xtream Codes (http://hote:port + identifiants).
     * - Sinon : l'adresse saisie est utilisée telle quelle (lien M3U complet).
     */
    fun playlistUrl(): String? {
        var base = serverUrl.trim()
        if (base.isEmpty()) return null
        if (!base.startsWith("http://") && !base.startsWith("https://")) base = "http://$base"
        if (username.isNotBlank()) {
            base = base.trimEnd('/')
            return "$base/get.php?username=${Uri.encode(username)}" +
                "&password=${Uri.encode(password)}&type=m3u_plus&output=ts"
        }
        return base
    }
}
