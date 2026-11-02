package com.flamerecruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Рекрут із молотовим (hostile_civilization:molotov) в РУЦІ кидає його в ціль. Дальність діє лише тоді, коли
 * предмет у руці; вогонь не кидається, якщо снаряд упреться в стіну або поруч свої.
 */
public class MolotovGoal extends Goal {
    private static final ResourceLocation ITEM_ID = new ResourceLocation("hostile_civilization", "molotov");
    private static final ResourceLocation PROJECTILE_ID = new ResourceLocation("hostile_civilization", "molotov_projectile");
    private static final int WINDUP = 8;

    private final AbstractRecruitEntity recruit;
    private long readyAt = 0L;
    private int windup;

    public MolotovGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    static boolean isMolotov(ItemStack s) {
        Item item = ForgeRegistries.ITEMS.getValue(ITEM_ID);
        return item != null && s.is(item);
    }

    @Override
    public boolean canUse() {
        if (recruit.level().getGameTime() < readyAt) {
            return false;
        }
        LivingEntity target = recruit.getTarget();
        if (target == null || !target.isAlive() || recruit.getState() == 3 || recruit.needsToGetFood()) {
            return false;
        }
        if (!isMolotov(recruit.getMainHandItem())) {
            return false;
        }
        double d = recruit.distanceTo(target);
        return d >= FlameConfig.MOLOTOV_MIN_RANGE.get() && d <= FlameConfig.MOLOTOV_MAX_RANGE.get()
                && Aim.clear(recruit, target) && !friendsNear(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = recruit.getTarget();
        return windup < WINDUP && t != null && t.isAlive() && isMolotov(recruit.getMainHandItem()) && Aim.clear(recruit, t);
    }

    @Override
    public void start() {
        windup = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = recruit.getTarget();
        if (target == null) {
            return;
        }
        recruit.getLookControl().setLookAt(target, 60.0F, 60.0F);
        if (++windup >= WINDUP && isMolotov(recruit.getMainHandItem()) && Aim.clear(recruit, target) && !friendsNear(target)) {
            throwIt(target);
            readyAt = recruit.level().getGameTime() + FlameConfig.MOLOTOV_COOLDOWN_TICKS.get();
        }
    }

    private boolean friendsNear(LivingEntity target) {
        UUID ownerId = recruit.getOwnerUUID();
        for (LivingEntity e : recruit.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.5D))) {
            if (e == recruit || e == target) {
                continue;
            }
            if (ownerId != null && (e.getUUID().equals(ownerId)
                    || (e instanceof AbstractRecruitEntity rr && ownerId.equals(rr.getOwnerUUID())))) {
                return true;
            }
        }
        return false;
    }

    private void throwIt(LivingEntity target) {
        if (!(recruit.level() instanceof ServerLevel level)) {
            return;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(PROJECTILE_ID);
        if (type == null) {
            return;
        }
        Entity e = type.create(level);
        if (!(e instanceof AbstractArrow arrow)) {
            return;
        }
        Vec3 eye = recruit.getEyePosition();
        double dx = target.getX() - recruit.getX();
        double dz = target.getZ() - recruit.getZ();
        double dy = target.getY(0.33D) - eye.y;
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.setOwner(recruit);
        arrow.setPos(eye.x, eye.y - 0.1D, eye.z);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(dx, dy + flat * 0.2D, dz, 1.6F, 3.0F);
        level.addFreshEntity(arrow);
        level.playSound(null, recruit.getX(), recruit.getY(), recruit.getZ(), SoundEvents.SPLASH_POTION_THROW,
                SoundSource.NEUTRAL, 0.8F, 0.8F);
        recruit.swing(InteractionHand.MAIN_HAND);
    }
}
