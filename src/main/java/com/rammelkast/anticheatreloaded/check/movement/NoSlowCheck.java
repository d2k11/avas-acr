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
package com.rammelkast.anticheatreloaded.check.movement;

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
import com.rammelkast.anticheatreloaded.util.MovementManager;
import com.rammelkast.anticheatreloaded.util.User;

public class NoSlowCheck {

	public static final Map<UUID, Long> LAST_RELEASE = new HashMap<UUID, Long>();
	public static final Map<UUID, Integer> VIOLATIONS = new HashMap<UUID, Integer>();

	public static void runCheck(Player player, PacketEvent event) {
		if (!AntiCheatReloaded.getManager().getCheckManager().willCheck(player, CheckType.NOSLOW))
			return;
		UUID uuid = player.getUniqueId();
		User user = AntiCheatReloaded.getManager().getUserManager().getUser(uuid);
		MovementManager movementManager = user.getMovementManager();
		Checks checksConfig = AntiCheatReloaded.getManager().getConfiguration().getChecks();
		final long time = System.currentTimeMillis();
		final long lastRelease = LAST_RELEASE.getOrDefault(uuid, 0L);
		LAST_RELEASE.put(uuid, time);
		if (lastRelease == 0L)
			return;

		long difference = time - lastRelease;
		long minimumDifference = checksConfig.getInteger(CheckType.NOSLOW, "minimumDifference");
		if (difference < minimumDifference
				&& movementManager.distanceXZ >= checksConfig.getDouble(CheckType.NOSLOW, "minimumDistXZ")) {
			int violations = VIOLATIONS.getOrDefault(uuid, 1);
			if (violations++ >= checksConfig.getInteger(CheckType.NOSLOW, "vlBeforeFlag")) {
				violations = 0;
				flag(player, event,
						"toggled use item too fast (diff=" + difference + ", min=" + minimumDifference + ")");
			}
			VIOLATIONS.put(uuid, violations);
		}
	}

	private static void flag(Player player, PacketEvent event, String message) {
		event.setCancelled(true);
		// We are currently not in the main server thread, so switch
		AntiCheatReloaded.sendToMainThread(new Runnable() {
			@Override
			public void run() {
				EventListener.log(new CheckResult(CheckResult.Result.FAILED, message).getMessage(), player,
						CheckType.NOSLOW, null);
				player.teleport(player.getLocation());
			}
		});
	}

}
