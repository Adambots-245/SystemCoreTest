package org.wpilib.schedulerlab.mechanisms;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.telemetry.TelemetryLoggable;
import org.wpilib.telemetry.TelemetryTable;

/** A purely simulated wrist joint (degrees), sibling to the Elevator on the Mechanism2d. */
public class Wrist implements Mechanism, TelemetryLoggable {
  /** Degrees per 20ms scheduler cycle (~150 deg/s). */
  public static final double STEP = 3.0;

  public static final double MIN_ANGLE = 0.0;
  public static final double MAX_ANGLE = 180.0;

  private double m_angle = 90.0; // degrees

  public Wrist() {
    setDefaultCommand(idle());
  }

  public double getAngle() {
    return m_angle;
  }

  public boolean atGoal(double goal) {
    return Math.abs(m_angle - goal) < 1e-6;
  }

  public void stepToward(double goal) {
    double clamped = Math.clamp(goal, MIN_ANGLE, MAX_ANGLE);
    double delta = clamped - m_angle;
    m_angle += Math.copySign(Math.min(STEP, Math.abs(delta)), delta);
  }

  public Command moveTo(double goalDegrees) {
    return run(coroutine -> {
          while (!atGoal(goalDegrees)) {
            stepToward(goalDegrees);
            coroutine.yield();
          }
        })
        .named("Wrist.moveTo(" + goalDegrees + ")");
  }

  /** Sweeps back and forth forever — only ends when canceled (used in race/awaitAny demos). */
  public Command sweep() {
    return run(coroutine -> {
          while (true) {
            while (!atGoal(MAX_ANGLE)) {
              stepToward(MAX_ANGLE);
              coroutine.yield();
            }
            while (!atGoal(MIN_ANGLE)) {
              stepToward(MIN_ANGLE);
              coroutine.yield();
            }
            coroutine.yield(); // the compiler plugin requires a yield directly in every loop
          }
        })
        .named("Wrist.sweep");
  }

  @Override
  public void logTo(TelemetryTable table) {
    table.log("angle", m_angle);
    table.log("commands", getRunningCommands().stream().map(Command::name).toArray(String[]::new));
  }
}
