package dev.timses.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicBoolean;

/** Botu yoklar, gelen durumu Snap olarak yayınlar. */
public final class Sync {
    public static volatile Snap snap = Snap.EMPTY;
    public static volatile String error = "";
    public static volatile long lastOkMillis = 0;
    public static volatile String pairCode = "";
    private static final AtomicBoolean inFlight = new AtomicBoolean(false);
    private static long lastPollMillis = 0;

    public static boolean connected() {
        return System.currentTimeMillis() - lastOkMillis < 5000;
    }

    public static void tick(Config cfg) {
        long now = System.currentTimeMillis();
        if (cfg.baseUrl.isBlank() || cfg.token.isBlank()) return;
        long interval = snap.linked ? 1000 : 2000;
        if (now - lastPollMillis < interval || !inFlight.compareAndSet(false, true)) return;
        lastPollMillis = now;
        final long sent = now;
        Api.state(cfg, snap.lyricsTrackId).whenComplete((o, err) -> {
            try {
                if (err != null) {
                    Throwable t = err.getCause() != null ? err.getCause() : err;
                    String m = String.valueOf(t.getMessage());
                    if (m.startsWith("401|")) { snap = Snap.EMPTY; error = "Oturum bitti, yeniden eşleştir."; }
                    else error = "Bağlanılamıyor: " + (m.contains("|") ? m.substring(m.indexOf('|') + 1) : m);
                    return;
                }
                long received = System.currentTimeMillis();
                apply(o, sent, received);
                error = "";
                lastOkMillis = received;
            } catch (Exception e) {
                error = "Bot yanıtı okunamadı.";
            } finally {
                inFlight.set(false);
            }
        });
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    static Snap.Anchor parseAnchor(JsonElement e) {
        if (e == null || !e.isJsonObject()) return null;
        JsonObject a = e.getAsJsonObject();
        return new Snap.Anchor(a.get("x").getAsDouble(), a.get("y").getAsDouble(), a.get("z").getAsDouble(),
                str(a, "dimension"), str(a, "world"), a.get("scale").getAsDouble(),
                a.get("offsetMs").getAsInt(), a.get("lines").getAsInt(), str(a, "setBy"));
    }

    private static void apply(JsonObject o, long sent, long received) {
        if (!o.has("linked") || !o.get("linked").getAsBoolean()) {
            Snap prev = snap;
            snap = new Snap(false, "idle", false, 0, 0, "", "", "", "none", prev.times, prev.texts, null, prev.lyricsTrackId);
            return;
        }
        Snap prev = snap;
        JsonObject track = o.has("track") && o.get("track").isJsonObject() ? o.getAsJsonObject("track") : null;
        String trackId = track == null ? "" : str(track, "id");
        String lyricsStatus = str(o, "lyricsStatus");
        String lyricsTrackId = prev.lyricsTrackId;
        long[] times = prev.times;
        String[] texts = prev.texts;
        if (o.has("lyrics") && o.get("lyrics").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("lyrics");
            times = new long[arr.size()];
            texts = new String[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                JsonArray pair = arr.get(i).getAsJsonArray();
                times[i] = pair.get(0).getAsLong();
                texts[i] = pair.get(1).getAsString();
            }
            lyricsTrackId = str(o, "lyricsTrackId");
        } else if (!"ready".equals(lyricsStatus) || !trackId.equals(lyricsTrackId)) {
            times = new long[0];
            texts = new String[0];
            lyricsTrackId = "";
        }
        // İstek gidiş-dönüş süresinin yarısı kadar gecikme payı düşülür.
        long base = received - (received - sent) / 2;
        snap = new Snap(true, str(o, "status"), o.get("playing").getAsBoolean(), o.get("positionMs").getAsLong(), base,
                trackId, track == null ? "" : str(track, "title"), track == null ? "" : str(track, "artist"),
                lyricsStatus, times, texts, parseAnchor(o.get("anchor")), lyricsTrackId);
    }
}
