@file:Suppress("UNUSED_PARAMETER")

package flavor.pie.mcmoji

import com.google.common.collect.HashMultimap
import com.google.common.reflect.TypeToken
import flavor.pie.kludge.*
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.gson.GsonConfigurationLoader
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.bstats.sponge.MetricsLite2
import org.spongepowered.api.asset.Asset
import org.spongepowered.api.asset.AssetId
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.command.TabCompleteEvent
import org.spongepowered.api.event.entity.living.humanoid.player.ResourcePackStatusEvent
import org.spongepowered.api.event.game.GameReloadEvent
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.event.game.state.GamePreInitializationEvent
import org.spongepowered.api.event.message.MessageChannelEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.resourcepack.ResourcePacks
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.inject.Inject

@Plugin(id = "mcmoji", name = "MCMoji", version = "1.2.0", description = "Adds emoji to Minecraft.", authors = ["pie_flavor"])
class MCMoji @Inject constructor(@ConfigDir(sharedRoot = false) private val dir: Path,
                                 @DefaultConfig(sharedRoot = false) private val configPath: Path,
                                 @DefaultConfig(sharedRoot = false) private val configLoader: ConfigurationLoader<CommentedConfigurationNode>,
                                 @AssetId("alternates.json") private val alternatesAsset: Asset,
                                 @AssetId("characters.json") private val charactersAsset: Asset,
                                 @AssetId("filenames.json") private val filenamesAsset: Asset,
                                 @AssetId("default.conf") private val configAsset: Asset,
                                 @AssetId("gun.alt.json") private val gunAltAsset: Asset,
                                 metrics: MetricsLite2) {

    companion object {
        const val STATIC_PACK_URL = "https://github.com/pie-flavor/mcmoji/raw/76e30d42d0fce4f76b93097c856f5e176320589a/pack/mcmoji_pack_v1.zip"
        const val DYNAMIC_PACK_URL = "https://github.com/pie-flavor/mcmoji/raw/master/pack/mcmoji_pack_v1.zip"
        val emojiMap: Map<String, Char> get() = emoji.toMap()
        private val emoji: MutableMap<String, Char> = mutableMapOf()
        private val byFirstTwo: HashMultimap<String, String> = HashMultimap.create()
        private val byFirstRegisteredId: MutableMap<Char, String> = mutableMapOf()
        private lateinit var packUrl: String
        private var sendResourcePack: Boolean = false
        private var customPack: Boolean = false
        private val noSend: MutableSet<UUID> = mutableSetOf()
        private var usePermissions: Boolean = false
        val noEmoji: Set<UUID> get() = noSend.toSet()
    }

    init {
        plugin = this
    }

    @[PublishedApi Listener]
    internal fun preInit(e: GamePreInitializationEvent) {
        loadConfig()
    }

    private fun loadConfig() {
        emoji.clear()
        byFirstTwo.clear()
        if (!Files.exists(configPath)) {
            configAsset.copyToFile(configPath)
            alternatesAsset.copyToDirectory(dir)
            charactersAsset.copyToDirectory(dir)
            filenamesAsset.copyToDirectory(dir)
            gunAltAsset.copyToDirectory(dir)
        }
        val config = configLoader.load().getValue(TypeToken.of(Config::class.java))!!
        if (config.version < 3) {
            if (config.version < 2) {
                if (config.`resource-pack` == "https://github.com/pie-flavor/mcmoji/raw/master/mcmoji_pack.zip") {
                    config.`resource-pack` = ""
                }
            }
            configLoader.save(configLoader.createEmptyNode().setValue(TypeToken.of(Config::class.java), config))
        }
        packUrl = if (config.`resource-pack` == "") {
            if (config.`pack-fetch-kind` == FetchKind.DYNAMIC) {
                DYNAMIC_PACK_URL
            } else {
                STATIC_PACK_URL
            }
        } else {
            config.`resource-pack`
        }
        customPack = config.`resource-pack` == ""
        sendResourcePack = config.`send-pack`
        usePermissions = config.`enable-permissions`
        val map = mutableMapOf<String, EmojiMap>()
        Files.newDirectoryStream(dir, "*.json").use {
            for (file in it) {
                if (".alt.json" in file.fileName.toString() && file.fileName.toString().substringBefore('.') !in config.alts) {
                    continue
                }
                val emojiMap = GsonConfigurationLoader.builder().setPath(file).build().load().getValue(TypeToken.of(EmojiMap::class.java))!!
                map[file.fileName.toString()] = emojiMap
            }
        }
        fun load(name: String) {
            val emojimap = map[name]!!
            emojimap.references.let {
                if (it.isNullOrEmpty()) {
                    for ((key, value) in emojimap.map) {
                        emoji.computeIfAbsent(key) { value.toInt().toChar() }
                        byFirstRegisteredId.putIfAbsent(value.toInt().toChar(), key)
                    }
                } else {
                    load(it)
                    for ((key, value) in emojimap.map) {
                        emoji.computeIfAbsent(key) { emoji[value]!! }
                    }
                }
            }
            map -= name
        }
        map.keys.filter { it.endsWith(".alt.json") }.forEach { load(it) }
        while (map.isNotEmpty()) {
            load(map.keys.first())
        }
        for ((key, _) in emoji) {
            if (key.length > 1) {
                byFirstTwo.put(key.substring(0..1), key)
            }
        }
    }

    @[PublishedApi Listener]
    internal fun reload(e: GameReloadEvent) {
        loadConfig()
    }

    @[PublishedApi Listener]
    internal fun init(e: GameInitializationEvent) {
        val resourcepack = commandSpecOf {
            executor(::onResourcepack)
            description("Attempts to download the resource pack.".text())
        }
        val reload = commandSpecOf {
            permission(Permissions.RELOAD)
            executor(::onReload)
            description("Reloads the configuration.".text())
        }
        val base = commandSpecOf {
            child(resourcepack, "resourcepack")
            child(reload, "reload")
        }
        CommandManager.register(this, base, "mcmoji")
    }

    @[PublishedApi Listener]
    internal fun onJoin(e: ClientConnectionEvent.Join) {
        if (sendResourcePack) {
            delay(1) {
                e.targetEntity.sendResourcePack(ResourcePacks.fromUri(URI(packUrl)))
            }
        }
    }

    @[PublishedApi Listener]
    internal fun onResourcePackResponse(e: ResourcePackStatusEvent) {
        when (e.status) {
            ResourcePackStatusEvent.ResourcePackStatus.DECLINED -> {
                e.player.sendMessage("You declined the resource pack! You will be unable to see chat emoji.".red())
                e.player.sendMessage("Type ".red() + "/mcmoji resourcepack".gold() + " if you change your mind.")
                noSend += e.player.uniqueId
            }
            ResourcePackStatusEvent.ResourcePackStatus.FAILED -> {
                e.player.sendMessage("You failed to download the resource pack for some reason.".red())
                e.player.sendMessage("Type ".red() + "/mcmoji resourcepack".gold() + " to try again.")
                noSend += e.player.uniqueId
                if (customPack) {
                    e.player.sendMessage("If this is a persistent issue, notify pie_flavor#7868 on Discord.".red())
                } else {
                    e.player.sendMessage("If this is a persistent issue, notify an admin.".red())
                }
            }
            ResourcePackStatusEvent.ResourcePackStatus.SUCCESSFULLY_LOADED -> noSend -= e.player.uniqueId
            else -> {}
        }
    }

    private val regex = """:([A-Za-z0-9-_]+):""".toRegex()

    @[PublishedApi Listener(order = Order.PRE)]
    internal fun onChat(e: MessageChannelEvent.Chat) {
        var message = e.rawMessage.toPlain()
        val matchResults = regex.findAll(message)
        for (result in matchResults) {
            val name = result.groups[1]
            if (name != null) {
                val replacement = emoji[name.value]
                if (replacement != null) {
                    if (!usePermissions || (e.cause.root() as? Player)?.hasPermission("mcmoji.emoji.${byFirstRegisteredId[replacement]!!}") == true) {
                        message = message.replace(":${name.value}:", replacement.toString())
                    }
                }
            }
        }
        e.formatter.setBody(message.text())
    }

    @[PublishedApi Listener]
    internal fun onTabComplete(e: TabCompleteEvent) {
        if (e.tabCompletions.isNotEmpty()) {
            return
        }
        val arg = if (e.rawMessage.contains(' ')) { e.rawMessage.substring(e.rawMessage.lastIndexOf(' ') + 1) } else { e.rawMessage }
        if (arg.count { it == ':' } % 2 == 1) {
            val name = arg.substring(arg.lastIndexOf(':') + 1)
            if (name.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
                val possibilities = byFirstTwo[name.substring(0..1)].filter { it.startsWith(name) }
                val narrowed = possibilities.map { it to emoji[it]!! }.sortedBy { it.first.length }.distinctBy { it.second }
                if (narrowed.isNotEmpty()) {
                    val splitPoint = arg.substring(0..(arg.lastIndexOf(':')))
                    val suggestions = narrowed.map { splitPoint + it.first + ':' }
                    e.tabCompletions.addAll(suggestions)
                }
            }
        }
    }

    private fun onResourcepack(src: CommandSource, args: CommandContext): CommandResult {
        if (src !is Player) {
            throw CommandException("You must be a player!".red())
        }
        src.sendResourcePack(ResourcePacks.fromUri(URI(packUrl)))
        return CommandResult.success()
    }

    private fun onReload(src: CommandSource, args: CommandContext): CommandResult {
        loadConfig()
        src.sendMessage("Successfully reloaded configs.".green())
        return CommandResult.success()
    }

}
