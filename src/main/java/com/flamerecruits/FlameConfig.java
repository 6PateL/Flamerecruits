package com.flamerecruits;

import net.minecraftforge.common.ForgeConfigSpec;

public class FlameConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue RANGE;
    public static final ForgeConfigSpec.IntValue FIRE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue BURST_TICKS;
    public static final ForgeConfigSpec.IntValue PAUSE_TICKS;
    public static final ForgeConfigSpec.BooleanValue AVOID_FRIENDLY_FIRE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        RANGE = b.comment("Distance (blocks) at which a recruit starts burning its target. The flames themselves reach about 10-12 blocks.")
                .defineInRange("range", 9.0D, 2.0D, 16.0D);
        FIRE_INTERVAL_TICKS = b.comment("Ticks between two flame puffs while firing (the player flamethrower fires every tick).")
                .defineInRange("fireIntervalTicks", 2, 1, 20);
        BURST_TICKS = b.comment("How long (ticks) a recruit fires continuously before pausing.")
                .defineInRange("burstTicks", 60, 5, 600);
        PAUSE_TICKS = b.comment("Pause (ticks) between two bursts.")
                .defineInRange("pauseTicks", 20, 0, 600);
        AVOID_FRIENDLY_FIRE = b.comment("Do not fire if the owner or another recruit of the same owner stands in the line of fire.")
                .define("avoidFriendlyFire", true);
        SPEC = b.build();
    }
}
