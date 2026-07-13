package net.minestom.server.event.inventory;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.InventoryEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.ItemStack;

public class InventoryDragEndEvent implements InventoryEvent, PlayerInstanceEvent {

    private final Player player;
    private final ItemStack cursorItem;

    public InventoryDragEndEvent(Player player, ItemStack cursorItem) {
        this.player = player;
        this.cursorItem = cursorItem;
    }

    @Override
    public AbstractInventory getInventory() {
        return player.getInventory();
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public ItemStack getCursorItem() {
        return cursorItem;
    }
}