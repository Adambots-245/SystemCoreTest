package org.wpilib.schedulerlab;

import static org.wpilib.units.Units.Seconds;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.wpilib.command3.Command;
import org.wpilib.command3.ParallelGroupBuilder;
import org.wpilib.command3.Scheduler;
import org.wpilib.command3.SchedulerEvent;
import org.wpilib.command3.StateMachine;
import org.wpilib.command3.Trigger;
import org.wpilib.networktables.BooleanEntry;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.schedulerlab.mechanisms.Elevator;
import org.wpilib.schedulerlab.mechanisms.Intake;
import org.wpilib.schedulerlab.mechanisms.Wrist;

/**
 * Eight self-contained labs, each exercising one Commands v3 scheduler concept. Every lab is a
 * command bound (globally) to a boolean on NetworkTables under the "SchedulerLab" table — flip a
 * boolean to true in AdvantageScope/Elastic/Glass (or the sim GUI NetworkTables view) to run a lab.
 * The flag resets itself to false when the lab finishes. No gamepad or hardware is needed.
 */
public final class Labs {
  /** Short lab titles, indexed by lab number, for the Driver Station display. */
  private static final String[] TITLES = {
    "",
    "Coroutine basics",
    "Async composition",
    "Groups & naming",
    "Priorities",
    "Scoped bindings",
    "Conflict detection",
    "StateMachine",
    "Telemetry",
  };

  /** Lab numbers currently running. Labs require no mechanisms, so several can run at once. */
  private final SortedSet<Integer> m_activeLabs = new TreeSet<>();

  private final Elevator m_elevator;
  private final Wrist m_wrist;
  private final Intake m_intake;

  private final NetworkTable m_table =
      NetworkTableInstance.getDefault().getTable("SchedulerLab");
  private final BooleanEntry m_lab5Inner;
  private final BooleanEntry m_lab7GamePiece;
  private final BooleanEntry m_verboseEvents;

  public Labs(Elevator elevator, Wrist wrist, Intake intake) {
    m_elevator = elevator;
    m_wrist = wrist;
    m_intake = intake;

    m_lab5Inner = m_table.getBooleanTopic("lab5InnerTrigger").getEntry(false);
    m_lab5Inner.set(false);
    m_lab7GamePiece = m_table.getBooleanTopic("lab7GamePiece").getEntry(false);
    m_lab7GamePiece.set(false);
    m_verboseEvents = m_table.getBooleanTopic("verboseEvents").getEntry(false);
    m_verboseEvents.set(false);

    // Print interesting scheduler events whenever SchedulerLab/verboseEvents is true (lab 8
    // flips it on temporarily). Mounted/Yielded fire every cycle for every command, so they are
    // filtered out to keep the console readable.
    Scheduler.getDefault()
        .addEventListener(
            event -> {
              if (m_verboseEvents.get()) {
                String line =
                    switch (event) {
                      case SchedulerEvent.Scheduled e -> "scheduled  " + e.command().name();
                      case SchedulerEvent.Completed e -> "completed  " + e.command().name();
                      case SchedulerEvent.Canceled e -> "canceled   " + e.command().name();
                      case SchedulerEvent.Interrupted e ->
                          "interrupted " + e.command().name() + " by " + e.interrupter().name();
                      case SchedulerEvent.CompletedWithError e ->
                          "ERROR in " + e.command().name() + ": " + e.error();
                      default -> null; // Mounted / Yielded: too chatty
                    };
                if (line != null) {
                  System.out.println("      [EVT] " + line);
                }
              }
            });

    bind(1, lab1CoroutineBasics());
    bind(2, lab2AsyncComposition());
    bind(3, lab3GroupsAndNaming());
    bind(4, lab4Priorities());
    bind(5, lab5ScopedBindings());
    bind(6, lab6ConflictDetection());
    bind(7, lab7StateMachine());
    bind(8, lab8Telemetry());
  }

  /** Binds a lab command to SchedulerLab/lab&lt;n&gt; and makes the flag self-resetting. */
  private void bind(int n, Command lab) {
    BooleanEntry flag = m_table.getBooleanTopic("lab" + n).getEntry(false);
    flag.set(false);
    Command wrapped =
        Command.noRequirements(
                co -> {
                  m_activeLabs.add(n);
                  co.await(lab);
                  m_activeLabs.remove(n);
                  flag.set(false);
                })
            .whenCanceled(
                () -> {
                  m_activeLabs.remove(n);
                  flag.set(false);
                })
            .named("Lab" + n);
    // A plain Trigger on the flag, not a NetworkButton: NetworkButton only fires while a remote
    // dashboard is connected, so flipping the flag in the Sim GUI's NetworkTables window (which
    // runs inside the robot program) would do nothing.
    new Trigger(flag::get).onTrue(wrapped);
  }

