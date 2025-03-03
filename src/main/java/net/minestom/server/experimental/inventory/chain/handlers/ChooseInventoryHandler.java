package net.minestom.server.experimental.inventory.chain.handlers;

import net.minestom.server.experimental.inventory.action.Action;
import net.minestom.server.experimental.inventory.chain.ClickHandler;
import net.minestom.server.experimental.inventory.chain.ClickHandlerChain;
import net.minestom.server.experimental.inventory.chain.ClickHandlerResponse;
import net.minestom.server.inventory.AbstractInventory;
import org.jetbrains.annotations.NotNull;

public final class ChooseInventoryHandler implements ClickHandler {

    @Override
    public void handle(Action action, ClickHandlerResponse response, @NotNull ClickHandlerChain chain) {
        final var player = action.getPlayer();
        final var packet = action.getPacket();
        final int windowId = packet.windowId();
        final boolean playerInventory = windowId == 0;
        final AbstractInventory inventory = playerInventory ? player.getInventory() : player.getOpenInventory();
        if (inventory == null) {
            chain.handle(action, response, chain);
            return;
        }
        response.setInventory(inventory);
        chain.handle(action, response, chain);
    }
}
