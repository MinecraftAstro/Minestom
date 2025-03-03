package net.minestom.server.experimental.inventory.action.factory;

import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

public sealed interface ButtonFactory permits PickupButtonFactory {

    SlotFactory create(ClientClickWindowPacket packet);

}
