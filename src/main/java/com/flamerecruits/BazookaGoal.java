package com.flamerecruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Рекрут із базукою Hostile Civilization (hostile_civilization:bazooka) стріляє снарядом, як гравець:
 * велика вогняна куля, що вибухає при влучанні. Пауза між пострілами - bazookaCooldownTicks.
 */
public class BazookaGoal extends Goal {
    private static final ResourceLocation ITEM_ID = new ResourceLocation("hostile_civilization", "bazooka");
    private static final int WINDUP = 12;

    private final AbstractRecruitEntity recruit;
    private long readyAt = 0L;
    private int windup;

    public BazookaGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    private static boolean isBazooka(ItemStack s) {
        Item item = ForgeRegistries.ITEMS.getValue(ITEM_ID);
        return item != null && s.is(item);
    }

    @Override
    public boolean canUse() {
        if (recruit.level().getGameTime() < readyAt || !recruit.level().isLoaded(recruit.blockPosition())) {
            return false;
        }
        LivingEntity target = recruit.getTarget();
        if (target == null || !target.isAlive() || recruit.getState() == 3 || recruit.needsToGetFood()) {
            return false;
        }
        if (!isBazooka(recruit.getMainHandItem()) && !hasInInventory()) {
            return false;
        }
        double d = recruit.distanceTo(target);
        if (d < FlameConfig.BAZOOKA_MIN_RANGE.get() || d > FlameConfig.BAZOOKA_MAX_RANGE.get()
                || !recruit.getSensing().hasLineOfSight(target)) {
            return false;
        }
        return !friendsNear(target);
    }

    private boolean hasInInventory() {
        recruit.switchMainHandItem(BazookaGoal::isBazooka);
        return isBazooka(recruit.getMainHandItem());
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = recruit.getTarget();
        return windup < WINDUP && t != null && t.isAlive() && recruit.getSensing().hasLineOfSight(t);
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
        if (++windup >= WINDUP && !friendsNear(target)) {
            fire(target);
            readyAt = recruit.level().getGameTime() + FlameConfig.BAZOOKA_COOLDOWN_TICKS.get();
        }
    }

    /** Свої (власник і рекрути власника) поруч із ціллю - у зону вибуху. */
    private boolean friendsNear(LivingEntity target) {
        UUID ownerId = recruit.getOwnerUUID();
        double r = Math.min(8.0D, FlameConfig.BAZOOKA_POWER.get() + 1.0D);
        for (LivingEntity e : recruit.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(r))) {
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

    private void fire(LivingEntity target) {
        if (!(recruit.level() instanceof ServerLevel level)) {
            return;
        }
        final int power = FlameConfig.BAZOOKA_POWER.get();
        final Level.ExplosionInteraction mode = FlameConfig.BAZOOKA_BLOCK_DAMAGE.get()
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        Vec3 eye = recruit.getEyePosition();
        Vec3 aim = target.getEyePosition().subtract(0.0D, target.getBbHeight() * 0.3D, 0.0D).subtract(eye).normalize();
        LargeFireball ball = new LargeFireball(level, recruit, aim.x, aim.y, aim.z, power) {
            @Override
            protected void onHit(HitResult result) {
                if (!this.level().isClientSide) {
                    this.level().explode(this, this.getX(), this.getY(), this.getZ(), (float) power, mode);
                    this.discard();
                }
            }

            @Override
            public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
                return false;
            }
        };
        ball.setOwner(recruit);
        ball.setPos(eye.x + aim.x, eye.y - 0.1D + aim.y, eye.z + aim.z);
        ball.shoot(aim.x, aim.y, aim.z, 2.5F, 0.3F);
        ball.xPower = aim.x * 0.2D;
        ball.yPower = aim.y * 0.2D;
        ball.zPower = aim.z * 0.2D;
        level.addFreshEntity(ball);
        level.playSound(null, recruit.getX(), recruit.getY(), recruit.getZ(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.NEUTRAL, 0.8F, 1.6F);
        recruit.swing(InteractionHand.MAIN_HAND);
    }
}
