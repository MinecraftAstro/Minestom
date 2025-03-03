package net.minestom.server.experimental.inventory.strategy;

import net.minestom.server.entity.Player;
import net.minestom.server.experimental.inventory.chain.ClickHandlerChain;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

public interface ClickStrategy {

    void handlePacket(ClientClickWindowPacket packet, ClickHandlerChain chain, Player player);

}
