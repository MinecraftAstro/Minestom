package net.minestom.server.pathfinding.validation;

import net.minestom.server.pathfinding.context.MobContext;
import net.minestom.server.pathfinding.data.Node;
import org.jetbrains.annotations.NotNull;

public interface NodeValidator {

    @NotNull
    ValidationStatus checkValidity(@NotNull Node oldNode,
                                   @NotNull Node newNode,
                                   @NotNull MobContext mobContext);
}