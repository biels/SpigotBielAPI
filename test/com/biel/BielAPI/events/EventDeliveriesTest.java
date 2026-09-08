package com.biel.BielAPI.events;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;

class EventDeliveriesTest {
    private static final class TestEvent extends Event {
        public HandlerList getHandlers() { throw new UnsupportedOperationException(); }
        @Override public boolean equals(Object other) { return other instanceof TestEvent; }
        @Override public int hashCode() { return 1; }
    }

    @Test void nestedEventsDoNotForgetTheOuterEvent() {
        var deliveries = new EventDeliveries();
        Event death = new TestEvent(), spawn = new TestEvent();
        assertTrue(deliveries.first(death));
        assertTrue(deliveries.first(spawn), "distinct objects remain distinct even with equal values");
        assertFalse(deliveries.first(death), "second death handler must not repeat rewards");
        assertFalse(deliveries.first(spawn));
        assertFalse(deliveries.first(death), "third death handler must not spawn another minion");
    }

    @Test void gameBusDeliversNestedEventsAndEachDeathOnlyOnce() {
        Event death = new TestEvent(), spawn = new TestEvent();
        List<Event> received = new ArrayList<>();
        EventBus bus = new EventBus() {
            @Override protected boolean shouldRegisterImmediately() { return false; }
            @Override protected Boolean verifyEvent(Event event) { return true; }
            @Override protected void gameEvent(Event event) {
                received.add(event);
                if (event == death) recieveEvent(spawn);
            }
        };
        bus.recieveEvent(death);
        bus.recieveEvent(death);
        bus.recieveEvent(death);
        assertEquals(2, received.size());
        assertSame(death, received.get(0));
        assertSame(spawn, received.get(1));
    }
}
