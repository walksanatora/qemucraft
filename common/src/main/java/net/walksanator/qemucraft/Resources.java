package net.walksanator.qemucraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static net.walksanator.qemucraft.QemuCraft.MOD_ID;

public class Resources implements PreparableReloadListener {
    public byte[] charset = null;
    public static ResourcesKT RES_KT = new ResourcesKT();

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2) {

        return CompletableFuture.supplyAsync(() -> {
            this.charset = loadImage(resourceManager,"charset.bin");
            return null;
        });
    }

    private static byte[] loadImage(ResourceManager manager, String name) {
        var res = manager.getResource(new ResourceLocation(MOD_ID, name));
        try {
            return res.get().open().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
