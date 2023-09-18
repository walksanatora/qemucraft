package net.walksanator.qemucraft.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.walksanator.qemucraft.QemuCraftClient;

public class QemuCraftClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Shaders.INSTANCE.init();
    }
}
