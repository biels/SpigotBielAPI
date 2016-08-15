package com.biel.BielAPI.Utils.Regions;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

public abstract class Region implements Iterable<Block>, Cloneable, ConfigurationSerializable{
	public static Region deserialize(String s){
		//T/[Serialization]
		String[] split = s.split("/", 2);
		
		Class<? extends Region> cls = getRegionType(split[0]);
		Region r = null;
		switch (split[0]) {
		case "e":
			
			break;

		default:
			break;
		}
		return r;
	}

	public Location readLocation(String s, World world){
		String[] p1 = s.split(",");    	
    	Location pont1 = new Location(world,Integer.parseInt(p1[0]),Integer.parseInt(p1[1]),Integer.parseInt(p1[2]));
    	return pont1;
	}
	static Class<? extends Region> getRegionType(String t){
		Map<String, Class<? extends Region>> m = new HashMap<String, Class<? extends Region>>();
		m.put("C", Cuboid.class);
		m.put("S", Sphere.class);
		m.put("Y", Sphere.class);
		return m.get(t);
	}
}
