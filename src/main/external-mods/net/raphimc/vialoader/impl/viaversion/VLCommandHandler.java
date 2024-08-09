/*
 * This file is part of ViaLoader - https://github.com/RaphiMC/ViaLoader
 * Copyright (C) 2020-2024 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.vialoader.impl.viaversion;

import com.viaversion.viaversion.commands.ViaCommandHandler;
import net.raphimc.vialoader.commands.subs.ConnectionsSubCommand;
import net.raphimc.vialoader.commands.subs.LeakDetectSubCommand;

public class VLCommandHandler extends ViaCommandHandler {

    public VLCommandHandler() {
        this.registerSubCommand(new LeakDetectSubCommand());
        this.registerSubCommand(new ConnectionsSubCommand());
    }

}
