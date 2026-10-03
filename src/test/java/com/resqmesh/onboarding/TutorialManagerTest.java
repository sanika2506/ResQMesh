package com.resqmesh.onboarding;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.DeviceStatus;
import com.resqmesh.model.Location;
import com.resqmesh.model.MedicalStation;
import com.resqmesh.model.StudentPhone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Step 12: Tutorial Manager & Interactive Onboarding Tests")
class TutorialManagerTest {

    private TutorialManager manager;

    @BeforeEach
    void setUp() {
        manager = new TutorialManager();
    }

    @Test
    @DisplayName("Initial state should be WELCOME and active")
    void testInitialState() {
        assertEquals(TutorialStep.WELCOME, manager.getCurrentStep());
        assertTrue(manager.isActive());
        assertFalse(manager.isCompleted());
        assertFalse(manager.isSkipped());
        assertNotNull(manager.getStepInstruction());
        assertNotNull(manager.getActionHint());
    }

    @Test
    @DisplayName("Step progression proceeds sequentially to completion")
    void testStepProgressionFullCycle() {
        manager.startTutorial();
        assertEquals(TutorialStep.WELCOME, manager.getCurrentStep());

        manager.nextStep();
        assertEquals(TutorialStep.ADD_DEVICE, manager.getCurrentStep());

        manager.nextStep();
        assertEquals(TutorialStep.CONNECT_DEVICES, manager.getCurrentStep());

        manager.nextStep();
        assertEquals(TutorialStep.DISPATCH_ALERT, manager.getCurrentStep());

        manager.nextStep();
        assertEquals(TutorialStep.REVIEW_OUTCOME, manager.getCurrentStep());

        manager.nextStep();
        assertEquals(TutorialStep.COMPLETED, manager.getCurrentStep());
        assertTrue(manager.isCompleted());
        assertFalse(manager.isActive());

        // Calling nextStep when completed should remain COMPLETED
        manager.nextStep();
        assertEquals(TutorialStep.COMPLETED, manager.getCurrentStep());
    }

    @Test
    @DisplayName("Previous step moves backwards correctly")
    void testPreviousStepNavigation() {
        manager.setCurrentStep(TutorialStep.REVIEW_OUTCOME);
        manager.previousStep();
        assertEquals(TutorialStep.DISPATCH_ALERT, manager.getCurrentStep());

        manager.previousStep();
        assertEquals(TutorialStep.CONNECT_DEVICES, manager.getCurrentStep());

        manager.previousStep();
        assertEquals(TutorialStep.ADD_DEVICE, manager.getCurrentStep());

        manager.previousStep();
        assertEquals(TutorialStep.WELCOME, manager.getCurrentStep());

        // Calling previousStep at WELCOME stays at WELCOME
        manager.previousStep();
        assertEquals(TutorialStep.WELCOME, manager.getCurrentStep());
    }

    @Test
    @DisplayName("Skipping or exiting tutorial marks it as skipped and inactive")
    void testSkipAndExitTutorial() {
        manager.setCurrentStep(TutorialStep.ADD_DEVICE);
        manager.skipTutorial();

        assertEquals(TutorialStep.SKIPPED, manager.getCurrentStep());
        assertTrue(manager.isSkipped());
        assertFalse(manager.isActive());

        // Reset and test exit
        manager.startTutorial();
        assertTrue(manager.isActive());
        manager.exitTutorial();
        assertTrue(manager.isSkipped());
    }

    @Test
    @DisplayName("Restarting tutorial restores WELCOME state and active flag")
    void testRestartTutorial() {
        manager.setCurrentStep(TutorialStep.COMPLETED);
        manager.restartTutorial();

        assertEquals(TutorialStep.WELCOME, manager.getCurrentStep());
        assertTrue(manager.isActive());
        assertFalse(manager.isCompleted());
    }

    @Test
    @DisplayName("Step satisfaction predicates validate prerequisites accurately")
    void testStepSatisfactionPredicates() {
        manager.setCurrentStep(TutorialStep.WELCOME);
        assertTrue(manager.isStepSatisfied(0, 0, 0, 0));

        manager.setCurrentStep(TutorialStep.ADD_DEVICE);
        assertFalse(manager.isStepSatisfied(1, 0, 0, 1));
        assertTrue(manager.isStepSatisfied(2, 0, 0, 2));
        assertTrue(manager.isStepSatisfied(5, 2, 0, 5));

        manager.setCurrentStep(TutorialStep.CONNECT_DEVICES);
        assertFalse(manager.isStepSatisfied(2, 0, 0, 2));
        assertTrue(manager.isStepSatisfied(2, 1, 0, 2));

        manager.setCurrentStep(TutorialStep.DISPATCH_ALERT);
        assertFalse(manager.isStepSatisfied(2, 1, 0, 2));
        assertTrue(manager.isStepSatisfied(2, 1, 1, 2));

        manager.setCurrentStep(TutorialStep.REVIEW_OUTCOME);
        assertTrue(manager.isStepSatisfied(2, 1, 1, 2));

        manager.setCurrentStep(TutorialStep.COMPLETED);
        assertTrue(manager.isStepSatisfied(0, 0, 0, 0));
    }

