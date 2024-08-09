/*
 * This file is part of ViaLegacy - https://github.com/RaphiMC/ViaLegacy
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
package net.raphimc.vialegacy.protocols.classic.protocolc0_0_18a_02toc0_0_16a_02;

import net.raphimc.vialegacy.api.protocol.StatelessProtocol;
import net.raphimc.vialegacy.protocols.classic.protocolc0_0_20a_27toc0_0_19a_06.ClientboundPacketsc0_19a;
import net.raphimc.vialegacy.protocols.classic.protocolc0_0_20a_27toc0_0_19a_06.ServerboundPacketsc0_19a;

public class Protocolc0_0_18a_02toc0_0_16a_02 extends StatelessProtocol<ClientboundPacketsc0_19a, ClientboundPacketsc0_19a, ServerboundPacketsc0_19a, ServerboundPacketsc0_19a> {

    public Protocolc0_0_18a_02toc0_0_16a_02() {
        super(ClientboundPacketsc0_19a.class, ClientboundPacketsc0_19a.class, ServerboundPacketsc0_19a.class, ServerboundPacketsc0_19a.class);
    }

}
