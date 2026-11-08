package dev.timses.lyrics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Kontrol menüsü: bağlantı, söz arama, konum, boyut, satır ve zaman ayarları (hepsi sadece sende). */
public final class MenuScreen extends Screen {
    private static final String[] SIDES = {"Sağ", "Sol", "İki yan", "Ortada"};
    private EditBox urlBox;
    private EditBox searchBox;
    private Button sideButton;
    private String message = "";

    public MenuScreen() {
        super(Component.literal("TimLyrics"));
    }

    private int top() {
        return Math.max(14, height / 2 - 120);
    }

    @Override
    protected void init() {
        Config cfg = TimLyricsClient.config;
        int cx = width / 2, left = cx - 130, top = top();

        urlBox = new EditBox(font, left, top, 196, 20, Component.literal("Bot adresi"));
        urlBox.setMaxLength(200);
        urlBox.setValue(cfg.baseUrl);
        urlBox.setHint(Component.literal("https://…trycloudflare.com"));
        addRenderableWidget(urlBox);
        addRenderableWidget(Button.builder(Component.literal("Kaydet"), b -> {
            String v = urlBox.getValue().trim();
            if (!v.isEmpty() && !Api.validUrl(v)) { message = "Adres https:// ile başlamalı."; return; }
            if (!v.equals(cfg.baseUrl)) { cfg.token = ""; Sync.snap = Snap.EMPTY; Sync.pairCode = ""; }
            cfg.baseUrl = v; cfg.save(); message = "Adres kaydedildi.";
        }).bounds(left + 200, top, 60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Discord ile eşleştir"), b -> pair())
                .bounds(left, top + 26, 260, 20).build());

        searchBox = new EditBox(font, left, top + 80, 196, 20, Component.literal("Söz ara"));
        searchBox.setMaxLength(120);
        searchBox.setHint(Component.literal("Söz yoksa/yanlışsa: şarkı - sanatçı"));
        addRenderableWidget(searchBox);
        addRenderableWidget(Button.builder(Component.literal("Ara"), b -> searchLyrics())
                .bounds(left + 200, top + 80, 60, 20).build());

        sideButton = Button.builder(sideLabel(cfg), b -> {
            cfg.side = (cfg.side + 1) % SIDES.length; cfg.save(); b.setMessage(sideLabel(cfg));
        }).bounds(left, top + 106, 260, 20).build();
        addRenderableWidget(sideButton);

        stepper(left, top + 132, -1, () -> cfg.scale = Math.max(0.4, Math.round((cfg.scale - 0.2) * 10) / 10.0));
        stepper(left, top + 132, 1, () -> cfg.scale = Math.min(4.0, Math.round((cfg.scale + 0.2) * 10) / 10.0));
        stepper(left, top + 156, -1, () -> cfg.lines = Math.max(1, cfg.lines - 1));
        stepper(left, top + 156, 1, () -> cfg.lines = Math.min(7, cfg.lines + 1));
        stepper(left, top + 180, -1, () -> cfg.localOffsetMs = Math.max(-5000, cfg.localOffsetMs - 100));
        stepper(left, top + 180, 1, () -> cfg.localOffsetMs = Math.min(5000, cfg.localOffsetMs + 100));

        addRenderableWidget(Button.builder(label(cfg), b -> {
            cfg.enabled = !cfg.enabled; cfg.save(); b.setMessage(label(cfg));
        }).bounds(left, top + 208, 126, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Kapat"), b -> onClose())
                .bounds(left + 134, top + 208, 126, 20).build());
    }

    private static Component sideLabel(Config c) {
        return Component.literal("Konum: " + SIDES[Math.max(0, Math.min(SIDES.length - 1, c.side))]);
    }

    private static Component label(Config c) {
        return Component.literal("Sözler: " + (c.enabled ? "Açık" : "Kapalı"));
    }

    private void stepper(int left, int y, int dir, Runnable change) {
        int x = dir < 0 ? left : left + 200;
        addRenderableWidget(Button.builder(Component.literal(dir < 0 ? "−" : "+"), b -> {
            change.run();
            TimLyricsClient.config.save();
        }).bounds(x, y, 60, 20).build());
    }

    private void pair() {
        Config cfg = TimLyricsClient.config;
        if (!Api.validUrl(cfg.baseUrl)) { message = "Önce bot adresini yazıp Kaydet'e bas."; return; }
        message = "Kod isteniyor…";
        Api.pair(cfg).whenComplete((o, err) -> Minecraft.getInstance().execute(() -> {
            if (err != null) { message = "Bağlanılamadı. Adresi kontrol et."; return; }
            cfg.token = o.get("token").getAsString();
            cfg.save();
            String prefix = o.has("prefix") ? o.get("prefix").getAsString() : "!";
            Sync.pairCode = prefix + "panel " + o.get("code").getAsString();
            Minecraft.getInstance().keyboardHandler.setClipboard(Sync.pairCode);
            message = "Komut panoya kopyalandı. Ses kanalındayken Discord'a yapıştır.";
        }));
    }

    private void searchLyrics() {
        Config cfg = TimLyricsClient.config;
        String q = searchBox.getValue().trim();
        if (cfg.token.isBlank() || !Sync.snap.linked) { message = "Önce Discord ile eşleştir."; return; }
        if (q.isEmpty()) { message = "Şarkı adını ve sanatçıyı yaz."; return; }
        message = "Aranıyor…";
        Api.lyricsQuery(cfg, q).whenComplete((o, err) -> Minecraft.getInstance().execute(() -> {
            if (err != null) {
                Throwable t = err.getCause() != null ? err.getCause() : err;
                String m = String.valueOf(t.getMessage());
                message = m.contains("|") ? m.substring(m.indexOf('|') + 1) : "Arama başarısız.";
                return;
            }
            message = "Yeniden aranıyor; birkaç saniye sonra sözler gelir.";
        }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        Snap s = Sync.snap;
        Config cfg = TimLyricsClient.config;
        int cx = width / 2, left = cx - 130, top = top();
        g.drawCenteredString(font, Component.literal("TimLyrics"), cx, top - 10, 0xFFFFFFFF);

        String conn = cfg.token.isBlank() ? "Eşleşmedi"
                : s.linked ? (Sync.connected() ? "Bağlı" : "Bağlantı yok") : "Kod bekleniyor";
        g.drawString(font, Component.literal("Durum: " + conn), left, top + 52, 0xFFB8FF9A, false);
        if (!Sync.pairCode.isEmpty() && !s.linked) {
            g.drawString(font, Component.literal(Sync.pairCode), left, top + 64, 0xFFFFE08A, false);
        } else if (s.linked) {
            String track = s.title.isEmpty() ? "Şarkı çalmıyor" : s.title;
            g.drawString(font, Component.literal(font.plainSubstrByWidth(track, 190)), left + 62, top + 52, 0xFFFFFFFF, false);
            String lyr = "ready".equals(s.lyricsStatus) ? "sözler hazır" : "loading".equals(s.lyricsStatus) ? "sözler aranıyor" : "söz bulunamadı";
            g.drawString(font, Component.literal(lyr), left, top + 64, 0xFFB3B3B3, false);
        }
        g.drawCenteredString(font, Component.literal(String.format("Boyut %.1f×", cfg.scale)), cx, top + 138, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.literal("Satır " + cfg.lines), cx, top + 162, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.literal("Zaman " + (cfg.localOffsetMs >= 0 ? "+" : "") + cfg.localOffsetMs + " ms"), cx, top + 186, 0xFFFFFFFF);
        String hint = !message.isEmpty() ? message : Sync.error;
        if (!hint.isEmpty()) g.drawCenteredString(font, Component.literal(font.plainSubstrByWidth(hint, 300)), cx, top + 234, 0xFFFFC9C9);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
