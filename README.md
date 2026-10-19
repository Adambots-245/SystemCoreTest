# SchedulerLab — a Commands v3 playground for WPILib 2027

SchedulerLab is a hardware-free robot project for learning the new coroutine-based **Commands v3**
scheduler (`org.wpilib.command3`) in WPILib 2027. It simulates three mechanisms (an elevator, a
wrist and an intake), draws them on a **Mechanism2d**, and explains each step with console
prints. You don't need a gamepad, motors, or sensors. You start each lab by setting a
NetworkTables boolean from a dashboard.

The project targets **WPILib 2027.0.0-alpha-7** and runs in desktop simulation or on a SystemCore.

## What you need

| Item | Version |
|---|---|
| WPILib installer | [2027.0.0-alpha-7](https://github.com/wpilibsuite/allwpilib/releases/tag/v2027.0.0-alpha-7). Supported systems: Windows 11 64-bit, Ubuntu 26.04, or macOS 15+. On macOS, run `xcode-select --install` first. |
| Dashboard | Nothing extra for simulation: the **Sim GUI** can start labs and draw the mechanism. **AdvantageScope** (included with WPILib) is optional, for graphs. |
| SystemCore *(optional)* | OS image **14 or newer** (alpha-7 will not run on images 10–13) |
| FIRST Driver Station *(optional, for a real robot)* | 2027 Alpha 7 |

The installer bundles Java 25, so you don't need to install a JDK yourself.

## Getting started

1. Install WPILib 2027 alpha-7. It installs into a `2027_alpha7` folder. Alpha releases install
   side by side, so an older alpha can stay installed.
2. Open this folder in **2027_alpha7 WPILib VS Code**. The installer creates a shortcut for it.
   Don't use a regular VS Code install; it may load an older WPILib extension.
3. Wait for the Java language server and Gradle to finish loading. The first build downloads
   dependencies.

### Run it in simulation (start here)

1. In VS Code, run **WPILib: Simulate Robot Code**, or run `./gradlew run` in a terminal (on
   Windows, `gradlew run`). Since alpha-7 there is no `simulateJava` task; desktop simulation is
   the standard `run` task. The Sim GUI window opens automatically.
2. **Start a lab:** in the Sim GUI's **NetworkTables** window, expand **Transitory Values →
   SchedulerLab** and change **lab1** from **false** to **true**. Each flag switches itself back
   to false when its lab finishes.
3. **See the mechanism:** in the Sim GUI's top menu, open **NetworkTables → Telemetry →
   SchedulerLab → Mech2d**.
4. Read the lab's explanation in the VS Code terminal, which is where the robot program prints.
   A short live summary is in the Sim GUI's **Display** window.

For graphs, and to see which command owns each mechanism, you can also connect
**AdvantageScope**. Its Mechanism tab can't draw alpha-7 data yet, so keep using the Sim GUI for
the drawing.

**Full step-by-step guide, including how to open AdvantageScope on Windows, macOS and Linux, plus
troubleshooting: [`docs/Watching-the-Labs.md`](docs/Watching-the-Labs.md).**

### Run it on a SystemCore

1. Plug the SystemCore into your computer with USB-C. It is reachable at **`172.26.0.1`** on
   Windows and **`172.27.0.1`** on macOS/Linux.
2. Run **WPILib: Deploy Robot Code**. For a USB deploy the team number in
   `.wpilib/wpilib_preferences.json` doesn't matter, because GradleRIO always tries the USB
   address. Set your real team number if you want to deploy over the robot network
   (`10.TE.AM.2`).
3. There's no Sim GUI on a real robot, so use **Glass** (it does appear in **WPILib: Start
   Tool**). Open **NetworkTables → NetworkTables Settings**, choose client mode, and enter the
   SystemCore's address. Then start labs and open **Mech2d** exactly as in simulation. For graphs
   in AdvantageScope, choose **File → Use Systemcore USB Address**, then **Connect to Robot**.
4. Console output appears in the Driver Station console. You can also run
   `ssh systemcore@<address>` (default password `systemcore`).

## The labs

| Flag | Demonstrates |
|---|---|
| `lab1` | Coroutine basics: one imperative body, a mandatory `yield()` in every loop, timed `wait()` |
| `lab2` | Async composition: `fork` / `await` / `awaitAll` / `awaitAny`. Children own mechanisms only while they run |
| `lab3` | Group builders and auto-generated names (`A -> B`, `(A & B)`, `(A \| B)`, deadline groups) |
| `lab4` | Priorities: equal or higher priority interrupts, lower priority is rejected. Each `ScheduleResult` names the command that blocked the attempt |
| `lab5` | Scoped bindings: a trigger created inside a command is removed when that command ends (uses `lab5InnerTrigger`) |
| `lab6` | Conflict detection: parallel groups or forks with shared requirements throw when they are built |
| `lab7` | `StateMachine`: declarative states built from commands (uses `lab7GamePiece`) |
| `lab8` | Telemetry: scheduler events, run IDs, per-command runtimes, the live command tree |

