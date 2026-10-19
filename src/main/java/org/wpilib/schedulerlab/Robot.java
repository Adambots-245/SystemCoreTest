package org.wpilib.schedulerlab;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Scheduler;
import org.wpilib.driverstation.DriverStationDisplay;
import org.wpilib.framework.TimedRobot;
import org.wpilib.schedulerlab.mechanisms.Elevator;
import org.wpilib.schedulerlab.mechanisms.Intake;
import org.wpilib.schedulerlab.mechanisms.Wrist;
import org.wpilib.smartdashboard.Mechanism2d;
import org.wpilib.smartdashboard.MechanismLigament2d;
import org.wpilib.smartdashboard.MechanismRoot2d;
import org.wpilib.system.DataLogManager;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.telemetry.TelemetryTable;
import org.wpilib.util.Color;
import org.wpilib.util.Color8Bit;

/**
 * SchedulerLab — a hardware-free playground for the WPILib 2027 Commands v3 scheduler.
 *
 * <p>Everything is driven from NetworkTables booleans under the "SchedulerLab" table (no gamepad
 * needed) and observed through console prints, the Driver Station display, and Telemetry (a
 * Mechanism2d, per-mechanism state, and the scheduler's command tree). See the labs in {@link
 * Labs}.
 */
public class Robot extends TimedRobot {
  private final Elevator m_elevator = new Elevator();
  private final Wrist m_wrist = new Wrist();
  private final Intake m_intake = new Intake();

  private final Labs m_labs = new Labs(m_elevator, m_wrist, m_intake);

  /** Published to NetworkTables under /Telemetry/SchedulerLab/. */
  private final TelemetryTable m_telemetry = Telemetry.getTable("SchedulerLab");

  private final Mechanism2d m_mech = new Mechanism2d(3, 3);
  private final MechanismLigament2d m_elevatorLig;
  private final MechanismLigament2d m_wristLig;
  private final MechanismLigament2d m_intakeLig;

  private static final Color8Bit ROLLERS_OFF = new Color8Bit(Color.GRAY);
  private static final Color8Bit ROLLERS_ON = new Color8Bit(Color.GREEN);

  public Robot() {
    DataLogManager.start();

    MechanismRoot2d towerRoot = m_mech.getRoot("tower", 1.5, 0);
    m_elevatorLig =
        towerRoot.append(
            new MechanismLigament2d("elevator", 0.5, 90, 8, new Color8Bit(Color.ORANGE)));
    m_wristLig =
        m_elevatorLig.append(
            new MechanismLigament2d("wrist", 0.5, 0, 6, new Color8Bit(Color.PURPLE)));
    // The intake pivots on a short post so that, when deployed (horizontal), it isn't drawn lying
    // on the bottom edge of the view. Ligament angles are relative to their parent: 0 = in line
    // with the post (stowed, pointing up), -90 = pointing right (deployed).
    MechanismRoot2d intakeRoot = m_mech.getRoot("intake", 0.4, 0);
    MechanismLigament2d intakePost =
        intakeRoot.append(
            new MechanismLigament2d("post", 0.25, 90, 4, new Color8Bit(Color.DIM_GRAY)));
    m_intakeLig = intakePost.append(new MechanismLigament2d("intake", 0.6, 0, 6, ROLLERS_OFF));

    System.out.println();
    System.out.println("==============================================================");
    System.out.println(" SchedulerLab — WPILib 2027 Commands v3 scheduler playground");
    System.out.println("==============================================================");
    System.out.println(" Connect a dashboard (AdvantageScope/Elastic/Glass) and set a");
    System.out.println(" boolean under the 'SchedulerLab' table to true to run a lab:");
    System.out.println("   lab1  Coroutine basics (yield / timed wait)");
    System.out.println("   lab2  Async composition (fork / await / awaitAll / awaitAny)");
    System.out.println("   lab3  Group builders & auto-generated names");
    System.out.println("   lab4  Priorities & interruption");
    System.out.println("   lab5  Scoped bindings (uses lab5InnerTrigger)");
    System.out.println("   lab6  Conflict detection");
    System.out.println("   lab7  StateMachine (uses lab7GamePiece)");
    System.out.println("   lab8  Telemetry (events, run IDs, timings)");
    System.out.println(" The flag resets itself when the lab finishes.");
    System.out.println(" Live state is under /Telemetry/SchedulerLab/.");
    System.out.println("==============================================================");
  }

  @Override
  public void robotPeriodic() {
    // The one required integration point: tick the v3 scheduler every loop.
    Scheduler.getDefault().run();

    // Reflect the simulated mechanism state on the Mechanism2d.
    m_elevatorLig.setLength(0.5 + m_elevator.getPosition());
    m_wristLig.setAngle(m_wrist.getAngle() - 90);
    m_intakeLig.setAngle(-m_intake.getAngle());
    m_intakeLig.setColor(m_intake.rollersOn() ? ROLLERS_ON : ROLLERS_OFF);

    // Telemetry is logged every loop (there is no one-time putData any more). Each mechanism is a
    // TelemetryLoggable and writes its own child table; the Scheduler is protobuf-serializable, so
    // tools that decode protobuf (e.g. AdvantageScope's sidebar) can browse the live command tree.
    m_telemetry.log("Mech2d", m_mech);
    m_telemetry.log("Elevator", m_elevator);
    m_telemetry.log("Wrist", m_wrist);
    m_telemetry.log("Intake", m_intake);
    m_telemetry.log("Scheduler", Scheduler.getDefault());

    // The Driver Station display (also shown in the Sim GUI's "Display" window): a quick readout
    // that needs no dashboard. Lines with the same caption replace each other until
    // updateLines() sends the batch (at most every 230 ms; extra batches are dropped).
    DriverStationDisplay.addData("Lab", m_labs.describeActiveLabs());
    DriverStationDisplay.addData(
        "Elevator", "%.2f m  %s", m_elevator.getPosition(), commandNames(m_elevator));
    DriverStationDisplay.addData(
        "Wrist", "%.0f deg  %s", m_wrist.getAngle(), commandNames(m_wrist));
    DriverStationDisplay.addData(
        "Intake",
        "%.0f deg, rollers %s  %s",
        m_intake.getAngle(),
        m_intake.rollersOn() ? "ON" : "off",
        commandNames(m_intake));
    DriverStationDisplay.updateLines();
  }

  /** The names of the commands that currently own a mechanism, e.g. "[Wrist[IDLE]]". */
  private static String commandNames(Mechanism mechanism) {
    return mechanism.getRunningCommands().stream().map(Command::name).toList().toString();
  }
}
