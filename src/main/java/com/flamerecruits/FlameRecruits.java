package com.flamerecruits;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(FlameRecruits.MODID)
public class FlameRecruits {
    public static final String MODID = "flamerecruits";

    public FlameRecruits() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FlameConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new FlameEvents());
        MinecraftForge.EVENT_BUS.register(new Throwers());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    /** Робимо вогнемет скінченним: задаємо максимальну міцність предмету (поле Item.maxDamage). */
    private void setup(FMLCommonSetupEvent event) {
        int dur = FlameConfig.DURABILITY.get();
        event.enqueueWork(MolotovDispenser::register);
        int charges = FlameConfig.BAZOOKA_CHARGES.get();
        if (charges > 0) {
            event.enqueueWork(() -> {
                Item bz = ForgeRegistries.ITEMS.getValue(new ResourceLocation("hostile_civilization", "bazooka"));
                if (bz != null && bz != net.minecraft.world.item.Items.AIR) {
                    ObfuscationReflectionHelper.setPrivateValue(Item.class, bz, charges, "f_41371_");
                }
            });
        }
        if (dur <= 0) {
            return;
        }
        event.enqueueWork(() -> {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("fromanotherworld", "flamethrower_mk_1"));
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                return;
            }
            ObfuscationReflectionHelper.setPrivateValue(Item.class, item, dur, "f_41371_");
        });
    }
}
