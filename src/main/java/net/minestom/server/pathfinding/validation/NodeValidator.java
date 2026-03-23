package net.minestom.server.pathfinding.validation;

import net.minestom.server.pathfinding.context.MobContext;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.options.PathfinderOptions;
import org.jetbrains.annotations.NotNull;

public interface NodeValidator {

    boolean isValidStart(@NotNull Node startNode,
                         @NotNull MobContext mobContext);

    @NotNull
    ValidationStatus checkValidity(@NotNull Node oldNode,
                                   @NotNull Node newNode,
                                   @NotNull MobContext mobContext,
                                   @NotNull PathfinderOptions options);
}