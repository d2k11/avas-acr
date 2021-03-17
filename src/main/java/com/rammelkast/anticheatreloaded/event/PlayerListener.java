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

package com.rammelkast.anticheatreloaded.event;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.rammelkast.anticheatreloaded.AntiCheatReloaded;
import com.rammelkast.anticheatreloaded.check.CheckResult;
import com.rammelkast.anticheatreloaded.check.CheckType;
import com.rammelkast.anticheatreloaded.check.combat.VelocityCheck;
import com.rammelkast.anticheatreloaded.check.movement.AimbotCheck;
import com.rammelkast.anticheatreloaded.check.movement.BoatFlyCheck;
import com.rammelkast.anticheatreloaded.check.movement.ElytraCheck;
import com.rammelkast.anticheatreloaded.check.movement.FastLadderCheck;
import com.rammelkast.anticheatreloaded.check.movement.FlightCheck;
import com.rammelkast.anticheatreloaded.check.movement.SpeedCheck;
import com.rammelkast.anticheatreloaded.check.movement.StrafeCheck;
import com.rammelkast.anticheatreloaded.check.movement.WaterWalkCheck;
import com.rammelkast.anticheatreloaded.check.player.IllegalInteract;
import com.rammelkast.anticheatreloaded.util.Distance;
import com.rammelkast.anticheatreloaded.util.Permission;
import com.rammelkast.anticheatreloaded.util.User;
import com.rammelkast.anticheatreloaded.util.Utilities;
import com.rammelkast.anticheatreloaded.util.VersionUtil;
import com.rammelkast.anticheatreloaded.util.XMaterial;

