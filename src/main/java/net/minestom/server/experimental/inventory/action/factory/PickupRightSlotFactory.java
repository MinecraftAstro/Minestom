package net.minestom.server.experimental.inventory.action.factory;

import net.minestom.server.entity.Player;
import net.minestom.server.experimental.inventory.action.Action;
import net.minestom.server.experimental.inventory.action.DropRightClickAction;
import net.minestom.server.experimental.inventory.action.RightClickAction;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

final class PickupRightSlotFactory implements SlotFactory {

    @Override
    public Action create(ClientClickWindowPacket packet, Player player) {
        short slot = packet.slot();
        if (slot == -999) {
            return new DropRightClickAction(player, packet);
        }
        return new RightClickAction(player, packet);
    }
}
