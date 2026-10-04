package com.resqmesh.config;

import com.resqmesh.model.Priority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Step 13: AppSettings Model & Validation Tests")
public class AppSettingsTest {

    @Test
    @DisplayName("Default constructor should initialize standard recommended values")
    void testDefaultValues() {
        AppSettings settings = new AppSettings();

        assertEquals(2.0, settings.getBatteryDrainPerHop(), 0.001);
        assertEquals(Priority.NORMAL, settings.getDefaultPriority());
        assertEquals(1.0, settings.getReplaySpeed(), 0.001);
        assertTrue(settings.isConfirmNetworkReplacement());
        assertTrue(settings.isConfirmClearHistory());
        assertTrue(settings.isConfirmClearNetwork());
        assertTrue(settings.isShowTutorialBanner());
        assertTrue(settings.isAutoSelectRecipient());
    }

    @Test
    @DisplayName("Copy constructor should clone all settings properties accurately")
    void testCopyConstructor() {
        AppSettings original = new AppSettings();
        original.setBatteryDrainPerHop(3.5);
        original.setDefaultPriority(Priority.CRITICAL);
        original.setReplaySpeed(2.0);
        original.setConfirmNetworkReplacement(false);
        original.setConfirmClearHistory(false);
        original.setConfirmClearNetwork(false);
        original.setShowTutorialBanner(false);
        original.setAutoSelectRecipient(false);

        AppSettings copy = new AppSettings(original);

        assertEquals(3.5, copy.getBatteryDrainPerHop(), 0.001);
        assertEquals(Priority.CRITICAL, copy.getDefaultPriority());
        assertEquals(2.0, copy.getReplaySpeed(), 0.001);
        assertFalse(copy.isConfirmNetworkReplacement());
        assertFalse(copy.isConfirmClearHistory());
        assertFalse(copy.isConfirmClearNetwork());
        assertFalse(copy.isShowTutorialBanner());
        assertFalse(copy.isAutoSelectRecipient());
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
    }

    @Test
    @DisplayName("Copy constructor with null argument should produce default settings")
    void testCopyConstructorWithNull() {
        AppSettings copy = new AppSettings(null);
        assertEquals(AppSettings.DEFAULT_BATTERY_DRAIN_PER_HOP, copy.getBatteryDrainPerHop(), 0.001);
        assertEquals(AppSettings.DEFAULT_PRIORITY, copy.getDefaultPriority());
    }

    @Test
    @DisplayName("resetToDefaults should restore all modified properties to factory defaults")
    void testResetToDefaults() {
        AppSettings settings = new AppSettings();
        settings.setBatteryDrainPerHop(5.0);
        settings.setDefaultPriority(Priority.HIGH);
        settings.setReplaySpeed(0.5);
        settings.setConfirmNetworkReplacement(false);
        settings.setConfirmClearHistory(false);
        settings.setShowTutorialBanner(false);

        settings.resetToDefaults();

        assertEquals(2.0, settings.getBatteryDrainPerHop(), 0.001);
        assertEquals(Priority.NORMAL, settings.getDefaultPriority());
        assertEquals(1.0, settings.getReplaySpeed(), 0.001);
        assertTrue(settings.isConfirmNetworkReplacement());
        assertTrue(settings.isConfirmClearHistory());
        assertTrue(settings.isConfirmClearNetwork());
        assertTrue(settings.isShowTutorialBanner());
        assertTrue(settings.isAutoSelectRecipient());
    }

    @Test
    @DisplayName("Battery drain setter should clamp to [0.0, 10.0] range")
    void testBatteryDrainClamping() {
        AppSettings settings = new AppSettings();

        settings.setBatteryDrainPerHop(-2.5);
        assertEquals(0.0, settings.getBatteryDrainPerHop(), 0.001);

        settings.setBatteryDrainPerHop(15.0);
        assertEquals(10.0, settings.getBatteryDrainPerHop(), 0.001);

        settings.setBatteryDrainPerHop(4.5);
        assertEquals(4.5, settings.getBatteryDrainPerHop(), 0.001);
    }

    @Test
    @DisplayName("Replay speed setter should clamp to [0.25, 4.0] range")
    void testReplaySpeedClamping() {
        AppSettings settings = new AppSettings();

        settings.setReplaySpeed(0.1);
        assertEquals(0.25, settings.getReplaySpeed(), 0.001);

        settings.setReplaySpeed(10.0);
        assertEquals(4.0, settings.getReplaySpeed(), 0.001);

        settings.setReplaySpeed(1.5);
        assertEquals(1.5, settings.getReplaySpeed(), 0.001);
    }

    @Test
    @DisplayName("Default priority setter should fall back to NORMAL when null")
    void testDefaultPriorityNullFallback() {
        AppSettings settings = new AppSettings();
        settings.setDefaultPriority(null);
        assertEquals(Priority.NORMAL, settings.getDefaultPriority());

        settings.setDefaultPriority(Priority.CRITICAL);
        assertEquals(Priority.CRITICAL, settings.getDefaultPriority());
    }

    @Test
    @DisplayName("toString should contain key setting names and values")
    void testToString() {
        AppSettings settings = new AppSettings();
        String str = settings.toString();
        assertTrue(str.contains("batteryDrainPerHop"));
        assertTrue(str.contains("defaultPriority"));
        assertTrue(str.contains("confirmClearHistory"));
    }
}
