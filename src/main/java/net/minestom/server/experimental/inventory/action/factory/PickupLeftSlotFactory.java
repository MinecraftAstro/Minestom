package net.minestom.server.experimental.inventory.action.factory;

import net.minestom.server.entity.Player;
import net.minestom.server.experimental.inventory.action.Action;
import net.minestom.server.experimental.inventory.action.DropLeftClickAction;
import net.minestom.server.experimental.inventory.action.LeftClickAction;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

final class PickupLeftSlotFactory implements SlotFactory {

    @Override
    public Action create(ClientClickWindowPacket packet, Player player) {
        short slot = packet.slot();
        if (slot == -999) {
            return new DropLeftClickAction(player, packet);
        }
        return new LeftClickAction(player, packet);
    }
}
