package com.resqmesh.config;

import com.resqmesh.model.EmergencyMessage;
import com.resqmesh.model.Location;
import com.resqmesh.model.MessageStatus;
import com.resqmesh.model.Priority;
import com.resqmesh.model.StudentPhone;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.simulation.SimulationEngine;
import com.resqmesh.simulation.SimulationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Step 13: AppSettingsManager Persistence & Simulation Engine Integration Tests")
public class AppSettingsManagerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should save custom settings to JSON and load them back accurately")
    void testSaveAndLoadSettings() throws Exception {
        File file = tempDir.resolve("custom-settings.json").toFile();
        AppSettingsManager manager = new AppSettingsManager(file);

        AppSettings original = new AppSettings();
        original.setBatteryDrainPerHop(3.5);
        original.setDefaultPriority(Priority.HIGH);
        original.setReplaySpeed(1.5);
        original.setConfirmNetworkReplacement(false);
        original.setConfirmClearHistory(false);
        original.setConfirmClearNetwork(false);
        original.setShowTutorialBanner(false);
        original.setAutoSelectRecipient(false);

        manager.saveSettings(original);
        assertTrue(file.exists());

        // Load afresh with a new manager pointing to the same file
        AppSettingsManager reloadManager = new AppSettingsManager(file);
        AppSettings loaded = reloadManager.getSettings();

        assertEquals(3.5, loaded.getBatteryDrainPerHop(), 0.001);
        assertEquals(Priority.HIGH, loaded.getDefaultPriority());
        assertEquals(1.5, loaded.getReplaySpeed(), 0.001);
        assertFalse(loaded.isConfirmNetworkReplacement());
        assertFalse(loaded.isConfirmClearHistory());
        assertFalse(loaded.isConfirmClearNetwork());
        assertFalse(loaded.isShowTutorialBanner());
        assertFalse(loaded.isAutoSelectRecipient());
    }

    @Test
    @DisplayName("Should return factory defaults without error if file does not exist")
    void testLoadSettingsMissingFile() {
        File nonExistent = tempDir.resolve("non-existent-settings.json").toFile();
        AppSettingsManager manager = new AppSettingsManager(nonExistent);

        AppSettings settings = manager.getSettings();
        assertNotNull(settings);
        assertEquals(2.0, settings.getBatteryDrainPerHop(), 0.001);
        assertEquals(Priority.NORMAL, settings.getDefaultPriority());
        assertTrue(settings.isConfirmClearHistory());
    }

    @Test
    @DisplayName("Should gracefully fall back to defaults if JSON file is corrupted or empty")
    void testLoadSettingsCorruptedFile() throws IOException {
        File corruptFile = tempDir.resolve("corrupted-settings.json").toFile();
        Files.writeString(corruptFile.toPath(), "{ not valid json @#! }", StandardCharsets.UTF_8);

        AppSettingsManager manager = new AppSettingsManager(corruptFile);
        AppSettings settings = manager.getSettings();

        assertNotNull(settings);
        assertEquals(2.0, settings.getBatteryDrainPerHop(), 0.001);
        assertEquals(Priority.NORMAL, settings.getDefaultPriority());
    }

    @Test
    @DisplayName("resetToDefaults should persist default settings to the file")
    void testResetToDefaultsPersists() throws Exception {
        File file = tempDir.resolve("reset-settings.json").toFile();
        AppSettingsManager manager = new AppSettingsManager(file);

        AppSettings custom = new AppSettings();
        custom.setBatteryDrainPerHop(5.0);
        custom.setDefaultPriority(Priority.CRITICAL);
        manager.saveSettings(custom);

        // Reset
        AppSettings reset = manager.resetToDefaults();
        assertEquals(2.0, reset.getBatteryDrainPerHop(), 0.001);
        assertEquals(Priority.NORMAL, reset.getDefaultPriority());

        // Verify reloaded from disk
        AppSettings reloaded = new AppSettingsManager(file).getSettings();
        assertEquals(2.0, reloaded.getBatteryDrainPerHop(), 0.001);
        assertEquals(Priority.NORMAL, reloaded.getDefaultPriority());
    }

    @Test
    @DisplayName("saveSettings should reject null argument with IllegalArgumentException")
    void testSaveNullSettings() {
        File file = tempDir.resolve("settings.json").toFile();
        AppSettingsManager manager = new AppSettingsManager(file);

        assertThrows(IllegalArgumentException.class, () -> manager.saveSettings(null));
    }

    @Test
    @DisplayName("Configurable battery drain setting should directly affect SimulationEngine consumption")
    void testSimulationEngineConfigurableDrain() {
        NetworkGraph graph = new NetworkGraph();
        StudentPhone dev1 = new StudentPhone("DEV-1", "Node A", new Location(0, 0), 100.0);
        StudentPhone dev2 = new StudentPhone("DEV-2", "Node B", new Location(10, 10), 100.0);
        graph.addDevice(dev1);
        graph.addDevice(dev2);
        graph.connect(dev1, dev2);

        SimulationEngine engine = new SimulationEngine(graph);

        // Standard default drain is 2.0%
        assertEquals(2.0, engine.getBatteryCostPerTransmission(), 0.001);

        // Apply custom setting: 4.5% per transmission
        AppSettings customSettings = new AppSettings();
        customSettings.setBatteryDrainPerHop(4.5);
        engine.setBatteryCostPerTransmission(customSettings.getBatteryDrainPerHop());
        assertEquals(4.5, engine.getBatteryCostPerTransmission(), 0.001);

        EmergencyMessage msg1 = new EmergencyMessage("MSG-1", dev1, dev2, "Test alert", Priority.NORMAL);
        SimulationResult result1 = engine.send(msg1);

        assertTrue(result1.isDelivered());
        assertEquals(MessageStatus.DELIVERED, msg1.getStatus());
        // Both nodes along the 2-node path should have 100.0 - 4.5 = 95.5% battery
        assertEquals(95.5, dev1.getBatteryLevel(), 0.001);
        assertEquals(95.5, dev2.getBatteryLevel(), 0.001);

        // Now test zero-drain setting (0.0% drain)
        customSettings.setBatteryDrainPerHop(0.0);
        engine.setBatteryCostPerTransmission(customSettings.getBatteryDrainPerHop());

        EmergencyMessage msg2 = new EmergencyMessage("MSG-2", dev1, dev2, "Zero drain alert", Priority.NORMAL);
        SimulationResult result2 = engine.send(msg2);

        assertTrue(result2.isDelivered());
        // Battery levels should remain unchanged at 95.5%
        assertEquals(95.5, dev1.getBatteryLevel(), 0.001);
        assertEquals(95.5, dev2.getBatteryLevel(), 0.001);
    }
}
