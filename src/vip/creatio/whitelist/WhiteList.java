package vip.creatio.whitelist;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public final class WhiteList extends Plugin {

    static WhiteList instance;
    private final File whitelist;
    private final File config_file;
    private Configuration whitelist_data;
    private Configuration config;
    //A list containing all whitelisted player's name;
    private final Set<String> setList = new HashSet<>();

    private String host, port, database, username, password;
    private static Connection conn;

    public WhiteList() throws IOException {

        instance = this;

        this.whitelist = new File(getDataFolder(), "whitelist.yml");
        if (!whitelist.exists()) {
            getDataFolder().mkdirs();
            whitelist.createNewFile();
        }

        this.config_file = new File(getDataFolder(), "config.yml");
        if (!config_file.exists()) {
            getDataFolder().mkdirs();
            config_file.createNewFile();
        }


        whitelist_data = ConfigurationProvider.getProvider(YamlConfiguration.class).load(whitelist);
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(config_file);
    }

    private void loadWhitelist() {
        whitelist_data.getStringList("whitelist").stream().filter(s -> s.length() > 4).forEach(setList::add);
    }

    private void saveWhitelist() {
        whitelist_data.set("whitelist", new ArrayList<>(setList));
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(whitelist_data, whitelist);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void getDatabase() {
        Configuration c = config.getSection("mysql");
        host = c.getString("host");
        port = c.getString("port");
        database = c.getString("database");
        username = c.getString("username");
        password = c.getString("password");
    }

    private void connect() throws SQLException, ClassNotFoundException {
        if (host != null) {
            if (conn != null && !conn.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://"
                            + this.host+ ":" + this.port + "/" + this.database,
                    this.username, this.password);
            Statement state = conn.createStatement();
            try {
                state.execute("CREATE TABLE creatio_auth (`name` CHAR(16), `offline_uuid` CHAR(36), `password` CHAR(32), `logged_in` CHAR(5));");
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, new Listeners());

        getProxy().getPluginManager().registerCommand(this, new WTC());

        loadWhitelist();

        getDatabase();
        if (host != null) {
            try {
                connect();
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        getProxy().getLogger().info("§8§l[§dW§5L§8§l] §rWhitelist loaded! Total " + setList.size() + " entries!");

        getProxy().getPluginManager().registerListener(this, new Listeners());
    }

    @Override
    public void onDisable() {
        saveWhitelist();

        getProxy().getLogger().info("§8§l[§dW§5L§8§l] §rWhitelist saved! Total " + setList.size() + " entries!");
    }

    Set<String> getWhiteList() {
        return setList;
    }

    Connection getMysql() {
        return conn;
    }

    void checkConnectionValid() throws SQLException {
        if (conn != null && conn.isClosed()) conn = DriverManager.getConnection("jdbc:mysql://"
                        + this.host+ ":" + this.port + "/" + this.database,
                this.username, this.password);
    }

    class WTC extends Command {

        public WTC() {
            super("wl");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!sender.hasPermission("whitelist.op")) return;

            int l = args.length - 1;

            if (l >= 0) {
                if (args[0].equalsIgnoreCase("add")) {
                    if (l >= 1 && args[1].length() > 4) {
                        sender.sendMessage(TextComponent.fromLegacyText("§8§l[§dW§5L§8§l] §rPlayer " + args[1] + " was added to whitelist"));
                        setList.add(args[1]);
                        saveWhitelist();
                        return;
                    }
                }

                if (args[0].equalsIgnoreCase("remove")) {
                    sender.sendMessage(TextComponent.fromLegacyText("§8§l[§dW§5L§8§l] §rPlayer " + args[1] + " was removed from whitelist"));
                    if (l >= 1) {
                        setList.remove(args[1]);
                        saveWhitelist();
                        return;
                    }
                }

                if (args[0].equalsIgnoreCase("list")) {
                    sender.sendMessage(TextComponent.fromLegacyText("§8§l[§dW§5L§8§l] §rNow listing all whitelisted players..."));
                    setList.stream().map(s -> "  §e" + s).map(TextComponent::fromLegacyText).forEach(sender::sendMessage);
                    sender.sendMessage(TextComponent.fromLegacyText("§8§l[§dW§5L§8§l] §rTotal " + setList.size() + " entries."));
                    return;
                }

                if (args[0].equalsIgnoreCase("reconnect")) {
                    sender.sendMessage(TextComponent.fromLegacyText("§8§l[§dW§5L§8§l] §rReconnecting database..."));
                    try {
                        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(config_file);
                        connect();
                    } catch (SQLException | ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            sender.sendMessage(TextComponent.fromLegacyText("§8§l[§dW§5L§8§l] §rUsage: /wl <add/remove/list> [...]"));
        }
    }
}
