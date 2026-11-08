package dev.timses.lyrics;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sözleri bakış yönünün sağında/solunda havada süzülür gibi çizer. Panel, bakış yönünü hafif
 * gecikmeyle izler: dönünce ya da yürüyünce seni takip eder ama "asılı" hissi verir.
 * Her oyuncu kendi ekranında görür; yazı genişliği söz uzunluğuna göre uzar.
 */
public final class LyricsHud {
    private static final int WRAP_PX = 200;
    private static final float WORLD_PER_PX = 0.045f;
    private static final double DISTANCE = 6.0;
    private static final double FOLLOW_SPEED = 7.0;

    private static double smYaw, smPitch;
    private static long lastNanos;
    private static boolean initialized;

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Config cfg = TimLyricsClient.config;
        Snap snap = Sync.snap;
        if (!cfg.enabled || !snap.linked || mc.player == null || mc.level == null || mc.options.hideGui || snap.trackId.isEmpty()) {
            initialized = false;
            return;
        }

        float partial = delta.getGameTimeDeltaPartialTick(false);
        Player p = mc.player;
        Vec3 eye = p.getEyePosition(partial);
        double yawDeg = p.getViewYRot(partial);
        double pitchDeg = p.getViewXRot(partial);

        long nowN = System.nanoTime();
        double dt = initialized ? (nowN - lastNanos) / 1e9 : 1.0;
        lastNanos = nowN;
        if (!initialized || dt > 0.25) {
            smYaw = yawDeg;
            smPitch = pitchDeg;
            initialized = true;
        } else {
            double k = 1 - Math.exp(-dt * FOLLOW_SPEED);
            smYaw += wrap(yawDeg - smYaw) * k;
            smPitch += (pitchDeg - smPitch) * k;
        }

        double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
        Vec3 fwd = dir(yaw, pitch);
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = right.cross(fwd);

        int w = g.guiWidth(), h = g.guiHeight();
        double focal = (h / 2.0) / Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        Font font = mc.font;
        Cam cam = new Cam(eye, fwd, right, up, w, h, focal);

        double scale = Math.max(0.3, cfg.scale);
        double lineH = 11 * WORLD_PER_PX * scale * 1.35;
        double angle = cfg.sideAngle;
        long now = System.currentTimeMillis();
        long pos = snap.positionAt(now) + cfg.localOffsetMs;
        int side = Math.max(0, Math.min(3, cfg.side));

        // Sütun merkezleri: bakış yönü + yana açı (gecikmeli takip).
        Vec3 rightCol = point(eye, smYaw + (side == 1 || side == 3 ? 0 : angle), smPitch);
        Vec3 leftCol = point(eye, smYaw - angle, smPitch);
        Vec3 centerCol = point(eye, smYaw, smPitch);
        Vec3 col = side == 0 ? rightCol : side == 1 ? leftCol : centerCol;
        int align = side == 0 ? 1 : side == 1 ? -1 : 0;

        if (snap.times.length == 0) {
            String label = "loading".equals(snap.lyricsStatus) ? "♪ Sözler aranıyor…" : "♪ " + snap.title;
            Vec3 c = side == 2 ? rightCol : col;
            drawLine(g, font, label, c, 0, cam, scale, 1f, 0xFFB3B3B3, side == 2 ? 1 : align);
            return;
        }

        int idx = snap.lineIndexAt(pos);
        double slide = idx >= 0 ? Math.min(1.0, (pos - snap.times[idx]) / 350.0) : 1.0;
        slide = 1 - Math.pow(1 - slide, 3);
        double glide = (1 - slide) * lineH;
        int n = Math.max(1, Math.min(7, cfg.lines));

        if (side == 2) {
            int leftCount = n == 1 ? 0 : Math.max(1, (n - 1) / 2);
            for (int j = 1; j <= leftCount; j++) {
                int i = idx - j;
                if (i < 0 || i >= snap.texts.length) continue;
                drawLine(g, font, snap.texts[i], leftCol, (j - 1) * lineH - glide, cam, scale, 1f, 0xFF7E7E7E, -1);
            }
            for (int k = 0; k < n - leftCount; k++) {
                int i = Math.max(idx, 0) + k;
                if (idx < 0) i = k;
                if (i >= snap.texts.length) continue;
                boolean current = i == idx;
                drawLine(g, font, snap.texts[i], rightCol, -k * lineH - glide, cam, scale, current ? 1.25f : 1f,
                        current ? 0xFFFFFFFF : 0xFFA8A8A8, 1);
            }
            return;
        }

        int before = (n - 1) / 2;
        for (int k = 0; k < n; k++) {
            int i = idx - before + k;
            if (i < 0 || i >= snap.texts.length) continue;
            boolean current = i == idx;
            double yOff = ((n - 1) / 2.0 - k) * lineH - glide;
            drawLine(g, font, snap.texts[i], col, yOff, cam, scale, current ? 1.25f : 1f,
                    current ? 0xFFFFFFFF : 0xFFA8A8A8, align);
        }
    }

    private record Cam(Vec3 eye, Vec3 fwd, Vec3 right, Vec3 up, int w, int h, double focal) { }

    private static double wrap(double d) {
        d = d % 360.0;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    private static Vec3 dir(double yawRad, double pitchRad) {
        return new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
    }

    private static Vec3 point(Vec3 eye, double yawDeg, double pitchDeg) {
        return eye.add(dir(Math.toRadians(yawDeg), Math.toRadians(pitchDeg)).scale(DISTANCE));
    }

    /** align: 1 = yazı noktadan sağa doğru uzar, -1 = sola doğru uzar, 0 = ortalı. */
    private static void drawLine(GuiGraphics g, Font font, String text, Vec3 center, double yOff, Cam c,
                                 double scale, float emphasis, int color, int align) {
        Vec3 d = new Vec3(center.x - c.eye.x, center.y + yOff - c.eye.y, center.z - c.eye.z);
        double zc = d.dot(c.fwd);
        if (zc < 0.5) return;
        double sx = c.w / 2.0 + d.dot(c.right) / zc * c.focal;
        double sy = c.h / 2.0 - d.dot(c.up) / zc * c.focal;
        float s = (float) (WORLD_PER_PX * scale * c.focal / zc) * emphasis;
        if (s < 0.25f || s > 8f) return;
        if (sx < -600 || sx > c.w + 600 || sy < -200 || sy > c.h + 200) return;

        List<FormattedCharSequence> parts = font.split(Component.literal(text), WRAP_PX);
        int lineStep = font.lineHeight + 1;
        int total = parts.size() * lineStep;
        g.pose().pushMatrix();
        g.pose().translate((float) sx, (float) sy);
        g.pose().scale(s, s);
        for (int j = 0; j < parts.size(); j++) {
            FormattedCharSequence part = parts.get(j);
            int width = font.width(part);
            int x = align > 0 ? 0 : align < 0 ? -width : -width / 2;
            g.drawString(font, part, x, -total / 2 + j * lineStep, color, true);
        }
        g.pose().popMatrix();
    }
}
