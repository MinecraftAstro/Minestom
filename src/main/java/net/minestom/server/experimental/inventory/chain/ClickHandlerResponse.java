package net.minestom.server.experimental.inventory.chain;

import net.minestom.server.inventory.AbstractInventory;

public sealed interface ClickHandlerResponse permits ClickHandlerResponseImpl {

    void setInventory(AbstractInventory inventory);

    AbstractInventory getInventory();

    void setAntiGhostItem(boolean antiGhostItem);

    boolean isAntiGhostItem();

    static ClickHandlerResponse create() {
        return new ClickHandlerResponseImpl();
    }

}
