package net.azisaba.changeresourcepack;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.joor.Reflect;

import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

public class ChangeResourcePack extends JavaPlugin {

    public static final String DEFAULT_HASH = "" + null;
    public static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.RED + "CRP" + ChatColor.GRAY + "]" + ChatColor.RESET;
    public static final String PREFIX_OK = ChatColor.GRAY + "< " + ChatColor.GREEN + "OK" + ChatColor.GRAY + " >" + ChatColor.RESET;
    public static final String PREFIX_BAD = ChatColor.GRAY + "< " + ChatColor.RED + "BAD" + ChatColor.GRAY + " >" + ChatColor.RESET;

    public String getResourcePack() {
        return Reflect.on(getServer())
                .call("getServer")
                .call("getResourcePack")
                .get();
    }

    public String getResourcePackHash() {
        return Reflect.on(getServer())
                .call("getServer")
                .call("getResourcePackHash")
                .get();
    }

    public void setResourcePack(String url, String hash) {
        Reflect.on(getServer())
                .call("getServer")
                .call("setResourcePack", url, hash);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && command.getName().equalsIgnoreCase("changepack")) {
            String url = args[0];
            String hash = args.length >= 2 ? args[1] : DEFAULT_HASH;
            setResourcePack(url, hash);
            sender.sendMessage(PREFIX + " サーバーリソースパックを " + ChatColor.GREEN + "正常に変更" + ChatColor.RESET + " しました！");
            sender.sendMessage(PREFIX + " 確認するには " + ChatColor.AQUA + "/showpack" + ChatColor.RESET + " と入力！");
            return true;
        }
        if (command.getName().equalsIgnoreCase("showpack")) {
            String url = getResourcePack();
            String hash = getResourcePackHash();
            sender.sendMessage(PREFIX + " --------------------------------");
            sender.sendMessage(PREFIX + " URL ) " + ChatColor.WHITE + url);
            sender.sendMessage(PREFIX + " ハッシュ値 ) " + ChatColor.WHITE + hash);
            sender.sendMessage(PREFIX + " --------------------------------");
            sender.sendMessage(PREFIX + " 正しく読み込めるかチェックするには " + ChatColor.YELLOW + "/debugpack" + ChatColor.RESET + " と入力！");
            return true;
        }
        if (command.getName().equalsIgnoreCase("debugpack")) {
            String url = getResourcePack();
            String hash = getResourcePackHash();
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                URL urlObj = null;
                try {
                    urlObj = new URL(url);
                    sender.sendMessage(PREFIX + " " + PREFIX_OK + " URLの構文は正しいです！");
                } catch (Exception ex) {
                    sender.sendMessage(PREFIX + " " + PREFIX_BAD + " URLの構文が正しくありません！");
                }

                URLConnection connectionObj = null;
                try {
                    Objects.requireNonNull(urlObj);
                    connectionObj = urlObj.openConnection();
                    sender.sendMessage(PREFIX + " " + PREFIX_OK + " URLへの接続を確立できました！");

                    String type = connectionObj.getContentType();
                    boolean goodType = "application/zip".equals(type);
                    sender.sendMessage(PREFIX + " " + (goodType ? PREFIX_OK : PREFIX_BAD) + " Content-Type は " + type + " です。");

                    long length = connectionObj.getContentLengthLong();
                    boolean goodLength = length > 0;
                    sender.sendMessage(PREFIX + " " + (goodLength ? PREFIX_OK : PREFIX_BAD) + " Content-Length は " + length + " です。");
                } catch (Exception ex) {
                    sender.sendMessage(PREFIX + " " + PREFIX_BAD + " URLへの接続を確立できません！");
                }

                byte[] content = null;
                try {
                    Objects.requireNonNull(urlObj);
                    content = IOUtils.toByteArray(urlObj);
                    long finalSize = content.length;
                    sender.sendMessage(PREFIX + " " + PREFIX_OK + " ファイルの取得に成功しました！ (" + finalSize + " Bytes)");
                } catch (Exception ex) {
                    sender.sendMessage(PREFIX + " " + PREFIX_BAD + " ファイルを取得できません！");
                }

                boolean goodHash = hash != null && hash.length() == 40;
                sender.sendMessage(PREFIX + " " + (goodHash ? PREFIX_OK : PREFIX_BAD) + " ハッシュ値: " + hash);

                try {
                    Objects.requireNonNull(hash);
                    Objects.requireNonNull(content);
                    String contentHash = DigestUtils.sha1Hex(content);
                    boolean matchHash = hash.equalsIgnoreCase(contentHash);
                    sender.sendMessage(PREFIX + " " + (matchHash ? PREFIX_OK : PREFIX_BAD) + " ファイルのハッシュ値: " + contentHash);
                } catch (Exception ex) {
                    sender.sendMessage(PREFIX + " " + PREFIX_BAD + " SHA1を取得できません！");
                }

                sender.sendMessage(PREFIX + " 完了しました！");
            });
            return true;
        }
        return false;
    }
}
