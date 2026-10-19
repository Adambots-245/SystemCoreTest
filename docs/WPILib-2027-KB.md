# WPILib 2027 / SystemCore — Technical Reference

*Current for **WPILib 2027.0.0-alpha-7** (released September 1, 2026). Checked against the alpha-7
WPILib and GradleRIO sources, the alpha-7 VS Code project templates, and the 2027 documentation at
[docs.wpilib.org/en/latest](https://docs.wpilib.org/en/latest/). For a gentler introduction, read
[`Wpilib Changes 2027.md`](Wpilib%20Changes%202027.md) first.*

Alpha APIs change between releases. If something here disagrees with the release you have
installed, trust the installed release and its release notes.

---

## 1. Versions and setup at a glance

| Item | alpha-7 value |
|---|---|
| WPILib release | `v2027.0.0-alpha-7` ([release notes and installers](https://github.com/wpilibsuite/allwpilib/releases/tag/v2027.0.0-alpha-7)) |
| Supported desktop OS | Windows 11 64-bit, Ubuntu 26.04, macOS 15+ (macOS needs `xcode-select --install`). Native Arm64 installers exist for Windows and Linux |
| Install folder | Windows `C:\Users\Public\wpilib\2027_alpha7` · macOS `~/.wpilib/2027_alpha7` · Linux `~/.local/share/wpilib/2027_alpha7` (or `$XDG_DATA_HOME/wpilib/2027_alpha7`) |
| VS Code | Use the bundled **2027_alpha7 WPILib VS Code**, not a separately installed VS Code |
| JDK | Java 25, bundled in `<install folder>/jdk` |
| Gradle | wrapper 9.4.1; plugin `org.wpilib.GradleRIO` version `2027.0.0-alpha-7` |
| SystemCore OS image | **14 or newer** is required for alpha-7 (images 10–13 only work with alpha-5/6) |
| Driver Station | FIRST Driver Station Alpha 7 (the NI Driver Station still connects, but newer features such as OpMode selection and Alerts need the 2027 DS) |
| Dashboards | AdvantageScope 2027 alpha, Elastic **alpha 9 or newer** (required for the new `Selectable`), Glass |
| Vendor libraries | Alpha-6 and older vendordeps **do not** work with alpha-7. Check the [SystemcoreTesting compatibility table](https://github.com/wpilibsuite/SystemcoreTesting/blob/main/README.md) |

Folder names during the alpha are not always the version number: an install folder (and the VS
Code shortcut) can be named for an earlier alpha than the WPILib version inside it. Alpha
installs sit side by side. In season the folder will just be the year.

### SystemCore network and access

| Item | Value |
|---|---|
| USB-C tether address | **`172.26.0.1`** on Windows, **`172.27.0.1`** on macOS/Linux |
| Built-in Wi-Fi access point | SSID `SYSTEMCORE`, password `PASSWORD`, address `172.30.0.1` |
| Ethernet address | Shown on the SystemCore's display |
| Robot network address | `10.TE.AM.2` once a team number is set |
| Web dashboard | `http://robot.local/`. If mDNS doesn't resolve, use the USB address directly. Use it to set the team number and Wi-Fi, install packages, and apply OS updates |
| SSH | user `systemcore`, password `systemcore`, home `/home/systemcore` |
| USB storage | mounted at `/U`, `/V`, … |
| CAN interfaces | `can_s0`–`can_s4` on SystemCore, `can_d0`–`can_d19` on MotionCore |

OS updates for image 12 and later are applied over the air from the web dashboard with an
`.llupdate` file from
[LimelightVision/systemcore-os-public](https://github.com/LimelightVision/systemcore-os-public).
Pick the file that matches your hardware revision (alpha or beta unit).

---

## 2. Platform changes, 2026 → 2027

### Since the first 2027 alphas

- **SystemCore replaces the roboRIO.** It has a Raspberry Pi CM5, an RP2350 for I/O, and runs
  real-time Linux. It is the biggest control-system change since the cRIO, and the same platform
  is shared with FTC from the 2027–28 season.
- **Java 25 / C++23** baseline. ZGC is the default Java garbage collector.
- **Package renames:** Java `edu.wpi.first.*` became `org.wpilib.*`, and C++ `frc::` became
  `wpi::`. A 2026 project must go through the VS Code importer.
- **NetworkTables 3 removed.** Only NT4 remains (`org.wpilib.networktables`).
- **`robotInit()` removed.** Use the `Robot()` constructor. The **"Test" mode is now "Utility"**
  (`utilityInit()` / `utilityPeriodic()`).
- `MotorController.set()` is now `setThrottle()`, and `stopMotor()` is now `disable()`. The FPGA
  clock is now the monotonic clock.
- **Removed hardware APIs:** relay, analog output, SPI and the SPI IMUs
  (ADIS16448/16470, ADXL345, ADXRS450), analog gyro, DMA, built-in accelerometer, glitch filter,
  interrupts, ultrasonic, analog trigger, Nidec, Servo, Jaguar, plus `MotorControllerGroup`,
  Ramsete\*, and Mecanum/SwerveControllerCommand.
- **Added:** multiple CAN buses, Smart IO, the onboard IMU (simulated since alpha-6), and Expansion
  Hub over USB. Ports, connectors, pinouts and wiring changes are covered in
  [`SystemCore-Electrical-Guide.md`](SystemCore-Electrical-Guide.md).
- **Tools removed:** Shuffleboard, SmartDashboard, PathWeaver, RobotBuilder.
- **New frameworks:** Commands v3 (§5) and the FTC-style **OpMode framework**
  (`org.wpilib.framework.OpModeRobot`, `org.wpilib.opmode.*`, `@Teleop` / `@Autonomous`).
- **New multi-platform 2027 FIRST Driver Station.**

### New in alpha-7 (breaking unless noted)

- **Main class back to a supplier:** `RobotBase.startRobot(Robot::new)`, not
  `startRobot(Robot.class)`. Projects from earlier 2027 alphas must change this by hand.
- **SmartDashboard, SendableChooser and the Sendable APIs are gone.** They are replaced by
  **Telemetry** (output) and **Tunables** (dashboard-adjustable values). The new chooser is
  `Selectable` (see §4).
- **All constants and enum values are ALL_CAPS** (for example `Color.GREEN`).
- **More package moves:** `Preferences` moved to `org.wpilib.preferences`, `Alert` moved to
  wpiutil (`org.wpilib.util.Alert`, with more features), some geometry classes moved to a `shape`
  package, and generated protobuf classes moved to separate `proto` packages.
- **AprilTag and CameraServer are now vendordeps.** `AprilTagFields` is part of a new integrated
  `Fields` class.
- **Gamepads:** face-button triggers use direction names (`faceUp/Down/Left/Right`), axes have a
  default deadband, the per-controller classes are back next to the generic `Gamepad`, and
  `DSGamepadChooser` and a raw rumble API were added. Joystick warnings are now Alerts.
- **All high-level CAN device classes take a `CANPort` enum** instead of an integer, so
  SystemCore and MotionCore ports can't be confused.
- **Integer timestamps are now nanoseconds** (silent break: `RobotController.getTime()`,
  `getMonotonicTime()`). DataLog and NT still use microseconds on disk and on the wire.
- **`DriverStationDisplay`:** a text display inside the DS that supports ANSI escape codes.
- The desktop tools and sim GUI use **SDL3** for rendering and gamepad input.
- C++: Catch2 replaces GoogleTest, and `std::expected` / `std::format` replace `wpi::expected` /
  `fmt::format`.
- Math: Rotation2d's `getRadians()/getDegrees()/getRotations()` now return wrapped angles (silent
  break), `Translation2d.getAngle()` returns an `Optional`, and the Trajectory API was rewritten.

---

## 3. Project anatomy (Java, alpha-7 template)

```
MyProject/
├── build.gradle                     # plugins: java, application, org.wpilib.GradleRIO 2027.0.0-alpha-7
├── settings.gradle                  # wpilibYear '2027_alpha7'; finds <install folder>/maven per OS
├── gradle/wrapper/                  # Gradle 9.4.1
├── .wpilib/wpilib_preferences.json  # {"projectYear": "2027_alpha7", "teamNumber": N}
├── .vscode/launch.json              # "WPILib Desktop Debug", "WPILib Systemcore Debug"
├── vendordeps/CommandsV3.json       # -> org.wpilib:commandsv3-java (renamed from commands3-java)
└── src/main/
    ├── deploy/                      # static files -> /home/systemcore/deploy
    └── java/.../Main.java           # RobotBase.startRobot(Robot::new)
```

Key `build.gradle` facts:
- The template now uses Gradle's **`application` plugin** with `deployArtifact.configureApplication`
  and `wpi.java.configureApplication`. Earlier alphas used the shadow plugin instead.
- Deploy block: `systemcore(getTargetTypeClass('SystemCore'))` with
  `team = project.wpilib.getTeamNumber()` and `useDefaultSystemcoreHostName()`. GradleRIO tries
  `robot.local`, `10.TE.AM.2`, the USB address (`172.26.0.1` on Windows, `172.27.0.1` otherwise)
  and `172.30.0.1`, so **a USB deploy works with any team number**.
- `wpi.java.configureTestTasks(test)` now adds the JVM flags that Commands v3 needs in unit tests
  (`--add-opens java.base/jdk.internal.vm=ALL-UNNAMED`, `--add-opens java.base/java.lang=ALL-UNNAMED`,
  `--enable-native-access=ALL-UNNAMED`). Before alpha-7 you had to add them by hand.
- **Desktop simulation is `./gradlew run`.** The `application` plugin's `run` task is set up
  with the sim extensions (`HALSIM_EXTENSIONS`), the native libraries, and the JVM flags. There is
  no `simulateJava` task in alpha-7. Typing it runs `simulateExternalJava`, because Gradle accepts
  abbreviated task names. That task only writes the launch configuration VS Code uses for
  debugging, and it starts no robot.
- Common tasks: `./gradlew build`, `./gradlew deploy`, `./gradlew run`, `./gradlew test`.

Java templates in alpha-7: `timed`, `timedskeleton`, `timeslice`, `timesliceskeleton`,
`commandv2`, `commandv2skeleton`, **`commandv3`**, **`commandv3skeleton`**, `opmode`,
`educational`, `robotbaseskeleton`, plus Romi/XRP variants. Commands v3 examples:
**`hatchbotcmdv3`** and **`rebuiltcmdv3`**.

### Package map (2026 → 2027 alpha-7)

| 2026 (`edu.wpi.first...`) | 2027 alpha-7 |
|---|---|
| `wpilibj.TimedRobot`, `RobotBase` | `org.wpilib.framework.TimedRobot`, `.RobotBase`, `.TimesliceRobot`, `.OpModeRobot` |
| `wpilibj2.command.*` (v2) | `org.wpilib.command2.*` |
| — (new) | `org.wpilib.command3.*` (Commands v3) |
| `wpilibj.smartdashboard.SmartDashboard`, `SendableChooser` | **removed** → `org.wpilib.telemetry.*` and `org.wpilib.tunable.*` (`Tunables`, `TunableDouble`, `Selectable`) |
| `wpilibj.smartdashboard.Mechanism2d`, `Field2d` | still `org.wpilib.smartdashboard.*`, now `TelemetryLoggable` |
| `networktables.*` | `org.wpilib.networktables.*` |
| `wpilibj.XboxController`, `Joystick`, `DriverStation` | `org.wpilib.driverstation.*` (`Gamepad`, `Joystick`, `GenericHID`, `DriverStation`, `MatchState`, `RobotState`, `DriverStationDisplay`, `DSGamepadChooser`) |
| `wpilibj.DataLogManager`, `Timer` | `org.wpilib.system.DataLogManager`, `org.wpilib.system.Timer` |
| `wpilibj.Preferences` | `org.wpilib.preferences.Preferences` |
| `wpilibj.Alert` | `org.wpilib.util.Alert` |
| `util.Color`, `Color8Bit` | `org.wpilib.util.Color`, `Color8Bit` |
| hardware (PWM, solenoids, …) | `org.wpilib.hardware.*` (CAN port enum: `org.wpilib.hardware.bus.CANPort`) |
| — (new) | `org.wpilib.opmode.*` (`PeriodicOpMode`, `@Teleop`, `@Autonomous`) |

---

## 4. Publishing data: Telemetry and Tunables

SmartDashboard is gone in alpha-7. The replacements are:

- **`Telemetry`** (`org.wpilib.telemetry`): output only. `RobotBase` registers a NetworkTables
  backend, so values appear under **`/Telemetry/...`** on NT.
- **`Tunables`** (`org.wpilib.tunable`): values a dashboard can change, published under
  **`/Tunables/...`**. `Selectable` replaces `SendableChooser`.

```java
private final TelemetryTable m_drive = Telemetry.getTable("Drive");   // -> /Telemetry/Drive/

public void robotPeriodic() {
  m_drive.log("leftVelocity", m_leftEncoder.getRate());
  m_drive.log("ready", atSpeed());
  Telemetry.log("RobotPose", m_poseEstimator.getEstimatedPosition()); // struct-serializable
  Telemetry.log("Field", m_field);                                    // TelemetryLoggable
}

private final TunableDouble m_intakeSpeed = Tunables.addDouble("Intake/speed", 0.65);
```

- **Log every loop.** There is no one-time `putData()`. Objects that used to be published once
  (Field2d, Mechanism2d) are now logged each cycle.
- Make your own classes **`TelemetryLoggable`** by implementing `logTo(TelemetryTable table)`.
  `Telemetry.log("Elevator", elevator)` then writes into the child table `/Telemetry/Elevator/`.
  This project's mechanisms do exactly that.
- Supported values: primitives, strings, primitive and `String[]` arrays, raw bytes,
  struct/protobuf-serializable objects, and collections (which need an explicit element type).
- The Commands v3 `Scheduler` is protobuf-serializable:
  `Telemetry.log("Scheduler", Scheduler.getDefault())` publishes the live command tree.
- Plain NT4 is still available for anything two-way:
  `NetworkTableInstance.getDefault().getTable("demo")`.
- On-robot logs: `DataLogManager.start()`.
- **Driver Station display (alpha-7):** `org.wpilib.driverstation.DriverStationDisplay` is a
  text panel inside the 2027 DS, mirrored in the Sim GUI's **Display** window. It works like FTC
  telemetry. In line mode (the default), call `addData(caption, value)` or
  `addData(caption, format, args...)`; a repeated caption replaces its line, and `addLine(text)`
  adds an uncaptioned line. Then call `updateLines()` to send the batch. Sends are rate-limited
  to one every 230 ms, and batches in between are dropped, so calling it every loop is fine.
  `setMode(Mode.RawAnsi)` plus `writeRawAnsi(...)` lets you draw the screen yourself with ANSI
  escape codes.
- Console prints from robot code show up in the sim terminal, and on a robot in the DS console or
  over SSH.

Migration guides: [telemetry](https://github.com/wpilibsuite/allwpilib/blob/main/telemetry/doc/telemetry.md#migration-from-wpilib-2026) ·
[tunables](https://github.com/wpilibsuite/allwpilib/blob/main/tunables/doc/tunables.md#migration-from-wpilib-2026).

---

## 5. Commands v3 (`org.wpilib.command3`) in depth

Official guide: [Commands v3 Programming](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/index.html).
Design doc: [`design-docs/commands-v3.md`](https://github.com/wpilibsuite/allwpilib/blob/main/design-docs/commands-v3.md).
Commands v3 is Java-only. Teams using C++ or Python stay on Commands v2.

### 5.1 Mental model
- A **`Command`** is an interface whose core is `void run(Coroutine coroutine)`. One imperative
  method body replaces v2's `initialize/execute/isFinished/end`.
- Commands are backed by JDK **continuations**. The scheduler mounts a command, runs it until it
  calls `coroutine.yield()`, freezes its stack, and resumes it next cycle. Scheduling is
  cooperative only, so **a loop that never yields freezes the whole robot program**.
- **`Mechanism`** (an *interface* since alpha-7) replaces "subsystem". Commands take exclusive
  ownership of the mechanisms they require, and **integer priority** decides conflicts.
- Everything is **single-threaded**. Don't touch the scheduler from other threads.

### 5.2 Building commands: a staged builder, checked at compile time
The types force the order: requirements → body → (options) → **name**. The name is mandatory
and is the last step.

```java
Command shoot = Command.requiring(shooter, feeder)      // requirements first
    .executing(co -> {                                   // the coroutine body
        shooter.spinUp();
        co.waitUntil(shooter::atSpeed);                  // yields until true
        feeder.feed();
        co.wait(Seconds.of(0.5));                        // timed yield
    })
    .withPriority(10)                                    // optional
    .whenCanceled(shooter::stop)                         // optional cancel hook
    .named("Shoot");                                     // last step: builds the Command
```

Entry points:
- `Command.requiring(mech, ...)`, `Command.noRequirements(body)`, `new StagedCommandBuilder()`
- `mechanism.run(body)`: shorthand for a single-mechanism command (finish with `.named(...)`)
- `mechanism.runRepeatedly(runnable)`: an infinite loop that yields automatically each cycle
- `mechanism.idle()` / `idleFor(Time)`: a parked, lowest-priority command
- `Command.waitUntil(cond)` / `Command.waitFor(time)`, then `.named(...)`
- Priorities: `Command.LOWEST_PRIORITY` (`Integer.MIN_VALUE`), `DEFAULT_PRIORITY` (0),
  `HIGHEST_PRIORITY` (`Integer.MAX_VALUE`)

Builder gotchas:
- Builders are **single-use**. Reusing one after `.named(...)` throws `IllegalStateException`.
- `.until(cond)` wraps the command in a race against `waitUntil(cond)`.
- `withTimeout(Time)` returns a race group named `"<name> [<t> timeout]"`.
- Methods are marked `@NoDiscard`, so dropping a built command or an unfinished builder is a
  compile error under the WPILib compiler plugin.

### 5.3 `Coroutine`: the API inside a command body

```java
boolean yield();                                // pause until the next cycle (required in every loop)
void park();                                    // yield forever; ends only by cancellation
ForkResult fork(Command...);                    // start children, don't wait
ForkResult await(Command);                      // start a child and wait for it to finish
ForkResult awaitAll(Command...);                // wait for all of them
ForkResult awaitAny(Command...);                // wait for the first, cancel the rest
void wait(Time duration);                       // timed yield (resolution = scheduler period)
WaitResult waitUntil(BooleanSupplier);          // conditional yield
WaitResult waitUntil(BooleanSupplier, Time);    // new in alpha-7: gives up after a timeout
void setCancelOnForkFailure(boolean);           // new in alpha-7 (default true)
Scheduler scheduler();                          // escape hatch
```

Rules:
- The WPILib compiler plugin **statically requires a `yield()` call in every loop** of a command
  body. A missing one is a compile error (`Missing call to coroutine.yield() inside loop`). A loop
  that only yields inside a nested loop still fails, because each loop needs a yield directly in
  its own body.
- Coroutine methods only work inside the command they belong to (`IllegalStateException`
  otherwise).
- **Children never outlive their parent.** When a parent exits or is canceled, all forked or
  awaited descendants are canceled. There is no fire-and-forget `ScheduleCommand` equivalent.
- **Two kinds of fork failure:**
  - Forking commands that **share a requirement with each other** is a bug in your code and
    throws `IllegalArgumentException`.
  - A child that **can't be scheduled** (for example, a higher-priority command owns its
    mechanism) cancels the *parent* coroutine by default. Since alpha-7 you can call
    `coroutine.setCancelOnForkFailure(false)` and inspect the returned `ForkResult` instead
    (`successful()`, `failed()`, `partialSuccess()`, `getFailedCommands()`,
    `awaitCompletion()`).
- Children scheduled from a running command start **immediately**, without waiting in the
  queue, so nested groups don't lose a cycle per level.

### 5.4 `Scheduler`

```java
Scheduler s = Scheduler.getDefault();       // also Scheduler.createIndependentScheduler() for tests
s.run();                                    // call from robotPeriodic()

ScheduleResult r = s.schedule(cmd);         // see below
s.isSchedulable(cmd);                       // predict the result without scheduling
s.cancel(cmd);  s.cancelAll();
s.setDefaultCommand(mech, cmd);             // cmd must require exactly that one mechanism
s.sideload(co -> { ... });                  // your own coroutine outside any command (scoped)
s.addPeriodic(runnable);                    // looped with a yield every cycle
```

**`ScheduleResult` is a sealed interface of records (alpha-7)**, not an enum. That lets you
`switch` over it exhaustively, and failures tell you which command blocked you:

| Record | `successful()` | Extra field |
|---|---|---|
| `Success(command)` | true | |
| `AlreadyRunning(command)` | true | |
| `LowerPriorityThanRunningCommand(command, alreadyRunning)` | false | the running command that won |
| `LowerPriorityThanQueuedCommand(command, queuedCommand)` | false | the queued command that won |

What `run()` does each cycle, in order:
1. Cancel commands whose scope has ended.
2. Unbind triggers whose scope has ended.
3. Run sideloads and periodic callbacks.
4. Poll the trigger event loop.
5. Queue default commands for idle mechanisms.
6. Promote queued commands, resolving conflicts.
7. Mount and run each command up to its next yield point.

Inspection and telemetry:
- `currentCommand()`, `isRunning/isScheduled/isScheduledOrRunning(cmd)`, `getRunningCommands()`,
  `getRunningCommandsFor(mech)`, `getQueuedCommands()`, `getParentOf(cmd)`
- `runId(cmd)` (unique per scheduling), `lastCommandRuntimeMs(cmd)`, `totalRuntimeMs(cmd)`,
  `lastRuntimeMs()` (the whole tick)
- `addEventListener(e -> ...)` receives `SchedulerEvent` records: `Scheduled`, `Mounted`,
  `Yielded`, `Completed`, `CompletedWithError`, `Canceled`, `Interrupted`. Each has
  `timestampNanos()` (nanoseconds since alpha-7; some docs pages still say `timestampMicros`).
  `Interrupted` is always followed by `Canceled`. `Mounted` and `Yielded` fire every cycle for
  every running command.
- `Scheduler` is `ProtobufSerializable`. Its snapshot of the command tree has `id`, `parent_id`,
  name, priority, requirements, and timings.

Error behavior: an uncaught exception in a command has its stack trace rewritten to point at the
place the command was scheduled or bound. The command's whole ancestry is canceled, and the
exception is **rethrown out of `Scheduler.run()`**.

Calling `cancel()` on the command that is currently running asks that command to cancel itself.
It yields immediately, and the scheduler then cancels it and its children.

### 5.5 Priorities and conflicts (replaces v2's interruptible flag)
- A conflict means the two commands' requirement sets overlap.
- A new command with **equal or higher** priority than a conflicting running command interrupts
  it and cancels it (`Interrupted`, then `Canceled`).
- A new command with **lower** priority is rejected with `LowerPriorityThanRunningCommand`.
- **Effective priority includes ancestors.** A child is compared using the highest priority in
  its parent chain, and a running command is defended with the highest priority in *its*
  parent chain.
- Ancestors never conflict with their own descendants.
- There is **no disabled-mode gating** and no per-command interrupt-behavior setting. Priority is
  the only lever. The scheduler runs commands whatever the enable state, and actuator safety
  comes from the HAL.

### 5.6 Mechanisms

```java
public class Elevator implements Mechanism {       // an interface since alpha-7
  public Elevator() {
    setDefaultCommand(idle());                     // nothing sets a default for you any more
  }

  public Command moveTo(double goal) {
    return run(co -> { while (!atGoal(goal)) { step(goal); co.yield(); } })
          .named("Elevator.moveTo(" + goal + ")");
  }
}
```
- **No automatic default command (alpha-7).** Until you call `setDefaultCommand(...)`, an idle
  mechanism has no running command. The official examples set `setDefaultCommand(idle())`, often
  with `idle()` overridden to do something safe (for example, stop the motors).
- `getName()` defaults to the class's simple name. Override it for a custom name.
  `idle()` is named `"<name>[IDLE]"` and runs at `LOWEST_PRIORITY`.
- `setDefaultCommand` requires a command that requires exactly this one mechanism. Give custom
  defaults a priority below 0 so normal commands can take over.
- Default-command bindings are scope-aware (§5.8). Set one inside an opmode or a command, and the
  previous default comes back when that scope ends.
- Combine with `TelemetryLoggable` to have a mechanism publish its own state (§4).

### 5.7 Groups: plain control flow, or builders
Prefer writing sequence and parallel logic *inside one command body* with `await`/`fork`. For
composition-style code, these builders exist:

- Sequence: `Command.sequence(a, b, c)` or `a.andThen(b)` → `SequentialGroupBuilder` →
  `.named("...")` or `.withAutomaticName()` (produces `"A -> B -> C"`).
- Parallel (all finish): `Command.parallel(...)` / `a.alongWith(b)` → auto-name `"(A & B)"`.
- Race (first to finish): `Command.race(...)` / `a.raceWith(b)` → auto-name `"(A | B)"`.
- Deadline: `new ParallelGroupBuilder().requiring(deadlineCmds).optional(racedCmds)` → auto-name
  `"[(A & B) * (X | Y)]"`.
- A group's priority is the highest of its members. A group owns the **union** of its members'
  requirements for its whole duration. Ownership that lasts only while a child runs applies to
  `fork`/`await` composition, not to these built-in groups.
- Building a parallel group whose members share a requirement throws.

### 5.8 Triggers, bindings, and scopes

```java
new Trigger(() -> someCondition)        // uses the default scheduler's event loop
    .onTrue(cmd);
// also: onFalse, whileTrue, whileFalse, toggleOnTrue, toggleOnFalse,
//       retryWhileTrue / retryWhileFalse (alpha-7: keep trying to schedule while the condition holds)
```
- Composition: `and`, `or`, `negate`, `debounce(Time[, type])`, `risingEdge()`, `fallingEdge()`
  (single-cycle pulses, so bind them with `onTrue`), and **`multiPress(count, window)`** (new in
  alpha-7, for double-tap and similar).
- `getAsBoolean()` returns the value from the last poll (false before the first scheduler tick).
  `unbind()` removes a trigger's bindings.
- **Binding scopes:** every trigger binding, manual schedule, and default-command override
  records the narrowest active scope when it is created: `Global`, an OpMode, or a `Command`.
  When that scope exits (the command ends, or the opmode is deselected), its bindings are
  unbound, their commands are canceled, and shadowed default commands are restored. Sideloads
  and periodic callbacks are scoped the same way since alpha-7.
- HID: `org.wpilib.command3.button.CommandGamepad`, with `faceDown()`, `faceRight()`,
  `faceLeft()`, `faceUp()`, bumpers, `leftTrigger(threshold)`, `dpadUp()` and so on. Trigger
  instances are cached per button. `RobotModeTriggers.autonomous()/teleop()/disabled()/utility()`.
- `NetworkButton` is a Trigger driven by an NT boolean, which is handy with no hardware.
  **Caveat:** its condition is `instance.isConnected() && value`, so it never fires unless at
  least one NT peer (a dashboard) is connected. Changing the value in the Sim GUI's NetworkTables
  window doesn't count, because the Sim GUI runs inside the robot program. If you want flags
  that also work from the Sim GUI, bind a plain `new Trigger(subscriber::get)` instead, as
  SchedulerLab does. A headless unit test of a `NetworkButton` has to set up a real NT server
  and client pair.

### 5.9 `StateMachine`
A declarative state machine that is itself a `Command`. It requires nothing, and its states'
commands own the mechanisms:

```java
var sm = new StateMachine("Intake");
var idle   = sm.addState(intake.idle());
var deploy = sm.addState(intake.deploy());
var eat    = sm.addState(intake.runRollers());
sm.setInitialState(idle);                       // required (checked by the compiler plugin)
idle.switchTo(deploy).when(() -> wantIntake);   // rising-edge condition while the state is active
deploy.switchTo(eat).whenComplete();            // fires when the state's command completes
eat.switchTo(idle).when(() -> haveGamePiece);
eat.exitStateMachine().when(() -> abort);
Scheduler.getDefault().schedule(sm);
```
Also: `switchFromAny(states...)`, dynamic targets with `switchTo(Supplier<State>)`,
`whenCompleteAnd(cond)`, and `onEnter`/`onExit` callbacks.
Gotchas: `when(...)` never fires for one-shot (never-yielding) state commands, so use
`whenComplete[And]` for those. The first matching transition wins. Using a state from a
different machine throws.

### 5.10 v2 → v3 vocabulary map

| v2 | v3 |
|---|---|
| `extends SubsystemBase` | `implements Mechanism` |
| `initialize/execute/isFinished/end` | one `run(Coroutine)` body with `yield()` |
| `CommandScheduler.getInstance().run()` | `Scheduler.getDefault().run()` |
| decorators (`withTimeout`, `andThen`, …) returning wrappers | staged builders ending in `.named(...)` / `.withAutomaticName()` |
| `interruptible` flag | integer `priority()` |
| `ProxyCommand` / `proxy()` | the default behavior of `fork`/`await` children |
| `ScheduleCommand` (fire and forget) | **removed**: children can't outlive parents |
| anonymous group names in telemetry | required names plus auto-generated group names |
| `Trigger.onTrue` (global, forever) | same names, but **scoped** (Global/OpMode/Command) |
| default command created by the framework | set explicitly with `setDefaultCommand(idle())` |
| — | `StateMachine`, event listeners, protobuf command tree, `runId`, per-command timing, `ForkResult` |

---

## 6. Build, run, and test workflow

1. **Simulate first:** `./gradlew run` (or **WPILib: Simulate Robot Code**). The Sim
   GUI is on by default, and you can enable the robot from it without a Driver Station.
2. **Build:** `./gradlew build`. It resolves WPILib from `<install folder>/maven`, so it also
   works offline.
3. **Deploy:** `./gradlew deploy` (or **WPILib: Deploy Robot Code**). It copies the program to
   `/home/systemcore` over SSH and restarts it.
4. **Watch:** read console output in the DS or over SSH, and view NT data (Telemetry included)
   in AdvantageScope, Elastic, or Glass pointed at the robot's address.
5. **Enable on real hardware** with the 2027 Driver Station. Remember that the v3 scheduler
   doesn't gate commands on enable state.

### Unit tests against Commands v3
- Set up the HAL in `@BeforeEach` with `HAL.initialize()` (`org.wpilib.hardware.hal.HAL`). In
  alpha-7 it takes no arguments.
- The JVM flags that v3 continuations need come from `wpi.java.configureTestTasks(test)` in
  alpha-7. Without them (on older alphas, or in a custom test task), constructing a mechanism or
  scheduler fails with `IllegalAccessException: module java.base does not open jdk.internal.vm`.
- Mechanisms register their default commands with the shared default scheduler. Create them once
  per test class, or use `Scheduler.createIndependentScheduler()` together with an overridden
  `getRegisteredScheduler()`.

### Known issues worth remembering
- **AdvantageScope and Elastic missing from "WPILib: Start Tool":** the alpha-7 extension only
  lists tools that have an entry in `<install folder>/tools/` (`<Name>.exe` on Windows,
  `<Name>.app` on macOS, a lowercase name on Linux). The installer puts these two in their own
  `advantagescope/` and `elastic/` folders and never adds a `tools/` entry. Windows gets
  Start-menu and desktop shortcuts under **2027_alpha7 WPILib Tools**. On macOS, the archives are
  often left unextracted as well. See [`Watching-the-Labs.md`](Watching-the-Labs.md).
- **AdvantageScope's Mechanism tab is blank ("0 segments") with alpha-7 data:** in alpha-7, a
  Mechanism2d root publishes a single `position` array instead of `x`/`y`. AdvantageScope
  v27.0.0-alpha-6 (the version bundled with alpha-7) only reads `x`/`y`, and its parser quietly
  gives up. Support was added to AdvantageScope's main branch on 2026-09-26 but isn't in a release
  yet. Until then, view Mechanism2d in Glass or the Sim GUI, which are updated for the new format.
- **Simulation crashes right after a WPILib update (macOS):** run `./gradlew clean`, or delete
  `build/`.
- **Deploy blocked on macOS when several VS Code copies are installed:** allow each of them in
  System Settings → Privacy & Security → Local Network.
- **"GradleRIO plugin not found":** the Gradle cache is corrupted. Delete `~/.gradle`.
- The sim's "real Driver Station" option was turned into a no-op in alpha-6 (to fix a crash).
  Standard simulation is unaffected.
- SystemCore alpha and beta units: USB 3 drive mounting can be unreliable, and on alpha units
  analog input values slowly rise.
- Current lists: [WPILib Known Issues](https://docs.wpilib.org/en/latest/docs/yearly-overview/known-issues.html)
  and [SystemcoreTesting issues](https://github.com/wpilibsuite/SystemcoreTesting/issues).

---

## 7. Sources

- [allwpilib alpha-7 release notes](https://github.com/wpilibsuite/allwpilib/releases/tag/v2027.0.0-alpha-7) and source (`commandsv3`, `telemetry`, `tunables`, `wpilibj`)
- [GradleRIO](https://github.com/wpilibsuite/GradleRIO) and [vscode-wpilib](https://github.com/wpilibsuite/vscode-wpilib) at tag `v2027.0.0-alpha-7` (project templates)
- [New for 2027](https://docs.wpilib.org/en/latest/docs/yearly-overview/yearly-changelog.html) · [Known Issues](https://docs.wpilib.org/en/latest/docs/yearly-overview/known-issues.html)
- [Commands v3 Programming](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/index.html) · [Commands v3 design doc](https://github.com/wpilibsuite/allwpilib/blob/main/design-docs/commands-v3.md)
- [Telemetry migration](https://github.com/wpilibsuite/allwpilib/blob/main/telemetry/doc/telemetry.md#migration-from-wpilib-2026) · [Tunables migration](https://github.com/wpilibsuite/allwpilib/blob/main/tunables/doc/tunables.md#migration-from-wpilib-2026)
- [SystemcoreTesting README](https://github.com/wpilibsuite/SystemcoreTesting/blob/main/README.md) (compatibility table, network addresses, imaging)
