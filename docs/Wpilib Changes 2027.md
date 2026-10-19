# FRC 2027: SystemCore, the WPILib Changes, and What It Shares with FTC

*Updated October 2026 for WPILib 2027.0.0-alpha-7 · Based on official WPILib documentation and release notes, and FIRST control-system blogs. For exact APIs, see [`WPILib-2027-KB.md`](WPILib-2027-KB.md).*

---

## Overview

For the **2026–27 season** (the **"2027" WPILib release**), FRC moves to a new control system built around **SystemCore**, a next-generation controller from Limelight that replaces the long-standing **roboRIO**. WPILib describes this as the biggest control-system update since the introduction of the cRIO. It is being designed hand-in-hand with FIRST Tech Challenge so the two programs converge on a shared platform.

There are three parts to the story: the **SystemCore hardware**, the substantial **changes to WPILib** that come with it, and the **unified software experience** now shared between FRC and FTC. All three are covered below, along with where the software stands today, whether you can test it, and how simulation works.

---

## Timeline at a glance

| Program | New controller available to compete | Notes |
|---|---|---|
| FIRST Robotics Competition (FRC) | **2026–27 season** (the "2027" WPILib release) | SystemCore replaces the roboRIO |
| FIRST Tech Challenge (FTC) | **2027–28 season** | One year behind FRC; SystemCore + MotionCore replace the Control Hub |
| Transition / legacy-legal window | Through **at least the 2030–31 season** | Legacy and hybrid setups stay legal to spread out the cost of upgrading |

Multiple WPILib seasons can be installed side by side on the same laptop, the same way they have coexisted since the move to VS Code. Vendors are expected to require new device firmware for the 2027 system, and the 2027 SystemCore software is **not** compatible with the Control Hub or roboRIO.

---

## SystemCore: the hardware

SystemCore is the new controller built by **Limelight**, designed specifically for FIRST. It replaces the **roboRIO** in FRC and the **Control Hub** in FTC.

