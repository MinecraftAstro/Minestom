package net.minestom.server.experimental.inventory.strategy;

import net.minestom.server.entity.Player;
import net.minestom.server.experimental.inventory.chain.ClickHandlerChain;
import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

final class StrategyContextImpl implements StrategyContext {

    private ClickStrategy clickStrategy;
    private Player player;
    private ClientClickWindowPacket packet;
    private ClickHandlerChain chain;

    @Override
    public void setChain(ClickHandlerChain chain) {
        this.chain = chain;
    }

    @Override
    public void setClickStrategy(ClickStrategy clickStrategy) {
        this.clickStrategy = clickStrategy;
    }

    @Override
    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public void setPacket(ClientClickWindowPacket packet) {
        this.packet = packet;
    }

    @Override
    public void execute() {
        if (this.clickStrategy == null) {
            throw new IllegalStateException("ClickStrategy is not set");
        }
        if (this.player == null) {
            throw new IllegalStateException("Player is not set");
        }
        if (this.packet == null) {
            throw new IllegalStateException("Packet is not set");
        }
        this.clickStrategy.handlePacket(this.packet, this.chain, this.player);
    }
}
