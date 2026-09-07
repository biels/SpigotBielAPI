package com.biel.BielAPI.ai;

import java.util.EnumSet;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

/**
 * Closes on the mob's target and hits it with the mob's own attack, which reads the
 * ATTACK_DAMAGE attribute: register that attribute on bodies that lack it (cow, chicken,
 * sheep) before adding this goal. Active while the target is within {@code pursuitRange};
 * hits when within {@code reach}. A body that also carries a {@link RangedAttackGoal}
 * gives this one the higher priority and a pursuit range equal to the ranged goal's
 * minimum range, so exactly one of them owns movement at any distance.
 */
public final class MeleeAttackGoal implements Goal<Mob> {
	public static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("bielapi", "melee_attack"));
	private static final int REPATH_TICKS = 10;

	private final Mob mob;
	private final double reach;
	private final double pursuitRange;
	private final int cooldownTicks;
	private final double approachSpeed;
	private int ticksUntilHit = 0;
	private int ticksUntilRepath = 0;

	public MeleeAttackGoal(Mob mob, double reach, double pursuitRange, int cooldownTicks, double approachSpeed) {
		this.mob = mob;
		this.reach = reach;
		this.pursuitRange = Math.max(reach, pursuitRange);
		this.cooldownTicks = Math.max(1, cooldownTicks);
		this.approachSpeed = approachSpeed;
	}

	@Override
	public boolean shouldActivate() {
		LivingEntity target = mob.getTarget();
		return target != null && target.isValid() && !target.isDead()
				&& mob.getLocation().distanceSquared(target.getLocation()) <= pursuitRange * pursuitRange;
	}

	@Override
	public void start() {
		ticksUntilRepath = 0;
	}

	@Override
	public void stop() {
		mob.getPathfinder().stopPathfinding();
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null) return;
		mob.lookAt(target);
		if (ticksUntilHit > 0) ticksUntilHit--;
		if (mob.getLocation().distanceSquared(target.getLocation()) <= reach * reach) {
			mob.getPathfinder().stopPathfinding();
			if (ticksUntilHit == 0) {
				mob.swingMainHand();
				mob.attack(target);
				ticksUntilHit = cooldownTicks;
			}
			return;
		}
		if (--ticksUntilRepath <= 0) {
			ticksUntilRepath = REPATH_TICKS;
			mob.getPathfinder().moveTo(target, approachSpeed);
		}
	}

	@Override
	public GoalKey<Mob> getKey() {
		return KEY;
	}

	@Override
	public EnumSet<GoalType> getTypes() {
		return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
	}
}
