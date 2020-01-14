package net.azisaba.changeresourcepack;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.joor.Reflect;

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

    public void saveResourcePack(String url, String hash) throws IOException {
        File propertiesFile = Reflect.on(getServer())
                .call("getServer")
                .field("options")
                .call("valueOf", "config")
                .get();
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(propertiesFile.toPath()));
        properties.setProperty("resource-pack", url);
        if ( properties.containsKey("resource-pack-hash") ) {
            properties.setProperty("resource-pack-hash", hash);
        }
        if ( properties.containsKey("resource-pack-sha1") ) {
            properties.setProperty("resource-pack-sha1", hash);
        }
        String comments = Files.newBufferedReader(propertiesFile.toPath()).readLine().substring(1);
        properties.store(Files.newBufferedWriter(propertiesFile.toPath()), comments);
    }

    public boolean isValidHash(String hash) {
        return hash != null && hash.length() == 40;
    }

    public String getHash(byte[] data) {
        return DigestUtils.sha1Hex(data);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ( args.length >= 1 && command.getName().equalsIgnoreCase("changepack") ) {
            String url = args[0];
            String hash = args.length >= 2 ? args[1] : DEFAULT_HASH;
            setResourcePack(url, hash);
            sender.sendMessage(PREFIX + " サーバーリソースパックを " + ChatColor.GREEN + "正常に変更" + ChatColor.RESET + " しました！");
            sender.sendMessage(PREFIX + " 確認するには " + ChatColor.AQUA + "/showpack" + ChatColor.RESET + " と入力！");
            return true;
        }
        if ( args.length >= 1 && command.getName().equalsIgnoreCase("changepack-url") ) {
            String url = args[0];
            String hash = getResourcePackHash();
            setResourcePack(url, hash);
            sender.sendMessage(PREFIX + " サーバーリソースパックの " + ChatColor.RED + "URLのみ" + ChatColor.RESET + " を " + ChatColor.GREEN + "正常に変更" + ChatColor.RESET + " しました！");
            sender.sendMessage(PREFIX + " 確認するには " + ChatColor.AQUA + "/showpack" + ChatColor.RESET + " と入力！");
            return true;
        }
        if ( args.length >= 1 && command.getName().equalsIgnoreCase("changepack-hash") ) {
            String url = getResourcePack();
            String hash = args[0];
            setResourcePack(url, hash);
            sender.sendMessage(PREFIX + " サーバーリソースパックの " + ChatColor.RED + "ハッシュ値のみ" + ChatColor.RESET + " を " + ChatColor.GREEN + "正常に変更" + ChatColor.RESET + " しました！");
            sender.sendMessage(PREFIX + " 確認するには " + ChatColor.AQUA + "/showpack" + ChatColor.RESET + " と入力！");
            return true;
        }
        if ( command.getName().equalsIgnoreCase("showpack") ) {
            String url = getResourcePack();
            String hash = getResourcePackHash();
            sender.sendMessage(PREFIX + " --------------------------------");
            sender.sendMessage(PREFIX + " URL ) " + ChatColor.WHITE + url);
            sender.sendMessage(PREFIX + " ハッシュ値 ) " + ChatColor.WHITE + hash);
            sender.sendMessage(PREFIX + " --------------------------------");
            sender.sendMessage(PREFIX + " 正しく読み込めるかチェックするには " + ChatColor.YELLOW + "/debugpack" + ChatColor.RESET + " と入力！");
            sender.sendMessage(PREFIX + " 再起動した後もこの設定を使うには " + ChatColor.DARK_RED + "/savepack" + ChatColor.RESET + " と入力！");
            return true;
        }
        if ( command.getName().equalsIgnoreCase("debugpack") ) {
            String url = getResourcePack();
            String hash = getResourcePackHash();
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                URL urlObj = null;
                try {
                    urlObj = new URL(url);
                    sender.sendMessage(PREFIX + " " + PREFIX_OK + " URLの構文は正しいです！");
                } catch ( Exception ex ) {
                    sender.sendMessage(PREFIX + " " + PREFIX_BAD + " URLの構文が正しくありません！");
                }

                URLConnection connectionObj;
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
                } catch ( Exception ex ) {
                    sender.sendMessage(PREFIX + " " + PREFIX_BAD + " URLへの接続を確立できません！");
                }

                byte[] content = null;
                try {
                    Objects.requireNonNull(urlObj);
                    content = IOUtils.toByteArray(urlObj);
                    long finalSize = content.length;
                    sender.sendMessage(PREFIX + " " + PREFIX_OK + " ファイルの取得に成功しました！ (" + finalSize + " Bytes)");
                } catch ( Exception ex ) {
                    sender.sendMessage(PREFIX + " " + PREFIX_BAD + " ファイルを取得できません！");
                }

                sender.sendMessage(PREFIX + " " + (isValidHash(hash) ? PREFIX_OK : PREFIX_BAD) + " ハッシュ値: " + hash);

                String contentHash = null;
                try {
                    Objects.requireNonNull(content);
                    contentHash = getHash(content);
                    sender.sendMessage(PREFIX + " " + (isValidHash(contentHash) ? PREFIX_OK : PREFIX_BAD) + " ファイルのハッシュ値: " + contentHash);
                } catch ( Exception ex ) {
                    sender.sendMessage(PREFIX + " " + PREFIX_BAD + " ファイルのハッシュ値を取得できません！");
                }

                boolean matchHash = Objects.equals(hash, contentHash);
                sender.sendMessage(PREFIX + " " + (matchHash ? PREFIX_OK : PREFIX_BAD) + " ハッシュ値が一致" + (matchHash ? "しました！" : "しません。"));

                sender.sendMessage(PREFIX + " 完了しました！");
            });
            return true;
        }
        if ( command.getName().equalsIgnoreCase("savepack") ) {
            String url = getResourcePack();
            String hash = getResourcePackHash();
            sender.sendMessage(PREFIX + " server.properties に情報を書き込んでいます...");
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    saveResourcePack(url, hash);
                } catch ( IOException ex ) {
                    sender.sendMessage(PREFIX + " " + ChatColor.RED + "書き込みに失敗しました。");
                }
                sender.sendMessage(PREFIX + " 書き込みが完了しました！");
            });
            return true;
        }
        return false;
    }
}
