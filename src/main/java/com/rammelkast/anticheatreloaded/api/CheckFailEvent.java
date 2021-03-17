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

package com.rammelkast.anticheatreloaded.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.rammelkast.anticheatreloaded.check.CheckType;
import com.rammelkast.anticheatreloaded.util.User;

/**
 * Fired when a player fails an AntiCheat check
 */

public class CheckFailEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final User user;
    private final CheckType type;

    public CheckFailEvent(User user, CheckType type) {
        this.user = user;
        this.type = type;
    }

    /**
     * Get the {@link com.rammelkast.anticheatreloaded.util.User} who failed the check
     *
     * @return a {@link com.rammelkast.anticheatreloaded.util.User}
     */
    public User getUser() {
        return user;
    }

    /**
     * Get the {@link com.rammelkast.anticheatreloaded.check.CheckType} failed
     *
     * @return a {@link com.rammelkast.anticheatreloaded.check.CheckType}
     */
    public CheckType getCheck() {
        return type;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
