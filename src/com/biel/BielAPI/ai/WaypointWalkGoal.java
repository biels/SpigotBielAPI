package com.biel.BielAPI.ai;

import java.util.EnumSet;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

/**
 * Walks a list of waypoints in order and holds at the last one. Meant for the lowest
 * priority among a mob's movement goals: any active attack goal takes movement away, and
 * the walk resumes from the current waypoint when the attack ends.
 */
public final class WaypointWalkGoal implements Goal<Mob> {
	public static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("bielapi", "waypoint_walk"));
	private static final int REPATH_TICKS = 10;

	private final Mob mob;
	private final List<Location> waypoints;
	private final double speed;
	private final double arriveDistance;
	private int next = 0;
	private int ticksUntilRepath = 0;

	public WaypointWalkGoal(Mob mob, List<Location> waypoints, double speed, double arriveDistance) {
		this.mob = mob;
		this.waypoints = List.copyOf(waypoints);
		this.speed = speed;
		this.arriveDistance = arriveDistance;
	}

	public boolean isFinished() {
		return next >= waypoints.size();
	}

	@Override
	public boolean shouldActivate() {
		return !isFinished();
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
		if (isFinished()) return;
		Location waypoint = waypoints.get(next);
		if (mob.getLocation().distanceSquared(waypoint) <= arriveDistance * arriveDistance) {
			next++;
			ticksUntilRepath = 0;
			if (isFinished()) {
				mob.getPathfinder().stopPathfinding();
				return;
			}
			waypoint = waypoints.get(next);
		}
		if (--ticksUntilRepath <= 0) {
			ticksUntilRepath = REPATH_TICKS;
			mob.getPathfinder().moveTo(waypoint, speed);
		}
	}

	@Override
	public GoalKey<Mob> getKey() {
		return KEY;
	}

	@Override
	public EnumSet<GoalType> getTypes() {
		return EnumSet.of(GoalType.MOVE);
	}
}
