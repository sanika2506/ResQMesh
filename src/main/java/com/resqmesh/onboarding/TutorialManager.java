package com.resqmesh.onboarding;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.DeviceStatus;

/**
 * Manages the state machine and instructional logic for user onboarding and tutorials.
 * Pure Java component decoupled from JavaFX UI for comprehensive headless testing.
 */
public class TutorialManager {

    private TutorialStep currentStep;

    public TutorialManager() {
        this.currentStep = TutorialStep.WELCOME;
    }

    /**
     * Starts the interactive tutorial from the beginning (Welcome step).
     */
    public void startTutorial() {
        this.currentStep = TutorialStep.WELCOME;
    }

    /**
     * Starts directly from Step 1 (Add Device).
     */
    public void startFromStep1() {
        this.currentStep = TutorialStep.ADD_DEVICE;
    }

    /**
     * Advances to the subsequent tutorial step.
     */
    public void nextStep() {
        switch (currentStep) {
            case WELCOME:
                currentStep = TutorialStep.ADD_DEVICE;
                break;
            case ADD_DEVICE:
                currentStep = TutorialStep.CONNECT_DEVICES;
                break;
            case CONNECT_DEVICES:
                currentStep = TutorialStep.DISPATCH_ALERT;
                break;
            case DISPATCH_ALERT:
                currentStep = TutorialStep.REVIEW_OUTCOME;
                break;
            case REVIEW_OUTCOME:
                currentStep = TutorialStep.COMPLETED;
                break;
            case COMPLETED:
            case SKIPPED:
            default:
                break;
        }
    }

    /**
     * Moves back to the preceding step, if applicable.
     */
    public void previousStep() {
        switch (currentStep) {
            case REVIEW_OUTCOME:
                currentStep = TutorialStep.DISPATCH_ALERT;
                break;
            case DISPATCH_ALERT:
                currentStep = TutorialStep.CONNECT_DEVICES;
                break;
            case CONNECT_DEVICES:
                currentStep = TutorialStep.ADD_DEVICE;
                break;
            case ADD_DEVICE:
                currentStep = TutorialStep.WELCOME;
                break;
            case WELCOME:
            case COMPLETED:
            case SKIPPED:
            default:
                break;
        }
    }

    /**
     * Dismisses or skips the onboarding tutorial.
     */
    public void skipTutorial() {
        this.currentStep = TutorialStep.SKIPPED;
    }

    /**
     * Exits the tutorial.
     */
    public void exitTutorial() {
        this.currentStep = TutorialStep.SKIPPED;
    }

    /**
     * Restarts the tutorial from the beginning.
     */
    public void restartTutorial() {
        this.currentStep = TutorialStep.WELCOME;
    }

    public TutorialStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(TutorialStep step) {
        if (step != null) {
            this.currentStep = step;
        }
    }

    /**
     * Checks if the tutorial is currently active (not completed and not skipped).
     */
    public boolean isActive() {
        return currentStep != TutorialStep.COMPLETED && currentStep != TutorialStep.SKIPPED;
    }

    public boolean isCompleted() {
        return currentStep == TutorialStep.COMPLETED;
    }

    public boolean isSkipped() {
        return currentStep == TutorialStep.SKIPPED;
    }

    /**
     * Returns detailed instructions for the current tutorial step.
     */
    public String getStepInstruction() {
        switch (currentStep) {
            case WELCOME:
                return "Welcome to ResQMesh! When cellular networks fail during disasters, ResQMesh routes emergency messages peer-to-peer using shortest-path BFS. Let's build your first mesh in 3 quick steps.";
            case ADD_DEVICE:
                return "Step 1: Register at least 2 devices. Phones are portable handhelds; Security and Medical Stations act as high-capacity backbone relay posts.";
            case CONNECT_DEVICES:
                return "Step 2: Establish wireless mesh links between devices so emergency messages have physical paths to propagate across.";
            case DISPATCH_ALERT:
                return "Step 3: Select an Origin Sender and a Target Recipient, choose a priority level (Normal, High, or Critical), and click 'Dispatch Emergency Alert'.";
            case REVIEW_OUTCOME:
                return "Step 4: Check the hop-by-hop transmission route! Notice the -2.0% battery deduction per hop, and click 'Watch Replay' to see the alert travel across the topology.";
            case COMPLETED:
                return "Congratulations! You have mastered the ResQMesh workflow. You can now build complex topologies, test offline nodes, and save/load network configurations.";
            case SKIPPED:
                return "Interactive tutorial is paused. Click 'Help & Guide' or 'Start Tutorial' anytime to resume.";
            default:
                return "";
        }
    }