public class PlayerListener extends EventListener {

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (getCheckManager().willCheck(player, CheckType.COMMAND_SPAM)
				&& !Permission.getCommandExempt(player, event.getMessage().split(" ")[0])) {
			CheckResult result = getBackend().checkCommandSpam(player, event.getMessage());
			if (result.failed()) {
				event.setCancelled(!silentMode());
				if (!silentMode())
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', result.getMessage()));
				getBackend().processCommandSpammer(player);
				log(null, player, CheckType.COMMAND_SPAM, result.getSubCheck());
			}
		}

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
		if (!event.isFlying()) {
			getBackend().logEnterExit(event.getPlayer());
		}

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
		if (event.getNewGameMode() != GameMode.CREATIVE) {
			getBackend().logEnterExit(event.getPlayer());
		}

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onProjectileLaunch(ProjectileLaunchEvent event) {
		if (event.getEntity().getShooter() instanceof Player) {
			Player player = (Player) event.getEntity().getShooter();

			if (event.getEntity() instanceof Arrow) {
				return;
			}

			if (getCheckManager().willCheck(player, CheckType.FAST_PROJECTILE)) {
				CheckResult result = getBackend().checkProjectile(player);
				if (result.failed()) {
					event.setCancelled(!silentMode());
					log(result.getMessage(), player, CheckType.FAST_PROJECTILE, result.getSubCheck());
				}
			}
		}

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		getBackend().logTeleport(event.getPlayer());
		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerChangeWorlds(PlayerChangedWorldEvent event) {
		getBackend().logTeleport(event.getPlayer());

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
		Player player = event.getPlayer();
		if (event.isSneaking()) {
			if (getCheckManager().willCheck(player, CheckType.SNEAK)) {
				CheckResult result = getBackend().checkSneakToggle(player);
				if (result.failed()) {
					event.setCancelled(!silentMode());
					log(result.getMessage(), player, CheckType.SNEAK, result.getSubCheck());
				}
			}
			// getBackend().logToggleSneak(event.getPlayer());
		}

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerVelocity(PlayerVelocityEvent event) {
		Player player = event.getPlayer();
		if (getCheckManager().willCheck(player, CheckType.FLIGHT)) {
			if (getBackend().justVelocity(player) && getBackend().extendVelocityTime(player)) {
				event.setCancelled(!silentMode());
				return;
			}
			getBackend().logVelocity(player);
		}
		
		// Part of Velocity check
		User user = AntiCheatReloaded.getManager().getUserManager()
				.getUser(event.getPlayer().getUniqueId());
		if (!user.getMovementManager().onGround)
			return;
		double motionY = event.getVelocity().getY();
		user.getMovementManager().velocityExpectedMotionY = motionY;
		// End part of Velocity check
		
		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();

		if (getCheckManager().willCheck(player, CheckType.CHAT_SPAM)) {
			CheckResult result = getBackend().checkChatSpam(player, event.getMessage());
			if (result.failed()) {
				event.setCancelled(!silentMode());
				if (!result.getMessage().equals("") && !silentMode()) {
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', result.getMessage()));
				}
				getBackend().processChatSpammer(player);
				AntiCheatReloaded.sendToMainThread(new Runnable() {
					@Override
					public void run() {
						log(null, player, CheckType.CHAT_SPAM, result.getSubCheck());
					}
				});
			}
		}

		if (getCheckManager().willCheck(player, CheckType.CHAT_UNICODE)) {
			CheckResult result = getBackend().checkChatUnicode(player, event.getMessage());
			if (result.failed()) {
				event.setCancelled(true);
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', result.getMessage()));
				AntiCheatReloaded.sendToMainThread(new Runnable() {
					@Override
					public void run() {
						log(null, player, CheckType.CHAT_UNICODE, result.getSubCheck());
					}
				});
			}
		}

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		getBackend().garbageClean(event.getPlayer());

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		getBackend().garbageClean(event.getPlayer());

		User user = getUserManager().getUser(event.getPlayer().getUniqueId());

		getConfig().getLevels().saveLevelFromUser(user);

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerVehicleCollide(VehicleEntityCollisionEvent event) {
		if (!(event.getVehicle() instanceof Boat) || !(event.getEntity() instanceof Player))
			return;

		Player player = (Player) event.getEntity();
		getBackend().logBoatCollision(player);
	}

	@EventHandler
	public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
		Player player = event.getPlayer();
		if (getCheckManager().willCheck(player, CheckType.SPRINT)) {
			CheckResult result = getBackend().checkSprintHungry(event);
			if (result.failed()) {
				event.setCancelled(!silentMode());
				log(result.getMessage(), player, CheckType.SPRINT, result.getSubCheck());
			} else {
				decrease(player);
			}
		}

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		PlayerInventory inv = player.getInventory();
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			ItemStack itemInHand;
			if (VersionUtil.isBountifulUpdate()) {
				itemInHand = VersionUtil.getItemInHand(player);
			} else {
				itemInHand = ((event.getHand() == EquipmentSlot.HAND) ? inv.getItemInMainHand()
						: inv.getItemInOffHand());
			}

			if (itemInHand.getType() == Material.BOW) {
				getBackend().logBowWindUp(player);
			} else if (Utilities.isFood(itemInHand.getType()) || Utilities.isFood(itemInHand.getType())) {
				getBackend().logEatingStart(player);
			}

			if (!VersionUtil.isBountifulUpdate()) {
				if (itemInHand.getType() == XMaterial.FIREWORK_ROCKET.parseMaterial()) {
					ElytraCheck.JUMP_Y_VALUE.remove(player.getUniqueId());
					if (player.isGliding()) {
						// TODO config max elytra height?
						ElytraCheck.JUMP_Y_VALUE.put(player.getUniqueId(), 9999.99D);
					}
				}
			}
		}

		Block block = event.getClickedBlock();
		if (block != null
				&& (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
			if (getCheckManager().willCheck(player, CheckType.ILLEGAL_INTERACT)) {
				CheckResult result = IllegalInteract.performCheck(player, event);
				if (result.failed()) {
					event.setCancelled(!silentMode());
					log(result.getMessage(), player, CheckType.ILLEGAL_INTERACT, result.getSubCheck());
				}
			}
		}

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		if (getCheckManager().willCheck(player, CheckType.ITEM_SPAM)) {
			CheckResult result = getBackend().checkFastDrop(player);
			if (result.failed()) {
				event.setCancelled(!silentMode());
				log(result.getMessage(), player, CheckType.ITEM_SPAM, result.getSubCheck());
			}
		}

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerEnterBed(PlayerBedEnterEvent event) {
		if (event.getBed().getType().name().endsWith("BED"))
			return;
		getBackend().logEnterExit(event.getPlayer());

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerExitBed(PlayerBedLeaveEvent event) {
		if (event.getBed().getType().name().endsWith("BED"))
			return;
		getBackend().logEnterExit(event.getPlayer());

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerAnimation(PlayerAnimationEvent event) {
		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		getBackend().logJoin(player);

		User user = new User(player.getUniqueId());
		user.setIsWaitingOnLevelSync(true);
		getConfig().getLevels().loadLevelToUser(user);
		getUserManager().addUser(user);

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());

		if (player.hasPermission("anticheat.admin") && !AntiCheatReloaded.getUpdateManager().isLatest()) {
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "ACR " + ChatColor.GRAY
					+ "Your version of AntiCheatReloaded is outdated! You can download "
					+ AntiCheatReloaded.getUpdateManager().getLatestVersion()
					+ " from the Spigot forums or DevBukkit.");
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		final Player player = event.getPlayer();
		if (getCheckManager().checkInWorld(player) && !getCheckManager().isOpExempt(player)) {
			final Location from = event.getFrom();
			final Location to = event.getTo();

			final Distance distance = new Distance(from, to);
			final double y = distance.getYDifference();

			final User user = getUserManager().getUser(player.getUniqueId());
			user.setTo(to.getX(), to.getY(), to.getZ());
			user.getMovementManager().handle(player, from, to, distance);

			if (getCheckManager().willCheckQuick(player, CheckType.FLIGHT) && !VersionUtil.isFlying(player)) {
				CheckResult result = FlightCheck.runCheck(player, distance);
				if (result.failed()) {
					if (!silentMode()) {
						event.setTo(user.getGoodLocation(from.clone()));
					}
					log(result.getMessage(), player, CheckType.FLIGHT, result.getSubCheck());
				}
			}
			if (getCheckManager().willCheckQuick(player, CheckType.ELYTRAFLY)) {
				CheckResult result = ElytraCheck.runCheck(player, distance);
				if (result.failed()) {
					log(result.getMessage(), player, CheckType.ELYTRAFLY, result.getSubCheck());
				}
			}
			if (getCheckManager().willCheckQuick(player, CheckType.BOATFLY)) {
				CheckResult result = BoatFlyCheck.runCheck(player, user.getMovementManager(), to);
				if (result.failed()) {
					if (!silentMode()) {
						player.eject();
						event.setTo(user.getGoodLocation(from.clone()));
					}
					log(result.getMessage(), player, CheckType.BOATFLY, result.getSubCheck());
				}
			}
			if (getCheckManager().willCheckQuick(player, CheckType.VCLIP)
					&& event.getFrom().getY() > event.getTo().getY()) {
				CheckResult result = getBackend().checkVClip(player, new Distance(event.getFrom(), event.getTo()));
				if (result.failed()) {
					if (!silentMode()) {
						int data = result.getData() > 3 ? 3 : result.getData();
						Location newloc = new Location(player.getWorld(), event.getFrom().getX(),
								event.getFrom().getY() + data, event.getFrom().getZ());
						if (newloc.getBlock().getType() == Material.AIR) {
							event.setTo(newloc);
						} else {
							event.setTo(user.getGoodLocation(from.clone()));
						}
						player.damage(3);
					}
					log(result.getMessage(), player, CheckType.VCLIP, result.getSubCheck());
				}
			}
			if (getCheckManager().willCheckQuick(player, CheckType.NOFALL)
					&& getCheckManager().willCheck(player, CheckType.FLIGHT)
					&& !Utilities.isClimbableBlock(player.getLocation().getBlock())
					&& event.getFrom().getY() > event.getTo().getY()) {
				CheckResult result = getBackend().checkNoFall(player, y);
				if (result.failed()) {
					if (!silentMode()) {
						event.setTo(user.getGoodLocation(from.clone()));
						// TODO better handling of this
					}
					log(result.getMessage(), player, CheckType.NOFALL, result.getSubCheck());
				}
			}

			if (event.getTo() != event.getFrom()) {
				double x = distance.getXDifference();
				double z = distance.getZDifference();
				if (getCheckManager().willCheckQuick(player, CheckType.SPEED)
						&& getCheckManager().willCheck(player, CheckType.FLIGHT)) {
					if (event.getFrom().getY() < event.getTo().getY()) {
						CheckResult result = SpeedCheck.checkVerticalSpeed(player, distance);
						if (result.failed()) {
							if (!silentMode()) {
								event.setTo(user.getGoodLocation(from.clone()));
							}
							log(result.getMessage(), player, CheckType.SPEED, result.getSubCheck());
						}
					}
					CheckResult result = SpeedCheck.checkXZSpeed(player, x, z, event.getTo());
					if (result.failed()) {
						if (!silentMode()) {
							event.setTo(user.getGoodLocation(from.clone()));
						}
						log(result.getMessage(), player, CheckType.SPEED, result.getSubCheck());
					}
				}
				if (getCheckManager().willCheckQuick(player, CheckType.WATER_WALK)) {
					CheckResult result = WaterWalkCheck.runCheck(player, x, y, z);
					if (result.failed()) {
						if (!silentMode()) {
							// TODO check this
							player.teleport(player.getLocation().clone().subtract(0, 0.52, 0));
						}
						log(result.getMessage(), player, CheckType.WATER_WALK, result.getSubCheck());
					}
				}
				if (getCheckManager().willCheckQuick(player, CheckType.SPIDER)) {
					CheckResult result = getBackend().checkSpider(player, y);
					if (result.failed()) {
						if (!silentMode()) {
							event.setTo(user.getGoodLocation(from.clone()));
						}
						log(result.getMessage(), player, CheckType.SPIDER, result.getSubCheck());
					}
				}
				if (getCheckManager().willCheckQuick(player, CheckType.FASTLADDER)) {
					// Does not use y value created before because that value is absolute
					CheckResult result = FastLadderCheck.runCheck(player,
							event.getTo().getY() - event.getFrom().getY());
					if (result.failed()) {
						if (!silentMode()) {
							event.setTo(user.getGoodLocation(from.clone()));
						}
						log(result.getMessage(), player, CheckType.FASTLADDER, result.getSubCheck());
					}
				}
				if (getCheckManager().willCheckQuick(player, CheckType.STRAFE)) {
					CheckResult result = StrafeCheck.runCheck(player, x, z, event.getFrom(), event.getTo());
					if (result.failed()) {
						if (!silentMode()) {
							event.setTo(user.getGoodLocation(from.clone()));
						}
						log(result.getMessage(), player, CheckType.STRAFE, result.getSubCheck());
					}
				}
			}
			if (getCheckManager().willCheckQuick(player, CheckType.AIMBOT)) {
				CheckResult result = AimbotCheck.runCheck(player, event);
				if (result.failed()) {
					log(result.getMessage(), player, CheckType.AIMBOT, result.getSubCheck());
				}
			}
			if (getCheckManager().willCheckQuick(player, CheckType.VELOCITY)) {
				CheckResult result = VelocityCheck.runCheck(player, distance);
				if (result.failed()) {
					log(result.getMessage(), player, CheckType.VELOCITY, result.getSubCheck());
				}
			}
		}

		AntiCheatReloaded.getManager().addEvent(event.getEventName(), event.getHandlers().getRegisteredListeners());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRespawn(PlayerRespawnEvent event) {
		getBackend().logTeleport(event.getPlayer());
	}

}
