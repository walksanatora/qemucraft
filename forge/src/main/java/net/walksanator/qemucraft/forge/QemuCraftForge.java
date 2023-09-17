package net.walksanator.qemucraft.forge;

import dev.architectury.platform.forge.EventBuses;
import net.walksanator.qemucraft.QemuCraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(QemuCraft.MOD_ID)
public class QemuCraftForge {
    public QemuCraftForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(QemuCraft.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        QemuCraft.init();
    }
}
