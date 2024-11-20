package dev.pierrot.models

data class PlayerSyncData(
    var eventExtend: String = "",
    var guildId: String = "",
    var voiceChannelId: String = "",
    var musicList: List<String> = emptyList(),
    var stats: Map<String, Any> = emptyMap(),
    var event: PlayerEvent? = null,
    var state: PlayerUpdateState? = null
)

data class PlayerEvent(
    var type: String = "",
    var guildId: String = "",
    var channelId: String = ""
)

data class PlayerUpdateState(
    var connected: Boolean = false,
    var position: Long = 0,
    var time: Long = 0
)