    @Test
    @DisplayName("Safe sample network loading requires confirmation only if network has existing data")
    void testSafeSampleNetworkOverwriteDetection() {
        // Empty network -> safe to load without confirmation prompt
        assertFalse(manager.requiresConfirmationToLoadSample(0, 0));

        // Network with devices -> requires confirmation to prevent accidental loss
        assertTrue(manager.requiresConfirmationToLoadSample(1, 0));
        assertTrue(manager.requiresConfirmationToLoadSample(5, 3));
        assertTrue(manager.requiresConfirmationToLoadSample(0, 2));
    }

    @Test
    @DisplayName("Dispatch validation returns friendly guidance for common configuration errors")
    void testDispatchValidationGuidance() {
        CommunicationDevice alice = new StudentPhone("DEV-1", "Alice's Phone", new Location(0, 0), 100.0);
        CommunicationDevice bob = new StudentPhone("DEV-2", "Bob's Phone", new Location(10, 10), 100.0);
        CommunicationDevice medical = new MedicalStation("MED-1", "Clinic", new Location(20, 20), 100.0);

        // 1. Under 2 devices registered
        String msgUnderTwo = manager.getDispatchValidationMessage(alice, bob, 1, 0);
        assertNotNull(msgUnderTwo);
        assertTrue(msgUnderTwo.contains("Setup Incomplete"));

        // 2. Missing sender and recipient
        String msgBothNull = manager.getDispatchValidationMessage(null, null, 3, 2);
        assertNotNull(msgBothNull);
        assertTrue(msgBothNull.contains("Incomplete Selection"));

        // 3. Missing sender
        String msgNullSender = manager.getDispatchValidationMessage(null, bob, 3, 2);
        assertNotNull(msgNullSender);
        assertTrue(msgNullSender.contains("Missing Sender"));

        // 4. Missing recipient
        String msgNullRecipient = manager.getDispatchValidationMessage(alice, null, 3, 2);
        assertNotNull(msgNullRecipient);
        assertTrue(msgNullRecipient.contains("Missing Recipient"));

        // 5. Same sender and recipient
        String msgSame = manager.getDispatchValidationMessage(alice, alice, 3, 2);
        assertNotNull(msgSame);
        assertTrue(msgSame.contains("Origin Sender and Target Recipient cannot be the same"));

        // 6. Sender offline
        alice.setStatus(DeviceStatus.OFFLINE);
        String msgSenderOffline = manager.getDispatchValidationMessage(alice, bob, 3, 2);
        assertNotNull(msgSenderOffline);
        assertTrue(msgSenderOffline.contains("Sender Offline"));
        alice.setStatus(DeviceStatus.ACTIVE);

        // 7. Sender battery 0%
        alice.consumeBattery(100.0);
        String msgSenderDepleted = manager.getDispatchValidationMessage(alice, bob, 3, 2);
        assertNotNull(msgSenderDepleted);
        assertTrue(msgSenderDepleted.contains("Sender Depleted") || msgSenderDepleted.contains("Sender Offline"));
        alice.recharge(100.0);

        // 8. Recipient offline
        medical.setStatus(DeviceStatus.OFFLINE);
        String msgRecipientOffline = manager.getDispatchValidationMessage(alice, medical, 3, 2);
        assertNotNull(msgRecipientOffline);
        assertTrue(msgRecipientOffline.contains("Recipient Offline"));
        medical.setStatus(DeviceStatus.ACTIVE);

        // 9. Mesh disconnected (0 links)
        String msgZeroLinks = manager.getDispatchValidationMessage(alice, bob, 2, 0);
        assertNotNull(msgZeroLinks);
        assertTrue(msgZeroLinks.contains("Mesh Disconnected"));

        // 10. Valid scenario returns null
        assertNull(manager.getDispatchValidationMessage(alice, bob, 2, 1));
    }
}
// End of TutorialManagerTest