Core specifications:
- Powered by a **Raspberry Pi Compute Module 5 (CM5)**.
- A **Raspberry Pi RP2350** handles reconfigurable I/O.
- **Quad-core ARM Cortex-A76** processor, **4 GB RAM**, **VideoCore VII GPU**, running **real-time Linux**.
- Built-in ports for **CAN, I2C, USB, and Ethernet**, plus reconfigurable I/O for **PWM, Digital I/O, and Analog Input**.
- Onboard **IMU**, status **LEDs**, and a small **display** (used, among other things, to show the device's IP address).

Reliability was a stated priority: reverse-polarity and short-circuit protection, ESD protection, robust software update/recovery, and keyed, latching connectors so cables can only be plugged in one way and stay put. SystemCore had already logged real competition mileage on FRC robots before its full rollout — used across more than a thousand offseason matches with positive feedback.

---

## Changes to WPILib

The move to SystemCore brings the largest batch of API changes in years. The most consequential ones for existing teams:

### Language and platform
- **Java 25 and C++23** are now the baseline.
- Supported Linux narrows to **only the latest Ubuntu LTS**. Windows 10 is no longer explicitly blocked, but future releases may break compatibility with it without warning, so a move to Windows 11 is advised.

### Package and namespace reorganization
- Java packages move from `edu.wpi.first` to **`org.wpilib`**; C++ namespaces move from `frc::` to **`wpi::`**, with new subpackages for better organization.
- The **VS Code importer** attempts to update existing code for these changes automatically during the import step (any 2026 project must be imported to build under 2027).

### New programming frameworks
- **Commands v3**: a new command-based framework for Java that runs alongside the existing Commands v2. The 2027 docs now include a full Commands v3 guide, and alpha-7 adds `commandv3` and `commandv3skeleton` project templates plus the `hatchbotcmdv3` and `rebuiltcmdv3` examples.
- **OpMode framework**: a new FTC-style program structure that reflects the FRC/FTC convergence, documented under *OpMode Framework* in the 2027 docs.

### Telemetry and Tunables replace SmartDashboard (alpha 7)
- The **SmartDashboard, SendableChooser and Sendable APIs have been removed** from robot code, not just the old dashboard tool.
- **Telemetry** is the new output-only API. It is hierarchical, supports struct and protobuf values, and publishes to NetworkTables under `/Telemetry/`. Objects such as Mechanism2d and Field2d are logged every loop instead of being registered once.
- **Tunables** cover values the dashboard can change. The replacement for SendableChooser is **`Selectable`**, which needs Elastic alpha 9 or newer.

### Hardware support changes (SystemCore-specific)
- **Added:** multiple CAN buses, Smart IO, the onboard IMU, and Expansion Hub support (over USB).
- **Removed (no longer present on SystemCore):** relays, analog output, SPI and SPI-based IMUs (ADIS16448, ADIS16470, ADXL345, ADXRS450), analog gyro, DMA, built-in accelerometer, digital glitch filter, interrupts, counter, ultrasonic, analog trigger, Nidec Brushless, Servo, and Jaguar.

### API cleanups that will touch most codebases
- **NetworkTables v3 removed** — NT4 only. Python users move from `pynetworktables` to `pyntcore`.
- **A single `Gamepad` class** replaced the individual `XboxController`, `PS4Controller`, `PS5Controller`, and `StadiaController` classes (touchpad support added). Alpha 7 brings the individual classes back alongside it, renames face buttons by direction (`faceUp/Down/Left/Right`), and adds a default deadband on axes.
- **`robotInit()` removed**: use the `Robot()` constructor instead.
- **Main class (alpha 7):** robots start with `RobotBase.startRobot(Robot::new)` again. Projects from earlier 2027 alphas used `Robot.class` and must be changed by hand.
- **All constants and enum values are now ALL_CAPS (alpha 7)**, for example `Color.GREEN`.
- **More package moves in alpha 7:** `Preferences` moved to `org.wpilib.preferences`, `Alert` moved to wpiutil, and some geometry classes moved to a `shape` package. **AprilTag and CameraServer are now vendor libraries**, and `AprilTagFields` is part of a new `Fields` class.
- **Timestamps are now nanoseconds (alpha 7).** This is a silent change for `RobotController.getTime()` and similar calls. DataLog and NT still use microseconds in files and on the network.
- **CAN device classes take a `CANPort` enum (alpha 7)**, so SystemCore and MotionCore ports can't be mixed up.
- **`MotorController.set()` renamed to `setThrottle()`**, and `StopMotor()` replaced by `Disable()`.
- **Removed:** `MotorControllerGroup`, `RamseteController`/`RamseteCommand`, and `Mecanum`/`SwerveControllerCommand`.
- The **"Test" robot mode is renamed "Utility"** to reflect broader use.
- Numerous math/geometry changes: swerve and differential kinematics functions are now immutable, geometry classes are marked final, `ChassisAccelerations` and acceleration kinematics added, plus the Kraken **X44** and **Minion** motors added to `DCMotor`.

### New 2027 Driver Station
- A new **multi-platform 2027 FIRST Driver Station** replaces the Windows-only NI Driver Station, running across platforms to match SystemCore's cross-platform direction.

### Tools removed entirely in 2027
These were deprecated in 2026 and are now gone: **Shuffleboard**, **SmartDashboard**, **PathWeaver**, and **RobotBuilder**. The replacements are Elastic, Glass, or AdvantageScope for dashboards, and Choreo, PathPlanner, or Bline for path planning. As of alpha 7, the `SmartDashboard` *class* used in robot code is gone too (see Telemetry above).

---

## Commands v3: key changes

Commands v3 (Java package `org.wpilib.command3`, vendordep `CommandsV3.json`) is a ground-up redesign of the command-based framework. It was introduced for 2027 and runs **alongside** Commands v2 rather than replacing it. It is Java-only. The points below come from the [Commands v3 guide](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/index.html), the design document, and the alpha-7 API. The headline ideas:

### Coroutines replace the init/execute/end/isFinished split
The single biggest change. In v2, a command's logic is spread across separate `initialize()`, `execute()`, `isFinished()`, and `end()` methods. In v3, a command is **one ordinary method** that receives a `Coroutine` and yields control back to the scheduler when it wants to pause:

```java
coroutine -> {
    initialize();
    while (!isFinished()) {
        execute();
        coroutine.yield();   // cede control back to the scheduler each loop
    }
    end();
}
```

This is backed by JDK continuations (a Java 21+ capability), which let the scheduler freeze a command's call stack mid-method ("unmount") and resume it later ("mount"). The practical payoff: command logic reads like normal sequential code, which directly fixes the classic beginner trap where a `while` loop inside `execute()` stalls the entire robot program. The trade-off is that it remains **cooperative** — every periodic loop *must* call `coroutine.yield()`, or it will hang (there is deliberately no preemptive multitasking and no multithreading).

### Async/await-style composition
Nested commands are scheduled with an async/await pattern via the coroutine — `coroutine.await(otherCommand)` runs a command to completion before continuing; commands can also be forked. Sequences and parallel groups can be expressed as plain control flow inside one command body, instead of chains of decorator objects. Forked/awaited commands cannot outlive the command that started them, and there is no longer a `ScheduleCommand`-style "fire and forget" analog.

### Builder API with forced naming (decorators are gone)
v2's decorator chain (each `.withTimeout(...)`, `.andThen(...)` returning a new wrapper object) is replaced by **staged builders**. A command can only be constructed once required configuration — including a **name** — is supplied, and this is enforced by the type system, so a missing name or body is a **compile-time error**, not a runtime surprise. Groups can auto-generate readable names (`"Step 1 -> Step 2"` for sequences, `"(A & B)"` for parallel groups, `"(A | B)"` for races), which makes telemetry far easier to read than the old `"SequentialCommandGroup"` entries.

### Cleaner mechanism ownership ("uncommanded behavior" fix)
A long-standing v2 problem: a group owns every mechanism any of its children need, for the group's whole duration, even while not actually driving that mechanism. v3 lets a nested command use a mechanism the parent doesn't require, and the parent only holds that mechanism **while the inner command is actually running**. Effectively all child commands behave like v2's "proxied" commands, without the boilerplate — though the built-in `ParallelGroup`/`Sequence` still take full ownership for safety, and a child can never interrupt its own parent.

### Priority levels instead of a binary interrupt flag
Commands carry a priority. A new command interrupts a running one that shares a mechanism only if it has **equal or higher priority**; higher-priority work can't be preempted by lower-priority work. This gives finer control than v2's single interruptible/non-interruptible flag (a common use case is LED/status indication where error states should win).

Priority conflicts can also be inspected. `Scheduler.schedule()` returns a `ScheduleResult` record that says whether the command was scheduled and, if not, which command blocked it. Since alpha 7, a command can choose to handle a failed `fork`/`await` itself instead of being canceled.

### Suspend and resume (designed, not yet available)
The design document describes **suspending** a command while a higher-priority command runs and **resuming** it afterward. As of alpha 7 this isn't implemented: an interrupted command is canceled.

### Mechanisms and default commands
Since alpha 7, `Mechanism` is an **interface** (`class Elevator implements Mechanism`). A mechanism has **no default command** until you set one, usually `setDefaultCommand(idle())`.

### Scoped triggers, bindings, and default commands
Bindings now have a **scope** — `Global`, `OpMode`, or `Command`. A trigger binding, manually scheduled command, or default-command override created inside a scope is automatically torn down when that scope exits (its commands canceled, default commands reverted to the enclosing scope's). This addresses the v2 sprawl of globally visible triggers that could fire at unintended times; for example, a binding created inside an autonomous routine only exists while that routine runs.

### Better transparency and telemetry
Every command, including nested ones, is visible to the scheduler, not just top-level commands. The `Scheduler` is protobuf-serializable: each command's `id`, `parent_id`, name, priority, required mechanisms, and timing are included, so tools can rebuild the full command tree and show the time spent in each command. AdvantageScope already decodes the data, although there is no dedicated command-tree view yet. An event listener API reports every schedule, interruption, completion, and cancellation.

### Ties into the new OpMode structure
v3 is built to work with the new FTC-style structure: `OpModeRobot`, `PeriodicOpMode`, a unified `CommandGamepad`, and `Scheduler.getDefault().run()` in the periodic loop. This supports the FRC/FTC convergence.

**Bottom line for teams:** v3 makes command logic look like ordinary code, enforces naming and validity at compile time, fixes the old mechanism-ownership quirks, and adds priorities and scoped bindings. Because v2 remains available, adoption can be incremental — but new concepts (coroutines, mandatory yielding, builders over decorators) mean it is a genuine relearn, not a drop-in rename.

---

## Is it available to test?

Yes — the 2027 software is publicly available in **alpha**, and the documentation is live.

- **WPILib 2027 alpha installers** are published on the [allwpilib releases page](https://github.com/wpilibsuite/allwpilib/releases). The WPILib [`SystemcoreTesting`](https://github.com/wpilibsuite/SystemcoreTesting) repository links all the alpha/beta software, OS images, and compatible vendor libraries. The current release is **Alpha 7** (September 2026). It requires **SystemCore image 14 or newer** and the **FIRST Driver Station Alpha 7**, and vendor libraries built for Alpha 6 or earlier do not work with it.
- The **2027 documentation** is published at `docs.wpilib.org` under the *2027* / *latest* version, including a *New for 2027* changelog and *Known Issues* list.
- **The alpha software is not compatible with the roboRIO or Control Hub** — it targets SystemCore only.
- **SystemCore hardware** itself went to selected **alpha test teams** rather than general retail. FRC alpha teams were announced in 2025; the FTC alpha application closed in November 2025. Anyone can follow progress through the public GitHub project even without a unit.

A practical implication: you do **not** need SystemCore hardware to start exploring the 2027 software. The alpha WPILib install plus desktop simulation lets you exercise much of the new API today (with the caveats noted below).

---

## Simulation and running code

The familiar desktop workflow carries over, and simulation is being actively updated for 2027.

- **Robot simulation still works as before.** The Simulation GUI, **device simulation**, **physics simulation**, and **unit testing** are all still part of the 2027 toolchain. Recent alphas added **onboard IMU simulation** for SystemCore. In alpha 7 the sim GUI and desktop tools switched to SDL3 (matching the 2027 Driver Station) and gained support for the Driver Station text display.
- **Build, deploy, and debug in VS Code are unchanged in shape.** You create a project, deploy, and view console output the same way; the import step migrates 2026 projects (including the package/namespace renames) on first open.
- **One current alpha limitation to be aware of:** the "real Driver Station" simulation option is temporarily a no-op (the HalSim DS extension was disabled to fix a crash), with support to be restored in a later release. Standard simulation is unaffected.
- Because NT3 is gone, dashboards and tools used with simulation must be NT4-compatible (Glass, Elastic, AdvantageScope all are).

In short: yes, you can simulate and run code much as before — the project, deploy, and simulation loop is intact, with the API and tooling updated for SystemCore.

---

## What SystemCore shares with FTC

Historically, FRC and FTC used entirely different hardware *and* different software stacks (FRC on roboRIO + WPILib; FTC on the Control Hub + the FTC SDK with OnBotJava/Blocks). The new control system deliberately unifies the two programs around shared building blocks.

### Shared hardware: SystemCore is the common component
**SystemCore is the single component used by both FRC and FTC.** FIRST describes the new design as *modular* so the same core controller serves both programs while each adds only what it needs. SystemCore is the "brain" in both cases — main processing plus core I/O — replacing the roboRIO in FRC and the Control Hub in FTC. This shared core underpins FIRST's stated goal of **"technology parity"** between the programs.

### Shared software: one unified WPILib experience
**WPILib becomes the software platform for both FRC and FTC** on SystemCore. FIRST is working with Limelight, the WPILib team, and the FTC tech team on a single cross-platform software experience. The new **OpMode framework** in WPILib is a direct sign of this convergence, mirroring FTC's structure.

Practical implications:
- **One library, two programs.** FTC teams move onto WPILib rather than the separate FTC SDK — same APIs, same command-based framework, same tooling.
- **Advanced capability transfers to FTC.** Mature FRC features such as **swerve drive support**, kinematics/odometry, pose estimation, and trajectory following become available to FTC teams without being rebuilt.
- **CAN comes to FTC.** Used in FRC since 2010, CAN arrives in FTC for the first time, enabling high-speed two-way communication and largely replacing I2C for sensors.
- **A cross-platform driver application.** Both programs use a desktop driver-station application running on modern Windows, Linux, and macOS.
- **Skills transfer between programs.** A student moving from FTC to FRC, or a mentor working across both, carries the same language, libraries, and mental model.

### A useful way to frame it
SystemCore + WPILib is the **shared platform layer** common to both programs, with each program adding a program-specific layer on top. FRC keeps its existing motor-controller and power-distribution ecosystem; FTC gets its own purpose-built additions (next section). The brain and the code are common; the surrounding electronics differ by program.

---

## What FTC adds on top of SystemCore

An FTC robot in the new system uses five main components, most of which are FTC-specific:

1. **SystemCore** — the shared brain.
2. **MotionCore** — an FTC-specific power and CAN distribution board. It distributes battery power and CAN, with 20 device ports for point-to-point wiring to motors/sensors (avoiding the daisy-chain wiring that has frustrated FRC CAN users) plus 3 quadrature encoder ports. It talks to SystemCore over higher-speed CAN FD and includes extensive protection (reverse polarity, overvoltage, ESD, per-bank short-circuit and current limiting, temperature monitoring).
3. **FIRST A301 Actuator** — a new CAN brushless smart motor designed specifically for FTC, intended to eventually replace *all* existing FTC motors and servos as the single legal actuator. Developed with REV Robotics.
4. **Battery Pack and Dock** — a move to an approved **18V (5S) lithium power-tool battery** in a robot-mounted dock, replacing current NiMH batteries. (The higher voltage means some 12V sensors will need voltage regulators.)
5. **Driver Station Device** — for now a cross-platform desktop application; the existing REV Driver Hub remains legal after a software update during the transition.

FRC does **not** use MotionCore, the A301, or the FTC battery system — it retains its own power distribution, motor controllers, and battery. **The shared element between the two programs is SystemCore (hardware) and WPILib (software), not the full electronics stack.**

---

## Practical implications

**For FRC teams (2026–27 season):**
- Plan to adopt SystemCore and the updated WPILib workflow in place of the roboRIO.
- Budget time for the API migration. Most existing codebases will be affected by the `org.wpilib` / `wpi::` renames, NT4-only, the gamepad changes, the `robotInit()` removal, the removed commands and controllers, and (from alpha 7) SmartDashboard → Telemetry/Tunables, ALL_CAPS constants, and `CANPort`. The importer handles much of it, but not all.
- Move off the removed tools (Shuffleboard, SmartDashboard, PathWeaver, RobotBuilder) to modern equivalents.
- Coordinate device-firmware updates with vendors; 2027 software does not run on the roboRIO.

**For FTC teams (2027–28 season):**
- FTC faces the largest conceptual shift: a new controller (SystemCore), new distribution (MotionCore), a new motor standard (A301), a new battery, *and* a new software library (WPILib in place of the FTC SDK).
- The upside is direct access to FRC's mature software and a skill set that transfers cleanly between programs.

**Getting a head start now:**
- The 2027 alpha WPILib and the 2027 docs are available today; desktop simulation lets teams learn the new API without owning SystemCore.

**Budgeting:**
- The legacy/hybrid-legal window through at least the 2030–31 season is designed to let teams spread out the investment.

**One-sentence summary:**
> With the 2027 control system, FRC and FTC converge on a shared brain (**SystemCore**) and a shared codebase (**WPILib** — now Java 25/C++23, NT4-only, reorganized namespaces, and new Commands v3 and OpMode frameworks), giving FTC access to FRC's advanced software while each program keeps its own program-specific electronics.

---

## Sources

- WPILib Documentation — *New for 2027* changelog (docs.wpilib.org, latest/2027)
- WPILib Documentation — *Systemcore Introduction* (2027)
- WPILib Documentation — *Known Issues* and *Robot Simulation* (2027)
- WPILib — *2026 Kickoff Release of WPILib* blog
- WPILib `SystemcoreTesting` GitHub repository (alpha/beta software and tooling)
- WPILib — *The 2027 FIRST Driver Station* blog
- FIRST — *Control System Update: FIRST Tech Challenge Edition* (Dec 2025)
- FIRST — *Introducing the Future Mobile Robot Controller*
- FIRST Mentor Conference — *SystemCore/WPILib 2027*
- WPILib Documentation: *Commands v3 Programming* (2027)
- WPILib `allwpilib`: *v2027.0.0-alpha-7 release notes*, the *Commands v3 design document*, the `org.wpilib.command3` API, the *Telemetry* and *Tunables* migration guides, and the `hatchbotcmdv3` / `rebuiltcmdv3` examples
- Chief Delphi — *SystemCore / MotionCore rollout questions* and *WPILib Commands v3 Championship Conference* discussions
