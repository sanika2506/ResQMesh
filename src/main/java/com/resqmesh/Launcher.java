package com.resqmesh;

/**
 * Entry point wrapper for launching JavaFX without requiring VM module options
 * when executed directly from some IDEs or standard jar packaging.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
