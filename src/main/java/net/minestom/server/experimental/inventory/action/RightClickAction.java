package net.minestom.server.experimental.inventory.action;

import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

public final class RightClickAction extends Action {

    public RightClickAction(Player player, ClientClickWindowPacket packet) {
        super(player, packet);
    }
}
