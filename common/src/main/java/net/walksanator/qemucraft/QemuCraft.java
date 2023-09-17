package net.walksanator.qemucraft;

import com.google.common.base.Suppliers;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class QemuCraft {
    public static final String MOD_ID = "qemucraft";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(MOD_ID,Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register(new ResourceLocation(MOD_ID,"example_tab"), () ->
            CreativeTabRegistry.create(Component.translatable("creativeTab.qemu.example_tab"), () ->
                new ItemStack(QemuCraft.EXAMPLE_ITEM.get()))
    );

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);
    public static final RegistrySupplier<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () ->
            new Item(new Item.Properties().arch$tab(QemuCraft.CREATIVE_TAB)));
    
    public static void init() {
        CREATIVE_TABS.register();
        ITEMS.register();
    }
}
