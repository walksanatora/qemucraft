//this was taked from RetroComputers

package net.dblsaiko.retrocomputers.client.gui

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import io.netty.buffer.Unpooled
import com.mojang.blaze3d.vertex.PoseStack
import dev.architectury.networking.NetworkManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import net.walksanator.qemucraft.QemuCraft
import net.walksanator.qemucraft.QemuCraftClient
import net.walksanator.qemucraft.ShaderExpectPlatform
import net.walksanator.qemucraft.blocks.TerminalEntity
import net.walksanator.qemucraft.util.math.Mat4
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.*
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import kotlin.experimental.xor
import kotlin.math.round

private val buf = BufferUtils.createByteBuffer(16384)

private val vbo = GL30.glGenBuffers()
private val vao = GL30.glGenVertexArrays()
private val screenTex = createTexture()
private val charsetTex = createTexture()

private const val scale = 8

class TerminalScreen(val te: TerminalEntity) : Screen(Component.translatable("block.qemucraft.terminal")) {

    private var uMvp = 0
    private var uCharset = 0
    private var uScreen = 0
    private var aXyz = 0
    private var aUv = 0

    private var fb: RenderTarget? = null

    override fun tick() {
        val minecraft = minecraft ?: return
        val dist = minecraft.player?.getEyePosition(1f)?.distanceToSqr(Vec3.atCenterOf(te.blockPos))
            ?: Double.POSITIVE_INFINITY
        if (dist > 10 * 10) minecraft.setScreen(null)
    }

    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        render(guiGraphics.pose(),i,j,f)
    }

    fun render(matrices: PoseStack, mouseX: Int, mouseY: Int, delta: Float) {
        //renderBackground(matrices)

        val sh = ShaderExpectPlatform.getShader();
        val fb = fb ?: return
        val mc = minecraft ?: return

        fb.setFilterMode(if ((mc.window.guiScale.toInt() % 2) == 0) GL11.GL_NEAREST else GL11.GL_LINEAR)

        fb.bindWrite(true)
        val mat = Mat4.ortho(0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 1.0f)

        GL30.glUseProgram(sh)
        GL30.glBindVertexArray(vao)
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo)

        GL20.glEnableVertexAttribArray(aXyz)
        GL20.glEnableVertexAttribArray(aUv)

        RenderSystem.activeTexture(GL13.GL_TEXTURE0)
        //RenderSystem.enableTexture()
        RenderSystem.bindTexture(screenTex)

        buf.clear()
        val fbuf = buf.asFloatBuffer()
        mat.intoBuffer(fbuf)
        fbuf.flip()
        GL30.glUniformMatrix4fv(uMvp, false, fbuf)

        GL30.glUniform1i(uScreen, 0)

        buf.clear()
        buf.put(te.screen)

        if (te.cm == 1 || (te.cm == 2 && (System.currentTimeMillis() / 500) % 2 == 0L)) {
            val ci = te.cx + te.cy * 80
            buf.put(ci, te.screen[ci] xor 0x80.toByte())
        }

        buf.rewind()
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R16I, 80, 60, 0, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_BYTE, buf.asIntBuffer())

        RenderSystem.activeTexture(GL13.GL_TEXTURE2)
        //RenderSystem.enableTexture()
        RenderSystem.bindTexture(charsetTex)
        GL30.glUniform1i(uCharset, 2)

        buf.clear()
        buf.put(te.charset)
        buf.rewind()
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R16I, 8, 256, 0, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_BYTE, buf.asIntBuffer())

        GL11.glDrawArrays(GL_TRIANGLES, 0, 6)

        GL20.glDisableVertexAttribArray(aXyz)
        GL20.glDisableVertexAttribArray(aUv)

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
        GL30.glUseProgram(0)

        RenderSystem.bindTexture(0)
        //RenderSystem.disableTexture()
        RenderSystem.activeTexture(GL13.GL_TEXTURE0)
        RenderSystem.bindTexture(0)

        mc.mainRenderTarget.bindWrite(true)

        val swidth = 8 * 80 * 0.5
        val sheight = 8 * 50 * 0.5
        val x1 = round(width / 2.0 - swidth / 2.0)
        val y1 = round(height / 2.0 - sheight / 2.0)

        matrices.pushPose()
        matrices.translate(x1, y1, -2000.0) // why the -2000? not sure

        val shader = mc.gameRenderer.blitShader
        shader.setSampler("DiffuseSampler", fb.colorTextureId)
        shader.MODEL_VIEW_MATRIX?.set(matrices.last().pose())
        shader.PROJECTION_MATRIX?.set(RenderSystem.getProjectionMatrix())
        shader.apply()

        val t = RenderSystem.renderThreadTesselator()
        val buf = t.builder
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
        buf.vertex(0.0, 0.0, 0.0).uv(0f, 1f).color(255, 255, 255, 255).endVertex()
        buf.vertex(0.0, sheight, 0.0).uv(0f, 0f).color(255, 255, 255, 255).endVertex()
        buf.vertex(swidth, sheight, 0.0).uv(1f, 0f).color(255, 255, 255, 255).endVertex()
        buf.vertex(swidth, 0.0, 0.0).uv(1f, 1f).color(255, 255, 255, 255).endVertex()
        buf.end()
        //BufferUploader.postDraw(buf)

        shader.clear()

        matrices.popPose()
    }

    override fun keyPressed(key: Int, scancode: Int, modifiers: Int): Boolean {
        if (super.keyPressed(key, scancode, modifiers)) return true

        val result: Byte? = when (key) {
            GLFW.GLFW_KEY_BACKSPACE -> 0x08
            GLFW.GLFW_KEY_ENTER -> 0x0D
            GLFW.GLFW_KEY_HOME -> 0x80
            GLFW.GLFW_KEY_END -> 0x81
            GLFW.GLFW_KEY_UP -> 0x82
            GLFW.GLFW_KEY_DOWN -> 0x83
            GLFW.GLFW_KEY_LEFT -> 0x84
            GLFW.GLFW_KEY_RIGHT -> 0x85
            else -> null
        }?.toByte()

        if (result != null) pushKey(result)

        return result != null
    }

    override fun charTyped(c: Char, modifiers: Int): Boolean {
        if (super.charTyped(c, modifiers)) return true

        val result: Byte? = when (c) {
            in '\u0001'..'\u007F' -> c.code.toByte()
            else -> null
        }

        if (result != null) pushKey(result)

        return result != null
    }

    private fun pushKey(c: Byte) {
        val buffer = FriendlyByteBuf(Unpooled.buffer())
        buffer.writeBlockPos(te.blockPos)
        buffer.writeByte(c.toInt())
        NetworkManager.sendToServer(QemuCraft.KEY_PRESS_PACKET, buffer)
    }

    override fun init() {
        //minecraft!!.keyboardHandler.setRepeatEvents(true)

        initDrawData()
        initFb()
    }

    private fun initDrawData() {
        val sh = ShaderExpectPlatform.getShader()

        GL30.glUseProgram(sh)
        GL30.glBindVertexArray(vao)
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo)

        uMvp = GL30.glGetUniformLocation(sh, "mvp")
        uCharset = GL30.glGetUniformLocation(sh, "charset")
        uScreen = GL30.glGetUniformLocation(sh, "screen")

        aXyz = GL30.glGetAttribLocation(sh, "xyz")
        aUv = GL30.glGetAttribLocation(sh, "uv")

        GL20.glVertexAttribPointer(aXyz, 3, GL_FLOAT, false, 20, 0)
        GL20.glVertexAttribPointer(aUv, 2, GL_FLOAT, false, 20, 12)

        buf.clear()

        floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            1f, 1f, 0f, 1f, 1f,
            1f, 0f, 0f, 1f, 0f,

            0f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 1f,
            1f, 1f, 0f, 1f, 1f
        ).forEach { buf.putFloat(it) }

        buf.rewind()

        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW)

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
        GL30.glUseProgram(0)
    }

    private fun initFb() {
        fb?.destroyBuffers()
        val scale = 4
        fb = TextureTarget(80 * 8 * scale, 50 * 8 * scale, false, Minecraft.ON_OSX)
    }

    override fun removed() {
        //minecraft!!.keyboardHandler.setRepeatEvents(false)
        fb?.destroyBuffers()
        fb = null
    }

    override fun isPauseScreen() = false

}

private fun createTexture(): Int {
    val tex = TextureUtil.generateTextureId()
    RenderSystem.bindTexture(tex)
    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT)
    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT)
    RenderSystem.bindTexture(0)
    return tex
}