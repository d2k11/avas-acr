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

package com.rammelkast.anticheatreloaded.manage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.rammelkast.anticheatreloaded.AntiCheatReloaded;
import com.rammelkast.anticheatreloaded.check.CheckType;
import com.rammelkast.anticheatreloaded.config.Configuration;
import com.rammelkast.anticheatreloaded.util.VersionUtil;

/**
 * <p/>
 * The manager that AntiCheat will check with to see if it should watch certain checks and certain players.
 */

public class CheckManager {
    private AntiCheatManager manager = null;
    private Configuration config;
    private static List<CheckType> checkIgnoreList = new ArrayList<CheckType>();
    private static Map<UUID, List<CheckType>> exemptList = new HashMap<UUID, List<CheckType>>();

    public CheckManager(AntiCheatManager manager) {
        this.manager = manager;
        this.config = manager.getConfiguration();
        this.loadCheckIgnoreList(this.config);
    }

    public void loadCheckIgnoreList(Configuration configuration) {
    	checkIgnoreList.clear();
        for (CheckType type : CheckType.values()) {
            if (!configuration.getChecks().isEnabled(type)) {
                checkIgnoreList.add(type);
            }
        }
        
        if (!checkIgnoreList.isEmpty()) {
        	Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "ACR " + ChatColor.DARK_GRAY + "> " + ChatColor.GRAY
					+ checkIgnoreList.size() + " check(s) have been disabled");
        }
    }
    
    /**
     * Turn a check on
     *
     * @param type The CheckType to enable
     */
    public void activateCheck(CheckType type, String className) {
        if (!isActive(type)) {
            manager.getLoggingManager().logFineInfo("The " + type.toString() + " check was activated by " + className + ".");
            checkIgnoreList.remove(type);
        }
    }

    /**
     * Turn a check off
     *
     * @param type The CheckType to disable
     */
    public void deactivateCheck(CheckType type, String className) {
        if (isActive(type)) {
            manager.getLoggingManager().logFineInfo("The " + type.toString() + " check was deactivated by " + className + ".");
            checkIgnoreList.add(type);
        }
    }

    /**
     * Determine whether a check is enabled
     *
     * @param type The CheckType to check
     * @return true if the check is active
     */
    public boolean isActive(CheckType type) {
    	if(AntiCheatReloaded.getPlugin().getTPS() >= AntiCheatReloaded.TpsMax) {
    		return true;
    	}
        return !checkIgnoreList.contains(type);
    }

    /**
     * Exempt a player from a check
     *
     * @param player The player
     * @param type   The check
     */
    public void exemptPlayer(Player player, CheckType type, String className) {
        if (!isExempt(player, type)) {
            if (!exemptList.containsKey(player.getUniqueId())) {
                exemptList.put(player.getUniqueId(), new ArrayList<CheckType>());
            }
            manager.getLoggingManager().logFineInfo(player.getName() + " was exempted from the " + type.toString() + " check by " + className + ".");
            exemptList.get(player.getUniqueId()).add(type);
        }
    }

    /**
     * Unexempt a player from a check
     *
     * @param player The player
     * @param type   The check
     */
    public void unexemptPlayer(Player player, CheckType type, String className) {
        if (isExempt(player, type)) {
            manager.getLoggingManager().logFineInfo(player.getName() + " was unexempted from the " + type.toString() + " check by " + className + ".");
            exemptList.get(player.getUniqueId()).remove(type);
        }
    }

    /**
     * Determine whether a player is exempt from a check
     *
     * @param player The player
     * @param type   The check
     */
    public boolean isExempt(Player player, CheckType type) {
    	if(AntiCheatReloaded.getPlugin().getTPS() >= AntiCheatReloaded.TpsMax) {
    		return true;
    	}
        return exemptList.containsKey(player.getUniqueId()) ? exemptList.get(player.getUniqueId()).contains(type) : false;
    }

    /**
     * Determine whether a player is exempt from all checks from op status
     *
     * @param player The player
     */
    public boolean isOpExempt(Player player) {
    	if(AntiCheatReloaded.getPlugin().getTPS() >= AntiCheatReloaded.TpsMax) {
    		return true;
    	}
        return (this.manager.getConfiguration().getConfig().exemptOp.getValue() && player.isOp());
    }

    /**
     * Determine whether a player should be checked in their world
     *
     * @param player The player
     * @return true if the player's world is enabled
     */
    public boolean checkInWorld(Player player) {
    	if(AntiCheatReloaded.getPlugin().getTPS() >= AntiCheatReloaded.TpsMax) {
    		return false;
    	}
        return !config.getConfig().disabledWorlds.getValue().contains(player.getWorld().getName());
    }

    /**
     * Run a quick version of the "willCheck" method, using the other non-check-specific methods beforehand
     *
     * @param player The player to check
     * @param type   The check being run
     * @return true if the check should run
     */
    public boolean willCheckQuick(Player player, CheckType type) {
    	if(AntiCheatReloaded.getPlugin().getTPS() >= AntiCheatReloaded.TpsMax) {
    		return false;
    	}
    	return
                isActive(type)
                        && !isExempt(player, type)
                        && !type.checkPermission(player);
    }

    /**
     * Determine whether a check should run on a player
     *
     * @param player The player to check
     * @param type   The check being run
     * @return true if the check should run
     */
    public boolean willCheck(Player player, CheckType type) {
    	if(AntiCheatReloaded.getPlugin().getTPS() >= AntiCheatReloaded.TpsMax) {
    		return false;
    	}
        boolean check = isActive(type)
                && checkInWorld(player)
                && !isExempt(player, type)
                && !type.checkPermission(player)
                && !isOpExempt(player);
        if ((type == CheckType.FLIGHT || type == CheckType.SPEED) && VersionUtil.isFlying(player))
        	return false;
        return check;
    }

    /**
     * Determine whether a player is actually online; not an NPC
     *
     * @param player Player to check
     * @return true if the player is a real person
     */
    public boolean isOnline(Player player) {
        // Check if the player is on the user list, e.g. is not an NPC
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }
}