Extra NetworkTables controls, all under `/SchedulerLab/`: `lab5InnerTrigger`, `lab7GamePiece`, and
`verboseEvents` (turns the scheduler event stream on).

### What gets published

Robot state goes through the 2027 **Telemetry** API, which writes to NetworkTables under
`/Telemetry/`:

| Path | Contents |
|---|---|
| `/Telemetry/SchedulerLab/Mech2d` | Mechanism2d drawing of the elevator, wrist and intake |
| `/Telemetry/SchedulerLab/Elevator/` | `height`, `commands` (names of the commands that own it right now) |
| `/Telemetry/SchedulerLab/Wrist/` | `angle`, `commands` |
| `/Telemetry/SchedulerLab/Intake/` | `angle`, `rollersOn`, `commands` |
| `/Telemetry/SchedulerLab/Scheduler` | The scheduler's command tree as protobuf. AdvantageScope decodes it, so you can expand it in the sidebar to browse the commands |

You can also watch without a dashboard. The **Driver Station display** shows which lab is
running, plus each mechanism's position and the commands that own it right now. It appears in the
Sim GUI's **Display** window, and on a real robot in the 2027 Driver Station.

## Project layout

```
src/main/java/org/wpilib/schedulerlab/
├── Main.java              # RobotBase.startRobot(Robot::new)
├── Robot.java             # TimedRobot: runs the scheduler and logs telemetry every loop
├── Labs.java              # the eight labs and their NetworkTables bindings
└── mechanisms/            # Elevator, Wrist, Intake (simulated; each implements Mechanism)
src/test/java/...          # headless JUnit tests for the scheduler behaviour the labs rely on
docs/                      # background reading (see below)
```

## Tests

`./gradlew test` runs headless scheduler tests in `src/test/java`. They cover command completion,
priority interruption and rejection, conflict detection, and child-command lifetime. Two
NetworkTables wiring tests check that a lab starts both ways: from a flag set inside the robot
program (as the Sim GUI does), and from a real client connected the way a dashboard would be.

## Commands v3 rules to know

- The WPILib compiler plugin **requires a `coroutine.yield()` directly inside every loop** in a
  command body. A loop that only yields through an inner loop is a *compile error* (see
  `Wrist.sweep()`).
- A mechanism has **no default command** until you set one. Each mechanism here calls
  `setDefaultCommand(idle())` in its constructor.
- `Scheduler.schedule()` returns a `ScheduleResult` record (`Success`, `AlreadyRunning`,
  `LowerPriorityThanRunningCommand`, `LowerPriorityThanQueuedCommand`). You can `switch` over it.
- The v3 scheduler doesn't stop commands when the robot is disabled, so these print and
  NetworkTables-only labs run even while disabled. On a real robot, motor outputs are still
  disabled by the HAL.

## Further reading

- [`docs/Watching-the-Labs.md`](docs/Watching-the-Labs.md): starting labs and viewing the
  mechanism in the Sim GUI, plus opening AdvantageScope on each OS for graphs and command details.
- [`docs/SystemCore-Electrical-Guide.md`](docs/SystemCore-Electrical-Guide.md): what changes
  electrically from the roboRIO, including ports, connectors and pinouts, power, CAN, Smart I/O
  limits, and alpha vs. production units, plus a wiring migration checklist.
- [`docs/Wpilib Changes 2027.md`](docs/Wpilib%20Changes%202027.md): an overview of SystemCore,
  what changed in WPILib for 2027, and what FRC and FTC now share.
- [`docs/WPILib-2027-KB.md`](docs/WPILib-2027-KB.md): a detailed technical reference covering
  project setup, Telemetry, and Commands v3 in depth.
- [WPILib 2027 documentation](https://docs.wpilib.org/en/latest/), including the
  [Commands v3 guide](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/index.html).

## Moving to a newer WPILib release

Alpha releases often contain breaking changes. When a new one comes out:

1. Install it, then in the new VS Code run **WPILib: Import a WPILib 2025-2026 Gradle project**
   to import this folder into a new location. Despite its title, the importer also handles
   earlier 2027 alpha projects, and it applies some code fixes for you.
2. Re-add the Commands v3 vendor library (**WPILib: Manage Vendor Libraries**), then build and
   fix whatever breaks.
3. Read the release notes on the
   [allwpilib releases page](https://github.com/wpilibsuite/allwpilib/releases).

## License

The WPILib template files are covered by the WPILib BSD license in
[`WPILib-License.md`](WPILib-License.md).
