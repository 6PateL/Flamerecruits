package com.flamerecruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.registries.ForgeRegistries;
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
        recruit.goalSelector.addGoal(0, new BazookaGoal(recruit));
    }

    private static final ResourceLocation ITEM_ID = new ResourceLocation("fromanotherworld", "flamethrower_mk_1");

    /** Гравець палить: кожні N тіків — 1 міцності. */
    @SubscribeEvent
    public void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || FlameConfig.DURABILITY.get() <= 0) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(ITEM_ID)) {
            return;
        }
        if (player.tickCount % FlameConfig.PLAYER_TICKS_PER_DAMAGE.get() == 0) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            if (stack.isEmpty()) {
                player.stopUsingItem();
            }
        }
    }

    /** Ремонт алмазами на ковалі. */
    @SubscribeEvent
    public void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (FlameConfig.DURABILITY.get() <= 0 || !left.isDamaged() || !event.getRight().is(Items.DIAMOND)
                || !ForgeRegistries.ITEMS.getKey(left.getItem()).equals(ITEM_ID)) {
            return;
        }
        ItemStack out = left.copy();
        int per = FlameConfig.REPAIR_PER_DIAMOND.get();
        int used = 0;
        while (used < event.getRight().getCount() && out.getDamageValue() > 0) {
            out.setDamageValue(Math.max(0, out.getDamageValue() - per));
            used++;
        }
        event.setOutput(out);
        event.setMaterialCost(used);
        event.setCost(used);
    }
}
