package net.walksanator.qemucraft;

import dev.architectury.registry.ReloadListenerRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.packs.PackType;

@Environment(EnvType.CLIENT)
public class QemuCraftClient {
    public static void initClient() {
        //ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES,SHADERS);
    }
}
