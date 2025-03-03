package net.minestom.server.experimental.inventory.action.factory;

import net.minestom.server.entity.Player;
import net.minestom.server.experimental.inventory.action.Action;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

public sealed interface SlotFactory permits PickupLeftSlotFactory, PickupRightSlotFactory {

    Action create(ClientClickWindowPacket packet, Player player);

}
