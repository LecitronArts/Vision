/*
 * This file is part of ViaFabricPlus - https://github.com/FlorianMichael/ViaFabricPlus
 * Copyright (C) 2021-2024 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and RK_01/RaphiMC
 * Copyright (C) 2023-2024 contributors
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

package de.florianmichael.viafabricplus.settings.base;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * This class represents a group of settings. It is used to group settings in the settings screen.
 * @see AbstractSetting
 */
public class SettingGroup {

    private final List<AbstractSetting<?>> settings = new ArrayList<>();
    private final Component name;

    public SettingGroup(Component name) {
        this.name = name;
    }

    /**
     * This list is used to store the settings of this group. It should not be touched directly by developers.
     * The list gets filled automatically when creating a new setting (see {@link AbstractSetting}).
     *
     * @return The list of settings.
     */
    public List<AbstractSetting<?>> getSettings() {
        return settings;
    }

    public Component getName() {
        return name;
    }

}
