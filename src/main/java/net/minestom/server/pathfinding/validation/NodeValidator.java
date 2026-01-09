package net.minestom.server.pathfinding.validation;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.instance.Instance;
import net.minestom.server.pathfinding.data.Node;
import org.jetbrains.annotations.NotNull;

public interface NodeValidator {

    boolean isValid(@NotNull Node oldNode,
                    @NotNull Node newNode,
                    @NotNull Instance instance,
                    @NotNull BoundingBox boundingBox);
}