package com.flamerecruits;

import net.minecraftforge.common.ForgeConfigSpec;

public class FlameConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue RANGE;
    public static final ForgeConfigSpec.IntValue FIRE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue BURST_TICKS;
    public static final ForgeConfigSpec.IntValue PAUSE_TICKS;
    public static final ForgeConfigSpec.BooleanValue AVOID_FRIENDLY_FIRE;
    public static final ForgeConfigSpec.IntValue DURABILITY;
    public static final ForgeConfigSpec.IntValue PLAYER_TICKS_PER_DAMAGE;
    public static final ForgeConfigSpec.IntValue RECRUIT_SHOTS_PER_DAMAGE;
    public static final ForgeConfigSpec.IntValue REPAIR_PER_DIAMOND;
    public static final ForgeConfigSpec.IntValue BAZOOKA_POWER;
    public static final ForgeConfigSpec.BooleanValue BAZOOKA_BLOCK_DAMAGE;
    public static final ForgeConfigSpec.IntValue BAZOOKA_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue BAZOOKA_MIN_RANGE;
    public static final ForgeConfigSpec.DoubleValue BAZOOKA_MAX_RANGE;

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
        DURABILITY = b.comment("Max durability of the flamethrower (0 = unlimited, as in the original mod). Needs a restart.")
                .defineInRange("durability", 200, 0, 100000);
        PLAYER_TICKS_PER_DAMAGE = b.comment("Ticks of continuous fire by a player that cost 1 durability.")
                .defineInRange("playerTicksPerDamage", 6, 1, 200);
        RECRUIT_SHOTS_PER_DAMAGE = b.comment("Flame puffs fired by a recruit that cost 1 durability.")
                .defineInRange("recruitShotsPerDamage", 3, 1, 200);
        REPAIR_PER_DIAMOND = b.comment("Durability restored by one diamond in an anvil.")
                .defineInRange("repairPerDiamond", 100, 1, 100000);
        BAZOOKA_POWER = b.comment("Explosion power of a recruit's bazooka shell (the player's one in Hostile Civilization is 12).")
                .defineInRange("bazookaPower", 8, 1, 20);
        BAZOOKA_BLOCK_DAMAGE = b.comment("Whether a recruit's bazooka shell destroys blocks (false protects villages).")
                .define("bazookaBlockDamage", false);
        BAZOOKA_COOLDOWN_TICKS = b.comment("Ticks between two shots (the player's cooldown is 600).")
                .defineInRange("bazookaCooldownTicks", 600, 20, 12000);
        BAZOOKA_MIN_RANGE = b.comment("A recruit will not fire at targets closer than this (blocks) - the blast would hit friends.")
                .defineInRange("bazookaMinRange", 9.0D, 2.0D, 32.0D);
        BAZOOKA_MAX_RANGE = b.comment("Maximum firing distance for the bazooka (blocks).")
                .defineInRange("bazookaMaxRange", 30.0D, 8.0D, 64.0D);
        SPEC = b.build();
    }
}
