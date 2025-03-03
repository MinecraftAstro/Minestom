package net.minestom.server.experimental.inventory.chain;

import net.minestom.server.inventory.AbstractInventory;

final class ClickHandlerResponseImpl implements ClickHandlerResponse {

    private AbstractInventory inventory;
    private boolean antiGhostItem;

    @Override
    public void setInventory(AbstractInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public AbstractInventory getInventory() {
        return this.inventory;
    }

    @Override
    public void setAntiGhostItem(boolean antiGhostItem) {
        this.antiGhostItem = antiGhostItem;
    }

    @Override
    public boolean isAntiGhostItem() {
        return this.antiGhostItem;
    }
}
