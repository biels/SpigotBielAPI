package com.biel.BielAPI.Utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Minecraft 1.8 Title
 * 
 * @version 1.0.4
 * @author Maxim Van de Wynckel
 */
public class Title {
	/* Title text and color */
	private String title = "";
	private ChatColor titleColor = ChatColor.WHITE;
	/* Subtitle text and color */
	private String subtitle = "";
	private ChatColor subtitleColor = ChatColor.WHITE;
	/* Title timings */
	private int fadeInTime = -1;
	private int stayTime = -1;
	private int fadeOutTime = -1;
	private boolean ticks = false;

	/**
	 * Create a new 1.8 title
	 * 
	 * @param title
	 *            Title
	 */
	public Title(String title) {
		this.title = title;
	}

	/**
	 * Create a new 1.8 title
	 * 
	 * @param title
	 *            Title text
	 * @param subtitle
	 *            Subtitle text
	 */
	public Title(String title, String subtitle) {
		this.title = title;
		this.subtitle = subtitle;
	}

	/**
	 * Copy 1.8 title
	 * 
	 * @param title
	 *            Title
	 */
	public Title(Title title) {
		// Copy title
		this.title = title.title;
		this.subtitle = title.subtitle;
		this.titleColor = title.titleColor;
		this.subtitleColor = title.subtitleColor;
		this.fadeInTime = title.fadeInTime;
		this.fadeOutTime = title.fadeOutTime;
		this.stayTime = title.stayTime;
		this.ticks = title.ticks;
	}

	/**
	 * Create a new 1.8 title
	 * 
	 * @param title
	 *            Title text
	 * @param subtitle
	 *            Subtitle text
	 * @param fadeInTime
	 *            Fade in time
	 * @param stayTime
	 *            Stay on screen time
	 * @param fadeOutTime
	 *            Fade out time
	 */
	public Title(String title, String subtitle, int fadeInTime, int stayTime,
			int fadeOutTime) {
		this.title = title;
		this.subtitle = subtitle;
		this.fadeInTime = fadeInTime;
		this.stayTime = stayTime;
		this.fadeOutTime = fadeOutTime;
	}

	/**
	 * Set title text
	 * 
	 * @param title
	 *            Title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Get title text
	 * 
	 * @return Title text
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Set subtitle text
	 * 
	 * @param subtitle
	 *            Subtitle text
	 */
	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	/**
	 * Get subtitle text
	 * 
	 * @return Subtitle text
	 */
	public String getSubtitle() {
		return this.subtitle;
	}

	/**
	 * Set the title color
	 * 
	 * @param color
	 *            Chat color
	 */
	public void setTitleColor(ChatColor color) {
		this.titleColor = color;
	}

	/**
	 * Set the subtitle color
	 * 
	 * @param color
	 *            Chat color
	 */
	public void setSubtitleColor(ChatColor color) {
		this.subtitleColor = color;
	}

	/**
	 * Set title fade in time
	 * 
	 * @param time
	 *            Time
	 */
	public void setFadeInTime(int time) {
		this.fadeInTime = time;
	}

	/**
	 * Set title fade out time
	 * 
	 * @param time
	 *            Time
	 */
	public void setFadeOutTime(int time) {
		this.fadeOutTime = time;
	}

	/**
	 * Set title stay time
	 * 
	 * @param time
	 *            Time
	 */
	public void setStayTime(int time) {
		this.stayTime = time;
	}

	/**
	 * Set timings to ticks
	 */
	public void setTimingsToTicks() {
		ticks = true;
	}

	/**
	 * Set timings to seconds
	 */
	public void setTimingsToSeconds() {
		ticks = false;
	}

	/**
	 * Send the title to a player
	 * 
	 * @param player
	 *            Player
	 */
	public void send(Player player) {
		int fadeInTicks = toTicks(fadeInTime, 10);
		int stayTicks = toTicks(stayTime, 70);
		int fadeOutTicks = toTicks(fadeOutTime, 20);
		player.sendTitle(titleColor + title, subtitleColor + subtitle,
				fadeInTicks, stayTicks, fadeOutTicks);
	}

	/**
	 * Broadcast the title to all players
	 */
	public void broadcast() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			send(p);
		}
	}

	/**
	 * Clear the title
	 * 
	 * @param player
	 *            Player
	 */
	public void clearTitle(Player player) {
		player.clearTitle();
	}

	/**
	 * Reset the title settings
	 * 
	 * @param player
	 *            Player
	 */
	public void resetTitle(Player player) {
		player.resetTitle();
	}

	private int toTicks(int time, int defaultTicks) {
		if (time < 0) {
			return defaultTicks;
		}
		return ticks ? time : Math.multiplyExact(time, 20);
	}
}
