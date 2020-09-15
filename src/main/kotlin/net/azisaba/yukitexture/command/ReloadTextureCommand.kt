package net.azisaba.yukitexture.command

import net.azisaba.yukitexture.YukiTexture
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ReloadTextureCommand(private val plugin: YukiTexture) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        plugin.reloadTex(sender)
        return true
    }
}