    /**
     * Returns a short actionable hint for the user interface.
     */
    public String getActionHint() {
        switch (currentStep) {
            case WELCOME:
                return "Click 'Next' or 'Start Tutorial' to begin provisioning devices.";
            case ADD_DEVICE:
                return "Navigate to 'Network & Devices', enter a device name, pick a role, and click '➕ Register Node'.";
            case CONNECT_DEVICES:
                return "In 'Network & Devices', select two distinct devices and click '🔗 Connect Wireless Link'.";
            case DISPATCH_ALERT:
                return "In 'Emergency Dispatch', pick a Sender and Target, then click '🚀 DISPATCH EMERGENCY ALERT'.";
            case REVIEW_OUTCOME:
                return "Review the delivery outcome badge, hop trail, and click '🎬 Watch Replay on Canvas'.";
            case COMPLETED:
                return "Explore custom topologies, simulate power outages, or load saved networks from JSON!";
            case SKIPPED:
                return "You can restart the tutorial at any time from the sidebar or Help tab.";
            default:
                return "";
        }
    }

    /**
     * Verifies if the requirements for the given step are satisfied.
     */
    public boolean isStepSatisfied(int deviceCount, int linkCount, int totalDispatches, int activeDevices) {
        switch (currentStep) {
            case WELCOME:
                return true;
            case ADD_DEVICE:
                return deviceCount >= 2;
            case CONNECT_DEVICES:
                return linkCount >= 1;
            case DISPATCH_ALERT:
                return totalDispatches >= 1;
            case REVIEW_OUTCOME:
                return totalDispatches >= 1;
            case COMPLETED:
            case SKIPPED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if loading a sample network would overwrite existing user network data.
     *
     * @param currentDeviceCount number of devices currently in the network graph
     * @param currentLinkCount   number of active links currently in the network graph
     * @return true if user confirmation is advised to prevent accidental loss
     */
    public boolean requiresConfirmationToLoadSample(int currentDeviceCount, int currentLinkCount) {
        return currentDeviceCount > 0 || currentLinkCount > 0;
    }

    /**
     * Validates dispatch inputs and produces a friendly, actionable guidance message if invalid.
     *
     * @param sender       selected sender device
     * @param recipient    selected recipient device
     * @param totalDevices total devices registered in the network
     * @param totalLinks   total active links in the network
     * @return null if valid, or a descriptive guidance message if invalid
     */
    public String getDispatchValidationMessage(CommunicationDevice sender, CommunicationDevice recipient, int totalDevices, int totalLinks) {
        if (totalDevices < 2) {
            return String.format("Setup Incomplete: The network currently has %d device(s). At least 2 devices are required to simulate message routing. Go to 'Network & Devices' to register nodes.", totalDevices);
        }
        if (sender == null && recipient == null) {
            return "Incomplete Selection: Please select both an Origin Sender node and a Target Recipient node from the dropdowns.";
        }
        if (sender == null) {
            return "Missing Sender: Please select an Origin Sender node to transmit the emergency message.";
        }
        if (recipient == null) {
            return "Missing Recipient: Please select a Target Recipient node to receive the emergency message.";
        }
        if (sender.equals(recipient)) {
            return "Invalid Route: Origin Sender and Target Recipient cannot be the same device. Please select two distinct nodes.";
        }
        if (sender.getStatus() == DeviceStatus.OFFLINE) {
            return String.format("Sender Offline: Node '%s' is currently OFFLINE and cannot transmit messages. Bring it online in 'Network & Devices' or select an operational node.", sender.getName());
        }
        if (sender.getBatteryLevel() <= 0.0) {
            return String.format("Sender Depleted: Node '%s' has 0%% battery. Recharge the node to 100%% before transmitting.", sender.getName());
        }
        if (recipient.getStatus() == DeviceStatus.OFFLINE) {
            return String.format("Recipient Offline: Target '%s' is currently OFFLINE. While messages may route toward it, delivery cannot be confirmed until it is online.", recipient.getName());
        }
        if (recipient.getBatteryLevel() <= 0.0) {
            return String.format("Recipient Depleted: Target '%s' has 0%% battery and cannot acknowledge receipt.", recipient.getName());
        }
        if (totalLinks <= 0) {
            return "Mesh Disconnected: Registered devices have 0 active wireless links between them. Connect devices in 'Network & Devices' to establish communication paths.";
        }
        return null;
    }
}
