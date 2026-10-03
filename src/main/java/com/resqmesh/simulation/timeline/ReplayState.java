package com.resqmesh.simulation.timeline;

/**
 * State machine states for the simulation replay engine.
 */
public enum ReplayState {
    /**
     * Session loaded, ready at initial frame.
     */
    IDLE,

    /**
     * Replay is actively progressing through timeline events.
     */
    PLAYING,

    /**
     * Replay is paused at the current event.
     */
    PAUSED,

    /**
     * Replay has traversed all events and reached the end of the timeline.
     */
    COMPLETED,

    /**
     * Replay has been stopped or reset to initial state.
     */
    STOPPED
}
