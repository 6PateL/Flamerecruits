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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Lets a Recruits unit that holds the From Another World Flamethrower burn its target.
 * Mirrors FlamethrowerItem#onUseTick (spread 5, velocity 1.2) without needing the mod at compile time.
 */
public class FlameGoal extends Goal {
    private static final ResourceLocation ITEM_ID = new ResourceLocation("fromanotherworld", "flamethrower_mk_1");
    private static final ResourceLocation FIRE_ID = new ResourceLocation("fromanotherworld", "flamethrower_fire");

    private final AbstractRecruitEntity recruit;
    private int phaseTicks = 0;
    private boolean firing = true;
    private int repath = 0;

    public FlameGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private static boolean isFlamethrower(ItemStack s) {
        Item item = ForgeRegistries.ITEMS.getValue(ITEM_ID);
        return item != null && s.is(item);
    }

    private boolean hasFlamethrower() {
        if (isFlamethrower(recruit.getMainHandItem())) {
            return true;
        }
        recruit.switchMainHandItem(FlameGoal::isFlamethrower);
        return isFlamethrower(recruit.getMainHandItem());
    }

    @Override
    public boolean canUse() {
        LivingEntity target = recruit.getTarget();
        if (target == null || !target.isAlive() || recruit.getState() == 3 || recruit.needsToGetFood()) {
            return false;
        }
        if (Math.abs(target.getY() - recruit.getY()) > 8.0D
                || (target.getY() < recruit.getY() - 2.5D && !recruit.getSensing().hasLineOfSight(target))) {
            return false;
        }
        return ForgeRegistries.ENTITY_TYPES.containsKey(FIRE_ID) && hasFlamethrower();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        recruit.setAggressive(true);
        phaseTicks = 0;
        firing = true;
    }

    @Override
    public void stop() {
        recruit.setAggressive(false);
        recruit.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = recruit.getTarget();
        if (target == null) {
            return;
        }
        recruit.getLookControl().setLookAt(target, 40.0F, 40.0F);

        double range = FlameConfig.RANGE.get();
        double dist = recruit.distanceTo(target);
        boolean canSee = recruit.getSensing().hasLineOfSight(target);
        boolean inRange = dist <= range && canSee;

        boolean hold = recruit.getShouldHoldPos();
        Entity owner = recruit.getOwner();
        boolean followOwnerClose = recruit.getShouldFollow() && owner != null && owner.distanceToSqr(recruit) <= 100.0D;
        if (inRange) {
            recruit.getNavigation().stop();
        } else if (!hold && !followOwnerClose && --repath <= 0) {
            repath = 10;
            recruit.getNavigation().moveTo(target, 1.1D);
        }

        // bursts: fire for a while, then pause
        phaseTicks++;
        if (firing && phaseTicks > FlameConfig.BURST_TICKS.get()) {
            firing = false;
            phaseTicks = 0;
        } else if (!firing && phaseTicks > FlameConfig.PAUSE_TICKS.get()) {
            firing = true;
            phaseTicks = 0;
        }
        if (!inRange || !firing || recruit.tickCount % FlameConfig.FIRE_INTERVAL_TICKS.get() != 0) {
            return;
        }
        if (FlameConfig.AVOID_FRIENDLY_FIRE.get() && friendlyInLine(target)) {
            return;
        }
        spit(target);
    }

    private void spit(LivingEntity target) {
        if (!(recruit.level() instanceof ServerLevel level)) {
            return;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(FIRE_ID);
        if (type == null) {
            return;
        }
        Entity e = type.create(level);
        if (!(e instanceof Projectile fire)) {
            return;
        }
        Vec3 eye = recruit.getEyePosition();
        Vec3 aim = target.getEyePosition().subtract(0.0D, target.getBbHeight() * 0.3D, 0.0D).subtract(eye);
        fire.setOwner(recruit);
        fire.setPos(eye.x, eye.y - 0.1D, eye.z);
        fire.shoot(aim.x, aim.y, aim.z, 1.2F, 5.0F);
        level.addFreshEntity(fire);
        level.playSound(null, recruit.getX(), recruit.getY(), recruit.getZ(), SoundEvents.FIRECHARGE_USE,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
        recruit.swing(InteractionHand.MAIN_HAND);
    }

    private boolean friendlyInLine(LivingEntity target) {
        Vec3 from = recruit.getEyePosition();
        Vec3 to = target.getEyePosition();
        AABB box = new AABB(from, to).inflate(1.0D);
        UUID ownerId = recruit.getOwnerUUID();
        for (LivingEntity e : recruit.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == recruit || e == target) {
                continue;
            }
            boolean friend = (ownerId != null && e.getUUID().equals(ownerId))
                    || (e instanceof AbstractRecruitEntity r && ownerId != null && ownerId.equals(r.getOwnerUUID()));
            if (friend && e.getBoundingBox().inflate(0.3D).clip(from, to).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
