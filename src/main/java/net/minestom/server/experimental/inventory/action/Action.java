package net.minestom.server.experimental.inventory.action;

import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

public abstract class Action {

    private final Player player;
    private final ClientClickWindowPacket packet;

    protected Action(Player player, ClientClickWindowPacket packet) {
        this.player = player;
        this.packet = packet;
    }

    public ClientClickWindowPacket getPacket() {
        return packet;
    }

    public Player getPlayer() {
        return player;
    }
}
