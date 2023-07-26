package com.github.sculkhorde.common.entity.boss.sculk_enderman;

import com.github.sculkhorde.core.EntityRegistry;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.Vec3;

public class EnderBubbleAttackGoal extends MeleeAttackGoal
{
    protected int maxAttackDuration = 0;
    protected int elapsedAttackDuration = 0;
    protected EnderBubbleAttackEntity attackBubble;

    public EnderBubbleAttackGoal(PathfinderMob mob, int durationInTicks) {
        super(mob, 0.0F, true);
        maxAttackDuration = durationInTicks;
    }

    private SculkEndermanEntity getSculkEnderman()
    {
        return (SculkEndermanEntity)this.mob;
    }

    @Override
    public boolean canUse()
    {
        if(!getSculkEnderman().isSpecialAttackReady() || mob.getTarget() == null)
        {
            return false;
        }

        if(getSculkEnderman().getHealth() > 0.5F * getSculkEnderman().getMaxHealth())
        {
            return false;
        }

        if(!mob.closerThan(mob.getTarget(), 3.0D))
        {
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse()
    {
        return elapsedAttackDuration < maxAttackDuration;
    }

    @Override
    public void start()
    {
        super.start();
        // TODO Trigger Animation

        //Disable mob's movement for 10 seconds
        this.mob.getNavigation().stop();

        //Spawn Ender Attack Bubble entity
        BlockPos spawnPos = new BlockPos((int) this.mob.getX(), (int) (this.mob.getY() + (this.mob.getBbHeight() / 2)), (int) this.mob.getZ());
        EnderBubbleAttackEntity.spawn( (ServerLevel) mob.level(), mob, spawnPos, EntityRegistry.ENDER_BUBBLE_ATTACK.get());

        getSculkEnderman().canTeleport = false;
        this.mob.setInvulnerable(true);
        getSculkEnderman().addEffect(new MobEffectInstance(MobEffects.REGENERATION, TickUnits.convertSecondsToTicks(10), 2));
    }

    @Override
    public void tick()
    {
        super.tick();
        elapsedAttackDuration++;
    }

    @Override
    public void stop()
    {
        super.stop();
        getSculkEnderman().resetSpecialAttackCooldown();
        attackBubble.discard();
        elapsedAttackDuration = 0;
        getSculkEnderman().canTeleport = true;
        this.mob.setInvulnerable(false);
    }
}
