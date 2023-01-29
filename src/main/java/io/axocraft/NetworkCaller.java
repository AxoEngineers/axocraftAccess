package io.axocraft;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

public class NetworkCaller {

    private String axoBackendURI;
    private String mojangPlayerURI;
    private String playerWalletsURI;
    private JSONParser parser = new JSONParser();

    NetworkCaller(String axoBackendURI, String mojangPlayerURI, String playerWalletsURI) {
        this.axoBackendURI = axoBackendURI;
        this.mojangPlayerURI = mojangPlayerURI;
        this.playerWalletsURI = playerWalletsURI;
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();

    public JSONArray getWalletsForUUID(String uuid) throws Exception {

        String completeURI = this.playerWalletsURI+uuid;
        System.out.println("Using URI:");
        System.out.println(completeURI);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(completeURI))
                //.setHeader("User-Agent", "Java 11 HttpClient Bot")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("status from wallet servor: ");
        System.out.println(response.statusCode());
        String resp = response.body();
        System.out.println("Got response from wallet server: ");
        System.out.println(resp);
        JSONArray wallets = (JSONArray) parser.parse(resp);
        return wallets;
    }

    public String stripPeriodFromPlayerName(String playerName) {
        if (playerName.charAt(0) == '.') {
            playerName = playerName.substring(1);
        }
        return playerName;
    }

    public String getUUIDForPlayer(String player) throws Exception {
        player = this.stripPeriodFromPlayerName(player);
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(this.mojangPlayerURI+player))
                //.setHeader("User-Agent", "Java 11 HttpClient Bot")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String resp = response.body();
        JSONObject json = (JSONObject) parser.parse(resp);
        String uuid = json.get("id").toString();
        if (uuid != null) {
            return uuid;
        } else {
            return "";
        }
    }

    public boolean checkHasAxos(String address) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(this.axoBackendURI+address))
                //.setHeader("User-Agent", "Java 11 HttpClient Bot")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        System.out.println("response from backend status: ");
        System.out.println(response.statusCode());

        // print response body
        System.out.println("response from backend: ");
        System.out.println(response.body());

        String resp = response.body();
        JSONObject json = (JSONObject) parser.parse(resp);
        String key = "axosOwned";
        JSONArray axos = (JSONArray) parser.parse(json.get(key).toString());
        System.out.println("has axos: ");
        System.out.println(axos);
        System.out.println(axos.size());

        if (axos.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

}
