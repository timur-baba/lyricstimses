package dev.timses.lyrics;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class TimLyricsClient implements ClientModInitializer {
    public static final String MOD_ID = "timlyrics";
    public static Config config = Config.load();
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openMenu = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.timlyrics.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, category));

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "lyrics"), LyricsHud::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Sync.tick(config);
            while (openMenu.consumeClick()) Minecraft.getInstance().setScreen(new MenuScreen());
        });
    }
}
