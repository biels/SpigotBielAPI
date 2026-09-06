package com.biel.BielAPI.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import com.biel.BielAPI.BielAPI;
import com.biel.BielAPI.Com;

public class ItemButton implements Listener {
	private static ArrayList<ItemButton> instances = new ArrayList<ItemButton>();

	/**
	 * The key every button stamps its own id under. One key for all buttons; the
	 * value says which button, so two buttons made of the same material with the
	 * same name are still two buttons.
	 */
	private static final String BUTTON_ID_KEY = "item_button_id";

	private OptionClickEventHandler handler;
	private Plugin plugin;

	private ItemStack itemStack;
	private String pName;
	private Object data;
	private final String buttonId = UUID.randomUUID().toString();
	private final NamespacedKey idKey;

	public ItemButton(ItemStack item, Player ply, OptionClickEventHandler handler) {
		this.handler = handler;
		this.pName = ply.getName();
		this.plugin = Com.getPlugin();
		this.idKey = new NamespacedKey(plugin, BUTTON_ID_KEY);
		this.itemStack = stampWithId(item);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		//------------------
		instances.add(this);
	}

	/**
	 * Marks an item as this button, in the one place on an item that the server
	 * is obliged to hand back untouched.
	 *
	 * The stack is stamped in place rather than copied, so a caller that keeps its
	 * own reference and puts that in the inventory places a stamped item too.
	 */
	private ItemStack stampWithId(ItemStack item) {
		if (item == null) {return null;}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {return item;}
		meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, buttonId);
		item.setItemMeta(meta);
		return item;
	}

	/**
	 * Whether an item the player is holding is this button.
	 *
	 * This used to compare {@link ItemStack#hashCode()}, which is a hash of the
	 * item's material, amount, durability and meta — a value comparison standing in
	 * for an identity. It stopped answering true on Paper 26.2 and every ItemButton
	 * in every game went dead: the stack stored here is built in plugin code, while
	 * the one arriving on the event has been through the server and back, and since
	 * 1.20.5 an item's meta is rebuilt from data components rather than carried as
	 * the NBT it was written as. Nothing promises those two hash the same, and they
	 * no longer do.
	 *
	 * A persistent data container does promise it. It exists so a plugin can mark an
	 * item as its own and find it again, it is preserved verbatim across the copy
	 * into and out of NMS, and it is untouched by anything the server normalises —
	 * which is the whole of the difference. It is also independent of where the item
	 * sits, what it is called, how many of it there are and what it is made of, so a
	 * button survives being moved down the hotbar, renamed or stacked.
	 */
	public Boolean isCurrentStack(ItemStack stack){
		if (itemStack == null || stack == null){return false;}
		ItemMeta meta = stack.getItemMeta();
		if (meta == null){return false;}
		return buttonId.equals(meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING));
	}
		
	public Player getPlayer(){
		return Bukkit.getPlayer(pName);
	}

	public Object getData() {
		return data;
	}
	public void setData(Object data) {
		this.data = data;
	}
	public void destroy() {
		HandlerList.unregisterAll(this);
		handler = null;
		plugin = null;
		
	}
	public static void clearButtons(Player ply){
		ArrayList<ItemButton> instancesToRemove = new ArrayList<ItemButton>();
		for (ItemButton but : instances){
			if (but.getPlayer() == ply){
				instancesToRemove.add(but);
			}
		}
		for (ItemButton but : instancesToRemove){
			if (but.getPlayer() == ply){
				but.destroy();
				instances.remove(but);
			}
		}
	}
	
	public ItemStack getItemStack() {
		return itemStack;
	}
	public void setItemStack(ItemStack itemStack) {
		this.itemStack = stampWithId(itemStack);
	}
	@EventHandler(priority=EventPriority.MONITOR)
	void onInteract(PlayerInteractEvent evt) {
		if (!isCurrentStack(evt.getItem())){return;}
		if (!pName.equals(evt.getPlayer().getName())){return;}
		if(evt.getAction() == Action.RIGHT_CLICK_AIR || evt.getAction() == Action.RIGHT_CLICK_BLOCK){
			Plugin plugin = this.plugin;
			OptionClickEvent e = new OptionClickEvent(evt.getPlayer(), evt);
			e.setData(data);
			if (handler != null) {
				handler.onOptionClick(e);
			}
			evt.setCancelled(true);
		}
	}

	public interface OptionClickEventHandler {
		void onOptionClick(OptionClickEvent event);
	}

	public class OptionClickEvent {
		private Player player;
		private Object data;
		private PlayerInteractEvent evt;
		public OptionClickEvent(Player player, PlayerInteractEvent evt) {
			this.player = player;
			this.evt = evt;
		}

		public Player getPlayer() {
			return player;
		}
		public Object getData() {
			return data;
		}

		public void setData(Object data) {
			this.data = data;
		}

		public PlayerInteractEvent getOnInteractEvent(){
			return evt;
		}

		
	}

	private static ItemStack setItemNameAndLore(ItemStack item, String name, String[] lore) {
		ItemMeta im = item.getItemMeta();
		im.setDisplayName(name);
		im.setLore(Arrays.asList(lore));
		item.setItemMeta(im);
		return item;
	}

}
