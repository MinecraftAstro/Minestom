package net.minestom.server.pathfinding.evaluator;

import net.minestom.server.coordinate.Point;
import net.minestom.server.pathfinding.context.MobContext;
import net.minestom.server.pathfinding.data.Node;
import net.minestom.server.pathfinding.evaluator.result.NodeEvaluationResult;
import net.minestom.server.pathfinding.options.PathfinderOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NodeEvaluator {

    @Nullable
    Node getValidStart(@NotNull MobContext mobContext,
                       @NotNull Point target);

    @NotNull
    NodeEvaluationResult isValidMove(@NotNull Node oldNode,
                                     @NotNull Node newNode,
                                     @NotNull MobContext mobContext,
                                     @NotNull PathfinderOptions options);
}