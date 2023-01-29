package io.axocraft;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.List;

public class WLPlayerListener implements Listener
{
    private final axocraftAccess plugin;
    private final String axoBackendURI = "https://axo-backend-pvj2l.ondigitalocean.app/axos?address=";
    private final String mojangPlayerURI = "https://api.mojang.com/users/profiles/minecraft/";
    private final String playerWalletsURI = "https://axo-backend-pvj2l.ondigitalocean.app/get_wallets_for_player?uuid=";
    //private final String playerWalletsURI = "http://127.0.0.1:8000/get_wallets_for_player?uuid=";
    private final NetworkCaller networkCaller = new NetworkCaller(axoBackendURI, mojangPlayerURI, playerWalletsURI);

    public WLPlayerListener(axocraftAccess instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) throws Exception {
        String testDingus = ".ABH1Z";
        testDingus = networkCaller.stripPeriodFromPlayerName(testDingus);
        System.out.println(testDingus);
        String dingusUUID = networkCaller.getUUIDForPlayer(testDingus);
        System.out.println(dingusUUID);
        JSONArray dingusWallets = networkCaller.getWalletsForUUID(dingusUUID);
        System.out.println(dingusWallets.get(0));
        boolean axus = networkCaller.checkHasAxos(dingusWallets.get(0).toString());
        System.out.println(axus);
        String playerName = event.getPlayer().getName();
        boolean hasAxos = false;
        String address = null;
        try {
            String playerUUID = networkCaller.getUUIDForPlayer(playerName);
            System.out.println("got player UUID: " + playerUUID);
            JSONArray addresses = networkCaller.getWalletsForUUID(playerUUID);
            int ticker = 0;
            while ((!hasAxos) & (ticker < addresses.size())) {
                address = addresses.get(ticker).toString();
                hasAxos = networkCaller.checkHasAxos(address);
                ticker++;
            }
        } catch (Exception e) {
            System.out.println("Exception verifying axos, falling back on whitelist....");
            System.out.println(e.toString());
        }
        if ( hasAxos ) {
            System.out.println("Player " + playerName + " with address: " + address);
            System.out.println("Has axos!");
            System.out.println("allow!");
        } else {
            System.out.println("Doesn't have axos. ngmi.");
            if (plugin.isWhitelistActive()) {
                //Check if whitelist.txt needs to be reloaded
                if (plugin.needReloadWhitelist()) {
                    System.out.println("Whitelist: Executing scheduled whitelist reload.");
                    plugin.reloadSettings();
                    plugin.resetNeedReloadWhitelist();
                }

                System.out.print("Whitelist: Player " + playerName + " is trying to join...");
                if (plugin.isOnWhitelist(playerName)) {
                    System.out.println("allow!");
                } else {
                    System.out.println("kick!");
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, plugin.getKickMessage());
                }
            }
        }
    }
}