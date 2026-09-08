package com.biel.BielAPI.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class PlayerWorldEventBus extends WorldEventBus {
	String playerName;
	public PlayerWorldEventBus(Player ply) {
		super(ply);
		if (ply != null) {
			this.playerName = ply.getName();
		}
	}

	protected Player getPlayer() {
		if(playerName == null)return null;
		return Bukkit.getPlayer(playerName);
	}

	protected void setPlayer(Player player) {
		this.playerName = player.getName();
	}
	protected boolean getPlayerSpecificEventFiltering(){
		return true;
	}
	@Override
	protected Boolean verifyEvent(Event evt) {
		Boolean worldVerifyEvent = super.verifyEvent(evt);
		if (getPlayerSpecificEventFiltering() && worldVerifyEvent) {
			//Bukkit.broadcastMessage("----");
			worldVerifyEvent = verifyReflection(evt, 6);
		}
		return worldVerifyEvent;
	}
	protected boolean verifyReflection(Object o, int depth){
		Player player = getPlayer();
		return player != null && EventUtils.interactsWithAny(o, java.util.List.of(player), depth);
	}
}
