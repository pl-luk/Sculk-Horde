package com.github.sculkhorde.common.entity;

import com.github.sculkhorde.common.entity.goal.*;
import com.github.sculkhorde.util.TargetParameters;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SculkCreeperEntity extends Creeper implements ISculkSmartEntity, GeoEntity
{
    private boolean isParticipatingInRaid = false;

    // Controls what types of entities this mob can target
    private TargetParameters TARGET_PARAMETERS = new TargetParameters(this).enableTargetHostiles().enableMustReachTarget();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SculkCreeperEntity(EntityType<? extends Creeper> entityType, Level level) {
        super(entityType, level);
    }
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new DespawnWhenIdle(this, TickUnits.convertMinutesToTicks(3)));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SwellGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new BlowUpPriorityBlockGoal(this, 1.0F, 3, 4, 5));
        this.goalSelector.addGoal(6, new PathFindToRaidLocation<>(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.targetSelector.addGoal(1, new NearestLivingEntityTargetGoal<>(this, true, true));
        this.targetSelector.addGoal(2, new TargetAttacker(this).setAlertAllies());
    }

    @Override
    public void checkDespawn() {}

    @Override
    public boolean isParticipatingInRaid() {
        return isParticipatingInRaid;
    }

    @Override
    public void setParticipatingInRaid(boolean isParticipatingInRaidIn) {
        isParticipatingInRaid = isParticipatingInRaidIn;
    }

    @Override
    public TargetParameters getTargetParameters() {
        return TARGET_PARAMETERS;
    }

    @Override
    public boolean isIdle() {
        return getTarget() == null;
    }

    public void explodeSculkCreeper()
    {
        if (!this.level.isClientSide)
        {
            this.dead = true;
            this.level.explode(this, this.getX(), this.getY(), this.getZ(), 4.0F, Level.ExplosionInteraction.MOB);
            this.discard();
        }
    }

    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("misc.idle");
    private static final RawAnimation RUN_ANIMATION = RawAnimation.begin().thenLoop("move.run");
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("move.walk");
    private static final RawAnimation SWELL_ANIMATION = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation DESWELL_ANIMATION = RawAnimation.begin().thenPlay("unattack");
    private final AnimationController LIVING_CONTROLLER = DefaultAnimations.genericLivingController(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        LIVING_CONTROLLER.setTransitionLength(5);
        controllers.add(
                LIVING_CONTROLLER,
                new AnimationController<>(this, "Walk_cycle", 5, this::poseWalkCycle),
                new AnimationController<>(this, "attack_cycle", 5, this::poseAttackCycle)
        );
    }

    // Create the animation handler for the body segment
    protected PlayState poseWalkCycle(AnimationState<SculkCreeperEntity> state)
    {
        if(!state.isMoving())
        {
            state.setAnimation(IDLE_ANIMATION);
        }
        else
        {
            state.setAnimation(RUN_ANIMATION);
        }

        return PlayState.CONTINUE;
    }

    // Create the animation handler for the body segment
    protected PlayState poseAttackCycle(AnimationState<SculkCreeperEntity> state)
    {
        if(this.getSwellDir() > 0)
        {
            state.setAnimation(SWELL_ANIMATION);
        }
        else if(this.getSwellDir() < 0)
        {
            state.setAnimation(DESWELL_ANIMATION);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}