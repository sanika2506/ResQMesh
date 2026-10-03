package com.resqmesh.simulation.timeline;

import com.resqmesh.model.CommunicationDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless, deterministic controller and state machine for simulation replay.
 * Decoupled from JavaFX rendering so that state transitions, event sequences,
 * and speeds can be rigorously verified in automated tests without requiring a display toolkit.
 */
public class ReplayController {

    public interface ReplayListener {
        default void onStateChanged(ReplayState oldState, ReplayState newState) {}
        default void onEventTriggered(SimulationEvent event, int eventIndex, int totalEvents) {}
        default void onHopProgress(CommunicationDevice from, CommunicationDevice to, int hopIndex, int totalHops) {}
        default void onCompleted() {}
        default void onReset() {}
    }

    private SimulationTimeline timeline;
    private ReplayState state = ReplayState.STOPPED;
    private int currentEventIndex = 0;
    private double speedFactor = 1.0;
    private final List<ReplayListener> listeners = new ArrayList<>();

    public ReplayController() {
    }

    public ReplayController(SimulationTimeline timeline) {
        loadTimeline(timeline);
    }

    public void addListener(ReplayListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ReplayListener listener) {
        listeners.remove(listener);
    }

    /**
     * Loads a new timeline session, resetting state to IDLE.
     */
    public void loadTimeline(SimulationTimeline timeline) {
        this.timeline = timeline;
        this.currentEventIndex = 0;
        changeState(ReplayState.IDLE);
    }

    /**
     * Starts replay from the beginning or resumes if paused.
     */
    public void play() {
        if (timeline == null || timeline.isEmpty()) {
            return;
        }

        if (state == ReplayState.PAUSED) {
            resume();
            return;
        }

        currentEventIndex = 0;
        changeState(ReplayState.PLAYING);
        triggerCurrentEvent();
    }

    /**
     * Pauses active replay at the current event.
     */
    public void pause() {
        if (state == ReplayState.PLAYING) {
            changeState(ReplayState.PAUSED);
        }
    }

    /**
     * Resumes paused replay.
     */
    public void resume() {
        if (state == ReplayState.PAUSED) {
            changeState(ReplayState.PLAYING);
        }
    }

    /**
     * Resets replay to the first event and halts playback.
     */
    public void reset() {
        currentEventIndex = 0;
        changeState(ReplayState.STOPPED);
        for (ReplayListener listener : listeners) {
            listener.onReset();
        }
    }

    /**
     * Advances to the next event in the timeline.
     *
     * @return true if an event was triggered, false if replay has reached completion.
     */
    public boolean stepNext() {
        if (timeline == null || currentEventIndex >= timeline.size() - 1) {
            changeState(ReplayState.COMPLETED);
            for (ReplayListener listener : listeners) {
                listener.onCompleted();
            }
            return false;
        }

        currentEventIndex++;
        triggerCurrentEvent();

        if (currentEventIndex == timeline.size() - 1) {
            changeState(ReplayState.COMPLETED);
            for (ReplayListener listener : listeners) {
                listener.onCompleted();
            }
        }
        return true;
    }

    private void triggerCurrentEvent() {
        if (timeline != null && currentEventIndex >= 0 && currentEventIndex < timeline.size()) {
            SimulationEvent event = timeline.getEvent(currentEventIndex);
            for (ReplayListener listener : listeners) {
                listener.onEventTriggered(event, currentEventIndex, timeline.size());
                if (event.getType() == SimulationEventType.HOP_FORWARDING) {
                    listener.onHopProgress(event.getSourceDevice(), event.getTargetDevice(),
                            event.getHopIndex(), event.getTotalHops());
                }
            }
        }
    }

    public void setSpeed(double speed) {
        if (speed <= 0) {
            throw new IllegalArgumentException("Speed factor must be positive.");
        }
        this.speedFactor = speed;
    }

    private void changeState(ReplayState newState) {
        if (this.state != newState) {
            ReplayState oldState = this.state;
            this.state = newState;
            for (ReplayListener listener : listeners) {
                listener.onStateChanged(oldState, newState);
            }
        }
    }

    public ReplayState getState() {
        return state;
    }

    public SimulationTimeline getTimeline() {
        return timeline;
    }

    public int getCurrentEventIndex() {
        return currentEventIndex;
    }

    public SimulationEvent getCurrentEvent() {
        if (timeline != null && currentEventIndex >= 0 && currentEventIndex < timeline.size()) {
            return timeline.getEvent(currentEventIndex);
        }
        return null;
    }

    public double getSpeedFactor() {
        return speedFactor;
    }

    public boolean isPlaying() {
        return state == ReplayState.PLAYING;
    }

    public boolean isPaused() {
        return state == ReplayState.PAUSED;
    }

    public boolean isCompleted() {
        return state == ReplayState.COMPLETED;
    }
}
