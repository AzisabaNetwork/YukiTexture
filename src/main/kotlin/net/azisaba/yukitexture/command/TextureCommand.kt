package net.azisaba.yukitexture.command

import net.azisaba.yukitexture.YukiTexture
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil
import org.bukkit.ChatColor as CC

class TextureCommand(private val plugin: YukiTexture) : TabExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isNotEmpty() && (args[0].equals("dontapply", ignoreCase = true) || args[0].equals("ignore", ignoreCase = true))) {
            handleDontApplyCommand(sender, args)
            return true
        }

        if (sender is Player) {
            plugin.applyTex(sender)
        } else {
            sender.sendMessage("${plugin.prefix} ${CC.RED}このコマンドはプレイヤーのみ実行できます。")
        }
        return true
    }

    private fun handleDontApplyCommand(sender: CommandSender, args: Array<String>) {
        if (!sender.hasPermission("yukitexture.command.admin")) {
            sender.sendMessage("${plugin.prefix} ${CC.RED}このコマンドを実行する権限がありません。")
            return
        }

        val subAction = if (args.size > 1) args[1].lowercase() else "help"
        when (subAction) {
            "list" -> {
                val servers = plugin.dontApplyTextureServers
                if (servers.isEmpty()) {
                    sender.sendMessage("${plugin.prefix} ${CC.YELLOW}除外サーバーは登録されていません。")
                } else {
                    sender.sendMessage("${plugin.prefix} ${CC.GREEN}除外サーバー一覧 (${servers.size}件):")
                    for (server in servers) {
                        sender.sendMessage("${CC.GRAY} - ${CC.AQUA}$server")
                    }
                }
            }
            "add" -> {
                if (args.size < 3 || args[2].trim().isEmpty()) {
                    sender.sendMessage("${plugin.prefix} ${CC.RED}追加するサーバー名を指定してください: /tex dontapply add <サーバー名>")
                    return
                }
                val serverName = args[2].trim()
                if (plugin.addDontApplyServer(serverName)) {
                    sender.sendMessage("${plugin.prefix} ${CC.GREEN}サーバー '${CC.AQUA}$serverName${CC.GREEN}' を除外リストに追加しました。")
                } else {
                    sender.sendMessage("${plugin.prefix} ${CC.YELLOW}サーバー '${CC.AQUA}$serverName${CC.YELLOW}' は既に登録されています。")
                }
            }
            "remove", "delete" -> {
                if (args.size < 3 || args[2].trim().isEmpty()) {
                    sender.sendMessage("${plugin.prefix} ${CC.RED}削除するサーバー名を指定してください: /tex dontapply remove <サーバー名>")
                    return
                }
                val serverName = args[2].trim()
                if (plugin.removeDontApplyServer(serverName)) {
                    sender.sendMessage("${plugin.prefix} ${CC.GREEN}サーバー '${CC.AQUA}$serverName${CC.GREEN}' を除外リストから削除しました。")
                } else {
                    sender.sendMessage("${plugin.prefix} ${CC.RED}サーバー '${CC.AQUA}$serverName${CC.RED}' は登録されていません。")
                }
            }
            else -> {
                sender.sendMessage("${plugin.prefix} ${CC.GOLD}使用方法:")
                sender.sendMessage("${CC.GRAY} - ${CC.YELLOW}/tex dontapply list ${CC.WHITE}: 除外サーバー一覧を表示")
                sender.sendMessage("${CC.GRAY} - ${CC.YELLOW}/tex dontapply add <サーバー名> ${CC.WHITE}: 除外サーバーを追加")
                sender.sendMessage("${CC.GRAY} - ${CC.YELLOW}/tex dontapply remove <サーバー名> ${CC.WHITE}: 除外サーバーを削除")
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (!sender.hasPermission("yukitexture.command.admin")) {
            return emptyList()
        }

        val completions = ArrayList<String>()

        if (args.size == 1) {
            StringUtil.copyPartialMatches(args[0], listOf("dontapply"), completions)
        } else if (args.size == 2 && (args[0].equals("dontapply", ignoreCase = true) || args[0].equals("ignore", ignoreCase = true))) {
            StringUtil.copyPartialMatches(args[1], listOf("list", "add", "remove"), completions)
        } else if (args.size == 3 && (args[0].equals("dontapply", ignoreCase = true) || args[0].equals("ignore", ignoreCase = true)) && (args[1].equals("remove", ignoreCase = true) || args[1].equals("delete", ignoreCase = true))) {
            StringUtil.copyPartialMatches(args[2], plugin.dontApplyTextureServers.toList(), completions)
        }

        return completions
    }
}
