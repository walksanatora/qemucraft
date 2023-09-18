package net.walksanator.qemucraft.fabric

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType.CLIENT_RESOURCES
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import net.walksanator.qemucraft.QemuCraft.MOD_ID
import net.walksanator.qemucraft.Resources
import org.lwjgl.opengl.GL30
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object Shaders {

    private var screen = 0

    fun screen() = screen

    fun init() {
        ResourceManagerHelper.get(CLIENT_RESOURCES).registerReloadListener(object : IdentifiableResourceReloadListener {

            override fun reload(s: PreparationBarrier, rm: ResourceManager, profiler: ProfilerFiller, profiler1: ProfilerFiller, executor: Executor, executor1: Executor): CompletableFuture<Void> {
                return CompletableFuture.runAsync({
                    if (screen != 0) GL30.glDeleteProgram(screen)

                    screen = Resources.RES_KT.loadShader(rm, "screen")
                }, executor1).thenCompose { s.wait(null) }
            }

            override fun getFabricId(): ResourceLocation = ResourceLocation(MOD_ID, "shaders")

        })
    }
}