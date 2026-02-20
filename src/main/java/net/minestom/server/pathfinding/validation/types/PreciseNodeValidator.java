package net.minestom.server.pathfinding.validation.types;

import net.minestom.server.pathfinding.context.MobContext;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.validation.NodeValidator;
import net.minestom.server.pathfinding.validation.ValidationStatus;
import org.jetbrains.annotations.NotNull;

public final class PreciseNodeValidator implements NodeValidator {

    @Override
    public @NotNull ValidationStatus checkValidity(@NotNull Node oldNode, @NotNull Node newNode, @NotNull MobContext mobContext) {
        return null;
    }
}