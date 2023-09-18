package net.walksanator.qemucraft.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.walksanator.qemucraft.QemuCraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.walksanator.qemucraft.QemuCraftClient;
import net.walksanator.qemucraft.Resources;

@Mod(QemuCraft.MOD_ID)
public class QemuCraftForge {
    public static int SHADER_NUM = 0;
    public QemuCraftForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(QemuCraft.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        QemuCraft.init();
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(QemuCraftClient::initClient);
        event.enqueueWork(() -> {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            SHADER_NUM = Resources.RES_KT.loadShader(rm,"screen");
        });
    }
}
