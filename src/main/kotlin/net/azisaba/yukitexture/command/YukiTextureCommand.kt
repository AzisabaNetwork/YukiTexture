package net.azisaba.yukitexture.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import kotlinx.coroutines.runBlocking
import net.azisaba.yukitexture.LOGGER
import net.azisaba.yukitexture.YukiTexture
import net.azisaba.yukitexture.uploader.UploaderManager
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender

@CommandAlias("yukitexture")
class YukiTextureCommand(
    private val plugin: YukiTexture,
) : BaseCommand() {
    @Default
    fun default(sender: CommandSender) {
        sender.sendMessage(Component.text("fmm... nothing is here..."))
    }

    @Subcommand("reload")
    @CommandPermission("$PERMISSION_ROOT.reload")
    fun reload(sender: CommandSender) {
        sender.sendMessage("Reloading...")
        plugin.reloadConfig()
        sender.sendMessage("Reload completed.")
    }

    @Subcommand("upload")
    @CommandPermission("$PERMISSION_ROOT.upload")
    fun upload(sender: CommandSender) {
        sender.sendMessage("Zipping...")
        val merger = plugin.resourcePackMerger
        val zipFile =
            merger.mergeAndZip(
                merger
                    .parseTargets(
                        plugin.yukiConfig.merger.mergeTargets,
                        plugin.dataFolder.parentFile,
                    ),
            )

        sender.sendMessage("Uploading...")
        UploaderManager.upload(
            plugin.uploaderName,
            zipFile,
        ) {
            it.fold({
                sender.sendMessage("Successfully to upload resourcepack zip file.")
            }) {
                sender.sendMessage("Failed to upload resourcepack.")
                LOGGER.error("Failed to upload", it)
            }
        }
    }

    @Subcommand("geturl")
    @CommandPermission("$PERMISSION_ROOT.geturl")
    fun getUrl(sender: CommandSender) {
        runBlocking {
            UploaderManager.getUrl(plugin.uploaderName)
        }.fold({
            sender.sendMessage("URL: $it")
        }) {
            sender.sendMessage("Failed to get url")
            LOGGER.error("Failed to get url", it)
        }
    }

    companion object {
        private const val PERMISSION_ROOT: String = "yukitexture.command.yukitexture"
    }
}
