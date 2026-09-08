package com.biel.BielAPI.events;

import java.util.concurrent.ConcurrentMap;
import org.bukkit.event.Event;
import com.google.common.collect.MapMaker;

/** Weak identity keys remember nested deliveries without retaining completed events. */
final class EventDeliveries {
    private final ConcurrentMap<Event, Boolean> delivered = new MapMaker().weakKeys().makeMap();

    boolean first(Event event) { return delivered.putIfAbsent(event, Boolean.TRUE) == null; }
}
