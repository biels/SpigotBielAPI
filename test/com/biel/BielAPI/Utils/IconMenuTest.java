package com.biel.BielAPI.Utils;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.biel.BielAPI.BielAPI;
import com.biel.BielAPI.events.EventBusManager;

class IconMenuTest {
    private Server previousServer;
    private Field serverField;
    private EventBusManager buses;
    private final List<Runnable> tasks = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();

    @BeforeEach void server() throws Exception {
        serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        previousServer = (Server) serverField.get(null);
        Field allocatorField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        allocatorField.setAccessible(true);
        BielAPI plugin = (BielAPI) ((sun.misc.Unsafe) allocatorField.get(null)).allocateInstance(BielAPI.class);
        plugin.evtgest = buses = new EventBusManager();
        PluginManager manager = stub(PluginManager.class, (method, args) -> method.equals("getPlugin") ? plugin : null);
        BukkitScheduler scheduler = stub(BukkitScheduler.class, (method, args) -> {
            if (method.equals("runTask") || method.equals("scheduleSyncDelayedTask")) {
                tasks.add((Runnable) args[1]);
                if (method.equals("scheduleSyncDelayedTask")) return tasks.size();
                return stub(BukkitTask.class, (name, values) -> null);
            }
            return null;
        });
        serverField.set(null, stub(Server.class, (method, args) -> switch (method) {
            case "getPluginManager" -> manager;
            case "getScheduler" -> scheduler;
            case "createInventory" -> { titles.add((String) args[2]); yield inventory((int) args[1]); }
            default -> null;
        }));
    }

    @AfterEach void restoreServer() throws Exception { serverField.set(null, previousServer); }

