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
package com.rammelkast.anticheatreloaded.check.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.rammelkast.anticheatreloaded.AntiCheatReloaded;
import com.rammelkast.anticheatreloaded.check.CheckResult;
import com.rammelkast.anticheatreloaded.check.CheckType;
import com.rammelkast.anticheatreloaded.check.combat.KillAuraCheck;
import com.rammelkast.anticheatreloaded.config.providers.Checks;
import com.rammelkast.anticheatreloaded.util.User;
import com.rammelkast.anticheatreloaded.util.Utilities;
import com.rammelkast.anticheatreloaded.util.VersionUtil;

public class IllegalInteract {

	private static final CheckResult PASS = new CheckResult(CheckResult.Result.PASSED);

	public static CheckResult performCheck(Player player, Event event) {
		Checks checksConfig = AntiCheatReloaded.getManager().getConfiguration().getChecks();
		if (event instanceof BlockPlaceEvent && checksConfig.isSubcheckEnabled(CheckType.ILLEGAL_INTERACT, "place")) {
			return checkBlockPlace(player, (BlockPlaceEvent) event);
		} else if (event instanceof BlockBreakEvent
				&& checksConfig.isSubcheckEnabled(CheckType.ILLEGAL_INTERACT, "break")) {
			return checkBlockBreak(player, (BlockBreakEvent) event);
		} else if (event instanceof PlayerInteractEvent
				&& checksConfig.isSubcheckEnabled(CheckType.ILLEGAL_INTERACT, "interact")) {
			return checkInteract(player, (PlayerInteractEvent) event);
		}
		return PASS;
	}

	private static CheckResult checkInteract(Player player, PlayerInteractEvent event) {
		User user = AntiCheatReloaded.getManager().getUserManager().getUser(player.getUniqueId());
		Checks checksConfig = AntiCheatReloaded.getManager().getConfiguration().getChecks();
		double distance = player.getEyeLocation().toVector().distance(event.getClickedBlock().getLocation().toVector());
		double maxDistance = player.getGameMode() == GameMode.CREATIVE
				? checksConfig.getDouble(CheckType.ILLEGAL_INTERACT, "interact", "creativeRange")
				: checksConfig.getDouble(CheckType.ILLEGAL_INTERACT, "interact", "survivalRange");

		maxDistance += user.isLagging() ? 0.12 : 0;
		maxDistance += user.getPing()
				* checksConfig.getInteger(CheckType.ILLEGAL_INTERACT, "interact", "pingCompensation");
		maxDistance += player.getVelocity().length()
				* checksConfig.getDouble(CheckType.ILLEGAL_INTERACT, "interact", "velocityMultiplier");
		if (distance > maxDistance) {
			return new CheckResult(CheckResult.Result.FAILED, "Interact",
					"tried to interact out of range (dist=" + distance + ", max=" + maxDistance + ")");
		}
		return PASS;
	}

	private static CheckResult checkBlockBreak(Player player, BlockBreakEvent event) {
		if (!isValidTarget(player, event.getBlock())) {
			return new CheckResult(CheckResult.Result.FAILED, "Break", "tried to break a block which was out of view");
		}
		return PASS;
	}

	private static CheckResult checkBlockPlace(Player player, BlockPlaceEvent event) {
		if (event.getBlock().getType().isSolid() && !isValidTarget(player, event.getBlock())) {
			return new CheckResult(CheckResult.Result.FAILED, "Place", "tried to place a block out of their view");
		}
		return PASS;
	}

	private static boolean isValidTarget(Player player, Block block) {
		Checks checksConfig = AntiCheatReloaded.getManager().getConfiguration().getChecks();
		double distance = player.getGameMode() == GameMode.CREATIVE ? 6.0
				: player.getLocation().getDirection().getY() > 0.9 ? 6.0 : 5.5;
		Block targetBlock = VersionUtil.getTargetBlock(player, ((int) Math.ceil(distance)));
		if (targetBlock == null) {
			// TODO better check here
			return true;
		}

		if (Utilities.isClimbableBlock(targetBlock)) {
			if (targetBlock.getLocation().distance(player.getLocation()) <= distance) {
				return true;
			}
		}

		if (targetBlock.equals(block)) {
			return true;
		}

		Location eyeLocation = player.getEyeLocation();
		double yawDifference = KillAuraCheck.calculateYawDifference(eyeLocation, block.getLocation());
		double playerYaw = player.getEyeLocation().getYaw();
		double angleDifference = Math.abs(180 - Math.abs(Math.abs(yawDifference - playerYaw) - 180));
		if (Math.round(angleDifference) > checksConfig.getInteger(CheckType.ILLEGAL_INTERACT, "maxAngleDifference")) {
			return false;
		}
		return true;
	}

}
