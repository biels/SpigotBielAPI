package com.biel.BielAPI.ai;

import java.util.EnumSet;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

/**
 * Faces the mob's target and fires a {@link Volley} at it every {@code cooldownTicks}
 * while it is between {@code minRange} and {@code range} with line of sight. A mob that
 * holds position never moves; one that does not walks toward a target beyond range. The
 * minimum range is where a {@link MeleeAttackGoal} at higher priority takes over, so the
 * two never compete for movement.
 */
public final class RangedAttackGoal implements Goal<Mob> {
	public static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("bielapi", "ranged_attack"));
	private static final int REPATH_TICKS = 10;

	private final Mob mob;
	private final double minRange;
	private final double range;
	private final int cooldownTicks;
	private final boolean holdPosition;
	private final double approachSpeed;
	private final Volley volley;
	private int ticksUntilShot;
	private int ticksUntilRepath = 0;

	public RangedAttackGoal(Mob mob, double minRange, double range, int cooldownTicks, boolean holdPosition, double approachSpeed, Volley volley) {
		this.mob = mob;
		this.minRange = minRange;
		this.range = range;
		this.cooldownTicks = Math.max(1, cooldownTicks);
		this.holdPosition = holdPosition;
		this.approachSpeed = approachSpeed;
		this.volley = volley;
		this.ticksUntilShot = this.cooldownTicks / 2;
	}

	@Override
	public boolean shouldActivate() {
		LivingEntity target = mob.getTarget();
		if (target == null || !target.isValid() || target.isDead()) return false;
		double distanceSquared = mob.getLocation().distanceSquared(target.getLocation());
		if (distanceSquared < minRange * minRange) return false;
		return !holdPosition || distanceSquared <= range * range;
	}

	@Override
	public void start() {
		ticksUntilRepath = 0;
	}

	@Override
	public void stop() {
		if (!holdPosition) mob.getPathfinder().stopPathfinding();
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null) return;
		mob.lookAt(target);
		if (ticksUntilShot > 0) ticksUntilShot--;
		boolean inRange = mob.getLocation().distanceSquared(target.getLocation()) <= range * range;
		if (inRange && mob.hasLineOfSight(target)) {
			if (!holdPosition) mob.getPathfinder().stopPathfinding();
			if (ticksUntilShot == 0) {
				volley.fire(mob, target);
				ticksUntilShot = cooldownTicks;
			}
			return;
		}
		if (!holdPosition && --ticksUntilRepath <= 0) {
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
