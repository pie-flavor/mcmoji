package flavor.pie.mcmoji

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable
class Config {
    @Setting var version: Int = 1
    @Setting var `resource-pack`: String = TODO()
}

@ConfigSerializable
class EmojiMap {
    @Setting var references: String? = null
    @Setting var map: Map<String, String> = mapOf()
}
