package net.minestom.server.experimental.inventory.strategy;

import net.minestom.server.entity.Player;
import net.minestom.server.experimental.inventory.chain.ClickHandlerChain;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

public sealed interface StrategyContext permits StrategyContextImpl {

    void setChain(ClickHandlerChain chain);

    void setClickStrategy(ClickStrategy clickStrategy);

    void setPlayer(Player player);

    void setPacket(ClientClickWindowPacket packet);

    void execute();

    static StrategyContext create() {
        return new StrategyContextImpl();
    }

}
