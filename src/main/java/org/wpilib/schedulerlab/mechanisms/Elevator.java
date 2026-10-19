package org.wpilib.schedulerlab.mechanisms;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.telemetry.TelemetryLoggable;
import org.wpilib.telemetry.TelemetryTable;

/**
 * A purely simulated elevator: no hardware, just a position that command bodies move a fixed step
 * per scheduler cycle. The command IS the control loop — straight-line coroutine code.
 */
public class Elevator implements Mechanism, TelemetryLoggable {
  /** Meters per 20ms scheduler cycle (~1 m/s). */
  public static final double STEP = 0.02;

  public static final double MIN_HEIGHT = 0.0;
  public static final double MAX_HEIGHT = 2.0;

  private double m_position; // meters

  public Elevator() {
    // Mechanisms have no default command unless you set one. idle() is a lowest-priority command
    // that just owns the mechanism, so any other command can take over at any time.
    setDefaultCommand(idle());
  }

  public double getPosition() {
    return m_position;
  }

  public boolean atGoal(double goal) {
    return Math.abs(m_position - goal) < 1e-6;
  }

  /** One simulated control-loop tick toward the goal. */
  public void stepToward(double goal) {
    double clamped = Math.clamp(goal, MIN_HEIGHT, MAX_HEIGHT);
    double delta = clamped - m_position;
    m_position += Math.copySign(Math.min(STEP, Math.abs(delta)), delta);
  }

  /**
   * Moves to a height. Note the shape: initialize/execute/isFinished collapsed into one imperative
   * body, with the mandatory coroutine.yield() inside the loop.
   */
  public Command moveTo(double goalMeters) {
    return run(coroutine -> {
          while (!atGoal(goalMeters)) {
            stepToward(goalMeters);
            coroutine.yield(); // cede control to the scheduler; resumed next cycle
          }
        })
        .named("Elevator.moveTo(" + goalMeters + ")");
  }

  /** A moveTo with an explicit priority, for the priority/interruption labs. */
  public Command holdAt(double goalMeters, int priority, String name) {
    return run(coroutine -> {
          while (true) {
            stepToward(goalMeters);
            coroutine.yield();
          }
        })
        .withPriority(priority)
        .named(name);
  }

  @Override
  public void logTo(TelemetryTable table) {
    table.log("height", m_position);
    table.log("commands", getRunningCommands().stream().map(Command::name).toArray(String[]::new));
  }
}
