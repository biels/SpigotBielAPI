package com.biel.BielAPI.events;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;

class EventRoutingTest {
    private static final class TestEvent extends Event {
        @Override public HandlerList getHandlers() { throw new UnsupportedOperationException(); }
    }
    private static class RecordingBus extends EventBus {
        final List<Event> received = new ArrayList<>();
        Runnable action = () -> {};
        @Override protected boolean shouldRegisterImmediately() { return false; }
        @Override protected void gameEvent(Event event) { received.add(event); action.run(); }
    }

    @Test void listenerRegistrationsDoNotOverlapWithinABukkitHandlerList() throws Exception {
        Map<Class<?>, List<Class<?>>> registrations = new HashMap<>();
        for (var method : GeneralListener.class.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(EventHandler.class)) continue;
            Class<?> event = method.getParameterTypes()[0];
            Class<?> handlerOwner = event.getMethod("getHandlerList").getDeclaringClass();
            registrations.computeIfAbsent(handlerOwner, ignored -> new ArrayList<>()).add(event);
        }
        for (var handlers : registrations.values()) {
            for (int first = 0; first < handlers.size(); first++) {
                for (int second = first + 1; second < handlers.size(); second++) {
                    Class<?> left = handlers.get(first), right = handlers.get(second);
                    assertFalse(left.isAssignableFrom(right) || right.isAssignableFrom(left),
                            "overlapping Bukkit registrations: " + left.getSimpleName() + " and " + right.getSimpleName());
                }
            }
        }
    }

    @Test void unregisterBeforeFirstEventCancelsRegistration() {
        var manager = new EventBusManager();
        var bus = new RecordingBus();
        manager.registerEventBus(bus);
        manager.unregisterEventBus(bus);
        manager.recieveEvent(new TestEvent());
        assertTrue(bus.received.isEmpty());
    }

    @Test void unregisterDuringDispatchSkipsTheRemainingSnapshotEntry() {
        var manager = new EventBusManager();
        var first = new RecordingBus();
        var removed = new RecordingBus();
        first.action = () -> manager.unregisterEventBus(removed);
        manager.registerEventBus(first);
        manager.registerEventBus(removed);
        manager.recieveEvent(new TestEvent());
        assertEquals(1, first.received.size());
        assertTrue(removed.received.isEmpty());
    }

    @Test void nestedRegistrationAndRemovalDoNotInvalidateIteration() {
        var manager = new EventBusManager();
        var first = new RecordingBus();
        var removed = new RecordingBus();
        var added = new RecordingBus();
        Event outer = new TestEvent(), nested = new TestEvent();
        first.action = () -> {
            first.action = () -> {};
            manager.unregisterEventBus(removed);
            manager.registerEventBus(added);
            manager.recieveEvent(nested);
        };
        manager.registerEventBus(first);
        manager.registerEventBus(removed);
        manager.recieveEvent(outer);
        assertEquals(List.of(outer, nested), first.received);
        assertEquals(List.of(nested), added.received);
        assertTrue(removed.received.isEmpty());
    }

    @Test void destroyedBusCannotBypassLifecycleWithItsOwnValidityCheck() {
        var bus = new RecordingBus() { @Override public boolean isValid() { return true; } };
        bus.destroyEventBus();
        bus.recieveEvent(new TestEvent());
        assertTrue(bus.received.isEmpty());
    }

    @Test void projectileUsesTheReportedBlockAndNeverGuessesPastAnEntity() {
        var bus = new EventBus() {
            Block reported;
            int blockHits;
            @Override protected boolean shouldRegisterImmediately() { return false; }
            @Override protected void onBlockHitByProjectile(ProjectileHitEvent event, Block block, Projectile projectile) {
                reported = block;
                blockHits++;
            }
        };
        Player shooter = stub(Player.class, Map.of());
        Projectile projectile = stub(Projectile.class, Map.of("getShooter", shooter, "getLocation", new Location(null, 0, 0, 0)));
        Block actualBlock = stub(Block.class, Map.of());
        bus.recieveEvent(new ProjectileHitEvent(projectile, actualBlock));
        assertSame(actualBlock, bus.reported);
        bus.recieveEvent(new ProjectileHitEvent(projectile, stub(Entity.class, Map.of())));
        assertEquals(1, bus.blockHits, "entity impacts must not break a block beyond the victim");
    }

    @Test void holderlessMenusBelongOnlyToTheViewersWorld() {
        World local = stub(World.class, Map.of()), foreign = stub(World.class, Map.of());
        var bus = new WorldEventBus() { @Override protected World getWorld() { return local; } };
        Inventory menu = stub(Inventory.class, Map.of());
        for (World playerWorld : List.of(local, foreign)) {
            Player viewer = stub(Player.class, Map.of("getWorld", playerWorld));
            InventoryView view = stub(InventoryView.class, Map.of("getPlayer", viewer, "getTopInventory", menu));
            assertEquals(playerWorld == local, bus.verifyEvent(new InventoryEvent(view)));
        }
        bus.destroyEventBus();
        assertFalse(bus.isValid(), "loaded world cannot keep a destroyed bus valid");
    }

    @Test void entityBlockFormationUsesTheBlockEventBranch() {
        var bus = new EventBus() {
            int formedBlocks, entityFormedBlocks;
            @Override protected boolean shouldRegisterImmediately() { return false; }
            @Override protected void onBlockForm(BlockFormEvent event, Block block) { formedBlocks++; }
            @Override protected void onEntityBlockForm(EntityBlockFormEvent event, Entity entity) { entityFormedBlocks++; }
        };
        Block block = stub(Block.class, Map.of("getLocation", new Location(null, 0, 0, 0)));
        bus.recieveEvent(new EntityBlockFormEvent(stub(Entity.class, Map.of()), block, stub(BlockState.class, Map.of())));
        assertEquals(1, bus.formedBlocks);
        assertEquals(1, bus.entityFormedBlocks);
    }

    public static final class Participants extends Event {
        private final Entity first, second;
        Participants(Entity first, Entity second) { this.first = first; this.second = second; }
        public Entity getFirst() { return first; }
        public Entity getSecond() { return second; }
        @Override public HandlerList getHandlers() { throw new UnsupportedOperationException(); }
    }

    @Test void playerFilteringChecksAllParticipantsAndProjectileSources() {
        Player owner = stub(Player.class, Map.of());
        Player unrelated = stub(Player.class, Map.of());
        Projectile projectile = stub(Projectile.class, Map.of("getShooter", owner));
        assertTrue(EventUtils.interactsWithAny(new ProjectileHitEvent(projectile), List.of(owner), 6));
        assertTrue(EventUtils.interactsWithAny(new Participants(unrelated, projectile), List.of(owner), 6));
        assertTrue(EventUtils.interactsWithAny(new Participants(projectile, unrelated), List.of(owner), 6));
        assertFalse(EventUtils.interactsWithAny(new ProjectileHitEvent(projectile), List.of(unrelated), 6));
        assertFalse(EventUtils.interactsWithAny(new ProjectileHitEvent(projectile), List.of(owner), 1));
        Map<String, Object> cycle = new HashMap<>();
        Mob loopingMob = stub(Mob.class, cycle);
        cycle.put("getTarget", loopingMob);
        Projectile looping = stub(Projectile.class, Map.of("getShooter", loopingMob));
        assertFalse(EventUtils.interactsWithAny(new ProjectileHitEvent(looping), List.of(owner), 6));
        var playerBus = new PlayerWorldEventBus((Player) null) {
            @Override protected Player getPlayer() { return owner; }
        };
        assertTrue(playerBus.verifyReflection(new ProjectileHitEvent(projectile), 6), "skills share the same participant filter");
    }

    @Test void historicalKillersAndMobTargetsAreNotUnrelatedEventParticipants() {
        Player spectator = stub(Player.class, Map.of());
        Player victim = stub(Player.class, Map.of("getKiller", spectator));
        assertFalse(EventUtils.interactsWithAny(new Participants(victim, null), List.of(spectator), 6));
        Mob shooter = stub(Mob.class, Map.of("getTarget", spectator));
        Projectile projectile = stub(Projectile.class, Map.of("getShooter", shooter));
        assertFalse(EventUtils.interactsWithAny(new ProjectileHitEvent(projectile), List.of(spectator), 6));
        assertTrue(EventUtils.interactsWithAny(new EntityDeathEvent(victim, stub(DamageSource.class, Map.of()), new ArrayList<>()),
                List.of(spectator), 6), "the actual death still belongs to its killer");
    }

    private static <T> T stub(Class<T> type, Map<String, Object> values) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("equals")) return proxy == args[0];
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            return values.get(method.getName());
        }));
    }
}
