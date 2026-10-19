package org.wpilib.schedulerlab.mechanisms;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.telemetry.TelemetryLoggable;
import org.wpilib.telemetry.TelemetryTable;

/** Simulated intake used by the StateMachine lab: a deploy angle plus a roller on/off state. */
public class Intake implements Mechanism, TelemetryLoggable {
  /** Degrees per cycle for the deploy pivot. */
  public static final double STEP = 4.0;

  public static final double STOWED = 0.0;
  public static final double DEPLOYED = 90.0;

  private double m_angle = STOWED;
  private boolean m_rollersOn;

  public Intake() {
    setDefaultCommand(idle());
  }

  public double getAngle() {
    return m_angle;
  }

  public boolean rollersOn() {
    return m_rollersOn;
  }

  private void stepToward(double goal) {
    double delta = goal - m_angle;
    m_angle += Math.copySign(Math.min(STEP, Math.abs(delta)), delta);
  }

  public Command deploy() {
    return run(coroutine -> {
          while (Math.abs(m_angle - DEPLOYED) > 1e-6) {
            stepToward(DEPLOYED);
            coroutine.yield();
          }
        })
        .named("Intake.deploy");
  }

  public Command stow() {
    return run(coroutine -> {
          while (Math.abs(m_angle - STOWED) > 1e-6) {
            stepToward(STOWED);
            coroutine.yield();
          }
        })
        .named("Intake.stow");
  }

  /** Runs the rollers until canceled; whenCanceled turns them off. */
  public Command runRollers() {
    return run(coroutine -> {
          m_rollersOn = true;
          coroutine.park(); // yield forever — ends only via cancellation/transition
        })
        .whenCanceled(() -> m_rollersOn = false)
        .named("Intake.runRollers");
  }

  @Override
  public void logTo(TelemetryTable table) {
    table.log("angle", m_angle);
    table.log("rollersOn", m_rollersOn);
    table.log("commands", getRunningCommands().stream().map(Command::name).toArray(String[]::new));
  }
}
