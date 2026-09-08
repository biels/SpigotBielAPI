package com.biel.BielAPI.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.projectiles.ProjectileSource;

public class EventUtils {
	public static boolean interactsWithAny(Object evt, List<Player> players, int depth){
		if (players.isEmpty()) return false;
		return interactsWithAny(evt, players, depth, new IdentityHashMap<>());
	}

	private static boolean interactsWithAny(Object object, List<Player> players, int depth, Map<Object, Integer> visited) {
		if (object == null || depth <= 0) return false;
		if (object instanceof Player player) return players.contains(player);
		if (object instanceof Projectile projectile) return interactsWithAny(projectile.getShooter(), players, depth - 1, visited);
		// Entity targets, vehicles and historical killers are not participants in every event.
		if (object instanceof Entity) return false;
		if (object instanceof EntityDeathEvent death) {
			Player killer = death.getEntity().getKiller();
			if (killer != null && players.contains(killer)) return true;
		}
		if (object instanceof InventoryEvent inventory && players.contains(inventory.getView().getPlayer())) return true;
		Integer previousDepth = visited.get(object);
		if (previousDepth != null && previousDepth >= depth) return false;
		visited.put(object, depth);
		for (Method getter : object.getClass().getMethods()) {
			if (!getter.getName().startsWith("get") || getter.getParameterCount() != 0) continue;
			Class<?> result = getter.getReturnType();
			if (!Entity.class.isAssignableFrom(result) && !Event.class.isAssignableFrom(result)
					&& !ProjectileSource.class.isAssignableFrom(result)) continue;
			try {
				Object related = getter.invoke(object);
				if (related instanceof Player player && players.contains(player)) return true;
				// An unrelated victim must not hide a later attacker or projectile shooter.
				if (interactsWithAny(related, players, depth - 1, visited)) return true;
			} catch (IllegalAccessException | InvocationTargetException ignored) {
				// An unavailable optional getter must not prevent checking the other participants.
			}
		}
		return false;
	}
}
