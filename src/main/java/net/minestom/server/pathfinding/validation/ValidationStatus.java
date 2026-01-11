package net.minestom.server.pathfinding.validation;

import net.minestom.server.pathfinding.data.Node;
import org.jetbrains.annotations.Nullable;

public final class ValidationStatus {

    private final boolean valid;
    private final Node updatedNode;

    public ValidationStatus(boolean valid) {
        this(valid, null);
    }

    public ValidationStatus(boolean valid,
                            @Nullable Node updatedNode) {
        this.valid = valid;
        this.updatedNode = updatedNode;
    }

    public boolean valid() {
        return valid;
    }

    @Nullable
    public Node updatedNode() {
        return updatedNode;
    }
}