package com.papyrusfollower.entity;

import com.papyrusfollower.PapyrusFollower;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * THE GREAT PAPYRUS. Feed a wild one spaghetti to befriend him; he then follows his owner,
 * fights whatever hurts them, and can be told to stay put with an empty-hand right click.
 */
public class PapyrusEntity extends TamableAnimal {
    private static final String[] TAME_LINES = {
            "NYEH HEH HEH! YOU HAVE BEFRIENDED THE GREAT PAPYRUS!",
            "SPAGHETTI?! HUMAN, YOU HAVE EXCELLENT TASTE! I WILL FOLLOW YOU!",
    };
    private static final String[] CHATTER_LINES = {
            "NYEH HEH HEH!",
            "I, THE GREAT PAPYRUS, AM A VERY GOOD COMPANION!",
            "WOWIE! WHAT A DELIGHTFUL ADVENTURE!",
            "DO YOU THINK THERE ARE ANY PUZZLES AROUND HERE? I AM VERY GOOD AT PUZZLES.",
            "I WILL NOT LET ANYTHING HARM YOU, HUMAN! EXCEPT MAYBE A CREEPER. THOSE ARE SCARY.",
            "WE SHOULD MAKE SOME SPAGHETTI LATER!",
    };
    private static final String[] SIT_LINES = {
            "VERY WELL! I WILL WAIT RIGHT HERE!",
            "I SHALL GUARD THIS SPOT WITH MY LIFE!",
    };
    private static final String[] FOLLOW_LINES = {
            "ONWARD, TO ADVENTURE!",
            "I'M COMING, HUMAN!",
    };
    private static final String[] DEATH_LINES = {
            "I... I WILL NEVER GIVE UP ON YOU, HUMAN...",
    };

    public PapyrusEntity(EntityType<? extends PapyrusEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1, 6.0F, 2.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(PapyrusFollower.SPAGHETTI);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean spaghetti = stack.is(PapyrusFollower.SPAGHETTI);

        if (!this.isTame()) {
            if (!spaghetti) {
                return super.mobInteract(player, hand);
            }
            if (!this.level().isClientSide) {
                stack.consume(1, player);
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setOrderedToSit(false);
                this.level().broadcastEntityEvent(this, (byte) 7);
                say(player, pick(TAME_LINES));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (!this.isOwnedBy(player)) {
            return super.mobInteract(player, hand);
        }

        if (spaghetti) {
            if (this.getHealth() >= this.getMaxHealth()) {
                return InteractionResult.PASS;
            }
            if (!this.level().isClientSide) {
                stack.consume(1, player);
                this.heal(8.0F);
                this.level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            if (!this.level().isClientSide) {
                boolean sit = !this.isOrderedToSit();
                this.setOrderedToSit(sit);
                this.navigation.stop();
                this.setTarget(null);
                say(player, pick(sit ? SIT_LINES : FOLLOW_LINES));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.isTame() && this.random.nextInt(6000) == 0
                && this.getOwner() instanceof Player owner && this.distanceToSqr(owner) < 256.0) {
            say(owner, pick(CHATTER_LINES));
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.getOwner() instanceof Player owner) {
            say(owner, pick(DEATH_LINES));
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    private void say(Player player, String line) {
        player.displayClientMessage(
                Component.literal("[Papyrus] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(line).withStyle(ChatFormatting.WHITE)),
                false);
    }

    private String pick(String[] lines) {
        return lines[this.random.nextInt(lines.length)];
    }
}
