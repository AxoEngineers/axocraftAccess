package io.axocraft;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;
import java.util.logging.Logger;
import java.util.Iterator;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

public class axocraftAccess extends JavaPlugin {

    private static Permission perms;
    private static Logger log = Logger.getLogger("Minecraft");
    private final String name = "[Whitelist]";

    //Constants
    private final String PROP_KICKMESSAGE = "kick-message";
    private final String PROP_WHITELIST_ADMINS = "whitelist-admins";
    private final String PROP_DISABLE_LIST = "disable-list-command";
    private final String PROP_USE_SQL = "sql-enable";
    private final String PROP_SQL_DRIVER_JAR = "sql-driver-jar";
    private final String PROP_SQL_DRIVER = "sql-driver";
    private final String PROP_SQL_CONNECTION = "sql-driver-connection";
    private final String PROP_SQL_QUERY = "sql-query";
    private final String PROP_SQL_QUERY_ADD = "sql-query-add";
    private final String PROP_SQL_QUERY_REMOVE = "sql-query-remove";
    private final String FILE_WHITELIST = "whitelist.txt";
    private final String FILE_WALLETS = "wallets.json";
    private final String FILE_CONFIG = "config.yml";

    //Attributes
    private FileWatcher m_Watcher;
    private FileWatcher m_WatcherWallets;
    private Timer m_Timer;
    private Timer m_TimerWallets;
    private File m_Folder;
    private boolean m_bWhitelistActive;
    private SQLConnection m_SqlConnection;

    //General settings
    private ArrayList<String> m_SettingsWhitelistAllow;
    private String m_strSettingsKickMessage;
    private boolean m_bSettingsListCommandDisabled;

    //SQL settings
    private boolean m_bSettingsSqlEnabled;
    private String m_strSettingsSqlDriverJar;
    private String m_strSettingsSqlDriver;
    private String m_strSettingsSqlConnection;
    private String m_strSettingsSqlQuery;
    private String m_strSettingsSqlQueryAdd;
    private String m_strSettingsSqlQueryRemove;

    public void onEnable() {
        System.out.println(("Starting the AxocraftAccess plugin!!"));
        m_Folder = getDataFolder();
        m_strSettingsKickMessage = "";
        m_SettingsWhitelistAllow = new ArrayList<String>();
        m_bWhitelistActive = true;
        m_bSettingsListCommandDisabled = false;
        m_bSettingsSqlEnabled = false;
        m_strSettingsSqlDriverJar = "";
        m_strSettingsSqlDriver = "";
        m_strSettingsSqlConnection = "";
        m_strSettingsSqlQuery = "";
        m_strSettingsSqlQueryAdd = "";
        m_strSettingsSqlQueryRemove = "";
        m_SqlConnection = null;


        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new WLPlayerListener(this), this);

        //Load up permission hooks
        if (!setupPermissions()) {
            log.severe(name + " - No Permission plugin detected, Disabling!");
            pm.disablePlugin(this);
            return;
        }

        //Create folders and files
        if (!m_Folder.exists()) {
            m_Folder.mkdir();
        }
        File fWhitelist = new File(m_Folder.getAbsolutePath() + File.separator + FILE_WHITELIST);
        if (!fWhitelist.exists()) {
            log.warning(name + " - Whitelist is missing, creating...");
            try {
                fWhitelist.createNewFile();
                System.out.println("done.");
            } catch (IOException ex) {
                System.out.println("failed.");
            }
        }
        File fWallets= new File(m_Folder.getAbsolutePath() + File.separator + FILE_WALLETS);
        if (!fWallets.exists()) {
            log.warning(name + " - Wallets lits is missing, creating...");
            try {
                fWallets.createNewFile();
                System.out.println("done.");
            } catch (IOException ex) {
                System.out.println("failed.");
            }
        }
        //Start file watcher
        m_Watcher = new FileWatcher(fWhitelist);
        m_WatcherWallets = new FileWatcher(fWallets);
        m_Timer = new Timer(true);
        m_Timer.schedule(m_Watcher, 0, 1000);
        m_TimerWallets = new Timer(true);
        m_TimerWallets.schedule(m_WatcherWallets, 0, 1000);