  /** Describes the labs that are running right now, e.g. "4 Priorities". */
  public String describeActiveLabs() {
    if (m_activeLabs.isEmpty()) {
      return "none - set SchedulerLab/lab1..lab8 to true";
    }
    return m_activeLabs.stream().map(n -> n + " " + TITLES[n]).collect(Collectors.joining(", "));
  }

  private static void banner(int n, String title, String... lines) {
    System.out.println();
    System.out.println("========== LAB " + n + ": " + title + " ==========");
    for (String line : lines) {
      System.out.println("  " + line);
    }
  }

  private static void say(int n, String msg) {
    System.out.println("[LAB" + n + "] " + msg);
  }

  /**
   * ScheduleResult is a sealed interface of records, so a switch can cover every outcome — and the
   * failure records say exactly which command blocked the attempt.
   */
  private static String describe(Scheduler.ScheduleResult result) {
    return switch (result) {
      case Scheduler.ScheduleResult.Success _ -> "SUCCESS";
      case Scheduler.ScheduleResult.AlreadyRunning _ -> "ALREADY RUNNING";
      case Scheduler.ScheduleResult.LowerPriorityThanRunningCommand r ->
          "REJECTED (outranked by running \"" + r.alreadyRunning().name() + "\")";
      case Scheduler.ScheduleResult.LowerPriorityThanQueuedCommand q ->
          "REJECTED (outranked by queued \"" + q.queuedCommand().name() + "\")";
    };
  }

  // ---------------------------------------------------------------------------------------------
  // Lab 1 — Coroutine basics: one imperative body instead of initialize/execute/isFinished/end
  // ---------------------------------------------------------------------------------------------
  private Command lab1CoroutineBasics() {
    return Command.requiring(m_elevator)
        .executing(
            co -> {
              banner(1, "Coroutine basics",
                  "One method body replaces v2's initialize/execute/isFinished/end.",
                  "A plain while-loop is safe BECAUSE it calls coroutine.yield() each pass —",
                  "in v2 this exact loop inside execute() would have frozen the robot.");

              say(1, "raising elevator to 1.5 m with a plain while + yield loop...");
              while (!m_elevator.atGoal(1.5)) {
                m_elevator.stepToward(1.5);
                co.yield(); // hand control back to the scheduler until the next cycle
              }
              say(1, "at 1.5 m. Pausing 1 s with coroutine.wait() (a timed yield)...");
              co.wait(Seconds.of(1));

              say(1, "lowering back to 0 m...");
              while (!m_elevator.atGoal(0)) {
                m_elevator.stepToward(0);
                co.yield();
              }
              say(1, "done — this body ran across hundreds of scheduler cycles.");
            })
        .named("Lab1 body");
  }

  // ---------------------------------------------------------------------------------------------
  // Lab 2 — Async composition: fork / await / awaitAll / awaitAny as plain control flow
  // ---------------------------------------------------------------------------------------------
  private Command lab2AsyncComposition() {
    return Command.noRequirements(
            co -> {
              banner(2, "Async composition (fork/await)",
                  "This parent command requires NOTHING. Children own the mechanisms only",
                  "while they actually run — v3's fix for v2's group-ownership problem.");

              say(2, "fork(wrist.moveTo(30)) — starts concurrently, parent keeps running");
              co.fork(m_wrist.moveTo(30));

              say(2, "await(elevator.moveTo(1.2)) — blocks this body, wrist still moving");
              co.await(m_elevator.moveTo(1.2));
              say(2, "elevator arrived. Current Wrist owners: "
                  + m_wrist.getRunningCommands().stream().map(Command::name).toList());

              say(2, "awaitAll(elevator.moveTo(0.2), wrist.moveTo(150)) — both must finish");
              co.awaitAll(m_elevator.moveTo(0.2), m_wrist.moveTo(150));

              say(2, "awaitAny(wrist.sweep(), 2s wait) — first to finish wins, loser is canceled");
              co.awaitAny(m_wrist.sweep(), Command.waitFor(Seconds.of(2)).named("2s timeout"));
              say(2, "the 2s timeout won; the endless sweep was canceled. Done.");
            })
        .named("Lab2 body");
  }

