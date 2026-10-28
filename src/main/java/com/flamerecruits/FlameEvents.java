package com.flamerecruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FlameEvents {
    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractRecruitEntity recruit)) {
            return;
        }
        for (WrappedGoal g : recruit.goalSelector.getAvailableGoals()) {
            if (g.getGoal() instanceof FlameGoal) {
                return;
            }
        }
        recruit.goalSelector.addGoal(1, new FlameGoal(recruit));
    }
}
