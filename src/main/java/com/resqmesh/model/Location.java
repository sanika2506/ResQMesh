package com.resqmesh.model;

import java.util.Objects;

/**
 * Represents a 2D spatial coordinate (x, y) of a device within the simulation grid.
 * Used to calculate transmission range and proximity between mesh devices.
 */
public class Location {

    private double x;
    private double y;

    /**
     * Constructs a Location with specific x and y coordinates.
     *
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     */
    public Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Calculates the straight-line Euclidean distance between this location and another location.
     * Formula: sqrt((x2 - x1)^2 + (y2 - y1)^2)
     *
     * @param other the target Location to measure distance to
     * @return the Euclidean distance as a double
     * @throws IllegalArgumentException if the other location is null
     */
    public double distanceTo(Location other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot calculate distance to a null location.");
        }
        double deltaX = this.x - other.x;
        double deltaY = this.y - other.y;
        return Math.hypot(deltaX, deltaY);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location location)) return false;
        return Double.compare(location.x, x) == 0 && Double.compare(location.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return String.format("Location(x=%.2f, y=%.2f)", x, y);
    }
}
