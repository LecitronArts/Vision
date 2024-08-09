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
package net.raphimc.vialoader.impl.platform;

import com.viaversion.viaversion.api.Via;
import net.raphimc.viabedrock.platform.ViaBedrockPlatform;
import net.raphimc.vialoader.util.JLoggerToSLF4J;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.logging.Logger;

public class ViaBedrockPlatformImpl implements ViaBedrockPlatform {

    private static final Logger LOGGER = new JLoggerToSLF4J(LoggerFactory.getLogger("ViaBedrock"));

    public ViaBedrockPlatformImpl() {
        this.init(new File(this.getDataFolder(), "viabedrock.yml"));
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public File getDataFolder() {
        return Via.getPlatform().getDataFolder();
    }

}
