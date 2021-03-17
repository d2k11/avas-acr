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
package com.rammelkast.anticheatreloaded.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.rammelkast.anticheatreloaded.AntiCheatReloaded;

public class VersionUtil {

	public static final MinecraftVersion CURRENT_VERSION;
	private static final List<String> SUPPORTED_VERSIONS;

	public static String getVersion() {
		return Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
	}

	public static boolean isSupported() {
		for (String versionId : SUPPORTED_VERSIONS) {
			if (getVersion().startsWith(versionId)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isOfVersion(String versionId) {
		if (getVersion().startsWith(versionId)) {
			return true;
		}
		return false;
	}

	/**
	 * @return the if server is running Bountiful Update (1.8)
	 */
	public static boolean isBountifulUpdate() {
		return isOfVersion("v1_8");
	}

	public static boolean isFlying(Player player) {
		if (isBountifulUpdate()) {
			return player.isFlying();
		}
		return player.isFlying() || player.isGliding() || player.hasPotionEffect(PotionEffectType.LEVITATION)
				|| AntiCheatReloaded.getManager().getBackend().justLevitated(player);
	}

	public static boolean isSlowFalling(Player player) {
		if (!CURRENT_VERSION.isAtLeast(MinecraftVersion.AQUATIC_UPDATE)) {
			return false;
		}
		return player.hasPotionEffect(PotionEffectType.SLOW_FALLING);
	}

	public static boolean isFrostWalk(Player player) {
		if (player.getInventory().getBoots() == null || isBountifulUpdate()) {
			return false;
		}
		return player.getInventory().getBoots().containsEnchantment(Enchantment.FROST_WALKER);
	}

	@SuppressWarnings("deprecation")
	public static ItemStack getItemInHand(Player player) {
		if (isBountifulUpdate()) {
			return player.getItemInHand();
		}
		return player.getInventory().getItemInMainHand();
	}

	public static int getPlayerPing(Player player) {
		try {
			Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
			int ping = (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
			return ping;
		} catch (Exception e) {
			return -1;
		}
	}

	public static Block getTargetBlock(Player player, int distance) {
		if (!CURRENT_VERSION.isAtLeast(MinecraftVersion.AQUATIC_UPDATE)) {
			return player.getTargetBlock((Set<Material>) null, distance);
		}
		return player.getTargetBlockExact(distance);
	}

	public static boolean isGliding(Player player) {
		if (isBountifulUpdate()) {
			return false;
		}
		return player.isGliding();
	}

	public static boolean isLevitationEffect(PotionEffect effect) {
		if (isBountifulUpdate()) {
			return false;
		}
		return effect.getType().equals(PotionEffectType.LEVITATION);
	}

	public static int getPotionLevel(Player player, PotionEffectType type) {
		if (isBountifulUpdate()) {
			for (PotionEffect effect : player.getActivePotionEffects()) {
				if (effect.getType().equals(type))
					return effect.getAmplifier() + 1;
			}
			return 0;
		}

		if (player.hasPotionEffect(type))
			return player.getPotionEffect(type).getAmplifier() + 1;
		return 0;
	}

	public static boolean isSwimming(Player player) {
		if (!CURRENT_VERSION.isAtLeast(MinecraftVersion.AQUATIC_UPDATE)) {
			return false;
		}
		return player.isSwimming();
	}

	static {
		SUPPORTED_VERSIONS = Arrays.asList(new String[] { "v1_16", "v1_15", "v1_14", "v1_13", "v1_12", "v1_8_R3" });
		CURRENT_VERSION = MinecraftVersion.getCurrentVersion();
	}
}
