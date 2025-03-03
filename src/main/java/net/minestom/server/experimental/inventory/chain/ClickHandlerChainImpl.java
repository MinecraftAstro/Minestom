package net.minestom.server.experimental.inventory.chain;

import java.util.ArrayList;
import java.util.List;

import net.minestom.server.experimental.inventory.action.Action;

final class ClickHandlerChainImpl implements ClickHandlerChain {

    private final List<ClickHandler> handlers = new ArrayList<>();
    private int index = 0;

    @Override
    public ClickHandlerChain addHandler(ClickHandler handler) {
        this.handlers.add(handler);
        return this;
    }

    @Override
    public void handle(Action action, ClickHandlerResponse response, ClickHandlerChain chain) {
        if (index < handlers.size()) {
            handlers.get(index++).handle(action, response, this);
        }
    }
}
