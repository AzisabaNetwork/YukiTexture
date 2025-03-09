package net.azisaba.yukitexture.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import net.azisaba.yukitexture.YukiTexture
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

    companion object {
        private const val PERMISSION_ROOT: String = "yukitexture.command.yukitexture"
    }
}
