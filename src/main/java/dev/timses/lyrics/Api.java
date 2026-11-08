package dev.timses.lyrics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** TimSesBot'un /mod/ uç noktalarıyla konuşur. */
public final class Api {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static String base(Config c) {
        String b = c.baseUrl.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b;
    }

    public static boolean validUrl(String url) {
        try {
            URI u = URI.create(url.trim());
            boolean local = "127.0.0.1".equals(u.getHost()) || "localhost".equals(u.getHost());
            return u.getHost() != null && ("https".equals(u.getScheme()) || ("http".equals(u.getScheme()) && local));
        } catch (Exception e) {
            return false;
        }
    }

    private static CompletableFuture<JsonObject> send(HttpRequest req) {
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(r -> {
            JsonObject o;
            try { o = JsonParser.parseString(r.body()).getAsJsonObject(); } catch (Exception e) { o = new JsonObject(); }
            if (r.statusCode() >= 400) {
                String msg = o.has("error") ? o.get("error").getAsString() : "HTTP " + r.statusCode();
                throw new IllegalStateException(r.statusCode() + "|" + msg);
            }
            return o;
        });
    }

    public static CompletableFuture<JsonObject> pair(Config c) {
        return send(HttpRequest.newBuilder(URI.create(base(c) + "/mod/pair"))
                .timeout(Duration.ofSeconds(8)).POST(HttpRequest.BodyPublishers.ofString("{}")).build());
    }

    public static CompletableFuture<JsonObject> state(Config c, String have) {
        String q = have == null || have.isEmpty() ? "" : "?have=" + have;
        return send(HttpRequest.newBuilder(URI.create(base(c) + "/mod/state" + q))
                .timeout(Duration.ofSeconds(6)).header("Authorization", "Bearer " + c.token).GET().build());
    }

    public static CompletableFuture<JsonObject> anchor(Config c, JsonObject body) {
        return send(HttpRequest.newBuilder(URI.create(base(c) + "/mod/anchor"))
                .timeout(Duration.ofSeconds(8)).header("Authorization", "Bearer " + c.token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build());
    }

    public static CompletableFuture<JsonObject> lyricsQuery(Config c, String query) {
        JsonObject body = new JsonObject();
        body.addProperty("query", query);
        return send(HttpRequest.newBuilder(URI.create(base(c) + "/mod/lyrics-query"))
                .timeout(Duration.ofSeconds(8)).header("Authorization", "Bearer " + c.token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build());
    }
}
