package com.example.iptvplayer

/** Liste affichée + position courante, partagées entre l'écran principal et le lecteur. */
object PlaylistHolder {
    var channels: List<Channel> = emptyList()
    var index: Int = 0
}
