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

package de.florianmichael.viafabricplus.fixes;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.event.ChangeProtocolVersionCallback;
import de.florianmichael.viafabricplus.event.PostGameLoadCallback;
import de.florianmichael.viafabricplus.fixes.data.EntityDimensionDiff;
import de.florianmichael.viafabricplus.fixes.data.ResourcePackHeaderDiff;
import de.florianmichael.viafabricplus.fixes.versioned.classic.CPEAdditions;
import de.florianmichael.viafabricplus.fixes.versioned.classic.GridItemSelectionScreen;
import de.florianmichael.viafabricplus.fixes.versioned.visual.ArmorHudEmulation1_8;
import de.florianmichael.viafabricplus.fixes.versioned.visual.FootStepParticle1_12_2;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.BedrockSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocols.classic.protocolc0_28_30toc0_28_30cpe.data.ClassicProtocolExtension;
import net.raphimc.vialegacy.protocols.classic.protocolc0_28_30toc0_28_30cpe.storage.ExtensionProtocolMetadataStorage;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * This class contains random fields and methods that are used to fix bugs on the client side
 */
public class ClientsideFixes {

    /**
     * Contains all tasks that are waiting for a packet to be received, this system can be used to sync ViaVersion tasks with the correct thread
     */
    private static final Map<String, Consumer<FriendlyByteBuf>> PENDING_EXECUTION_TASKS = new ConcurrentHashMap<>();

    /**
     * This identifier is an internal identifier that is used to identify packets that are sent by ViaFabricPlus
     */
    public static final String PACKET_SYNC_IDENTIFIER = UUID.randomUUID() + ":" + UUID.randomUUID();

    /**
     * This identifier is an internal identifier used to store the item count in <= 1.10 to implement negative item counts
     */
    public static final String ITEM_COUNT_NBT_TAG = "VFP_1_10_ItemCount_" + System.currentTimeMillis();

    public static void init() {
        // Register additional CPE features
        CPEAdditions.modifyMappings();

        // Check if the pack format mappings are correct
        ResourcePackHeaderDiff.checkOutdated();

        PostGameLoadCallback.EVENT.register(() -> {
            // Handles and updates entity dimension changes in <= 1.17
            EntityDimensionDiff.init();

            // Ticks the armor hud manually in <= 1.8.x
            ArmorHudEmulation1_8.init();
        });

        // Reloads some clientside stuff when the protocol version changes
        ChangeProtocolVersionCallback.EVENT.register((oldVersion, newVersion) -> Minecraft.getInstance().execute(() -> {
            // Clear all font caches to enforce a reload of all fonts (this is needed because we change the font renderer behavior)
            for (FontSet storage : Minecraft.getInstance().fontManager.fontSets.values()) {
                storage.glyphs.clear();
                storage.glyphInfos.clear();
            }

            // Reloads all bounding boxes of the blocks that we changed
            for (Block block : BuiltInRegistries.BLOCK) {
                if (block instanceof AnvilBlock || block instanceof BedBlock || block instanceof BrewingStandBlock
                        || block instanceof CarpetBlock || block instanceof CauldronBlock || block instanceof ChestBlock
                        || block instanceof EnderChestBlock || block instanceof EndPortalBlock || block instanceof EndPortalFrameBlock
                        || block instanceof FarmBlock || block instanceof FenceBlock || block instanceof FenceGateBlock
                        || block instanceof HopperBlock || block instanceof LadderBlock || block instanceof LeavesBlock
                        || block instanceof WaterlilyBlock || block instanceof IronBarsBlock || block instanceof PistonBaseBlock
                        || block instanceof PistonHeadBlock || block instanceof SnowLayerBlock || block instanceof WallBlock
                        || block instanceof CropBlock || block instanceof PinkPetalsBlock
                ) {
                    for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                        state.initCache();
                    }
                }
            }

            // Rebuilds the item selection screen grid
            if (newVersion.olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
                GridItemSelectionScreen.INSTANCE.itemGrid = null;
            }
        }));

        // Register the footstep particle
        FootStepParticle1_12_2.init();
    }

    /**
     * Calculates the maximum chat length for the selected protocol version in {@link ProtocolTranslator#getTargetVersion()}
     *
     * @return The maximum chat length
     */
    public static int getChatLength() {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
            final ClientPacketListener handler = Minecraft.getInstance().getConnection();
            final ExtensionProtocolMetadataStorage extensionProtocol = ( handler.getConnection()).viaFabricPlus$getUserConnection().get(ExtensionProtocolMetadataStorage.class);

            if (extensionProtocol != null && extensionProtocol.hasServerExtension(ClassicProtocolExtension.LONGER_MESSAGES)) {
                return Short.MAX_VALUE * 2;
            } else {
                return 64 - (Minecraft.getInstance().getUser().getName().length() + 2);
            }
        } else if (ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            return 512;
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_9_3)) {
            return 100;
        } else {
            return 256;
        }
    }

    /**
     * Replaces the default port when parsing a server address if the default port should be replaced
     *
     * @param address The original address of the server
     * @param version The protocol version
     * @return The server address with the replaced default port
     */
    public static ServerAddress replaceDefaultPort(final String address, final ProtocolVersion version) {
        // If the default port for this entry should be replaced, check if the address already contains a port
        // We can't just replace vanilla's default port because a bedrock server might be running on the same port
        if (BedrockSettings.global().replaceDefaultPort.getValue() && Objects.equals(version, BedrockProtocolVersion.bedrockLatest) && !address.contains(":")) {
            return ServerAddress.parseString(address + ":" + ProtocolConstants.BEDROCK_DEFAULT_PORT);
        } else {
            return ServerAddress.parseString(address);
        }
    }

    /**
     * Executes a sync task and returns the uuid of the task
     *
     * @param task The task to execute
     * @return The uuid of the task
     */
    public static String executeSyncTask(final Consumer<FriendlyByteBuf> task) {
        final String uuid = UUID.randomUUID().toString();
        PENDING_EXECUTION_TASKS.put(uuid, task);
        return uuid;
    }

    public static void handleSyncTask(final FriendlyByteBuf buf) {
        final String uuid = buf.readUtf();

        if (PENDING_EXECUTION_TASKS.containsKey(uuid)) {
            Minecraft.getInstance().execute(() -> { // Execute the task on the main thread
                final var task = PENDING_EXECUTION_TASKS.remove(uuid);
                task.accept(buf);
            });
        }
    }

}
