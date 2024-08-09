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

package de.florianmichael.viafabricplus.settings.impl;

import de.florianmichael.viafabricplus.settings.base.BooleanSetting;
import de.florianmichael.viafabricplus.settings.base.ModeSetting;
import de.florianmichael.viafabricplus.settings.base.SettingGroup;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class GeneralSettings extends SettingGroup {

    private static final GeneralSettings INSTANCE = new GeneralSettings();

    private final MutableComponent[] ORIENTATION_OPTIONS = new MutableComponent[] {
            Component.translatable("base.viafabricplus.none"),
            Component.translatable("base.viafabricplus.left_top"),
            Component.translatable("base.viafabricplus.right_top"),
            Component.translatable("base.viafabricplus.left_bottom"),
            Component.translatable("base.viafabricplus.right_bottom")
    };

    public final ModeSetting multiplayerScreenButtonOrientation = new ModeSetting(this, Component.translatable("general_settings.viafabricplus.multiplayer_screen_button_orientation"), 2, ORIENTATION_OPTIONS);
    public final ModeSetting addServerScreenButtonOrientation = new ModeSetting(this, Component.translatable("general_settings.viafabricplus.add_server_screen_button_orientation"), 2, ORIENTATION_OPTIONS);
    public final ModeSetting directConnectScreenButtonOrientation = new ModeSetting(this, Component.translatable("general_settings.viafabricplus.direct_connect_screen_button_orientation"), 2, ORIENTATION_OPTIONS);
    public final ModeSetting removeNotAvailableItemsFromCreativeTab = new ModeSetting(this, Component.translatable("general_settings.viafabricplus.filter_creative_tabs"),
            Component.translatable("base.viafabricplus.vanilla_and_modded"),
            Component.translatable("base.viafabricplus.vanilla_only"),
            Component.translatable("base.viafabricplus.off")
    );
    public final BooleanSetting saveSelectedProtocolVersion = new BooleanSetting(this, Component.translatable("general_settings.viafabricplus.save_selected_protocol_version"), true);
    public final BooleanSetting showExtraInformationInDebugHud = new BooleanSetting(this, Component.translatable("general_settings.viafabricplus.extra_information_in_debug_hud"), true);
    public final BooleanSetting showClassicLoadingProgressInConnectScreen = new BooleanSetting(this, Component.translatable("general_settings.viafabricplus.show_classic_loading_progress"), true);
    public final BooleanSetting showAdvertisedServerVersion = new BooleanSetting(this, Component.translatable("general_settings.viafabricplus.show_advertised_server_version"), true);
    public final ModeSetting ignorePacketTranslationErrors = new ModeSetting(this, Component.translatable("general_settings.viafabricplus.ignore_packet_translation_errors"),
            Component.translatable("base.viafabricplus.kick"),
            Component.translatable("base.viafabricplus.cancel_and_notify"),
            Component.translatable("base.viafabricplus.cancel")
    );
    public final BooleanSetting loadSkinsAndSkullsInLegacyVersions = new BooleanSetting(this, Component.translatable("general_settings.viafabricplus.load_skins_and_skulls_in_legacy_versions"), true);
    public final BooleanSetting emulateInventoryActionsInAlphaVersions = new BooleanSetting(this, Component.translatable("general_settings.viafabricplus.emulate_inventory_actions_in_alpha_versions"), true);

    public GeneralSettings() {
        super(Component.translatable("setting_group_name.viafabricplus.general"));
        emulateInventoryActionsInAlphaVersions.setTooltip(Component.translatable("base.viafabricplus.this_will_require_a_restart"));
    }

    public static Button.Builder withOrientation(final Button.Builder builder, final int orientationIndex, final int width, final int height) {
        return switch (orientationIndex) {
            case 1 -> builder.pos(5, 5);
            case 2 -> builder.pos(width - 98 - 5, 5);
            case 3 -> builder.pos(5, height - 20 - 5);
            case 4 -> builder.pos(width - 98 - 5, height - 20 - 5);
            default -> builder;
        };
    }

    public static GeneralSettings global() {
        return INSTANCE;
    }

}
