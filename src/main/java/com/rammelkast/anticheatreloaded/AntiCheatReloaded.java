/*
 * AntiCheatReloaded for Bukkit and Spigot.
 * Copyright (c) 2012-2015 AntiCheat Team
 * Copyright (c) 2016-2020 Rammelkast
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.rammelkast.anticheatreloaded;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.rammelkast.anticheatreloaded.command.CommandHandler;
import com.rammelkast.anticheatreloaded.config.Configuration;
import com.rammelkast.anticheatreloaded.event.BlockListener;
import com.rammelkast.anticheatreloaded.event.EntityListener;
import com.rammelkast.anticheatreloaded.event.InventoryListener;
import com.rammelkast.anticheatreloaded.event.PlayerListener;
import com.rammelkast.anticheatreloaded.event.VehicleListener;
import com.rammelkast.anticheatreloaded.manage.AntiCheatManager;
import com.rammelkast.anticheatreloaded.metrics.Metrics;
import com.rammelkast.anticheatreloaded.util.PacketListener;
import com.rammelkast.anticheatreloaded.util.UpdateManager;
import com.rammelkast.anticheatreloaded.util.User;
import com.rammelkast.anticheatreloaded.util.VersionUtil;

public class AntiCheatReloaded extends JavaPlugin {

	public static final List<UUID> MUTE_ENABLED_MODS = new ArrayList<UUID>();
	public static final double TpsMax = 10;
	
	private static AntiCheatManager manager;
	private static AntiCheatReloaded plugin;
	private static List<Listener> eventList = new ArrayList<Listener>();
	private static Configuration config;
	private static boolean verbose;
	private static ProtocolManager protocolManager;
	private static SecureRandom random;
	private static Long loadTime;
	private static UpdateManager updateManager;
	
	private double tps = -1;
	private String symbiosisMetric = "None";

	@Override
	public void onEnable() {
		plugin = this;
		random = new SecureRandom();
		loadTime = System.currentTimeMillis();
		manager = new AntiCheatManager(this, getLogger());
		eventList.add(new PlayerListener());
		eventList.add(new BlockListener());
		eventList.add(new EntityListener());
		eventList.add(new VehicleListener());
		eventList.add(new InventoryListener());
		// Order is important in some cases, don't screw with these unless
		// needed, especially config
		this.setupConfig();
		this.setupEvents();
		this.setupCommands();
		// Enterprise must come before levels
		this.setupEnterprise();
		this.restoreLevels();
		
		if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
			this.setupProtocol();
		} else {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "ACR " + ChatColor.DARK_GRAY + "> " + ChatColor.RED
					+ "Could not find ProtocolLib. Shutting down!");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		
		Bukkit.getConsoleSender()
				.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "ACR " + ChatColor.DARK_GRAY + "> " + ChatColor.GRAY
						+ "Running Minecraft version " + VersionUtil.getVersion() + " "
						+ (VersionUtil.isSupported() ? (ChatColor.GREEN + "(supported)")
								: (ChatColor.RED + "(NOT SUPPORTED!)")));

		getLogger().info("Enabling packet listeners");
		PacketListener.listenMovementPackets();
		PacketListener.listenKeepAlivePackets();
		PacketListener.listenUseItemPackets();
		
		updateManager = new UpdateManager();
		
		// End tests
		verboseLog("Finished loading.");

		// Metrics
		getServer().getScheduler().runTaskLater(this, new Runnable() {
			@Override
			public void run() {
				checkForSymbiosis();
				try {
					Metrics metrics = new Metrics(AntiCheatReloaded.this, 202);
					metrics.addCustomChart(new Metrics.SingleLineChart("cheaters_kicked", new Callable<Integer>() {
						@Override
						public Integer call() throws Exception {
							int kicked = playersKicked;
							// Reset so we don't keep sending the same value
							playersKicked = 0;
							return kicked;
						}
					}));
					metrics.addCustomChart(new Metrics.SimplePie("protocollib_version", new Callable<String>() {
						@Override
						public String call() throws Exception {
							return Bukkit.getPluginManager().getPlugin("ProtocolLib").getDescription().getVersion();
						}
					}));
					metrics.addCustomChart(new Metrics.SimplePie("nms_version", new Callable<String>() {
						@Override
						public String call() throws Exception {
							return VersionUtil.getVersion();
						}
					}));
					metrics.addCustomChart(new Metrics.SimplePie("symbiosis", new Callable<String>() {
						@Override
						public String call() throws Exception {
							return symbiosisMetric;
						}
					}));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 90L);
		
		// Launch TPS check
		new BukkitRunnable() {
            long sec;
            long currentSec;
            int ticks;

            public void run() {
                sec = (System.currentTimeMillis() / 1000L);
                if (currentSec == sec) {
                    ticks += 1;
                } else {
                    currentSec = sec;
                    tps = (tps == 0.0D ? ticks : (tps + ticks) / 2.0D);
                    ticks = 1;
                }
                
                // Check for updates every 12 hours
                if (ticks % 864000 == 0)
                	updateManager.update();
            }
        }.runTaskTimer(this, 40L, 1L);
	}

	@Override
	public void onDisable() {
		verboseLog("Saving user levels...");
		config.getLevels().saveLevelsFromUsers(getManager().getUserManager().getUsers());

		AntiCheatManager.close();
		getServer().getScheduler().cancelTasks(this);
		cleanup();
	}

	private void setupProtocol() {
		protocolManager = ProtocolLibrary.getProtocolManager();
		verboseLog("Hooked into ProtocolLib");
	}

	private void setupEvents() {
		for (Listener listener : eventList) {
			getServer().getPluginManager().registerEvents(listener, this);
			verboseLog("Registered events for ".concat(listener.toString().split("@")[0].split(".anticheat.")[1]));
		}
	}

	private void setupCommands() {
		getCommand("anticheat").setExecutor(new CommandHandler());
		verboseLog("Registered commands.");
	}

	private void setupConfig() {
		config = manager.getConfiguration();
		verboseLog("Setup the config.");
	}

	private void setupEnterprise() {
		if (config.getConfig().enterprise.getValue()) {
			if (config.getEnterprise().loggingEnabled.getValue()) {
				config.getEnterprise().database.cleanEvents();
			}
		}
	}

	private void restoreLevels() {
		for (Player player : getServer().getOnlinePlayers()) {
			UUID uuid = player.getUniqueId();

			User user = new User(uuid);
			user.setIsWaitingOnLevelSync(true);
			config.getLevels().loadLevelToUser(user);

			manager.getUserManager().addUser(user);
			verboseLog("Data for " + uuid + " loaded");
		}
	}

	public static AntiCheatReloaded getPlugin() {
		return plugin;
	}

	public static AntiCheatManager getManager() {
		return manager;
	}

	public static SecureRandom getRandom() {
		return random;
	}
	
	public static String getVersion() {
		return manager.getPlugin().getDescription().getVersion();
	}

	private void cleanup() {
		eventList = null;
		manager = null;
		config = null;
	}
	
	public static void debugLog(final String string) {
		Bukkit.getScheduler().runTask(getPlugin(), new Runnable() {
			public void run() {
				if (getManager().getConfiguration().getConfig().debugMode.getValue()) {
					manager.debugLog("[DEBUG] " + string);
				}
			}
		});
	}

	public void verboseLog(final String string) {
		if (verbose) {
			getLogger().info(string);
		}
	}

	public void setVerbose(boolean b) {
		verbose = b;
	}

	public Long getLoadTime() {
		return loadTime;
	}

	public static ProtocolManager getProtocolManager() {
		return protocolManager;
	}

	/**
	 * Amount of players kicked since start
	 */
	private int playersKicked = 0;

	public void onPlayerKicked() {
		this.playersKicked++;
	}

	/**
	 * Creates metric that checks for anti-cheat symbiosis
	 * I use this to see if AntiCheatReloaded is actively being used
	 * together with other anti-cheats, so I can account for that.
	 */
	protected void checkForSymbiosis() {
		if (Bukkit.getPluginManager().getPlugin("NoCheatPlus") != null) {
			if (this.symbiosisMetric.equals("None")) {
				this.symbiosisMetric = "NoCheatPlus";
			} else {
				this.symbiosisMetric += ", NoCheatPlus";
			}
		}
		if (Bukkit.getPluginManager().getPlugin("Matrix") != null) {
			if (this.symbiosisMetric.equals("None")) {
				this.symbiosisMetric = "Matrix";
			} else {
				this.symbiosisMetric += ", Matrix";
			}
		}
		if (Bukkit.getPluginManager().getPlugin("AAC") != null) {
			if (this.symbiosisMetric.equals("None")) {
				this.symbiosisMetric = "AAC";
			} else {
				this.symbiosisMetric += ", AAC";
			}
		}
		if (Bukkit.getPluginManager().getPlugin("Spartan") != null) {
			if (this.symbiosisMetric.equals("None")) {
				this.symbiosisMetric = "Spartan";
			} else {
				this.symbiosisMetric += ", Spartan";
			}
		}
		if (Bukkit.getPluginManager().getPlugin("Negativity") != null) {
			if (this.symbiosisMetric.equals("None")) {
				this.symbiosisMetric = "Negativity";
			} else {
				this.symbiosisMetric += ", Negativity";
			}
		}
	}

	public static void sendToMainThread(Runnable runnable) {
		Bukkit.getScheduler().runTask(AntiCheatReloaded.getPlugin(), runnable);
	}
	
	public void sendToStaff(String message) {
		Bukkit.getOnlinePlayers().forEach(player -> {
			if (player.hasPermission("anticheat.system.alert")) {
				if (!MUTE_ENABLED_MODS.contains(player.getUniqueId())) {
					player.sendMessage(message);
				}
			}
		});
	}
	
	public static UpdateManager getUpdateManager() {
		return updateManager;
	}
	
	public double getTPS() {
		if (this.tps < 0 || this.tps > 20) {
			return 20;
		}
		return this.tps;
	}
	
}
