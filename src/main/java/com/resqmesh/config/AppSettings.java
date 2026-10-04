package com.resqmesh.config;

import com.resqmesh.model.Priority;

import java.util.Objects;

/**
 * Encapsulates persistent application and simulation engine settings for ResQMesh.
 * 
 * Supports:
 * - Simulation battery consumption per hop
 * - Default emergency message priority preset
 * - Visual topology animation replay speed
 * - Safety confirmation dialog toggles (network replacement, clear history, clear network)
 * - User interface preference toggles (tutorial banner, auto-recipient selection)
 */
public class AppSettings {

    public static final double DEFAULT_BATTERY_DRAIN_PER_HOP = 2.0;
    public static final Priority DEFAULT_PRIORITY = Priority.NORMAL;
    public static final double DEFAULT_REPLAY_SPEED = 1.0;
    public static final boolean DEFAULT_CONFIRM_NETWORK_REPLACEMENT = true;
    public static final boolean DEFAULT_CONFIRM_CLEAR_HISTORY = true;
    public static final boolean DEFAULT_CONFIRM_CLEAR_NETWORK = true;
    public static final boolean DEFAULT_SHOW_TUTORIAL_BANNER = true;
    public static final boolean DEFAULT_AUTO_SELECT_RECIPIENT = true;

    public static final double MIN_BATTERY_DRAIN = 0.0;
    public static final double MAX_BATTERY_DRAIN = 10.0;
    public static final double MIN_REPLAY_SPEED = 0.25;
    public static final double MAX_REPLAY_SPEED = 4.0;

    private double batteryDrainPerHop = DEFAULT_BATTERY_DRAIN_PER_HOP;
    private Priority defaultPriority = DEFAULT_PRIORITY;
    private double replaySpeed = DEFAULT_REPLAY_SPEED;
    private boolean confirmNetworkReplacement = DEFAULT_CONFIRM_NETWORK_REPLACEMENT;
    private boolean confirmClearHistory = DEFAULT_CONFIRM_CLEAR_HISTORY;
    private boolean confirmClearNetwork = DEFAULT_CONFIRM_CLEAR_NETWORK;
    private boolean showTutorialBanner = DEFAULT_SHOW_TUTORIAL_BANNER;
    private boolean autoSelectRecipient = DEFAULT_AUTO_SELECT_RECIPIENT;

    public AppSettings() {
        // Defaults initialized above
    }

    public AppSettings(AppSettings other) {
        if (other != null) {
            this.batteryDrainPerHop = other.batteryDrainPerHop;
            this.defaultPriority = (other.defaultPriority != null) ? other.defaultPriority : DEFAULT_PRIORITY;
            this.replaySpeed = other.replaySpeed;
            this.confirmNetworkReplacement = other.confirmNetworkReplacement;
            this.confirmClearHistory = other.confirmClearHistory;
            this.confirmClearNetwork = other.confirmClearNetwork;
            this.showTutorialBanner = other.showTutorialBanner;
            this.autoSelectRecipient = other.autoSelectRecipient;
        }
    }

    /**
     * Resets all parameters to factory defaults.
     */
    public void resetToDefaults() {
        this.batteryDrainPerHop = DEFAULT_BATTERY_DRAIN_PER_HOP;
        this.defaultPriority = DEFAULT_PRIORITY;
        this.replaySpeed = DEFAULT_REPLAY_SPEED;
        this.confirmNetworkReplacement = DEFAULT_CONFIRM_NETWORK_REPLACEMENT;
        this.confirmClearHistory = DEFAULT_CONFIRM_CLEAR_HISTORY;
        this.confirmClearNetwork = DEFAULT_CONFIRM_CLEAR_NETWORK;
        this.showTutorialBanner = DEFAULT_SHOW_TUTORIAL_BANNER;
        this.autoSelectRecipient = DEFAULT_AUTO_SELECT_RECIPIENT;
    }

