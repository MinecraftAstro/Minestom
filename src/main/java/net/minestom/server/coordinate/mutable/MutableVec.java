package net.minestom.server.coordinate.mutable;

public final class MutableVec {

    private double x;
    private double y;
    private double z;

    public MutableVec() {
        this(0.0D);
    }

    public MutableVec(double value) {
        this(value, value, value);
    }

    public MutableVec(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double x() {
        return x;
    }

    public int blockX() {
        return (int) Math.floor(x);
    }

    public void setX(double x) {
        this.x = x;
    }

    public double y() {
        return y;
    }

    public int blockY() {
        return (int) Math.floor(y);
    }

    public void setY(double y) {
        this.y = y;
    }

    public double z() {
        return z;
    }

    public int blockZ() {
        return (int) Math.floor(z);
    }

    public void setZ(double z) {
        this.z = z;
    }
}