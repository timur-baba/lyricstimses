package dev.timses.lyrics;

/** Bottan gelen son durumun değişmez kopyası; HUD iş parçacığıyla güvenle paylaşılır. */
public final class Snap {
    public static final Snap EMPTY = new Snap(false, "idle", false, 0, 0, "", "", "", "none", new long[0], new String[0], null, "");

    public static final class Anchor {
        public final double x, y, z;
        public final String dimension, world, setBy;
        public final double scale;
        public final int offsetMs, lines;

        public Anchor(double x, double y, double z, String dimension, String world, double scale, int offsetMs, int lines, String setBy) {
            this.x = x; this.y = y; this.z = z;
            this.dimension = dimension; this.world = world;
            this.scale = scale; this.offsetMs = offsetMs; this.lines = lines; this.setBy = setBy;
        }
    }

    public final boolean linked;
    public final String status;
    public final boolean playing;
    public final long positionMs;
    /** positionMs değerinin bu bilgisayar saatindeki karşılığı (gecikme payı düşülmüş). */
    public final long baseMillis;
    public final String trackId, title, artist, lyricsStatus;
    public final long[] times;
    public final String[] texts;
    public final Anchor anchor;
    public final String lyricsTrackId;

    public Snap(boolean linked, String status, boolean playing, long positionMs, long baseMillis, String trackId,
                String title, String artist, String lyricsStatus, long[] times, String[] texts, Anchor anchor, String lyricsTrackId) {
        this.linked = linked; this.status = status; this.playing = playing;
        this.positionMs = positionMs; this.baseMillis = baseMillis;
        this.trackId = trackId; this.title = title; this.artist = artist; this.lyricsStatus = lyricsStatus;
        this.times = times; this.texts = texts; this.anchor = anchor; this.lyricsTrackId = lyricsTrackId;
    }

    public long positionAt(long nowMillis) {
        return playing ? positionMs + (nowMillis - baseMillis) : positionMs;
    }

    /** position anında söylenen satırın dizini; ilk satırdan önce -1. */
    public int lineIndexAt(long position) {
        int lo = 0, hi = times.length - 1, result = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (times[mid] <= position) { result = mid; lo = mid + 1; } else { hi = mid - 1; }
        }
        return result;
    }
}
