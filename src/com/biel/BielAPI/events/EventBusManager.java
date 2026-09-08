package com.biel.BielAPI.events;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.event.Event;

import com.biel.BielAPI.Com;

public class EventBusManager {
	private final Set<EventBus> buses = new LinkedHashSet<>();
	public synchronized void recieveEvent(Event evt) {
		unregisterInvalidBuses();
		List<EventBus> dispatchSnapshot = new ArrayList<EventBus>(buses);
		for(EventBus bus: dispatchSnapshot){
			// Nested callbacks can remove a bus that is still in this event's snapshot.
			if (!buses.contains(bus)) continue;
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
		if (!bus.isDestroyed()) buses.add(bus);
	}
	public synchronized void unregisterEventBus(EventBus bus){
		if (buses.remove(bus)) bus.registrationRemoved();
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
	public synchronized String getStats(){
		return MessageFormat.format("AB: {0}, TA:0, TR:0", buses.size());
	}
}
