// this was taken from RetroComputers
package net.walksanator.qemucraft.blocks

import net.dblsaiko.retrocomputers.client.gui.TerminalScreen
import net.minecraft.client.Minecraft
import net.walksanator.qemucraft.util.unsigned
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResult.FAIL
import net.minecraft.world.InteractionResult.SUCCESS
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.walksanator.qemucraft.QemuCraft
import kotlin.experimental.xor

class TerminalBlock(settings: Properties) : Block(settings), EntityBlock, BlockEntityTicker<TerminalEntity> {

    override fun use(state: BlockState, world: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        val te = world.getBlockEntity(pos) as? TerminalEntity ?: return FAIL
        if (world.isClientSide) Minecraft.getInstance().setScreen(TerminalScreen(te))
        return SUCCESS
    }

    override fun tick(world: Level, pos: BlockPos, state: BlockState, data: TerminalEntity) {
        var error = false

        when (data.command.toInt()) {
            1 -> data.getIndices(data.bx2, data.by2, data.bw, data.bh).forEach { data.screen[it] = data.bx1.toByte() }
            2 -> data.getIndices(data.bx2, data.by2, data.bw, data.bh).forEach { data.screen[it] = data.screen[it] xor 0x80.toByte() }
            3 -> data.getIndices(data.bx2, data.by2, data.bw, data.bh).zip(data.getIndices(data.bx1, data.by1, data.bw, data.bh)).forEach { (dest, src) -> data.screen[dest] = data.screen[src] }
            4 -> QemuCraft.RESOURCES.charset.copyInto(data.charset)
            255 -> Unit
            else -> error = true
        }

        if (data.command in 1..4) world.sendBlockUpdated(pos, state, state, 3)

        data.command = if (error) -1 else 0
    }

    override fun <T : BlockEntity?> getTicker(world: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        return if (!world.isClientSide && type == QemuCraft.TERMINAL_BE_TYPE.get()) {
            @Suppress("UNCHECKED_CAST")
            this as BlockEntityTicker<T>
        } else {
            null
        }
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState) = TerminalEntity(pos, state)

}

class TerminalEntity(pos: BlockPos, state: BlockState) : BlockEntity(QemuCraft.TERMINAL_BE_TYPE.get(), pos, state) {

    var busId: Byte = 1

    val screen = ByteArray(80 * 50) { 0x20 }
    val charset = QemuCraft.RESOURCES.charset!!.clone()
    val kb = ByteArray(16)

    var command: Byte = 0

    var row = 0
    var cx = 0
    var cy = 0
    var cm = 2
    var kbs = 0
    var kbp = 0

    var bx1 = 0
    var by1 = 0
    var bx2 = 0
    var by2 = 0
    var bw = 0
    var bh = 0

    var char = 0

    fun pushKey(byte: Byte): Boolean {
        return if ((kbp + 1) % 16 != kbs) {
            kb[kbp] = byte
            kbp = (kbp + 1) % 16
            true
        } else false
    }

    fun getIndices(x1: Int, y1: Int, w: Int, h: Int): Sequence<Int> = sequence {
        for (i in 0 until h) for (j in 0 until w) {
            val x = j + x1
            val y = i + y1

            if (x in 0 until 80 && y in 0 until 60)
                yield(x + 80 * y)
        }
    }

    fun readData(at: Byte): Byte {
        return when (val at = at.unsigned) {
            0x00 -> row.toByte()
            0x01 -> cx.toByte()
            0x02 -> cy.toByte()
            0x03 -> cm.toByte()
            0x04 -> kbs.toByte()
            0x05 -> kbp.toByte()
            0x06 -> kb[kbs]
            0x07 -> command
            0x08 -> bx1.toByte()
            0x09 -> by1.toByte()
            0x0A -> bx2.toByte()
            0x0B -> by2.toByte()
            0x0C -> bw.toByte()
            0x0D -> bh.toByte()
            0x0E -> char.toByte()
            in 0x10..0x5F -> screen[row * 80 + at - 0x10]
            in 0x60..0x67 -> charset[char * 8 + at - 0x60]
            else -> 0
        }
    }

     fun storeData(at: Byte, data: Byte) {
        when (val at = at.unsigned) {
            0x00 -> row = data.unsigned % 50
            0x01 -> cx = data.unsigned % 80
            0x02 -> cy = data.unsigned % 50
            0x03 -> cm = data.unsigned % 3
            0x04 -> kbs = data.unsigned % 16
            0x05 -> kbp = data.unsigned % 16
            0x06 -> kb[kbs] = data
            0x07 -> command = data
            0x08 -> bx1 = data.unsigned % 80
            0x09 -> by1 = data.unsigned % 50
            0x0A -> bx2 = data.unsigned % 80
            0x0B -> by2 = data.unsigned % 50
            0x0C -> bw = data.unsigned
            0x0D -> bh = data.unsigned
            0x0E -> char = data.unsigned
            in 0x10..0x5F -> screen[row * 80 + at - 0x10] = data
            in 0x60..0x67 -> charset[char * 8 + at - 0x60] = data
        }

        val needsClientUpdate = at.unsigned in setOf(0x01, 0x02, 0x03) + (0x10..0x67)
        setChanged()
    }

    override fun getUpdateTag(): CompoundTag {
        val tag = super.getUpdateTag()
        // these are big, TODO: only send changed data
        tag.putByteArray("screen", screen)
        tag.putByteArray("charset", charset)
        tag.putByte("cx", cx.toByte())
        tag.putByte("cy", cy.toByte())
        tag.putByte("cm", cm.toByte())
        return tag
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.putByteArray("screen", screen)
        tag.putByteArray("charset", charset)
        tag.putByteArray("kb", kb)
        tag.putByte("command", command)
        tag.putByte("row", row.toByte())
        tag.putByte("cx", cx.toByte())
        tag.putByte("cy", cy.toByte())
        tag.putByte("cm", cm.toByte())
        tag.putByte("kbs", kbs.toByte())
        tag.putByte("kbp", kbp.toByte())
        tag.putByte("bx1", bx1.toByte())
        tag.putByte("by1", by1.toByte())
        tag.putByte("bx2", bx2.toByte())
        tag.putByte("by2", by2.toByte())
        tag.putByte("bw", bw.toByte())
        tag.putByte("bh", bh.toByte())
        tag.putByte("char", char.toByte())
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        val world = level

        tag.getByteArray("screen").copyInto(screen)
        tag.getByteArray("charset").copyInto(charset)
        cx = tag.getByte("cx").unsigned
        cy = tag.getByte("cy").unsigned
        cm = tag.getByte("cm").unsigned

        if (world == null || !world.isClientSide) {
            tag.getByteArray("screen").copyInto(screen)
            tag.getByteArray("charset").copyInto(charset)
            tag.getByteArray("kb").copyInto(kb)
            command = tag.getByte("command")
            row = tag.getByte("row").unsigned
            kbs = tag.getByte("kbs").unsigned
            kbp = tag.getByte("kbp").unsigned
            bx1 = tag.getByte("bx1").unsigned
            by1 = tag.getByte("by1").unsigned
            bx2 = tag.getByte("bx2").unsigned
            by2 = tag.getByte("by2").unsigned
            bw = tag.getByte("bw").unsigned
            bh = tag.getByte("bh").unsigned
            char = tag.getByte("char").unsigned
        }
    }

}