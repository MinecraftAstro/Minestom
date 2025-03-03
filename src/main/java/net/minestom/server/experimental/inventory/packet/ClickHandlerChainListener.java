package net.minestom.server.experimental.inventory.packet;

import net.minestom.server.entity.Player;
import net.minestom.server.experimental.inventory.chain.ClickHandlerChain;
import net.minestom.server.experimental.inventory.strategy.PickupStrategy;
import net.minestom.server.experimental.inventory.strategy.StrategyContext;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

public final class ClickHandlerChainListener {

    private static final ClickHandlerChain CHAIN = ClickHandlerChain.createWithDefaults();

    public static void clickWindowListener(ClientClickWindowPacket packet, Player player) { // Per Packet Call
        StrategyContext context = StrategyContext.create();
        context.setPlayer(player);
        context.setPacket(packet);
        var strategy = switch (packet.clickType()) {
            case PICKUP -> new PickupStrategy();
            default -> throw new IllegalStateException("Unexpected value: " + packet.clickType());
        };
        context.setClickStrategy(strategy);
        context.execute();
    }


}
