package com.lecteur.iptv.data

import java.io.Reader

object M3uParser {
    private fun attr(line: String, key: String): String? {
        val k = "$key=\""
        val i = line.indexOf(k, ignoreCase = true)
        if (i < 0) return null
        val s = i + k.length
        val e = line.indexOf('"', s)
        return if (e < 0) null else line.substring(s, e).takeIf { it.isNotBlank() }
    }

    fun parse(reader: Reader): List<Channel> {
        val result = ArrayList<Channel>()
        var name: String? = null
        var logo: String? = null
        var group = "Divers"
        var id = 0
        reader.buffered(65536).forEachLine { raw ->
            val line = raw.trim()
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                logo = attr(line, "tvg-logo")
                group = (attr(line, "group-title") ?: "Divers").intern()
                name = line.substringAfterLast(',', "").trim()
                    .ifBlank { attr(line, "tvg-name") ?: "Chaîne ${id + 1}" }
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                val n = name
                if (n != null) {
                    result += Channel(id++, n, line, logo, group)
                    name = null; logo = null; group = "Divers"
                }
            }
        }
        return result
    }

    fun parse(text: String): List<Channel> = parse(text.reader())
}
