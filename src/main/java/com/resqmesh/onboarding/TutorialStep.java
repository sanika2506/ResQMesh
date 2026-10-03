package com.resqmesh.onboarding;

/**
 * Enumeration of guided onboarding steps for the ResQMesh interactive tutorial.
 */
public enum TutorialStep {
    WELCOME(0, "Welcome to ResQMesh", "Overview of offline mesh disaster communication."),
    ADD_DEVICE(1, "Step 1: Register Devices", "Learn device roles and register student phones, security posts, or medical centers."),
    CONNECT_DEVICES(2, "Step 2: Connect Mesh Links", "Establish bidirectional wireless links to form a resilient routing topology."),
    DISPATCH_ALERT(3, "Step 3: Dispatch Emergency Alert", "Select sender, recipient, and priority to broadcast a message via shortest-path BFS."),
    REVIEW_OUTCOME(4, "Step 4: Review Route & Replay", "Inspect hop-by-hop transmission, battery consumption, and replay message propagation."),
    COMPLETED(5, "Tutorial Completed", "You are now ready to design custom disaster networks and simulate emergency communication."),
    SKIPPED(6, "Tutorial Dismissed", "Tutorial was dismissed. You can restart it at any time.");

    private final int stepNumber;
    private final String title;
    private final String summary;

    TutorialStep(int stepNumber, String title, String summary) {
        this.stepNumber = stepNumber;
        this.title = title;
        this.summary = summary;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }
}