    @Test void titlesNeedNoIdentifierAndIdenticalTitlesStayIndependent() {
        var firstPlayer = new Viewer();
        var secondPlayer = new Viewer();
        int[] clicks = new int[2];
        IconMenu first = menu(event -> { clicks[0]++; event.setWillClose(false); });
        IconMenu second = menu(event -> { clicks[1]++; event.setWillClose(false); });
        assertEquals("Shop", first.getName());
        assertEquals("AB: 0, TA:0, TR:0", buses.getStats(), "unopened menus do not receive global events");
        first.open(firstPlayer.player);
        second.open(secondPlayer.player);
        assertEquals(List.of("Shop", "Shop"), titles);
        click(firstPlayer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        tick();
        assertArrayEquals(new int[]{1, 0}, clicks);
    }

    @Test void ownInventoryCanBeRearrangedButCannotTransferMenuItems() {
        var viewer = new Viewer();
        int[] purchases = {0};
        menu(event -> purchases[0]++).open(viewer.player);
        for (InventoryAction action : List.of(InventoryAction.PICKUP_ALL, InventoryAction.PLACE_ALL,
                InventoryAction.SWAP_WITH_CURSOR, InventoryAction.HOTBAR_SWAP, InventoryAction.DROP_ONE_SLOT)) {
            assertFalse(click(viewer, 10, ClickType.LEFT, action).isCancelled(), action.toString());
        }
        for (InventoryAction action : List.of(InventoryAction.MOVE_TO_OTHER_INVENTORY,
                InventoryAction.COLLECT_TO_CURSOR, InventoryAction.UNKNOWN)) {
            assertTrue(click(viewer, 10, ClickType.SHIFT_LEFT, action).isCancelled(), action.toString());
        }
        assertFalse(click(viewer, -999, ClickType.LEFT, InventoryAction.DROP_ALL_CURSOR).isCancelled());
        assertEquals(0, purchases[0]);
        assertTrue(tasks.isEmpty());
    }

    @Test void onlyOrdinaryOptionClicksActivateAndCancelledClicksAreRespected() {
        var viewer = new Viewer();
        int[] purchases = {0};
        menu(event -> { purchases[0]++; event.setWillClose(false); }).open(viewer.player);
        for (ClickType click : List.of(ClickType.NUMBER_KEY, ClickType.SWAP_OFFHAND, ClickType.DROP,
                ClickType.CONTROL_DROP, ClickType.DOUBLE_CLICK, ClickType.MIDDLE, ClickType.SHIFT_LEFT)) {
            assertTrue(click(viewer, 0, click, InventoryAction.HOTBAR_SWAP).isCancelled());
        }
        InventoryClickEvent blocked = new InventoryClickEvent(viewer.view, InventoryType.SlotType.CONTAINER, 0,
                ClickType.LEFT, InventoryAction.PICKUP_ALL);
        blocked.setCancelled(true);
        buses.recieveEvent(blocked);
        assertTrue(tasks.isEmpty());
        assertTrue(click(viewer, 8, ClickType.LEFT, InventoryAction.PICKUP_ALL).isCancelled(), "empty menu cells stay protected");
        assertTrue(click(viewer, 0, ClickType.RIGHT, InventoryAction.PICKUP_HALF).isCancelled());
        assertEquals(0, purchases[0], "option work is deferred until after the click response");
        tick();
        assertEquals(1, purchases[0]);
    }

    @Test void dragsAreAllowedOnlyWhenEverySlotIsInThePlayerInventory() {
        var viewer = new Viewer();
        menu(event -> {}).open(viewer.player);
        assertFalse(drag(viewer, 10, 11).isCancelled());
        assertTrue(drag(viewer, 0, 1).isCancelled());
        assertTrue(drag(viewer, 0, 10).isCancelled());
    }

    @Test void duplicateQueuedClicksDispatchOnceAndPersistentMenusCanBeUsedAgain() {
        var viewer = new Viewer();
        int[] purchases = {0};
        menu(event -> { purchases[0]++; event.setWillClose(false); }).open(viewer.player);
        click(viewer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        click(viewer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        tick();
        assertEquals(1, purchases[0]);
        click(viewer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        tick();
        assertEquals(2, purchases[0]);
    }

    @Test void queuedActionsDoNotRunAfterTheMenuIsClosed() {
        var viewer = new Viewer();
        int[] purchases = {0};
        IconMenu menu = menu(event -> purchases[0]++);
        menu.open(viewer.player);
        click(viewer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        viewer.close();
        tick();
        assertEquals(0, purchases[0]);
        assertTrue(menu.isDestroyed());
    }

    @Test void defaultCloseNeverClosesAReplacementOpenedByTheHandler() {
        var viewer = new Viewer();
        IconMenu replacement = menu(event -> {});
        menu(event -> replacement.open(event.getPlayer())).open(viewer.player);
        click(viewer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        tick();
        Inventory next = viewer.view.getTopInventory();
        tick();
        assertSame(next, viewer.view.getTopInventory());
        assertFalse(replacement.isDestroyed());
    }

    @Test void defaultCloseRemainsProtectedUntilItClosesItsOwnWindow() {
        var viewer = new Viewer();
        int[] purchases = {0};
        menu(event -> purchases[0]++).open(viewer.player);
        click(viewer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        tick();
        assertTrue(click(viewer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL).isCancelled());
        tick();
        assertEquals(1, purchases[0]);
        assertSame(viewer.bottom, viewer.view.getTopInventory());
    }

    @Test void reopeningTheSameMenuDuringItsHandlerKeepsTheNewSessionAlive() {
        var viewer = new Viewer();
        IconMenu menu = menu(event -> event.getMenu().open(event.getPlayer()));
        menu.open(viewer.player);
        Inventory previous = viewer.view.getTopInventory();
        click(viewer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        tick();
        tick();
        assertNotSame(previous, viewer.view.getTopInventory());
        assertNotSame(viewer.bottom, viewer.view.getTopInventory());
        assertFalse(menu.isDestroyed());
    }

    @Test void closingOneViewerDoesNotDestroyOtherSessionsAndDestroyClosesRemainingViews() {
        var first = new Viewer();
        var second = new Viewer();
        IconMenu menu = menu(event -> {});
        menu.open(first.player);
        menu.open(second.player);
        first.close();
        assertFalse(menu.isDestroyed());
        menu.destroy();
        assertSame(second.bottom, second.view.getTopInventory());
        assertTrue(menu.isDestroyed());
        assertThrows(IllegalStateException.class, () -> menu.open(first.player));
    }

    @Test void cancelledOpenDiscardsTheUnviewedSession() {
        var viewer = new Viewer();
        viewer.rejectOpen = true;
        IconMenu menu = menu(event -> {});
        menu.open(viewer.player);
        assertTrue(menu.isDestroyed());
    }

    @Test void handlerFailureDoesNotPermanentlyLockTheMenu() {
        var viewer = new Viewer();
        menu(event -> { throw new IllegalStateException("failed purchase"); }).open(viewer.player);
        click(viewer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        assertThrows(IllegalStateException.class, this::tick);
        click(viewer, 0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        assertEquals(1, tasks.size());
    }

    private IconMenu menu(IconMenu.OptionClickEventHandler handler) {
        IconMenu menu = new IconMenu("Shop", 9, handler);
        menu.getOptionNames()[0] = "Buy";
        return menu;
    }
    private InventoryClickEvent click(Viewer viewer, int rawSlot, ClickType click, InventoryAction action) {
        InventoryClickEvent event = new InventoryClickEvent(viewer.view, InventoryType.SlotType.CONTAINER,
                rawSlot, click, action, click == ClickType.NUMBER_KEY ? 0 : -1);
        buses.recieveEvent(event);
        return event;
    }
    private InventoryDragEvent drag(Viewer viewer, int... slots) {
        Map<Integer, ItemStack> items = new HashMap<>();
        // Routing depends only on raw slots, not registry-backed item contents.
        for (int slot : slots) items.put(slot, null);
        InventoryDragEvent event = new InventoryDragEvent(viewer.view, null, null, false, items);
        buses.recieveEvent(event);
        return event;
    }
    private void tick() {
        List<Runnable> ready = new ArrayList<>(tasks);
        tasks.clear();
        ready.forEach(Runnable::run);
    }
    private Inventory inventory(int size) {
        return stub(Inventory.class, (method, args) -> method.equals("getSize") ? size : null);
    }
    private final class Viewer {
        final Inventory bottom = inventory(36);
        final UUID id = UUID.randomUUID();
        final Player player;
        InventoryView view;
        boolean rejectOpen;
        Viewer() {
            player = stub(Player.class, (method, args) -> switch (method) {
                case "getUniqueId" -> id;
                case "isOnline" -> true;
                case "getOpenInventory" -> view;
                case "openInventory" -> {
                    if (rejectOpen) yield null;
                    close();
                    view = view((Inventory) args[0]);
                    yield view;
                }
                case "closeInventory" -> { close(); yield null; }
                default -> null;
            });
            view = view(bottom);
        }
        InventoryView view(Inventory top) {
            return stub(InventoryView.class, (method, args) -> switch (method) {
                case "getPlayer" -> player;
                case "getTopInventory" -> top;
                case "getBottomInventory" -> bottom;
                default -> null;
            });
        }
        void close() {
            InventoryView closing = view;
            view = view(bottom);
            if (closing != null) buses.recieveEvent(new InventoryCloseEvent(closing));
        }
    }
    @FunctionalInterface private interface Answer { Object call(String method, Object[] args); }
    private static <T> T stub(Class<T> type, Answer answer) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("equals")) return proxy == args[0];
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            Object value = answer.call(method.getName(), args);
            if (value != null) return value;
            if (method.getReturnType() == boolean.class) return false;
            if (method.getReturnType() == int.class) return 0;
            return null;
        }));
    }
}
