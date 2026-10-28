package com.flamerecruits;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(FlameRecruits.MODID)
public class FlameRecruits {
    public static final String MODID = "flamerecruits";

    public FlameRecruits() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FlameConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new FlameEvents());
    }
}
