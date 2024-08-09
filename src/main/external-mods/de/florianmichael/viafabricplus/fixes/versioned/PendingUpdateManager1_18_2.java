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

package de.florianmichael.viafabricplus.fixes.versioned;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * No-op implementation of {@link BlockStatePredictionHandler} for 1.18.2 and lower since those versions don't have the
 * {@link BlockStatePredictionHandler} class.
 */
public class PendingUpdateManager1_18_2 extends BlockStatePredictionHandler {

    @Override
    public void retainKnownServerState(BlockPos pos, BlockState state, LocalPlayer player) {
    }

    @Override
    public boolean updateKnownServerState(BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public void endPredictionsUpTo(int maxProcessableSequence, ClientLevel world) {
    }

    @Override
    public BlockStatePredictionHandler startPredicting() {
        return this;
    }

    @Override
    public void close() {
    }

    @Override
    public int currentSequence() {
        return 0;
    }

    @Override
    public boolean isPredicting() {
        return false;
    }

}
