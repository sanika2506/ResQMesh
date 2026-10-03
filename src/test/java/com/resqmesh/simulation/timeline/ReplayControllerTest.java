package com.resqmesh.simulation.timeline;

import com.resqmesh.model.*;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;
import com.resqmesh.simulation.SimulationEngine;
import com.resqmesh.simulation.SimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests for ReplayController state machine (Step 11).
 * Verifies:
 * - Replay state transitions (IDLE -> PLAYING -> PAUSED -> PLAYING -> COMPLETED / STOPPED)
 * - Step-by-step event triggers and listener callbacks
 * - Replay speed factor configuration and input validation
 * - Reset behavior restoring index to 0 and state to STOPPED
 * - Failed delivery replay behavior (no false hops or route advancement)
 */
public class ReplayControllerTest {

    private NetworkGraph graph;
    private SimulationEngine engine;
    private StudentPhone alice;
    private StudentPhone bob;
    private MedicalStation medical;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph, new ShortestPathStrategy());

        alice = new StudentPhone("DEV-1", "Alice's Phone", new Location(0, 0), 100.0);
        bob = new StudentPhone("DEV-2", "Bob's Phone", new Location(20, 20), 100.0);
        medical = new MedicalStation("MED-1", "Medical Center", new Location(40, 40), 100.0);

        graph.addDevice(alice);
        graph.addDevice(bob);
        graph.addDevice(medical);

        graph.connect(alice, bob);
        graph.connect(bob, medical);
    }

    private SimulationTimeline createDeliveredTimeline() {
        EmergencyMessage msg = new EmergencyMessage("MSG-1", alice, medical, "Distress alert", Priority.NORMAL);
        SimulationResult result = engine.send(msg);
        return SimulationTimeline.fromSimulation(msg, result);
    }

    private SimulationTimeline createFailedTimeline() {
        graph.disconnect(bob, medical);
        EmergencyMessage msg = new EmergencyMessage("MSG-FAIL", alice, medical, "Blocked alert", Priority.NORMAL);
        SimulationResult result = engine.send(msg);
        return SimulationTimeline.fromSimulation(msg, result);
    }

    @Test
    @DisplayName("ReplayController: Initial state after loading timeline is IDLE at index 0")
    void testInitialState() {
        SimulationTimeline timeline = createDeliveredTimeline();
        ReplayController controller = new ReplayController();
        controller.loadTimeline(timeline);

        assertEquals(ReplayState.IDLE, controller.getState());
        assertEquals(0, controller.getCurrentEventIndex());
        assertEquals(1.0, controller.getSpeedFactor());
        assertFalse(controller.isPlaying());
        assertFalse(controller.isPaused());
    }

    @Test
    @DisplayName("ReplayController: Play transition from IDLE activates PLAYING state")
    void testPlayStateTransition() {
        SimulationTimeline timeline = createDeliveredTimeline();
        ReplayController controller = new ReplayController(timeline);

        final List<ReplayState> stateHistory = new ArrayList<>();
        controller.addListener(new ReplayController.ReplayListener() {
            @Override
            public void onStateChanged(ReplayState oldState, ReplayState newState) {
                stateHistory.add(newState);
            }
        });

        controller.play();

        assertEquals(ReplayState.PLAYING, controller.getState());
        assertTrue(controller.isPlaying());
        assertEquals(1, stateHistory.size());
        assertEquals(ReplayState.PLAYING, stateHistory.get(0));
    }

    @Test
    @DisplayName("ReplayController: Pause and Resume transitions preserve event index")
    void testPauseAndResumeStateTransitions() {
        SimulationTimeline timeline = createDeliveredTimeline();
        ReplayController controller = new ReplayController(timeline);

        controller.play();
        controller.stepNext(); // Advance to index 1
        assertEquals(1, controller.getCurrentEventIndex());

        controller.pause();
        assertEquals(ReplayState.PAUSED, controller.getState());
        assertTrue(controller.isPaused());
        assertEquals(1, controller.getCurrentEventIndex(), "Pausing must not alter current event index");

        controller.resume();
        assertEquals(ReplayState.PLAYING, controller.getState());
        assertTrue(controller.isPlaying());
        assertEquals(1, controller.getCurrentEventIndex(), "Resuming must continue from current event index");
    }

    @Test
    @DisplayName("ReplayController: Stepping through entire timeline triggers completion")
    void testStepNextThroughCompletion() {
        SimulationTimeline timeline = createDeliveredTimeline();
        ReplayController controller = new ReplayController(timeline);

        int totalEvents = timeline.size();
        controller.play();

        boolean[] completedTriggered = new boolean[]{false};
        controller.addListener(new ReplayController.ReplayListener() {
            @Override
            public void onCompleted() {
                completedTriggered[0] = true;
            }
        });

        // Step through remaining events
        for (int i = 0; i < totalEvents - 1; i++) {
            controller.stepNext();
        }

        assertEquals(ReplayState.COMPLETED, controller.getState());
        assertTrue(controller.isCompleted());
        assertTrue(completedTriggered[0], "onCompleted callback should be fired upon finishing timeline");
    }

    @Test
    @DisplayName("ReplayController: Reset restores event index to 0 and state to STOPPED")
    void testResetRestoresInitialIndex() {
        SimulationTimeline timeline = createDeliveredTimeline();
        ReplayController controller = new ReplayController(timeline);

        controller.play();
        controller.stepNext();
        controller.stepNext();
        assertTrue(controller.getCurrentEventIndex() > 0);

        boolean[] resetCalled = new boolean[]{false};
        controller.addListener(new ReplayController.ReplayListener() {
            @Override
            public void onReset() {
                resetCalled[0] = true;
            }
        });

        controller.reset();

        assertEquals(ReplayState.STOPPED, controller.getState());
        assertEquals(0, controller.getCurrentEventIndex());
        assertTrue(resetCalled[0]);
    }

    @Test
    @DisplayName("ReplayController: Speed factor configuration and validation")
    void testSpeedFactorConfiguration() {
        ReplayController controller = new ReplayController();

        controller.setSpeed(0.5);
        assertEquals(0.5, controller.getSpeedFactor());

        controller.setSpeed(2.0);
        assertEquals(2.0, controller.getSpeedFactor());

        assertThrows(IllegalArgumentException.class, () -> controller.setSpeed(0.0),
                "Speed factor must be positive");
        assertThrows(IllegalArgumentException.class, () -> controller.setSpeed(-1.5),
                "Speed factor must be positive");
    }

    @Test
    @DisplayName("ReplayController: Failed delivery replay behavior has no intermediate hop progress")
    void testFailedDeliveryReplayControllerBehavior() {
        SimulationTimeline failedTimeline = createFailedTimeline();
        ReplayController controller = new ReplayController(failedTimeline);

        List<CommunicationDevice> hopsRecorded = new ArrayList<>();
        controller.addListener(new ReplayController.ReplayListener() {
            @Override
            public void onHopProgress(CommunicationDevice from, CommunicationDevice to, int hopIndex, int totalHops) {
                hopsRecorded.add(from);
                hopsRecorded.add(to);
            }
        });

        controller.play();
        // Failed timeline has 2 events: Created, Failed
        assertEquals(0, controller.getCurrentEventIndex());
        controller.stepNext();
        assertEquals(1, controller.getCurrentEventIndex());
        assertTrue(controller.isCompleted());

        assertTrue(hopsRecorded.isEmpty(), "Failed delivery replay must NEVER trigger onHopProgress callbacks");
    }

    @Test
    @DisplayName("ReplayController: Listener triggers for discrete timeline events")
    void testReplayListenerCallbacks() {
        SimulationTimeline timeline = createDeliveredTimeline();
        ReplayController controller = new ReplayController(timeline);

        List<SimulationEvent> triggeredEvents = new ArrayList<>();
        controller.addListener(new ReplayController.ReplayListener() {
            @Override
            public void onEventTriggered(SimulationEvent event, int eventIndex, int totalEvents) {
                triggeredEvents.add(event);
            }
        });

        controller.play();
        while (!controller.isCompleted()) {
            controller.stepNext();
        }

        assertEquals(timeline.size(), triggeredEvents.size(),
                "All timeline events must be triggered in order through the listener");
    }
}
