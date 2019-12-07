package net.azisaba.changeresourcepack;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.joor.Reflect;

public class ChangeResourcePack extends JavaPlugin {

    public static final String DEFAULT_HASH = "" + null;

    public void setResourcePack(String url, String hash) {
        Reflect.on(Bukkit.getServer())
                .call("getServer")
                .call("setResourcePack", url, hash);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("set")) {
            String url = args[1];
            String hash = args.length >= 3 ? args[2] : DEFAULT_HASH;
            setResourcePack(url, hash);
            sender.sendMessage(ChatColor.BLUE + "サーバーリソースパックを " + url + " に変更しました。");
            sender.sendMessage(ChatColor.BLUE + "サーバーリソースパックのハッシュ値を " + hash + " に変更しました。");
            return true;
        }
        return false;
    }
}
