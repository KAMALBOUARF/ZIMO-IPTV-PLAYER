package com.example.iptvplayer

data class Channel(
    val name: String,
    val url: String,
    val logo: String?,
    val group: String
)

object M3uParser {
    private const val NO_GROUP = "Sans groupe"
    private val logoRe = Regex("tvg-logo=\"([^\"]*)\"")
    private val groupRe = Regex("group-title=\"([^\"]*)\"")

    fun parse(text: String): List<Channel> {
        val out = ArrayList<Channel>()
        var name = ""
        var logo: String? = null
        var group = NO_GROUP
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF") -> {
                    name = line.substringAfterLast(',', "").trim()
                    logo = logoRe.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                    group = groupRe.find(line)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                        ?: NO_GROUP
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    out.add(Channel(name.ifBlank { line }, line, logo, group))
                    name = ""
                    logo = null
                    group = NO_GROUP
                }
            }
        }
        return out
    }
}
