package vip.creatio.whitelist;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Listeners implements Listener {

    @EventHandler
    public void onLogin(LoginEvent e) {
        if (!WhiteList.instance.getWhiteList().contains(e.getConnection().getName())) {
            //Kick the player
            e.setCancelReason(TextComponent.fromLegacyText("§4§l你不在白名单上！ 请联系服务器管理员解决"));
            e.setCancelled(true);
            return;
        }
        try {
            WhiteList.instance.checkConnectionValid();
        } catch (SQLException ee) {
            ee.printStackTrace();
        }

        Connection conn = WhiteList.instance.getMysql();
        if (conn != null) {
            if (!e.getConnection().isOnlineMode()) {
                try {
                    Statement statement = conn.createStatement();
                    ResultSet rs = statement.executeQuery("SELECT `name` FROM `creatio_auth` WHERE `name` = \"" + e.getConnection().getName() + "\"");
                    if (!rs.next()) {
                        conn.createStatement()
                                .execute("INSERT INTO `creatio_auth` (`name`, `offline_uuid`, `logged_in`) VALUES (\"" + e.getConnection().getName() + "\", \"" + e.getConnection().getUniqueId().toString() + "\", \"false\")");
                    }
                } catch (SQLException ee) {
                    throw new RuntimeException(ee);
                }
            }
        }
    }

    @EventHandler
    public void onLeft(PlayerDisconnectEvent e) {
        Connection conn = WhiteList.instance.getMysql();
        if (conn != null) {
            try {
                Statement statement = conn.createStatement();
                statement.execute("UPDATE `creatio_auth` SET `logged_in` = \"false\" WHERE `name` = \"" + e.getPlayer().getName() + "\";");
            } catch (SQLException ee) {
                ee.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.getSender() instanceof ProxiedPlayer) {
            if (!event.getMessage().startsWith("/register") && !event.getMessage().startsWith("/login")) {
                ProxiedPlayer player = (ProxiedPlayer) event.getSender();
                try {
                    Statement statement = WhiteList.instance.getMysql().createStatement();
                    ResultSet rs = statement.executeQuery("SELECT `offline_uuid`,`logged_in` FROM `creatio_auth` WHERE `name` = \""
                            + player.getName() + "\"");
                    if (rs.next() && rs.getString(1).equalsIgnoreCase(player.getUniqueId().toString())
                            && rs.getString("logged_in").equals("false")) {
                        player.sendMessage(new TextComponent("§8§l[§d§lAu§5§lth§8§l]§c你需要先进行登录才能这么做!"));
                        event.setCancelled(true);
                    }
                } catch (SQLException ee) {
                    ee.printStackTrace();
                }
            }
        }
    }
}