        File fConfig = new File(m_Folder.getAbsolutePath() + File.separator + FILE_CONFIG);
        if (!fConfig.exists()) {
            log.warning(name + " Config is missing, creating...");
            try
            {
                fConfig.createNewFile();
                Properties propConfig = new Properties();
                propConfig.setProperty(PROP_KICKMESSAGE, "You must own an AxolittlesNFT to join! Please go to the axolittles discord to verify ownership.");
                propConfig.setProperty(PROP_WHITELIST_ADMINS, "Name1,Name2,Name3");
                propConfig.setProperty(PROP_DISABLE_LIST, "false");

                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(fConfig.getAbsolutePath()));
                propConfig.store(stream, "Auto generated config file, please modify");
            } catch (IOException ex) {
            }
        }
        loadWhitelistSettings();

        PluginDescriptionFile pdfFile = this.getDescription();
        log.info(name + " -  version " + pdfFile.getVersion() + " is enabled!");
    }

    public void onDisable() {
        m_Timer.cancel();
        m_Timer.purge();
        m_Timer = null;
        m_TimerWallets.cancel();
        m_TimerWallets.purge();
        m_TimerWallets = null;
        log.info(name + " - disabled!");
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            perms = permissionProvider.getProvider();
        }
        return (perms != null);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        System.out.println("Invoking whitelist command code!!");
        System.out.println("Sender is: " + sender.getName());
        System.out.println("arg0 is: " + args[0]);
        System.out.println("args length is: " + args.length);

        if (!hasPerm(sender, "whitelist.add") || !hasPerm(sender, "whitelist.add")) {
            System.out.println("Слава Україні");
            sender.sendMessage("You are very sneak!!");
            return true;
        }
        if (args.length < 1 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.YELLOW + "Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/axess reload  (reloads the whitelist and settings)");
            sender.sendMessage(ChatColor.YELLOW + "/axess add [player]  (adds a player to the whitelist)");
            sender.sendMessage(ChatColor.YELLOW + "/axess remove [player]  (removes a player from the whitelist)");
            sender.sendMessage(ChatColor.YELLOW + "/axess on|off  (actives/deactivates whitelist)");
            sender.sendMessage(ChatColor.YELLOW + "/axess list  (list whitelist entries)");
            return true;
        } else if (args[0].equalsIgnoreCase("reload") && hasPerm(sender, "whitelist.admin")) {
            if ( reloadSettings() ) {
                log.info(sender.getName() + " has reloaded the whitelist.");
                sender.sendMessage(ChatColor.GREEN + "Settings and whitelist reloaded");
            } else
                sender.sendMessage(ChatColor.RED + "Could not reload whitelist...");
            return true;
        } else if (args[0].equalsIgnoreCase("add") && hasPerm(sender, "whitelist.add")) {
            if ( args.length < 2 ) {
                sender.sendMessage(ChatColor.RED + "Parameter missing: Player name");
            } else {
                if (addPlayerToWhitelist(args[1])) {
                    sender.sendMessage(ChatColor.GREEN + "Player \"" + args[1] + "\" has been added to the whitelist!");
                    log.info(name + " - " + sender.getName() + " has added " + args[1] + " to the whitelist.");
                } else
                    sender.sendMessage(ChatColor.RED + "Could not add player \"" + args[1] + "\"");
            }
            return true;
        } else if(args[0].equalsIgnoreCase("remove") && hasPerm(sender, "whitelist.remove")) {
            if ( args.length < 2 ) {
                sender.sendMessage(ChatColor.RED + "Parameter missing: Player name");
            }
            else {
                if ( removePlayerFromWhitelist( args[1] )) {
                    log.info(name + " - " + sender.getName() + " has removed " + args[1] + " to the whitelist.");
                    sender.sendMessage(ChatColor.GREEN + "Player \"" + args[1] + "\" removed");
                } else
                    sender.sendMessage(ChatColor.RED + "Could not remove player \"" + args[1] + "\"");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("on") && hasPerm(sender, "whitelist.admin")) {
            setWhitelistActive(true);
            log.info(name + " - " + sender.getName() + " has enabled the whitelist.");
            sender.sendMessage(ChatColor.GREEN + "Whitelist activated!");
            return true;
        }else if (args[0].equalsIgnoreCase("off") && hasPerm(sender, "whitelist.admin")) {
            setWhitelistActive(false);
            log.info(name + " - " + sender.getName() + " has deactivated the whitelist.");
            sender.sendMessage(ChatColor.RED + "Whitelist deactivated!");
            return true;
        } if (args[0].equalsIgnoreCase("list") && hasPerm(sender, "whitelist.list")) {
            if ( !isListCommandDisabled() )
                sender.sendMessage(ChatColor.RED + "List command is disabled!");
            else
                sender.sendMessage(ChatColor.YELLOW + "Players on whitelist: " + ChatColor.GRAY + getFormatedAllowList());
            return true;
        } else {
            sender.sendMessage("Something is wrong!");
            return true;
        }
    }

    public JSONArray getWalletsForPlayer(String player) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(m_Folder.getAbsolutePath() + File.separator + FILE_WALLETS));
        JSONArray ja = (JSONArray) obj;
        Iterator itr2 = ja.iterator();
        while (itr2.hasNext()) {
            JSONObject item = (JSONObject) parser.parse(itr2.next().toString());
            Object walletsRaw = item.get(player);
            if (walletsRaw != null) {
                JSONArray wallets = (JSONArray) parser.parse(walletsRaw.toString());
                System.out.println("Got wallets for player: " + player + wallets.toString());
                return wallets;
            }
        }
        return new JSONArray();

    }

    public boolean loadWhitelistSettings() {
        System.out.print("Whitelist: Trying to load whitelist and settings...");
        try
        {
            //1. Load whitelist.txt
            m_SettingsWhitelistAllow.clear();
            BufferedReader reader = new BufferedReader(new FileReader((m_Folder.getAbsolutePath() + File.separator + FILE_WHITELIST)));
            String line = reader.readLine();
            while (line != null) {
                m_SettingsWhitelistAllow.add(line);
                line = reader.readLine();
            }
            reader.close();

            //2. Load fConfig.yml
            Properties propConfig = new Properties();
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream(m_Folder.getAbsolutePath() + File.separator + FILE_CONFIG));
            propConfig.load(stream);
            m_strSettingsKickMessage = propConfig.getProperty(PROP_KICKMESSAGE);
            if (m_strSettingsKickMessage == null) {
                m_strSettingsKickMessage = "";
            }
            String rawDisableListCommand = propConfig.getProperty(PROP_DISABLE_LIST);
            if (rawDisableListCommand != null) {
                m_bSettingsListCommandDisabled = Boolean.parseBoolean(rawDisableListCommand);
            }
            String rawUseSql = propConfig.getProperty(PROP_USE_SQL);
            if (rawUseSql != null) {
                m_bSettingsSqlEnabled = Boolean.parseBoolean(rawUseSql);
            }
            m_strSettingsSqlDriver = propConfig.getProperty(PROP_SQL_DRIVER);
            if (m_strSettingsSqlDriver == null) {
                m_strSettingsSqlDriver = "";
            }
            m_strSettingsSqlConnection = propConfig.getProperty(PROP_SQL_CONNECTION);
            if (m_strSettingsSqlConnection == null) {
                m_strSettingsSqlConnection = "";
            }
            m_strSettingsSqlQuery = propConfig.getProperty(PROP_SQL_QUERY);
            if (m_strSettingsSqlQuery == null) {
                m_strSettingsSqlQuery = "";
            }
            m_strSettingsSqlQueryAdd = propConfig.getProperty(PROP_SQL_QUERY_ADD);
            if (m_strSettingsSqlQueryAdd == null) {
                m_strSettingsSqlQueryAdd = "";
            }
            m_strSettingsSqlQueryRemove = propConfig.getProperty(PROP_SQL_QUERY_REMOVE);
            if (m_strSettingsSqlQueryRemove == null) {
                m_strSettingsSqlQueryRemove = "";
            }
            m_strSettingsSqlDriverJar = propConfig.getProperty(PROP_SQL_DRIVER_JAR);
            if ( m_bSettingsSqlEnabled ) {
                m_SqlConnection = new SQLConnection(m_strSettingsSqlDriver, m_strSettingsSqlConnection, m_strSettingsSqlQuery, m_strSettingsSqlQueryAdd, m_strSettingsSqlQueryRemove, m_strSettingsSqlDriverJar);
            } else {
                if ( m_SqlConnection != null )
                    m_SqlConnection.Cleanup();
                m_SqlConnection = null;
            }

            System.out.println("done.");
        }
        catch (Exception ex) {
            System.out.println("failed: " + ex);
            return false;
        }
        return true;
    }

    private boolean hasPerm(CommandSender sender, String permission) {
        if (sender instanceof Player) {
            return perms.has((Player) sender, permission);
        }
        return true;
    }
    public boolean saveWhitelist() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter((m_Folder.getAbsolutePath() + File.separator + FILE_WHITELIST)));
            for (String player : m_SettingsWhitelistAllow) {
                writer.write(player);
                writer.newLine();
            }
            writer.close();
        } catch (Exception ex) {
            System.out.println(ex);
            return false;
        }
        return true;
    }

    public boolean isOnWhitelist(String playerName) {
        if ( m_bSettingsSqlEnabled && m_SqlConnection != null ) {
            return m_SqlConnection.isOnWhitelist(playerName, true);
        } else {
            for (String player : m_SettingsWhitelistAllow) {
                if (player.compareToIgnoreCase(playerName) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addPlayerToWhitelist(String playerName) {
        if ( m_SqlConnection != null ) { //SQL mode
            if ( !isOnWhitelist(playerName) ) {
                return m_SqlConnection.addPlayerToWhitelist(playerName, true);
            }
        } else { //whitelist.txt mode
            if (!isOnWhitelist(playerName)) {
                m_SettingsWhitelistAllow.add(playerName);
                return saveWhitelist();
            }
        }
        return false;
    }

    public boolean removePlayerFromWhitelist(String playerName) {
        if ( m_SqlConnection != null ) { //SQL mode
            if ( isOnWhitelist(playerName) ) {
                return m_SqlConnection.removePlayerFromWhitelist(playerName, true);
            }
        }
        else { //whitelist.txt mode
            for (int i = 0; i < m_SettingsWhitelistAllow.size(); i++) {
                if (playerName.compareToIgnoreCase(m_SettingsWhitelistAllow.get(i)) == 0) {
                    m_SettingsWhitelistAllow.remove(i);
                    return saveWhitelist();
                }
            }
        }
        return false;
    }

    public boolean reloadSettings() {
        return loadWhitelistSettings();
    }

    public String getKickMessage() {
        return m_strSettingsKickMessage;
    }

    public String getFormatedAllowList() {
        String result = "";
        for (String player : m_SettingsWhitelistAllow) {
            if (result.length() > 0) {
                result += ", ";
            }
            result += player;
        }
        return result;
    }

    public boolean isWhitelistActive() {
        return m_bWhitelistActive;
    }

    public void setWhitelistActive(boolean isWhitelistActive) {
        m_bWhitelistActive = isWhitelistActive;
    }

    public boolean isListCommandDisabled() {
        if ( m_SqlConnection != null )
            return false;
        return m_bSettingsListCommandDisabled;
    }

    public boolean needReloadWhitelist() {
        if ( m_Watcher != null )
            return m_Watcher.wasFileModified();
        return false;
    }

    public void resetNeedReloadWhitelist() {
        if ( m_Watcher != null )
            m_Watcher.resetFileModifiedState();
    }

    public boolean needReloadWallets() {
        if ( m_WatcherWallets != null )
            return m_WatcherWallets.wasFileModified();
        return false;
    }

    public void resetNeedReloadWallets() {
        if ( m_WatcherWallets != null )
            m_WatcherWallets.resetFileModifiedState();
    }
}