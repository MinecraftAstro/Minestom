package net.minestom.server.experimental.inventory.action.factory;

import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

public sealed interface ModeFactory permits ModeFactoryImpl {

    ButtonFactory create(ClientClickWindowPacket packet);

    static ModeFactory create() {
        return new ModeFactoryImpl();
    }

}
