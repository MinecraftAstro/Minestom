package net.minestom.server.experimental.inventory.chain;

import net.minestom.server.experimental.inventory.action.Action;
import net.minestom.server.experimental.inventory.chain.handlers.ChooseInventoryHandler;
import net.minestom.server.experimental.inventory.chain.handlers.SkipCreativeClickActionsHandler;
import net.minestom.server.experimental.inventory.chain.handlers.UpdateInventoryHandler;

public sealed interface ClickHandlerChain permits ClickHandlerChainImpl {

    ClickHandlerChain addHandler(ClickHandler handler);

    void handle(Action action, ClickHandlerResponse response, ClickHandlerChain chain);

    static ClickHandlerChain createWithDefaults() {
        return new ClickHandlerChainImpl()
                .addHandler(new ChooseInventoryHandler())
                .addHandler(new SkipCreativeClickActionsHandler())
                .addHandler(new UpdateInventoryHandler());
    }
}
