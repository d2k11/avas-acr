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
import org.bukkit.event.player.PlayerMoveEvent;

import com.rammelkast.anticheatreloaded.AntiCheatReloaded;
import com.rammelkast.anticheatreloaded.check.Backend;
import com.rammelkast.anticheatreloaded.check.CheckResult;
import com.rammelkast.anticheatreloaded.check.CheckType;
import com.rammelkast.anticheatreloaded.config.providers.Checks;

public class AimbotCheck {

	public static final Map<UUID, Float> LAST_DELTA_YAW = new HashMap<UUID, Float>();
	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);
	
	public static CheckResult runCheck(Player player, PlayerMoveEvent event) {
		Backend backend = AntiCheatReloaded.getManager().getBackend();
		Checks checksConfig = AntiCheatReloaded.getManager().getConfiguration().getChecks();
		if (backend.isMovingExempt(player)) {
			return PASS;
		}
		
		UUID uuid = player.getUniqueId();
		float dYaw = Math.abs(event.getTo().getYaw() - event.getFrom().getYaw());
		float dPitch = Math.abs(event.getTo().getPitch() - event.getFrom().getPitch());
		// Not interesting
		if (dYaw < 0.05 && dPitch < 0.05) {
			return PASS;
		}
		
		final float lastDeltaYaw = LAST_DELTA_YAW.getOrDefault(uuid, -1F);
		if (lastDeltaYaw == -1F) {
			LAST_DELTA_YAW.put(uuid, dYaw);
			return PASS;
		}
		LAST_DELTA_YAW.put(uuid, dYaw);
		
		float absoluteYawDifference = Math.abs(dYaw - lastDeltaYaw);
		int minYaw = checksConfig.getInteger(CheckType.AIMBOT, "minYaw");
		int maxYaw = checksConfig.getInteger(CheckType.AIMBOT, "maxYaw");
		if (absoluteYawDifference < 1E-8 && minYaw > 30 && dYaw < maxYaw)
			return new CheckResult(CheckResult.Result.FAILED, "repeated yaw difference (dYaw=" + dYaw + ")");
		return PASS;
	}

}
