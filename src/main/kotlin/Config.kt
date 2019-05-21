package flavor.pie.mcmoji

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable
class Config {
    @Setting var version: Int = 2
    @Setting var `resource-pack`: String = ""
    @Setting var `send-pack`: Boolean = true
    @Setting var alts: List<String> = listOf()
    @Setting var `enable-permissions` = false
    @Setting var `pack-fetch-kind` = FetchKind.DYNAMIC
}

enum class FetchKind {
    DYNAMIC, STATIC
}

@ConfigSerializable
class EmojiMap {
    @Setting var references: String? = null
    @Setting var map: Map<String, String> = mapOf()
}
