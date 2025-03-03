package net.minestom.server.experimental.inventory;

import net.minestom.server.experimental.inventory.action.Action;

import java.util.Queue;

public interface InventoryHolder {

    Queue<Action> getTransactions();

    void addTransaction(Action action);

}
