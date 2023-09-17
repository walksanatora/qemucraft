package net.walksanator.qemucraft.fabric;

import net.walksanator.qemucraft.QemuCraft;
import net.fabricmc.api.ModInitializer;

public class QemuCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        QemuCraft.init();
    }
}
