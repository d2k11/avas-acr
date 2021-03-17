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

package com.rammelkast.anticheatreloaded.check.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.comphenix.protocol.events.PacketEvent;
import com.rammelkast.anticheatreloaded.AntiCheatReloaded;
import com.rammelkast.anticheatreloaded.check.CheckResult;
import com.rammelkast.anticheatreloaded.check.CheckType;
import com.rammelkast.anticheatreloaded.config.providers.Checks;
import com.rammelkast.anticheatreloaded.event.EventListener;
import com.rammelkast.anticheatreloaded.util.User;

/**
 * @author Rammelkast
 */
public class MorePacketsCheck {

	public static final Map<UUID, Long> LAST_PACKET_TIME = new HashMap<UUID, Long>();
	public static final Map<UUID, Long> EXEMPT_TIMINGS = new HashMap<UUID, Long>();
	public static final Map<UUID, Double> PACKET_BALANCE = new HashMap<UUID, Double>();

	public static void runCheck(Player player, PacketEvent event) {
		// Confirm if we should even check for MorePackets
		Checks checksConfig = AntiCheatReloaded.getManager().getConfiguration().getChecks();
		double tps = AntiCheatReloaded.getPlugin().getTPS();
		if (!AntiCheatReloaded.getManager().getCheckManager().willCheck(player, CheckType.MOREPACKETS)
				|| tps < checksConfig.getDouble(CheckType.MOREPACKETS, "minimumTps")) {
			return;
		}
		
		if(AntiCheatReloaded.getPlugin().getTPS() >= AntiCheatReloaded.TpsMax) {
    		return;
    	}

		UUID uuid = player.getUniqueId();
		User user = AntiCheatReloaded.getManager().getUserManager().getUser(uuid);
		int maxPing = checksConfig.getInteger(CheckType.MOREPACKETS, "maxPing");
		boolean disableForLagging = checksConfig.getBoolean(CheckType.MOREPACKETS, "disableForLagging");
		if (AntiCheatReloaded.getManager().getBackend().isDoing(player, EXEMPT_TIMINGS, -1)
				|| (maxPing > 0 && user.getPing() > maxPing) || (disableForLagging && user.isLagging())) {
			return;
		}
		long packetTimeNow = System.currentTimeMillis();
		long lastPacketTime = LAST_PACKET_TIME.getOrDefault(uuid, packetTimeNow - 50L);
		double packetBalance = PACKET_BALANCE.getOrDefault(uuid, 0D);

		long rate = packetTimeNow - lastPacketTime;
		packetBalance += 50;
		packetBalance -= rate;
		int triggerBalance = checksConfig.getInteger(CheckType.MOREPACKETS, "triggerBalance");
		int minimumClamp = checksConfig.getInteger(CheckType.MOREPACKETS, "minimumClamp");
		if (packetBalance >= triggerBalance) {
			int ticks = (int) Math.round(packetBalance / 50);
			packetBalance = -1 * (triggerBalance / 2);
			flag(player, event, "overshot timer by " + ticks + " tick(s)");
		} else if (packetBalance < -1 * minimumClamp) {
			// Clamp minimum, 50ms=1tick of lag leniency
			packetBalance = -1 * minimumClamp;
		}

		LAST_PACKET_TIME.put(uuid, packetTimeNow);
		PACKET_BALANCE.put(uuid, packetBalance);
	}

	private static void flag(Player player, PacketEvent event, String message) {
		event.setCancelled(true);
		// We are currently not in the main server thread, so switch
		AntiCheatReloaded.sendToMainThread(new Runnable() {
			@Override
			public void run() {
				EventListener.log(new CheckResult(CheckResult.Result.FAILED, message).getMessage(), player,
						CheckType.MOREPACKETS, null);
				player.teleport(player.getLocation());
			}
		});
	}

	public static void compensate(Player player) {
		UUID uuid = player.getUniqueId();
		Checks checksConfig = AntiCheatReloaded.getManager().getConfiguration().getChecks();
		double packetBalance = PACKET_BALANCE.getOrDefault(uuid, 0D);
		PACKET_BALANCE.put(uuid, packetBalance - checksConfig.getInteger(CheckType.MOREPACKETS, "teleportCompensation"));
	}

}
