package com.flamerecruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Базука та молотов для УСІХ типів рекрутів. Працює не через goalSelector (де ближній бій/лук перебивали ціль),
 * а через тік рекрута: предмет має бути в руці (основній або лівій). Якщо тип рекрута не може тримати предмет
 * в основній руці (лучник, щитоносець...) - достатньо лівої руки; предмет із рюкзака переходить у ліву руку
 * лише коли є ціль у зоні дії. Мін/макс дальність діють тільки з предметом у руці.
 */
public class Throwers {
    private static final ResourceLocation BAZOOKA = new ResourceLocation("hostile_civilization", "bazooka");
    private static final ResourceLocation MOLOTOV = new ResourceLocation("hostile_civilization", "molotov");
    private static final ResourceLocation MOLOTOV_PROJ = new ResourceLocation("hostile_civilization", "molotov_projectile");
    private static final String T_READY = "flamerecruits_ready";
    private static final String T_WIND = "flamerecruits_wind";

    static boolean is(ItemStack s, ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item != null && item != net.minecraft.world.item.Items.AIR && s.is(item);
    }

    @SubscribeEvent
    public void onTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AbstractRecruitEntity r) || r.level().isClientSide
                || r.tickCount % 2 != 0 || !r.isAlive()) {
            return;
        }
        LivingEntity target = r.getTarget();
        if (target == null || !target.isAlive() || r.getState() == 3 || r.needsToGetFood()) {
            r.getPersistentData().putInt(T_WIND, 0);
            return;
        }
        long now = r.level().getGameTime();
        if (now < r.getPersistentData().getLong(T_READY)) {
            return;
        }
        double d = r.distanceTo(target);
        boolean bazooka = tryWeapon(r, target, d, BAZOOKA, FlameConfig.BAZOOKA_MIN_RANGE.get(),
                FlameConfig.BAZOOKA_MAX_RANGE.get(), 12, true);
        if (!bazooka) {
            tryWeapon(r, target, d, MOLOTOV, FlameConfig.MOLOTOV_MIN_RANGE.get(),
                    FlameConfig.MOLOTOV_MAX_RANGE.get(), 8, false);
        }
    }

    private boolean tryWeapon(AbstractRecruitEntity r, LivingEntity target, double d, ResourceLocation id,
                              double min, double max, int windup, boolean bazooka) {
        if (d < min || d > max) {
            return false;
        }
        InteractionHand hand = null;
        if (is(r.getMainHandItem(), id)) {
            hand = InteractionHand.MAIN_HAND;
        } else if (is(r.getOffhandItem(), id)) {
            hand = InteractionHand.OFF_HAND;
        } else if (r.getOffhandItem().isEmpty() && pullToOffhand(r, id)) {
            hand = InteractionHand.OFF_HAND;
        }
        if (hand == null) {
            return false;
        }
        if (!Aim.clear(r, target) || friendsNear(r, target, bazooka ? Math.min(8.0D, FlameConfig.BAZOOKA_POWER.get() + 1.0D) : 3.5D)) {
            r.getPersistentData().putInt(T_WIND, 0);
            return true;
        }
        r.getLookControl().setLookAt(target, 60.0F, 60.0F);
        int w = r.getPersistentData().getInt(T_WIND) + 2;
        r.getPersistentData().putInt(T_WIND, w);
        if (w >= windup) {
            r.getPersistentData().putInt(T_WIND, 0);
            if (bazooka) {
                fireBazooka(r, target);
            } else {
                throwMolotov(r, target);
            }
            r.swing(hand);
            if (bazooka && FlameConfig.BAZOOKA_CHARGES.get() > 0) {
                final InteractionHand usedHand = hand;
                ItemStack st = r.getItemInHand(usedHand);
                st.hurtAndBreak(1, r, e -> e.broadcastBreakEvent(usedHand));
            }
            r.getPersistentData().putLong(T_READY, r.level().getGameTime()
                    + (bazooka ? FlameConfig.BAZOOKA_COOLDOWN_TICKS.get() : FlameConfig.MOLOTOV_COOLDOWN_TICKS.get()));
        }
        return true;
    }

    /** Перекладає предмет з рюкзака в ліву руку (в обхід canHoldItem). */
    private boolean pullToOffhand(AbstractRecruitEntity r, ResourceLocation id) {
        var inv = r.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (is(s, id)) {
                ItemStack one = s.copy();
                one.setCount(1);
                // у лівій руці тримаємо цілу стопку, якщо це базука; молотови - по одному
                if (is(s, BAZOOKA)) {
                    inv.setItem(i, ItemStack.EMPTY);
                    r.setItemSlot(EquipmentSlot.OFFHAND, s);
                } else {
                    s.shrink(1);
                    if (s.isEmpty()) {
                        inv.setItem(i, ItemStack.EMPTY);
                    }
                    r.setItemSlot(EquipmentSlot.OFFHAND, one);
                }
                return true;
            }
        }
        return false;
    }

    private boolean friendsNear(AbstractRecruitEntity r, LivingEntity target, double radius) {
        UUID ownerId = r.getOwnerUUID();
        for (LivingEntity e : r.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius))) {
            if (e == r || e == target) {
                continue;
            }
            if (ownerId != null && (e.getUUID().equals(ownerId)
                    || (e instanceof AbstractRecruitEntity rr && ownerId.equals(rr.getOwnerUUID())))) {
                return true;
            }
        }
        return false;
    }

    private void fireBazooka(AbstractRecruitEntity r, LivingEntity target) {
        if (!(r.level() instanceof ServerLevel level)) {
            return;
        }
        final int power = FlameConfig.BAZOOKA_POWER.get();
        final Level.ExplosionInteraction mode = FlameConfig.BAZOOKA_BLOCK_DAMAGE.get()
                ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
        Vec3 eye = r.getEyePosition();
        Vec3 aim = target.getEyePosition().subtract(0.0D, target.getBbHeight() * 0.3D, 0.0D).subtract(eye).normalize();
        LargeFireball ball = new LargeFireball(level, r, aim.x, aim.y, aim.z, power) {
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
        ball.setOwner(r);
        ball.setPos(eye.x + aim.x, eye.y - 0.1D + aim.y, eye.z + aim.z);
        ball.shoot(aim.x, aim.y, aim.z, 2.5F, 0.3F);
        ball.xPower = aim.x * 0.2D;
        ball.yPower = aim.y * 0.2D;
        ball.zPower = aim.z * 0.2D;
        level.addFreshEntity(ball);
        level.playSound(null, r.getX(), r.getY(), r.getZ(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.NEUTRAL, 0.8F, 1.6F);
    }

    private void throwMolotov(AbstractRecruitEntity r, LivingEntity target) {
        if (!(r.level() instanceof ServerLevel level)) {
            return;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(MOLOTOV_PROJ);
        if (type == null) {
            return;
        }
        Entity e = type.create(level);
        if (!(e instanceof AbstractArrow arrow)) {
            return;
        }
        Vec3 eye = r.getEyePosition();
        double dx = target.getX() - r.getX();
        double dz = target.getZ() - r.getZ();
        double dy = target.getY(0.33D) - eye.y;
        double flat = Math.sqrt(dx * dx + dz * dz);
        arrow.setOwner(r);
        arrow.setPos(eye.x, eye.y - 0.1D, eye.z);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(dx, dy + flat * 0.2D, dz, 1.6F, 3.0F);
        level.addFreshEntity(arrow);
        level.playSound(null, r.getX(), r.getY(), r.getZ(), SoundEvents.SPLASH_POTION_THROW,
                SoundSource.NEUTRAL, 0.8F, 0.8F);
    }
}