  // ---------------------------------------------------------------------------------------------
  // Lab 3 — Group builders and forced/auto-generated names
  // ---------------------------------------------------------------------------------------------
  private Command lab3GroupsAndNaming() {
    return Command.noRequirements(
            co -> {
              banner(3, "Groups & naming",
                  "v2 decorators are gone; groups come from builders whose terminal step is a",
                  "NAME — .named(...) or .withAutomaticName(). Names below were auto-generated.");

              Command seq =
                  Command.sequence(m_elevator.moveTo(1.0), m_wrist.moveTo(45))
                      .withAutomaticName();
              say(3, "sequence name:  \"" + seq.name() + "\"");
              co.await(seq);

              Command par =
                  m_elevator.moveTo(0.5).alongWith(m_wrist.moveTo(135)).withAutomaticName();
              say(3, "parallel name:  \"" + par.name() + "\"");
              co.await(par);

              Command race =
                  m_wrist.sweep()
                      .raceWith(Command.waitFor(Seconds.of(1.5)).named("1.5s"))
                      .withAutomaticName();
              say(3, "race name:      \"" + race.name() + "\"");
              co.await(race);

              Command deadline =
                  new ParallelGroupBuilder()
                      .requiring(m_elevator.moveTo(0.0))
                      .optional(m_wrist.sweep())
                      .withAutomaticName();
              say(3, "deadline name:  \"" + deadline.name() + "\"");
              co.await(deadline);
              say(3, "done — these names are what telemetry shows instead of");
              say(3, "\"SequentialCommandGroup\".");
            })
        .named("Lab3 body");
  }

  // ---------------------------------------------------------------------------------------------
  // Lab 4 — Priorities replace v2's binary interruptible flag
  // ---------------------------------------------------------------------------------------------
  private Command lab4Priorities() {
    return Command.noRequirements(
            co -> {
              banner(4, "Priorities",
                  "Equal-or-higher priority interrupts a conflicting command; lower priority is",
                  "REJECTED at schedule time with an explicit ScheduleResult.");

              Scheduler s = co.scheduler();
              Command low = m_elevator.holdAt(0.3, -10, "LowHold(p=-10)");
              Command high = m_elevator.holdAt(1.8, 10, "HighHold(p=10)");

              say(4, "schedule LowHold(p=-10):        " + describe(s.schedule(low)));
              co.wait(Seconds.of(1));

              say(4, "schedule moveTo(1.0) (p=0):     "
                  + describe(s.schedule(m_elevator.moveTo(1.0)))
                  + "  <- interrupts LowHold (0 >= -10)");
              co.wait(Seconds.of(0.5));

              say(4, "schedule HighHold(p=10):        " + describe(s.schedule(high))
                  + "  <- interrupts moveTo (10 >= 0)");
              co.wait(Seconds.of(0.5));

              say(4, "schedule moveTo(0.5) (p=0):     "
                  + describe(s.schedule(m_elevator.moveTo(0.5))));

              say(4, "canceling HighHold; the idle default command takes the elevator back.");
              say(4, "idle() doesn't move anything, so the elevator stays at "
                  + String.format("%.2f m.", m_elevator.getPosition()));
              s.cancel(high);
            })
        .named("Lab4 body");
  }

  // ---------------------------------------------------------------------------------------------
  // Lab 5 — Scoped bindings: a trigger created inside a command dies with the command
  // ---------------------------------------------------------------------------------------------
  private Command lab5ScopedBindings() {
    return Command.noRequirements(
            co -> {
              banner(5, "Scoped bindings",
                  "Bindings record the scope they were created in (Global / OpMode / Command).",
                  "This trigger is created INSIDE a running command, so it lives only as long",
                  "as this lab does.");

              new Trigger(m_lab5Inner::get).onTrue(m_wrist.moveTo(170));

              say(5, "for the next 10 s, setting SchedulerLab/lab5InnerTrigger to true");
              say(5, "moves the wrist to 170 degrees. Try it now...");
              co.wait(Seconds.of(10));

              m_lab5Inner.set(false);
              say(5, "lab over — the binding was auto-unbound with this command's scope.");
              say(5, "Toggling lab5InnerTrigger now does NOTHING. (Try it!)");
            })
        .named("Lab5 body");
  }

