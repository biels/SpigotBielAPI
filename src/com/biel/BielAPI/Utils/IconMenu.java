package com.biel.BielAPI.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.biel.BielAPI.Com;
import com.biel.BielAPI.events.*;

public class IconMenu extends EventBus{

	private String name;
	private int size;
	private OptionClickEventHandler handler;
	private Plugin plugin;

	private String[] optionNames;
	private ItemStack[] optionIcons;
	private final Map<Inventory, MenuSession> openedInventories = new IdentityHashMap<>();

	private static final class MenuSession {
		final Player player;
		boolean actionPending;
		MenuSession(Player player) { this.player = player; }
	}

	@Override
	protected boolean shouldRegisterImmediately() { return false; }

	public IconMenu(String name, int size, OptionClickEventHandler handler) {
		if (size < 9 || size > 54 || size % 9 != 0) throw new IllegalArgumentException("Menu size must be 9 to 54, in rows of nine");
		this.name = name;
		this.size = size;
		this.handler = handler;
		this.plugin = Com.getPlugin();
		this.optionNames = new String[size];
		this.optionIcons = new ItemStack[size];
	}

	public IconMenu setOption(int position, ItemStack icon, String name, String... info) {
		optionNames[position] = name;
		optionIcons[position] = setItemNameAndLore(icon, name, info);
		return this;
	}
	
	public IconMenu setOption(int position, ItemStack icon, String name, ArrayList<String> info) {
		optionNames[position] = name;
		optionIcons[position] = setItemNameAndLore(icon, name, info.toArray(new String[info.size()]));
		return this;
	}

	public void open(Player player) {
		if (isDestroyed()) throw new IllegalStateException("Cannot reopen a destroyed menu");
		Inventory inventory = Bukkit.createInventory(player, size, name);
		for (int i = 0; i < optionIcons.length; i++) {
			if (optionIcons[i] != null) {
				inventory.setItem(i, optionIcons[i].clone());
			}
		}
		openedInventories.put(inventory, new MenuSession(player));
		registerEventBus();
		try {
			player.openInventory(inventory);
		} finally {
			// A cancelled open must not leave a registered menu without a viewer.
			if (!isViewing(player, inventory)) {
				openedInventories.remove(inventory);
				if (openedInventories.isEmpty()) destroy();
			}
		}
	}

	public void destroy() {
		if (isDestroyed()) return;
		destroyEventBus();
		handler = null;
		plugin = null;
		optionNames = null;
		optionIcons = null;
		Map<Inventory, MenuSession> closing = new IdentityHashMap<>(openedInventories);
		openedInventories.clear();
		for (var entry : closing.entrySet()) {
			Player player = entry.getValue().player;
			if (isViewing(player, entry.getKey())) player.closeInventory();
		}
	}
	public boolean isThisOne(Inventory inventory, InventoryHolder h) {
		return openedInventories.containsKey(inventory);
	}
	@Override
	protected void onInventoryClose(InventoryCloseEvent evt, Inventory inv) {
		super.onInventoryClose(evt, inv);
		Inventory inventory = evt.getInventory();
		MenuSession session = openedInventories.get(inventory);
		if (session != null && session.player.equals(evt.getPlayer())) {
			openedInventories.remove(inventory);
			if (openedInventories.isEmpty()) destroy();
		}
	}
	@Override
	protected void onInventoryClick(InventoryClickEvent evt, Inventory inv) {
		super.onInventoryClick(evt, inv);
		Inventory inventory = evt.getInventory();
		if (!isThisOne(inventory, evt.getWhoClicked())) return;
		int slot = evt.getRawSlot();
		if (slot < 0 || slot >= size) {
			InventoryAction action = evt.getAction();
			if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.COLLECT_TO_CURSOR
					|| action == InventoryAction.UNKNOWN) evt.setCancelled(true);
			return;
		}
		boolean alreadyCancelled = evt.isCancelled();
		evt.setCancelled(true);
		if (alreadyCancelled || (evt.getClick() != ClickType.LEFT && evt.getClick() != ClickType.RIGHT)) return;
		MenuSession session = openedInventories.get(inventory);
		if (!session.player.equals(evt.getWhoClicked()) || session.actionPending || optionNames[slot] == null || plugin == null) return;
		session.actionPending = true;
		String optionName = optionNames[slot];
		// Answer the cancelled click before potentially expensive option work.
		Bukkit.getScheduler().runTask(plugin, () -> dispatchOptionClick(inventory, session, slot, optionName));
	}

	@Override
	protected void onInventoryDrag(InventoryDragEvent evt, Inventory inv) {
		if (isThisOne(evt.getInventory(), evt.getWhoClicked())
				&& evt.getRawSlots().stream().anyMatch(slot -> slot >= 0 && slot < size)) evt.setCancelled(true);
	}

	private static boolean isViewing(Player player, Inventory inventory) {
		return player.isOnline() && player.getOpenInventory().getTopInventory() == inventory;
	}

	private void dispatchOptionClick(Inventory inventory, MenuSession session, int slot, String optionName) {
		Player player = session.player;
		OptionClickEventHandler handler = this.handler;
		Plugin plugin = this.plugin;
		if (handler == null || plugin == null || openedInventories.get(inventory) != session || !isViewing(player, inventory)) return;
		OptionClickEvent e = new OptionClickEvent(player, slot, optionName, this);
		try {
			handler.onOptionClick(e);
		} catch (RuntimeException | Error exception) {
			session.actionPending = false;
			throw exception;
		}
		if (e.willDestroy()) {
			destroy();
		} else if (e.willClose()) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				if (openedInventories.get(inventory) == session && isViewing(player, inventory)) player.closeInventory();
			});
		} else {
			session.actionPending = false;
		}
	}


	public interface OptionClickEventHandler {
		void onOptionClick(OptionClickEvent event);
	}

	public class OptionClickEvent {
		private Player player;
		private int position;
		private String name;
		private boolean close;
		private boolean destroy;
		private IconMenu menu;
		public OptionClickEvent(Player player, int position, String name, IconMenu menu) {
			this.player = player;
			this.position = position;
			this.name = name;
			this.close = true;
			this.destroy = false;
			this.menu = menu;
		}

		public Player getPlayer() {
			return player;
		}

		public int getPosition() {
			return position;
		}

		public String getName() {
			return name;
		}

		public boolean willClose() {
			return close;
		}

		public boolean willDestroy() {
			return destroy;
		}

		public void setWillClose(boolean close) {
			this.close = close;
		}

		public void setWillDestroy(boolean destroy) {
			this.destroy = destroy;
		}
		public IconMenu getMenu(){
			return menu;
		}
	}

	private static ItemStack setItemNameAndLore(ItemStack item, String name, String[] lore) {
		item = item.clone();
		ItemMeta im = item.getItemMeta();
		im.setDisplayName(name);
		im.setLore(Arrays.asList(lore));
		item.setItemMeta(im);
		return item;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getOptionNames() {
		return optionNames;
	}

	public void setOptionNames(String[] optionNames) {
		this.optionNames = optionNames;
	}

	public ItemStack[] getOptionIcons() {
		return optionIcons;
	}

	public void setOptionIcons(ItemStack[] optionIcons) {
		this.optionIcons = optionIcons;
	}

}
