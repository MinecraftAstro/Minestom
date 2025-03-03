package net.minestom.server.experimental.inventory.chain.handlers;

import net.minestom.server.entity.GameMode;
import net.minestom.server.experimental.inventory.action.Action;
import net.minestom.server.experimental.inventory.chain.ClickHandler;
import net.minestom.server.experimental.inventory.chain.ClickHandlerChain;
import net.minestom.server.experimental.inventory.chain.ClickHandlerResponse;
import org.jetbrains.annotations.NotNull;

public final class SkipCreativeClickActionsHandler implements ClickHandler {

    @Override
    public void handle(Action action, ClickHandlerResponse response, @NotNull ClickHandlerChain chain) {
        final var player = action.getPlayer();
        final var packet = action.getPacket();
        final byte button = packet.button();
        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;

        final boolean requireCreative = switch (packet.clickType()) {
            case CLONE -> packet.slot() != -999; // Permit middle click dropping
            case QUICK_CRAFT -> button == 8 || button == 9 || button == 10;
            default -> false;
        };

        if (requireCreative && !isCreative) return;
        chain.handle(action, response, chain);
    }
}
