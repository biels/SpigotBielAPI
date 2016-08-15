package com.biel.BielAPI.Utils.fireworks;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

public class FireworkSpawner {
	
	public static void spawn(Location location, FireworkEffect effect, Player...players){
		if(location==null||effect==null||players==null||players.length==0)return;
		
		//Generate some basic info
		int entityId=FireworkSpawner.getNextEntityId();
		UUID entityUuid=UUID.randomUUID();
		
		//Spawn firework entity
		new PH_PO_SpawnEntity(entityId, entityUuid, 76/*ID of firework entity type*/, 0, location.getX(), location.getY(), location.getZ(),
				location.getYaw(), location.getPitch(), 0, 0, 0).send(players);
		
		//From here we put things inside a try-catch to make sure the player always receives a destroy packet (otherwise their client will crash)
		try{
			//Send firework meta
			ItemStack item=new ItemStack(Material.FIREWORK);
			FireworkMeta meta=(FireworkMeta)item.getItemMeta();
			meta.addEffect(effect);
			item.setItemMeta(meta);
			
			jDataWatcherItem<Byte> i1=new jDataWatcherItem<Byte>(jDataWatcherObject.entity_byte, (byte)0);
			jDataWatcherItem<Integer> i2=new jDataWatcherItem<Integer>(jDataWatcherObject.entity_int, 300);
			jDataWatcherItem<String> i3=new jDataWatcherItem<String>(jDataWatcherObject.entity_string, "");
			jDataWatcherItem<Boolean> i4=new jDataWatcherItem<Boolean>(jDataWatcherObject.entity_boolean1, false);
			jDataWatcherItem<Boolean> i5=new jDataWatcherItem<Boolean>(jDataWatcherObject.entity_boolean2, false);
			jDataWatcherItem<ItemStack> i6=new jDataWatcherItem<ItemStack>(jDataWatcherObject.entityfireworks_itemstack, item);
			
			i1.setFlag(false);
			i2.setFlag(false);
			i3.setFlag(false);
			i4.setFlag(false);
			i5.setFlag(false);
			i6.setFlag(false);
			
			new PH_PO_EntityMetadata(entityId, Arrays.asList(i1, i2, i3, i4, i5, i6)).send(players);
			
			//Play explosion effect
			new PH_PO_EntityStatus(entityId, (byte)17/*Explode status ID*/).send(players);
		}finally{
			//Send destroy packet (very important, otherwise the client will crash)
			new PH_PO_EntityDestroy(entityId).send(players);
		}
	}
	public static void spawn(Location location, FireworkEffect effect, List<Player> players){
		FireworkSpawner.spawn(location, effect, players.toArray(new Player[players.size()]));
	}
	
	/**
	 * Utils
	 */
	private static int currentEntityId=Integer.MAX_VALUE;
	private static int getNextEntityId(){
		return FireworkSpawner.currentEntityId--;
	}
}
