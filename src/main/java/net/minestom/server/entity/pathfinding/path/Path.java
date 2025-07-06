package net.minestom.server.entity.pathfinding.path;

import net.minestom.server.entity.pathfinding.nodes.Node;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Path {

    private final List<Node> nodes;

    public Path() {
        this.nodes = new ArrayList<>();
    }

    @NotNull
    public List<Node> getNodes() {
        return nodes;
    }

    public enum State {

    }
}