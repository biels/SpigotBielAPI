package com.biel.BielAPI.events;

import java.text.MessageFormat;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.world.WorldEvent;

import com.biel.BielAPI.Com;

public class WorldEventBus extends EventBus {
	private UUID worldUUID;

	@Override
	protected boolean shouldRegisterImmediately() {
		return false;
	}
	
	public WorldEventBus() {
		super();
	}
	public WorldEventBus(World world) {
		super();
		setWorld(world);
	}
	public WorldEventBus(Player ply) {
		super();
		if(ply != null){
			setWorld(ply.getWorld());
		}
	}

	protected World getWorld() {
		if(worldUUID == null)return null;
		return Bukkit.getWorld(worldUUID);
	}
	
	protected void setWorld(World world) {
		if(world == null){
			worldUUID = null;
			return;
		}
		this.worldUUID = world.getUID();
		registerEventBus();
	}
	@Override
	public boolean isValid() {
		return !isDestroyed() && getWorld() != null;
	}
	@Override
	protected Boolean verifyEvent(Event evt) {
		if(!isValid())return false;
		if (evt instanceof WorldEvent){
			WorldEvent e = (WorldEvent)evt;
			return e.getWorld() == getWorld();
		}
		if (evt instanceof EntityEvent){
			EntityEvent e = (EntityEvent)evt;
			return e.getEntity().getWorld() == getWorld();
		}
		if (evt instanceof PlayerEvent){
			PlayerEvent e = (PlayerEvent)evt;
			return e.getPlayer().getWorld() == getWorld();
		}
		if (evt instanceof BlockEvent){
			BlockEvent e = (BlockEvent)evt;
			return e.getBlock().getWorld() == getWorld();
		}
		if (evt instanceof InventoryEvent){
			InventoryEvent e = (InventoryEvent)evt;
			return e.getView().getPlayer().getWorld() == getWorld();
		}
		Com.getPlugin().getLogger().fine(MessageFormat.format("Event no verificat: {0} @ WorldEventBus", evt.getEventName()));
		return true;
	}
//	@Override
//	public String toString() {
//		// TODO Auto-generated method stub
//		World w = getWorld();
//		if (w == null){return "WorldEventBus, W: null";}
//		return "WorldEventBus, W: " + w.getName();
//	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		if (getWorld() == null)return super.toString() + " w: null";
		return MessageFormat.format("{0}[World =]", super.toString(), getWorld().getName());
	}
}
