package com.biel.BielAPI.ai;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.entity.RangedEntity;

/**
 * How a {@link RangedAttackGoal} fires. Two implementations cover every body: a mob that
 * is natively ranged (snow golem, skeleton, blaze) shoots its own projectile with vanilla
 * inaccuracy; any other mob throws a projectile of the caller's choice.
 */
@FunctionalInterface
public interface Volley {
	void fire(Mob shooter, LivingEntity target);

	/** The mob's own ranged attack; only for bodies that are a {@link RangedEntity}. */
	static Volley innate() {
		return (shooter, target) -> {
			if (!(shooter instanceof RangedEntity ranged)) {
				throw new IllegalStateException(shooter.getType() + " has no innate ranged attack; use Volley.thrown");
			}
			ranged.rangedAttack(target, 1F);
		};
	}

	/**
	 * Spawns {@code projectileClass} at the shooter's eyes, owned by the shooter, aimed at
	 * the target's chest with a small lob so a slow projectile still lands at range.
	 */
	static Volley thrown(Class<? extends Projectile> projectileClass, double speed) {
		return (shooter, target) -> {
			Location from = shooter.getEyeLocation();
			Location to = target.getLocation().add(0, target.getHeight() * 0.6, 0);
			Vector direction = to.toVector().subtract(from.toVector());
			double distance = direction.length();
			if (distance < 0.01) return;
			Vector velocity = direction.normalize().multiply(speed);
			velocity.setY(velocity.getY() + distance * 0.03);
			shooter.getWorld().spawn(from, projectileClass, projectile -> {
				projectile.setShooter(shooter);
				projectile.setVelocity(velocity);
			});
			shooter.swingMainHand();
		};
	}
}
