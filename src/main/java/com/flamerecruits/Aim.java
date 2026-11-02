package com.flamerecruits;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Перевірка, що снаряд долетить до цілі, а не вріжеться в стіну прямо перед стрільцем. */
final class Aim {
    private Aim() {}

    static boolean clear(Mob shooter, LivingEntity target) {
        Vec3 from = shooter.getEyePosition();
        Vec3 to = target.getEyePosition().subtract(0.0D, target.getBbHeight() * 0.3D, 0.0D);
        if (!shooter.getSensing().hasLineOfSight(target)) {
            return false;
        }
        HitResult hit = shooter.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, shooter));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < 1.5D * 1.5D;
    }
}
