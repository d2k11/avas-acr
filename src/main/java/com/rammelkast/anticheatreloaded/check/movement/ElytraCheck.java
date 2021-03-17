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
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.rammelkast.anticheatreloaded.AntiCheatReloaded;
import com.rammelkast.anticheatreloaded.check.CheckResult;
import com.rammelkast.anticheatreloaded.util.Distance;
import com.rammelkast.anticheatreloaded.util.Utilities;
import com.rammelkast.anticheatreloaded.util.VersionUtil;

public class ElytraCheck {

	public static final HashMap<UUID, Double> JUMP_Y_VALUE = new HashMap<UUID, Double>();
	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);

	public static CheckResult runCheck(Player player, Distance distance) {
		// No elytra in server version or not relevant
		if (VersionUtil.isBountifulUpdate() || Utilities.isNearWater(player))
			return PASS;
		
		UUID uuid = player.getUniqueId();
		if (distance.getYDifference() > AntiCheatReloaded.getManager().getBackend().getMagic().TELEPORT_MIN()) {
			// This was a teleport, so skip check.
			JUMP_Y_VALUE.remove(uuid);
			return PASS;
		}

		if (player.isFlying() || player.hasPotionEffect(PotionEffectType.LEVITATION) || !player.isGliding()) {
			JUMP_Y_VALUE.remove(uuid);
			return PASS;
		}

		double changeY = distance.toY() - distance.fromY();
		boolean upwardMovement = changeY > 0;
		if (player.getInventory().getItemInMainHand().getType() == Material.TRIDENT) {
			if (upwardMovement) {
				// TODO continue check here
				JUMP_Y_VALUE.remove(uuid);
				return PASS;
			}
		}

		if (changeY == 0.0D) {
			// Seen no false positives here yet
			return new CheckResult(CheckResult.Result.FAILED, "had no Y-axis dropoff when gliding with Elytra");
		}

		if (!JUMP_Y_VALUE.containsKey(uuid)) {
			// Distance + player height
			JUMP_Y_VALUE.put(uuid, distance.toY() + 1.8D);
			return PASS;
		}
		
		double lastY = JUMP_Y_VALUE.get(uuid);
		if (lastY < distance.toY()) {
			double diff = distance.toY() - lastY;
			if (diff > 0.7675) {
				if (!AntiCheatReloaded.getManager().getBackend().silentMode()) {
					Location to = player.getLocation();
					to.setY(to.getY() - diff);
					player.teleport(to);
				}
				return new CheckResult(CheckResult.Result.FAILED, "tried to glide above jump level");
			}
		}
		return PASS;
	}

}
