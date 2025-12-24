package net.minestom.server.pathfinding.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Path {

    private final List<Node> nodes;

    public Path(@NotNull List<Node> nodes) {
        this.nodes = nodes;
    }
}