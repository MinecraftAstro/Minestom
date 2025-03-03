package net.minestom.server.experimental.inventory.strategy;

import net.minestom.server.entity.Player;
import net.minestom.server.experimental.inventory.action.Action;
import net.minestom.server.experimental.inventory.action.factory.ModeFactory;
import net.minestom.server.experimental.inventory.chain.ClickHandlerChain;
import net.minestom.server.experimental.inventory.chain.ClickHandlerResponse;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

public final class PickupStrategy implements ClickStrategy {

    @Override
    public void handlePacket(ClientClickWindowPacket packet, ClickHandlerChain chain, Player player) {
        Action action = ModeFactory.create().create(packet).create(packet).create(packet, player);
        chain.handle(action, ClickHandlerResponse.create(), chain);
    }
}
