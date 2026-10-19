# Running and Watching the Labs

You can do everything in the **Sim GUI**, the window that opens when you simulate the robot:
start labs, watch the mechanism move, and read the live status. **AdvantageScope** is an
optional extra for graphs and for seeing which command owns each mechanism.

These steps are for **WPILib 2027 alpha-7**.

---

## Part 1: The Sim GUI (no extra tools)

### Start the simulation

In the 2027_alpha7 WPILib VS Code, press **Ctrl+Shift+P** (**Cmd+Shift+P** on a Mac), run
**WPILib: Simulate Robot Code**, and pick the GUI option if it asks. You can also run
`gradlew run` in the terminal (`./gradlew run` on macOS/Linux). The **Robot Simulation** window
opens.

### Start a lab

1. Find the window titled **NetworkTables** inside the Sim GUI. If you closed it, reopen it
   from the top menu with **NetworkTables → NetworkTables View**.
2. Expand **Transitory Values → SchedulerLab**.
3. Next to **lab1**, change the dropdown from **false** to **true**. The lab starts right away,
   and its explanation prints in the VS Code terminal.
4. When the lab finishes, the flag switches itself back to **false**. Set it to **true** again to
   rerun the lab.

Labs 5 and 7 also use **lab5InnerTrigger** and **lab7GamePiece**, which you change the same way.
Lab 7 waits for you to "pick up" a game piece.

### See the mechanism

1. In the top menu, open **NetworkTables → Telemetry → SchedulerLab → Mech2d**.
2. A window opens showing:
   - the **orange** elevator (the bar gets longer as it rises),
   - the **purple** wrist on top of it,
   - the **gray** intake on a short dark post at the left. It stands upright when stowed,
     swings out flat when deployed, and turns **green** while its rollers run (lab 7).
3. Start lab 1, 2 or 3 and watch the parts move.

Drag and resize the windows however you like. The Sim GUI remembers the layout for next time.

### Read the live status

The Sim GUI's **Display** window shows which lab is running, each mechanism's position, and the
commands that own each mechanism right now. On a real robot the same text appears in the 2027
Driver Station. The full explanation of each lab step is printed in the **VS Code terminal**.

---

## Part 2: AdvantageScope (optional: graphs and command details)

AdvantageScope is good at **graphing** values over time and at browsing every published value.
It comes with the WPILib install.

> **Known issue:** the AdvantageScope bundled with WPILib alpha-7 (v27.0.0-alpha-6) can't draw
> alpha-7 Mechanism2d data yet. Its **Mechanism** tab stays blank ("0 segments"). WPILib alpha-7
> changed the Mechanism2d data format, and AdvantageScope's fix isn't in a release yet. Use the
> Sim GUI's Mechanism2d window (Part 1) to see the drawing. Everything else in AdvantageScope
> works.

### Open AdvantageScope

First try **WPILib: Start Tool** in VS Code. In alpha-7, AdvantageScope usually **isn't listed**
there. That's a known gap in the alpha installer and doesn't mean anything is broken. Open it
directly instead:

**Windows**
- **Start menu:** open Start, scroll to the **2027_alpha7 WPILib Tools** folder, and click
  **AdvantageScope (WPILib) 2027_alpha7**. The same shortcut is in the **2027_alpha7 WPILib
  Tools** folder on your desktop.
- If there's no shortcut, open File Explorer, go to
  `C:\Users\Public\wpilib\2027_alpha7\advantagescope\`, and double-click
  **`AdvantageScope (WPILib).exe`**.
- Once it's running, right-click its taskbar icon and choose **Pin to taskbar**.
- If the `advantagescope` folder is missing or empty, run the WPILib installer again. As a last
  resort, download `advantagescope-wpilib-win-x64.zip` from the
  [v27.0.0-alpha-6 release](https://github.com/Mechanical-Advantage/AdvantageScope/releases/tag/v27.0.0-alpha-6)
  and extract it into that folder.

**macOS**
1. In Finder, choose **Go → Go to Folder…** and enter `~/.wpilib/2027_alpha7/advantagescope`.
2. If the folder only contains a `.tar.gz` file, double-click it to extract it. The installer
   often fails to do this on macOS.
3. Open **AdvantageScope (WPILib).app**.

*Optional: to make it appear in Start Tool,* run this in Terminal and then reload VS Code
(**Developer: Reload Window**):

```sh
cd ~/.wpilib/2027_alpha7/tools
ln -sfn "../advantagescope/AdvantageScope (WPILib).app" AdvantageScope.app
```

**Linux:** look for **AdvantageScope (WPILib) 2027_alpha7** in your applications menu, or run
`~/.local/share/wpilib/2027_alpha7/advantagescope/advantagescope-wpilib`.

### Connect and use it

1. With the simulation running, choose **File → Connect to Simulator → Default**
   (**Ctrl+Shift+K**, or **Cmd+Shift+K** on a Mac). The sidebar fills with tables, including
   **SchedulerLab** and **Telemetry**.
2. **Start a lab from AdvantageScope (optional):** click the **slider icon** next to the sidebar
   search box to turn on **Tuning Mode** (it turns purple). Expand **SchedulerLab** and click the
   red circle next to a lab. AdvantageScope can't change anything until Tuning Mode is on.
3. **Graph the motion:** click **+** at the top and choose **Line Graph**. Drag
   **Telemetry → SchedulerLab → Elevator → height** and **Wrist → angle** onto the graph. Run
   **lab 4** (Priorities) and watch the elevator get pulled to different heights as
   higher-priority commands take over.
4. **See which command owns each mechanism:** under **Telemetry → SchedulerLab**,
   **Elevator / Wrist / Intake → commands** lists the current owners, for example
   `Elevator[IDLE]`. **Scheduler** expands into the full command tree.

### On a real SystemCore

With the code deployed to a SystemCore that's plugged in over USB-C, open the **File** menu.
Choose **Use Systemcore USB Address**, then **Connect to Robot** (**Ctrl+K**, or **Cmd+K** on
a Mac).

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Changing a lab flag to **true** does nothing | Check that the simulation is still running and the VS Code terminal shows no error. Run only one simulation at a time. |
| The **NetworkTables** window is missing in the Sim GUI | Use the top menu: **NetworkTables → NetworkTables View**. |
| **Telemetry** isn't in the Sim GUI's NetworkTables menu | The robot program hasn't started publishing yet. Wait a second after the window opens, then try again. |
| AdvantageScope's **Mechanism** tab is blank ("0 segments") | Known issue with the alpha-7 bundle (see Part 2). Use the Sim GUI's Mech2d window. |
| AdvantageScope isn't in **WPILib: Start Tool** | Normal in alpha-7. Open it from the shortcut or the install folder (see Part 2). |
| AdvantageScope's sidebar is empty | The simulation isn't running, or you chose **Connect to Robot** instead of **Connect to Simulator**. |
| Clicking a flag in AdvantageScope does nothing | Tuning Mode is off. Click the slider icon until it is purple. |
| Windows asks to allow network access the first time | Allow it on **private** networks. The dashboard and the robot program talk over the network, even on the same computer. |

## Other tools

- **Glass** has the same NetworkTables window and Mechanism2d view as the Sim GUI, and it
  connects to a real robot as well.
- **Elastic** can start labs with a **Toggle Switch** widget for each `SchedulerLab/labN`. It
  doesn't appear in Start Tool in alpha-7 either. Use the **Elastic (WPILib) 2027_alpha7**
  shortcut on Windows, or the `elastic` folder next to `advantagescope`.
