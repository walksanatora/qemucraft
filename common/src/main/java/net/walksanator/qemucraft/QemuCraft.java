package net.walksanator.qemucraft;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.walksanator.qemucraft.blocks.TerminalBlock;
import net.walksanator.qemucraft.blocks.TerminalEntity;

import java.util.function.Supplier;

public class QemuCraft {
    public static final Resources RESOURCES = new Resources();
    public static final String MOD_ID = "qemucraft";

    public static final ResourceLocation KEY_PRESS_PACKET = new ResourceLocation(MOD_ID,"key_press");

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(MOD_ID,Registries.CREATIVE_MODE_TAB);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MOD_ID,Registries.BLOCK);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(MOD_ID,Registries.BLOCK_ENTITY_TYPE);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);
    public static final RegistrySupplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register(new ResourceLocation(MOD_ID,"example_tab"), () ->
            CreativeTabRegistry.create(Component.translatable("creativeTab.qemu.example_tab"), () ->
                new ItemStack(QemuCraft.EXAMPLE_ITEM.get()))
    );

    public static final RegistrySupplier<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () ->
            new Item(new Item.Properties().arch$tab(QemuCraft.CREATIVE_TAB)));

    public static final RegistrySupplier<Block> TERMINAL_BLOCK = blockItem("terminal_block",()->new TerminalBlock(BlockBehaviour.Properties.of()),new Item.Properties().arch$tab(CREATIVE_TAB));

    public static final RegistrySupplier<BlockEntityType<TerminalEntity>> TERMINAL_BE_TYPE = BLOCK_ENTITY_TYPES.register(new ResourceLocation(MOD_ID,"terminal_blockentity"),
            () -> BlockEntityType.Builder.of(TerminalEntity::new,TERMINAL_BLOCK.get()).build(null)
    );


    public static void init() {
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES,RESOURCES);
        CREATIVE_TABS.register();
        BLOCKS.register();
        BLOCK_ENTITY_TYPES.register();
        ITEMS.register();

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, KEY_PRESS_PACKET, (buf, context) -> {
            BlockPos pos = buf.readBlockPos();
            byte key = buf.readByte();
            if (pos.distToCenterSqr(context.getPlayer().getEyePosition(1f)) > 100) {return;};
            context.getPlayer().getServer().execute(() -> {
                TerminalEntity te = context.getPlayer().level().getBlockEntity(pos, TERMINAL_BE_TYPE.get()).get();
                te.pushKey(key);
            });
        });
    }

    public static <T extends Block> RegistrySupplier<T> blockItem(String name, Supplier<T> block, Item.Properties props) {
        RegistrySupplier<T> blockRegistered = BLOCKS.register(new ResourceLocation(MOD_ID, name), block);
        ITEMS.register(new ResourceLocation(MOD_ID, name), () -> new BlockItem(block.get(), props));
        return blockRegistered;
    }
}