  // ---------------------------------------------------------------------------------------------
  // Lab 6 — Conflict detection: parallel members may not share requirements
  // ---------------------------------------------------------------------------------------------
  private Command lab6ConflictDetection() {
    return Command.noRequirements(
            co -> {
              banner(6, "Conflict detection",
                  "Two commands that require the same mechanism can never run in parallel —",
                  "the framework throws when the group is CONSTRUCTED, not at runtime.");

              say(6, "building parallel(elevator.moveTo(1.0), elevator.moveTo(0.0))...");
              try {
                Command bad =
                    Command.parallel(m_elevator.moveTo(1.0), m_elevator.moveTo(0.0))
                        .withAutomaticName();
                say(6, "UNEXPECTED: group built successfully as \"" + bad.name() + "\"");
              } catch (IllegalArgumentException e) {
                say(6, "rejected as expected: " + e.getMessage());
              }

              say(6, "fork() of two conflicting commands throws too...");
              try {
                co.fork(m_wrist.moveTo(10), m_wrist.moveTo(20));
              } catch (IllegalArgumentException e) {
                say(6, "rejected as expected: " + e.getMessage());
              }

              say(6, "a SEQUENCE of the same two commands is fine (they run one at a time):");
              co.await(
                  Command.sequence(m_elevator.moveTo(0.8), m_elevator.moveTo(0.0))
                      .withAutomaticName());
              say(6, "done.");
            })
        .named("Lab6 body");
  }

  // ---------------------------------------------------------------------------------------------
  // Lab 7 — StateMachine: declarative states built on v3 commands
  // ---------------------------------------------------------------------------------------------
  private Command lab7StateMachine() {
    return Command.noRequirements(
            co -> {
              banner(7, "StateMachine",
                  "A declarative state machine that is itself a Command. States wrap commands;",
                  "transitions fire on conditions (rising edge) or on command completion.",
                  "Set SchedulerLab/lab7GamePiece to true to simulate acquiring a game piece.");

              StateMachine sm = new StateMachine("IntakeCycle");
              StateMachine.State deploying = sm.addState(m_intake.deploy());
              StateMachine.State eating = sm.addState(m_intake.runRollers());
              StateMachine.State stowing = sm.addState(m_intake.stow());
              sm.setInitialState(deploying);

              deploying.onEnter(() -> say(7, "state -> DEPLOYING (pivot moving out)"));
              eating.onEnter(() -> say(7, "state -> EATING (rollers on; toggle lab7GamePiece)"));
              stowing.onEnter(() -> say(7, "state -> STOWING (rollers off, pivot returning)"));
              stowing.onExit(() -> say(7, "state machine exiting"));

              deploying.switchTo(eating).whenComplete();
              eating.switchTo(stowing).when(() -> m_lab7GamePiece.get());
              stowing.exitStateMachine().whenComplete();

              // Race the machine against a timeout so the lab can't hang forever if nobody
              // toggles the game-piece flag.
              co.awaitAny(sm, Command.waitFor(Seconds.of(30)).named("Lab7 30s timeout"));
              m_lab7GamePiece.set(false);
              say(7, "done.");
            })
        .named("Lab7 body");
  }

  // ---------------------------------------------------------------------------------------------
  // Lab 8 — Telemetry: scheduler events, run IDs, per-command timing, the live command tree
  // ---------------------------------------------------------------------------------------------
  private Command lab8Telemetry() {
    return Command.noRequirements(
            co -> {
              banner(8, "Telemetry",
                  "Every command (nested ones included) is visible to the scheduler: events,",
                  "unique run IDs, and per-command runtimes. Event stream is on for this lab.");

              m_verboseEvents.set(true);
              Scheduler s = co.scheduler();

              Command routine =
                  Command.sequence(m_elevator.moveTo(0.6), m_wrist.moveTo(60), m_wrist.moveTo(90))
                      .withAutomaticName();
              say(8, "forking \"" + routine.name() + "\" and sampling scheduler state...");
              co.fork(routine);

              int cycle = 0;
              while (s.isScheduledOrRunning(routine)) {
                if (++cycle % 25 == 0) { // roughly every half second
                  say(8, "tree: "
                      + s.getRunningCommands().stream()
                          .map(c -> {
                            Command parent = s.getParentOf(c);
                            return parent == null ? c.name() : c.name() + " (child of " + parent.name() + ")";
                          })
                          .toList());
                  say(8, String.format(
                      "routine runId=%d totalRuntime=%.3f ms, whole scheduler tick=%.3f ms",
                      s.runId(routine), s.totalRuntimeMs(routine), s.lastRuntimeMs()));
                }
                co.yield();
              }

              m_verboseEvents.set(false);
              say(8, "done — the same tree is published as protobuf at");
              say(8, "/Telemetry/SchedulerLab/Scheduler (expand it in AdvantageScope's sidebar).");
            })
        .named("Lab8 body");
  }
}
