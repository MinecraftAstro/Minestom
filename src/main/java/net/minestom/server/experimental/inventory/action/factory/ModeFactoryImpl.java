package net.minestom.server.experimental.inventory.action.factory;

import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

final class ModeFactoryImpl implements ModeFactory {

    @Override
    public ButtonFactory create(ClientClickWindowPacket packet) {
        return switch (packet.clickType()) {
            case PICKUP:
            default:
                yield new PickupButtonFactory();
        };
    }
}
