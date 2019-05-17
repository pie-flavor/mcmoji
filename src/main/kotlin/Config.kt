package flavor.pie.mcmoji

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable
class Config {
    @Setting var version: Int = 2
    @Setting var `resource-pack`: String = "https://github.com/pie-flavor/mcmoji/raw/master/mcmoji_pack.zip"
    @Setting var `send-pack`: Boolean = true
    @Setting var alts: List<String> = listOf()
}

@ConfigSerializable
class EmojiMap {
    @Setting var references: String? = null
    @Setting var map: Map<String, String> = mapOf()
}
