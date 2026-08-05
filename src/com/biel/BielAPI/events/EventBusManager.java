package com.biel.BielAPI.events;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.event.Event;

import com.biel.BielAPI.Com;

public class EventBusManager {
	ArrayList<EventBus> buses = new ArrayList<EventBus>();
	ArrayList<EventBus> toAdd = new ArrayList<EventBus>();
	ArrayList<EventBus> toRemove = new ArrayList<EventBus>();
	public synchronized void recieveEvent(Event evt) {
		unregisterInvalidBuses();
		buses.addAll(toAdd);
		toAdd.clear();
		buses.removeAll(toRemove);
		toRemove.clear();
		List<EventBus> dispatchSnapshot = new ArrayList<EventBus>(buses);
		for(EventBus bus: dispatchSnapshot){
			try {
				bus.recieveEvent(evt);
			} catch (RuntimeException exception) {
				Com.getPlugin().getLogger().log(Level.SEVERE,
						"EventBus failure in " + bus + " while handling " + evt.getEventName(),
						exception);
			}
		}
	}
	public synchronized void registerEventBus(EventBus bus){
		if(buses.contains(bus) || toAdd.contains(bus)){
			Com.getPlugin().getLogger().warning("El canal d'esdeveniments ja existeix @ " + bus);
		}else{
			//System.out.println("Afegit el canal d'esdeveniments a la cua " + bus.getClass().getName());
			toAdd.add(bus);
		}
	}
	public synchronized void unregisterEventBus(EventBus bus){
		if(buses.contains(bus) && !toRemove.contains(bus)){
			toRemove.add(bus);
			//System.out.println("Esborrat el canal d'esdeveniments " + bus.getClass().getName());
		}else{
			Com.getPlugin().getLogger().warning("El canal d'esdeveniments ja no existia");
		}
	}
	public synchronized void unregisterInvalidBuses(){
		ArrayList<EventBus> toremove = new ArrayList<EventBus>();
		for(EventBus bus : buses){
			if(!bus.isValid() || bus.isDestroyed()){
				toremove.add(bus);
			}
		}
		for (EventBus eventBus : toremove) {
			unregisterEventBus(eventBus);			
		}
	}
	public String getStats(){
		return  MessageFormat.format("AB: {0}, TA:{1}, TR:{2}", buses.size(), toAdd.size(), toRemove.size());
	}
}
