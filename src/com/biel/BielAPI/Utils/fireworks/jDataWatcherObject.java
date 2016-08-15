package com.biel.BielAPI.Utils.fireworks;

import org.bukkit.inventory.ItemStack;

public class jDataWatcherObject<T> implements iNmsObject{
	
	private static int version;
	
	static{
		String v=ProtocolUtils.version;
		if(v.startsWith("v1_9")){
			if(v.endsWith("R1"))jDataWatcherObject.version=0;
			else jDataWatcherObject.version=1;
		}
		else if(v.startsWith("v1_10")){
			jDataWatcherObject.version=2;
		}
	}
	private static String v(String...values){
		return values[jDataWatcherObject.version];
	}
	
	public static final jDataWatcherObject<Byte> entity_byte=new jDataWatcherObject<>(v("ax", "ay", "aa"), "Entity");
	public static final jDataWatcherObject<Integer> entity_int=new jDataWatcherObject<>(v("ay", "az", "az"), "Entity");
	public static final jDataWatcherObject<String> entity_string=new jDataWatcherObject<>(v("az", "aA", "aA"), "Entity");
	public static final jDataWatcherObject<Boolean> entity_boolean1=new jDataWatcherObject<>(v("aA", "aB", "aB"), "Entity");
	public static final jDataWatcherObject<Boolean> entity_boolean2=new jDataWatcherObject<>(v("aB", "aC", "aC"), "Entity");
	public static final jDataWatcherObject<Boolean> entity_boolean3=new jDataWatcherObject<>(v(null, null, "aD"), "Entity");
	public static final jDataWatcherObject<ItemStack> entityfireworks_itemstack=new jDataWatcherObject<>("FIREWORK_ITEM", "EntityFireworks");
	
	
	
	private final Object nms;
	
	private jDataWatcherObject(String fieldname, String classname){
		this.nms=ProtocolUtils.refl_fieldGet0(fieldname, classname);
	}
	
	/**
	 * Build
	 */
	@Override
	public Object build(){
		return this.nms;
	}
}
