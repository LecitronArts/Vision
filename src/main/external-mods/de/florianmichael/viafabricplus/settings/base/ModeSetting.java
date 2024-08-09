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

import com.google.gson.JsonObject;
import de.florianmichael.viafabricplus.screen.VFPListEntry;
import de.florianmichael.viafabricplus.screen.settings.ModeSettingRenderer;
import de.florianmichael.viafabricplus.util.ChatUtil;
import java.util.Arrays;
import net.minecraft.network.chat.MutableComponent;

public class ModeSetting extends AbstractSetting<MutableComponent> {

    private final MutableComponent[] options;

    public ModeSetting(SettingGroup parent, MutableComponent name, MutableComponent... options) {
        this(parent, name, 0, options);
    }

    public ModeSetting(SettingGroup parent, MutableComponent name, int defaultOption, MutableComponent... options) {
        super(parent, name, options[defaultOption]);
        this.options = options;
    }

    @Override
    public VFPListEntry makeSettingRenderer() {
        return new ModeSettingRenderer(this);
    }

    @Override
    public void write(JsonObject object) {
        object.addProperty(getTranslationKey(), mapTranslationKey(ChatUtil.uncoverTranslationKey(getValue())));
    }

    @Override
    public void read(JsonObject object) {
        final String selected = object.get(getTranslationKey()).getAsString();
        for (MutableComponent option : options) {
            if (mapTranslationKey(ChatUtil.uncoverTranslationKey(option)).equals(selected)) {
                setValue(option);
                break;
            }
        }
    }

    public void setValue(int index) {
        super.setValue(options[index]);
    }

    public int getIndex() {
        return Arrays.stream(options).toList().indexOf(getValue());
    }

    public MutableComponent[] getOptions() {
        return options;
    }

}
