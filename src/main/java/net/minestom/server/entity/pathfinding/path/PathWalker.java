package net.minestom.server.entity.pathfinding.path;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PathWalker {

    @NotNull
    BoundingBox getBoundingBox();

    @Nullable
    Instance getInstance();

    @NotNull
    Pos getPosition();
}