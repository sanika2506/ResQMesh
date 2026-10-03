package com.resqmesh.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying the OOP model implementation:
 * Encapsulation, Abstraction, Inheritance, and Polymorphism.
 */
class DeviceModelTest {

    @Test
    @DisplayName("Location: Euclidean distance calculation and validation")
    void testLocationDistance() {
        Location loc1 = new Location(0.0, 0.0);
        Location loc2 = new Location(3.0, 4.0);

        // 3-4-5 right triangle distance test
        assertEquals(5.0, loc1.distanceTo(loc2), 0.001);
        assertEquals(5.0, loc2.distanceTo(loc1), 0.001);
        assertEquals(0.0, loc1.distanceTo(loc1), 0.001);

        assertThrows(IllegalArgumentException.class, () -> loc1.distanceTo(null));
    }

    @Test
    @DisplayName("CommunicationDevice: Encapsulation and battery state transitions")
    void testBatteryAndStatusTransitions() {
        Location loc = new Location(10, 20);
        StudentPhone phone = new StudentPhone("DEV-101", "Alice's Phone", loc, 100.0);

        // Initial state: ACTIVE
        assertEquals("DEV-101", phone.getId());
        assertEquals("Alice's Phone", phone.getName());
        assertEquals(100.0, phone.getBatteryLevel(), 0.01);
        assertEquals(DeviceStatus.ACTIVE, phone.getStatus());
        assertTrue(phone.isAvailable());

        // Consume battery down to 15% (<= 20% LOW_BATTERY threshold)
        phone.consumeBattery(85.0);
        assertEquals(15.0, phone.getBatteryLevel(), 0.01);
        assertEquals(DeviceStatus.LOW_BATTERY, phone.getStatus());
        assertTrue(phone.isAvailable());

        // Consume remaining battery to 0% -> OFFLINE
        phone.consumeBattery(15.0);
        assertEquals(0.0, phone.getBatteryLevel(), 0.01);
        assertEquals(DeviceStatus.OFFLINE, phone.getStatus());
        assertFalse(phone.isAvailable());

        // Recharge back to 50% -> ACTIVE
        phone.recharge(50.0);
        assertEquals(50.0, phone.getBatteryLevel(), 0.01);
        assertEquals(DeviceStatus.ACTIVE, phone.getStatus());
        assertTrue(phone.isAvailable());
    }

    @Test
    @DisplayName("Polymorphism: StudentPhone cannot forward CRITICAL messages")
    void testStudentPhoneForwardingRules() {
        Location loc = new Location(5, 5);
        CommunicationDevice phone = new StudentPhone("STU-1", "Bob's Phone", loc, 80.0);

        // Can forward LOW, NORMAL, HIGH
        assertTrue(phone.canForward(Priority.LOW));
        assertTrue(phone.canForward(Priority.NORMAL));
        assertTrue(phone.canForward(Priority.HIGH));

        // CANNOT forward CRITICAL
        assertFalse(phone.canForward(Priority.CRITICAL));

        // When offline, cannot forward anything
        phone.consumeBattery(80.0);
        assertFalse(phone.isAvailable());
        assertFalse(phone.canForward(Priority.LOW));
        assertFalse(phone.canForward(Priority.NORMAL));
        assertFalse(phone.canForward(Priority.HIGH));
        assertFalse(phone.canForward(Priority.CRITICAL));
    }

    @Test
    @DisplayName("Polymorphism: SecurityStation and MedicalStation can forward all priorities")
    void testStationForwardingRules() {
        Location loc = new Location(50, 50);
        CommunicationDevice security = new SecurityStation("SEC-1", "Main Gate Post", loc);
        CommunicationDevice medical = new MedicalStation("MED-1", "Health Center", loc);

        // Both can forward all priorities, including CRITICAL
        for (Priority p : Priority.values()) {
            assertTrue(security.canForward(p), "SecurityStation should forward " + p);
            assertTrue(medical.canForward(p), "MedicalStation should forward " + p);
        }

        // When offline, cannot forward
        security.setStatus(DeviceStatus.OFFLINE);
        assertFalse(security.canForward(Priority.CRITICAL));

        medical.setStatus(DeviceStatus.OFFLINE);
        assertFalse(medical.canForward(Priority.CRITICAL));
    }

    @Test
    @DisplayName("Validation: Device constructor input checks")
    void testConstructorValidation() {
        Location validLoc = new Location(0, 0);

        assertThrows(IllegalArgumentException.class, () -> new StudentPhone(null, "Name", validLoc));
        assertThrows(IllegalArgumentException.class, () -> new StudentPhone("", "Name", validLoc));
        assertThrows(IllegalArgumentException.class, () -> new StudentPhone("ID-1", null, validLoc));
        assertThrows(IllegalArgumentException.class, () -> new StudentPhone("ID-1", "", validLoc));
        assertThrows(IllegalArgumentException.class, () -> new StudentPhone("ID-1", "Name", null));
    }
}
