package net.minestom.server.experimental.inventory.chain;

import net.minestom.server.experimental.inventory.action.Action;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ClickHandler {
    void handle(@NotNull Action action, @NotNull ClickHandlerResponse response, @NotNull ClickHandlerChain chain);
}
