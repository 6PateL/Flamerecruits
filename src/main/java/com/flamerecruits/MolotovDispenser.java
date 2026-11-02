package com.flamerecruits;

import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.registries.ForgeRegistries;

/** Роздатчик викидає молотов як снаряд. */
final class MolotovDispenser {
    private MolotovDispenser() {}

    static void register() {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("hostile_civilization", "molotov"));
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hostile_civilization", "molotov_projectile"));
        if (item == null || type == null || item == net.minecraft.world.item.Items.AIR) {
            return;
        }
        DispenserBlock.registerBehavior(item, new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level level, Position pos, ItemStack stack) {
                Entity e = type.create(level);
                if (e instanceof AbstractArrow arrow) {
                    arrow.setPos(pos.x(), pos.y(), pos.z());
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                    return arrow;
                }
                throw new IllegalStateException("molotov projectile is not an arrow");
            }

            @Override
            protected float getUncertainty() {
                return 3.0F;
            }

            @Override
            protected float getPower() {
                return 1.4F;
            }
        });
    }
}
