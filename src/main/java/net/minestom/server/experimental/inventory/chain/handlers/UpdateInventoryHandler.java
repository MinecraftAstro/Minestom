package net.minestom.server.experimental.inventory.chain.handlers;

import net.minestom.server.experimental.inventory.action.Action;
import net.minestom.server.experimental.inventory.chain.ClickHandler;
import net.minestom.server.experimental.inventory.chain.ClickHandlerChain;
import net.minestom.server.experimental.inventory.chain.ClickHandlerResponse;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.common.PingPacket;
import net.minestom.server.network.packet.server.play.SetCursorItemPacket;
import org.jetbrains.annotations.NotNull;

public final class UpdateInventoryHandler implements ClickHandler {

    @Override
    public void handle(Action action, ClickHandlerResponse response, @NotNull ClickHandlerChain chain) {
        final var player = action.getPlayer();
        final var packet = action.getPacket();
        final int windowId = packet.windowId();
        final boolean playerInventory = windowId == 0;
        final AbstractInventory inventory = player.getInventory();
        if (response.isAntiGhostItem()) {
            player.getInventory().update(player);
            // Prevent ghost item when the click is cancelled
            if (!playerInventory) {
                inventory.update(player);
            }
        }


        // Prevent the player from picking a ghost item in cursor
        ItemStack cursorItem = player.getInventory().getCursorItem();
        player.sendPacket(new SetCursorItemPacket(cursorItem));

        // (Why is the ping packet necessary?)
        player.sendPacket(new PingPacket((1 << 30) | (windowId << 16)));
        chain.handle(action, response, chain);
    }
}
