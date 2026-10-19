package org.wpilib.schedulerlab;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.wpilib.command3.Scheduler;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.networktables.BooleanPublisher;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.schedulerlab.mechanisms.Elevator;
import org.wpilib.schedulerlab.mechanisms.Intake;
import org.wpilib.schedulerlab.mechanisms.Wrist;

/**
 * Verifies the lab-flag wiring both ways a student can start a lab: from inside the robot program
 * with no dashboard connected (as the Sim GUI's NetworkTables window does), and from a real
 * NetworkTables client connected over the network (as AdvantageScope does).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LabsWiringTest {
  private static final int PORT = 25810; // avoid clashing with a running sim/robot on 5810

  private static final Elevator elevator = new Elevator();
  private static final Wrist wrist = new Wrist();
  private static final Intake intake = new Intake();

  @SuppressWarnings("unused")
  private static final Labs labs = new Labs(elevator, wrist, intake);

  private static final NetworkTableInstance client = NetworkTableInstance.create();

  private final Scheduler scheduler = Scheduler.getDefault();

  @BeforeEach
  void setup() {
    assert HAL.initialize();
    scheduler.cancelAll();
  }

  @AfterAll
  static void teardown() {
    client.stopClient();
    NetworkTableInstance.getDefault().stopServer();
  }

  @Test
  @Order(1) // must run before any NT client has connected
  void settingFlagInsideTheRobotProgramRunsLab() {
    NetworkTableInstance local = NetworkTableInstance.getDefault();
    assertFalse(local.isConnected(), "no dashboard should be connected for this test");

    try (BooleanPublisher lab3 = local.getTable("SchedulerLab").getBooleanTopic("lab3").publish()) {
      scheduler.run(); // let the trigger observe the initial false
      lab3.set(true);
      for (int i = 0; i < 5; i++) {
        scheduler.run();
      }
      assertTrue(
          scheduler.getRunningCommands().stream().anyMatch(c -> c.name().equals("Lab3")),
          "lab3 should start from a local flag change, with no dashboard connected");
      lab3.set(false);
    }
  }

  @Test
  @Order(2)
  void dashboardSettingLab1FlagRunsLab1() throws InterruptedException {
    NetworkTableInstance server = NetworkTableInstance.getDefault();
    server.startServer("", "127.0.0.1", "", PORT);
    client.startClient("wiring-test");
    client.setServer("127.0.0.1", PORT);

    for (int i = 0; i < 200 && !(server.isConnected() && client.isConnected()); i++) {
      Thread.sleep(10);
    }
    assertTrue(server.isConnected(), "NT client should have connected to the server");

    try (BooleanPublisher lab1 =
        client.getTable("SchedulerLab").getBooleanTopic("lab1").publish()) {
      scheduler.run(); // let the trigger observe the initial false
      lab1.set(true);

      boolean moved = false;
      for (int i = 0; i < 400 && !moved; i++) {
        Thread.sleep(5); // give NT time to deliver the update to the robot-side subscriber
        scheduler.run();
        moved = elevator.getPosition() > 1.0;
      }
      assertTrue(moved, "lab1 should have driven the elevator up after its NT flag went true");
    }
  }
}
