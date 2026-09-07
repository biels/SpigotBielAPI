package com.biel.BielAPI.ai;

import java.util.EnumSet;
import java.util.function.Predicate;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

/**
 * The one target selector: every {@code rescanTicks} while the mob has no target, picks
 * the nearest living entity within {@code radius} that the caller's predicate accepts
 * (and that the mob can see, when required) and makes it the mob's target. Keeps it while
 * it stays acceptable, alive and within one and a half times the radius; line of sight
 * may lapse for a short grace period before the target is dropped.
 */
public final class NearestTargetGoal implements Goal<Mob> {
	public static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("bielapi", "nearest_target"));
	private static final int LINE_OF_SIGHT_GRACE_TICKS = 60;

	private final Mob mob;
	private final double radius;
	private final boolean requireLineOfSight;
	private final int rescanTicks;
	private final Predicate<LivingEntity> hostile;
	private int ticksUntilScan = 0;
	private int ticksWithoutSight = 0;

	public NearestTargetGoal(Mob mob, double radius, boolean requireLineOfSight, int rescanTicks, Predicate<LivingEntity> hostile) {
		this.mob = mob;
		this.radius = radius;
		this.requireLineOfSight = requireLineOfSight;
		this.rescanTicks = Math.max(1, rescanTicks);
		this.hostile = hostile;
	}

	@Override
	public boolean shouldActivate() {
		if (isUsable(mob.getTarget())) return true;
		if (--ticksUntilScan > 0) return false;
		ticksUntilScan = rescanTicks;
		LivingEntity candidate = nearestHostile();
		if (candidate == null) return false;
		mob.setTarget(candidate);
		ticksWithoutSight = 0;
		return mob.getTarget() == candidate;
	}

	@Override
	public boolean shouldStayActive() {
		LivingEntity target = mob.getTarget();
		if (!isUsable(target)) return false;
		if (mob.getLocation().distanceSquared(target.getLocation()) > radius * radius * 2.25) return false;
		if (requireLineOfSight) {
			ticksWithoutSight = mob.hasLineOfSight(target) ? 0 : ticksWithoutSight + 1;
			if (ticksWithoutSight > LINE_OF_SIGHT_GRACE_TICKS) return false;
		}
		return true;
	}

	@Override
	public void stop() {
		mob.setTarget(null);
		ticksWithoutSight = 0;
	}

	@Override
	public GoalKey<Mob> getKey() {
		return KEY;
	}

	@Override
	public EnumSet<GoalType> getTypes() {
		return EnumSet.of(GoalType.TARGET);
	}

	private boolean isUsable(LivingEntity target) {
		return target != null && target.isValid() && !target.isDead() && target.getWorld() == mob.getWorld() && hostile.test(target);
	}

	private LivingEntity nearestHostile() {
		LivingEntity nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (LivingEntity candidate : mob.getWorld().getNearbyLivingEntities(mob.getLocation(), radius)) {
			if (candidate == mob || !isUsable(candidate)) continue;
			double distance = candidate.getLocation().distanceSquared(mob.getLocation());
			if (distance >= nearestDistance) continue;
			if (requireLineOfSight && !mob.hasLineOfSight(candidate)) continue;
			nearest = candidate;
			nearestDistance = distance;
		}
		return nearest;
	}
}
