package net.minestom.server.experimental.inventory.action.factory;

import net.minestom.server.network.packet.client.play.ClientClickWindowPacket;

final class PickupButtonFactory implements ButtonFactory {
    @Override
    public SlotFactory create(ClientClickWindowPacket packet) {
        return switch (packet.button()) {
            case 0 -> new PickupLeftSlotFactory();
            case 1 -> new PickupRightSlotFactory();
            default -> throw new UnsupportedOperationException("Unsupported button: " + packet.button());
        };
    }
}
