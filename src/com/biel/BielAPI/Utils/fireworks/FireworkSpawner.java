package com.biel.BielAPI.Utils.fireworks;

import java.util.List;

import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import com.biel.BielAPI.BielAPI;
import com.biel.BielAPI.Com;

public class FireworkSpawner {
	
	public static void spawn(Location location, FireworkEffect effect, Player...players){
		if(location==null||effect==null||players==null||players.length==0)return;

		World world = location.getWorld();
		if (world == null) return;
		BielAPI plugin = Com.getPlugin();
		if (plugin == null) return;
		Firework firework = world.spawn(location, Firework.class, spawnedFirework -> {
			spawnedFirework.setVisibleByDefault(false);
			FireworkMeta meta = spawnedFirework.getFireworkMeta();
			meta.clearEffects();
			meta.addEffect(effect);
			spawnedFirework.setFireworkMeta(meta);
		});
		for (Player player : players) {
			if (player != null) player.showEntity(plugin, firework);
		}
		firework.detonate();
	}
	public static void spawn(Location location, FireworkEffect effect, List<Player> players){
		FireworkSpawner.spawn(location, effect, players.toArray(new Player[players.size()]));
	}
}