    public double getBatteryDrainPerHop() {
        return batteryDrainPerHop;
    }

    public void setBatteryDrainPerHop(double batteryDrainPerHop) {
        if (batteryDrainPerHop < MIN_BATTERY_DRAIN) {
            this.batteryDrainPerHop = MIN_BATTERY_DRAIN;
        } else if (batteryDrainPerHop > MAX_BATTERY_DRAIN) {
            this.batteryDrainPerHop = MAX_BATTERY_DRAIN;
        } else {
            this.batteryDrainPerHop = batteryDrainPerHop;
        }
    }

    public Priority getDefaultPriority() {
        return defaultPriority != null ? defaultPriority : DEFAULT_PRIORITY;
    }

    public void setDefaultPriority(Priority defaultPriority) {
        this.defaultPriority = (defaultPriority != null) ? defaultPriority : DEFAULT_PRIORITY;
    }

    public double getReplaySpeed() {
        return replaySpeed;
    }

    public void setReplaySpeed(double replaySpeed) {
        if (replaySpeed < MIN_REPLAY_SPEED) {
            this.replaySpeed = MIN_REPLAY_SPEED;
        } else if (replaySpeed > MAX_REPLAY_SPEED) {
            this.replaySpeed = MAX_REPLAY_SPEED;
        } else {
            this.replaySpeed = replaySpeed;
        }
    }

    public boolean isConfirmNetworkReplacement() {
        return confirmNetworkReplacement;
    }

    public void setConfirmNetworkReplacement(boolean confirmNetworkReplacement) {
        this.confirmNetworkReplacement = confirmNetworkReplacement;
    }

    public boolean isConfirmClearHistory() {
        return confirmClearHistory;
    }

    public void setConfirmClearHistory(boolean confirmClearHistory) {
        this.confirmClearHistory = confirmClearHistory;
    }

    public boolean isConfirmClearNetwork() {
        return confirmClearNetwork;
    }

    public void setConfirmClearNetwork(boolean confirmClearNetwork) {
        this.confirmClearNetwork = confirmClearNetwork;
    }

    public boolean isShowTutorialBanner() {
        return showTutorialBanner;
    }

    public void setShowTutorialBanner(boolean showTutorialBanner) {
        this.showTutorialBanner = showTutorialBanner;
    }

    public boolean isAutoSelectRecipient() {
        return autoSelectRecipient;
    }

    public void setAutoSelectRecipient(boolean autoSelectRecipient) {
        this.autoSelectRecipient = autoSelectRecipient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppSettings that = (AppSettings) o;
        return Double.compare(that.batteryDrainPerHop, batteryDrainPerHop) == 0 &&
                Double.compare(that.replaySpeed, replaySpeed) == 0 &&
                confirmNetworkReplacement == that.confirmNetworkReplacement &&
                confirmClearHistory == that.confirmClearHistory &&
                confirmClearNetwork == that.confirmClearNetwork &&
                showTutorialBanner == that.showTutorialBanner &&
                autoSelectRecipient == that.autoSelectRecipient &&
                defaultPriority == that.defaultPriority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(batteryDrainPerHop, defaultPriority, replaySpeed,
                confirmNetworkReplacement, confirmClearHistory, confirmClearNetwork,
                showTutorialBanner, autoSelectRecipient);
    }

    @Override
    public String toString() {
        return "AppSettings{" +
                "batteryDrainPerHop=" + batteryDrainPerHop +
                ", defaultPriority=" + defaultPriority +
                ", replaySpeed=" + replaySpeed +
                ", confirmNetworkReplacement=" + confirmNetworkReplacement +
                ", confirmClearHistory=" + confirmClearHistory +
                ", confirmClearNetwork=" + confirmClearNetwork +
                ", showTutorialBanner=" + showTutorialBanner +
                ", autoSelectRecipient=" + autoSelectRecipient +
                '}';
    }
}
