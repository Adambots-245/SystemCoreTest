# SystemCore Electrical Guide: What Changes from the roboRIO

*Compiled October 2026 from Limelight's SystemCore specification and cable documents, FIRST
Community blog posts, the WPILib 2027 docs and alpha-7 source code, the WPILib SystemcoreTesting
repository, the 2026 FRC inspection checklist, and public alpha-tester reports. Sources are listed
at the end.*

> **Read this first: alpha units and production units are different.**
> Teams in the 2025–26 alpha test used **alpha** SystemCores. The units FRC teams get for 2027
> (sold from **November 18, 2026**, and also included in the 2027 Kickoff Kit) are **production**
> units with different power and CAN connectors. This guide points out where they differ.
> Much of the detailed electrical data still comes from Limelight's **alpha** spec sheet,
> because we couldn't find a published production spec sheet. The **2027 game manual** (released at
> Kickoff) decides what is legal. Where this guide quotes rules, they are the **2026** rules,
> used as a baseline.

---

## 1. The big picture

SystemCore replaces the roboRIO as the robot controller. It's about the size of a large
smartphone. Most of the robot's electrical system **stays the same**:

| Stays the same | Changes |
|---|---|
| Battery, 120 A main breaker, main power wire sizes | How the controller itself is powered (new connector, cable in the box) |
| Power distribution: CTRE PDP / PDP 2.0, REV PDH, AndyMark AMPD | CAN: **5 separate buses** instead of 1, with a new connector on production units |
| CAN motor controllers, pneumatics hubs (PCM / PH), CAN sensors | PWM, DIO and analog ports merge into **6 "Smart I/O" ports** with new 3-pin connectors |
| VH-109 radio, Ethernet to the radio | **Analog inputs read 0–3.3 V** (roboRIO: 0–5 V) |
| Robot Signal Light (same approved models) | RSL uses a new 2-pin connector, powered at 10 V by SystemCore |
| Wire-gauge and breaker rules | No MXP, SPI, relay or analog-output ports; no power for servos |

The usual connector type is now the **Molex SL** family, with latching housings and gold-plated
pins. Old 0.1" "PWM-style" plugs don't latch into it.

---

## 2. Port map

### Production SystemCore (2027)

| Port | Count | Connector | What it's for |
|---|---|---|---|
| **Power / Bridge** | 1 | 4-pin **Molex Micro-Fit+** (black, "A" polarization) | Power input on production units. Also carries CAN to an FTC MotionCore. |
| **CAN** | 5 | 2-pin **Molex SL** | Five independent CAN-FD-capable buses (`can_s0`–`can_s4`) |
| **Smart I/O** | 6 | 3-pin **Molex SL** | Each port: digital in/out, PWM out, analog in, addressable LED, edge counting; encoders use pairs |
| **I2C** | 2 | 4-pin **Molex SL** | I2C sensors (pinout differs from the roboRIO, see §6) |
| **RSL** | 1 | 2-pin **Molex SL** | Robot Signal Light, 10 V |
| **USB-A** (host) | 4 | USB 3.0 Type-A | Cameras, USB drives for logs, CANivore, REV Expansion Hub |
| **USB-C "LINK"** (device) | 1 | USB-C, data only | Laptop tether for programming, and OS recovery |
| **Ethernet** | 1 | RJ45, 1 Gbps | To the VH-109 radio's RIO port |
| **M.2** | 1 | A+E key, PCIe 2.0 | Accelerators such as Hailo-8. The cover must stay installed. |
| **Config button** | 1 | – | New on production units |
| Built in | – | – | IMU, 0.96" OLED display, 3 status LEDs, 2.4/5 GHz Wi-Fi |

On **alpha** units, power and the 5 CAN ports used **Weidmuller push-in terminals** with ferruled
wires instead (see §3 and §4). Alpha units also differ in their digital-input pull resistors and
have no Config button. See the comparison table in §13.

### roboRIO → SystemCore at a glance

| roboRIO 2 | SystemCore |
|---|---|
| 10 PWM + 10 DIO + 4 analog in + MXP expansion | **6 Smart I/O total**, each configurable |
| 1 CAN bus | **5 CAN buses** |
| 1 I2C (+ MXP) | 2 I2C |
| SPI, RS-232, 4 relay, analog out, MXP | **Not available** |
| 2 USB-A host, USB-B device | 4 USB-A host (USB 3.0), USB-C device |
| PWM header provides 6 V for servos | Smart I/O supplies only **3.3 V** (1 A shared). No servo power. |
| PWM signal is 5 V logic | PWM signal is **3.3 V** logic |
| Analog in 0–5 V | Analog in **0–3.3 V** |
| Built-in accelerometer only | Built-in IMU (gyro + accelerometer) |

---

## 3. Powering SystemCore

### Production units

- Power goes into the **4-pin Micro-Fit+ "Bridge" port**. FIRST says **production units ship
  with power input cables**, so use the supplied cable. Don't make your own until the official
  FRC wiring guide is out.
- **Micro-Fit+ pinout**, looking at the cable connector (from Limelight's cable spec):

  | Pin | Signal | Wire color |
  |---|---|---|
  | 1 | CAN L | Green |
  | 2 | CAN H | Yellow |
  | 3 | **+V (battery voltage)** | **Red** |
  | 4 | **GND** | **Black** |

  Power-only cables leave pins 1 and 2 empty. Limelight's spec includes a 1.2 m
  **bare-wire power cable** (20 AWG twisted red/black) and an **XT30** adapter cable.
- **Wire it straight to the power distribution board at battery voltage.** The WPILib
  SystemcoreTesting instructions warn: *"Do not use a regulator to power the device. It expects to receive battery voltage directly.
  Some regulators (such as the VRM) cannot provide enough power to the device under full load."*
- **2026 rule baseline (R615):** the robot controller had to be connected directly to the PD's
  power terminals through a **10 A fuse or breaker**. Expect the 2027 rule to be similar. Check
  the 2027 manual.

### Alpha units

- Power goes into a 2-position **Weidmuller push-in terminal** (Weidmuller 1825640000), using
  **18 AWG wire with ferrules**. The SystemcoreTesting instructions recommend white Weidmuller
  ferrules.
- **Never power an alpha unit from both the Weidmuller terminal and the Bridge port at the same
  time.**

### Electrical ratings (alpha spec sheet)

| Item | Value |
|---|---|
| Input range | **5–26 V** (survives 4.5–35 V). Works from a 12 V FRC battery or an 18 V FTC pack. |
| Power use | 6 W idle, **40 W max** |
| Protection | Reverse-polarity protection; ESD protection on every port |
| Undervoltage lockout | 3.69 V |
| **Brownout** | **6.3 V** by default and configurable (one table in the spec says "6 V nominal"). Restores at 7.5 V. During brownout, **PWM and digital outputs are turned off**. For comparison, the roboRIO 2 defaults to 6.75 V and the original roboRIO uses 6.3 V. A roboRIO brownout also disabled CAN motor controllers and pneumatics. SystemCore's spec mentions only PWM and digital outputs. |

Alpha teams reported brownouts from short voltage dips under hard driving. Use well-charged
batteries (one test team swaps any battery resting under 12 V), keep the main power wiring short
and solid, and set current limits on your motor controllers.

---

## 4. CAN: five buses instead of one

| | Detail |
|---|---|
| Buses | **5** on SystemCore, named `can_s0`–`can_s4`. In code, choose one with WPILib's `CANPort` enum. Every CAN device class now takes a `CANPort`, and so do vendor libraries such as Phoenix 6 and REVLib. |
| Connector | **Production:** 2-pin Molex SL. **Alpha:** Weidmuller push-in, **22 AWG ferruled** wire. |
| SL CAN pinout | Pin 1 = **CAN H (yellow)**, pin 2 = **CAN L (green)** |
| Termination | **120 Ω built in at the SystemCore end of every bus.** Each bus still needs a terminator at its *far* end. For example, use the REV PDH's configurable termination, the CTRE PDP's termination jumper, or a 120 Ω resistor. |
| Wire | Twisted pair, 40–80 twists per meter. Yellow = CAN H, green = CAN L (the same colors as before). |
| Speed | The hardware supports **CAN FD up to 8 Mbps**. As of October 2026 the buses run classic CAN at **1 Mbps** by default. FD mode can only be set from a terminal on the SystemCore. There's no dashboard option and no saved setting, and replies on the SystemcoreTesting issue say the FD negotiation isn't settled yet. |
| CANivore | Still supported. Plug it into a USB-A port and install CTRE's `canivore-usb` package from the web dashboard. |

**What this means for wiring:**
- Each of the 5 buses is a **separate daisy chain** with its own far-end terminator. Don't join
  buses together.
- Plan which devices go on which bus. For example: swerve drive on bus 0, intake and shooter on
  bus 1, PD and pneumatics on bus 2. Splitting the load lowers each bus's utilization. Write
  the plan down and label the cables, because code must name the right `CANPort` for each
  device. Limelight suggests colored sleeves, Velcro straps or rubber bands to tell the buses
  apart.
- On production units, your devices' CAN pigtails need Molex SL plugs to reach SystemCore.
  FIRST says the **2027 Kickoff Kit black tote includes complete Molex SL cables and parts**.

---

## 5. Smart I/O: six ports do the work of PWM, DIO and analog

The roboRIO had separate PWM, DIO and analog headers. SystemCore has **6 Smart I/O ports**, and
code decides what each one does.

**Pinout** (3-pin Molex SL, from Limelight's cable spec):

| Pin | Signal | Wire color |
|---|---|---|
| 1 | GND | Black |
| 2 | VCC (**3.3 V** out) | Red |
| 3 | SIGNAL | White |

### What each port can do

| Use | Notes |
|---|---|
| **Digital input** (limit switch, beam break, button) | 3.3 V logic. Inputs tolerate up to 24 V. **Production units use pull-ups, like the roboRIO**: wire switches between SIGNAL and GND as before. **Alpha units have 4.7 kΩ pull-downs**: wire the switch between SIGNAL and **3.3 V** instead, or add a pull-up of about 400 Ω. |
| **Digital output** | 3.3 V logic, 10 mA max per signal pin |
| **PWM output** (PWM motor controllers) | Up to 6 at once. The signal is **3.3 V** (the roboRIO's was 5 V). Test any older PWM device before relying on it. **No power is supplied for servos** (see below). |
| **Analog input** (potentiometer, analog sensor) | **0–3.3 V only** (roboRIO: 0–5 V). Power potentiometers from the port's 3.3 V pin so the full range fits. A 5 V sensor needs a voltage divider. Known issue: on **alpha** units, analog readings slowly drift upward. |
| **Quadrature encoder** | Uses **two** ports, and only as the pairs **0+1, 2+3 or 4+5**. That means **3 encoders at most**, which would use all six ports. Prefer CAN encoders or encoders built into the motor. |
| **Absolute (duty-cycle) encoder** | One port each, e.g. a REV Through Bore in absolute mode |
| **Edge counter / tachometer** | One port each (`EdgeCounter`, `Tachometer` in alpha-7) |
| **Addressable LEDs** | The data signal is 3.3 V. **Power the LED strip from its own supply, not from the Smart I/O 3.3 V pin.** |

### Power budget and limits
- **All Smart I/O and I2C ports share one 1 A, 3.3 V supply.** If you go over, the supply shuts
  off and reports a fault until you power-cycle the robot.
- Signal pins are limited to 10 mA each.
- During brownout, PWM and digital outputs are turned off.

### Servos
WPILib's `Servo` class has been removed, because *"Systemcore doesn't have the ability to give
servos the power they demand."* Use a **REV Servo Power Module** (it passes the signal through
and supplies 6 V power) or a CAN servo controller such as the **REV Servo Hub**.

### Plan your six ports early
Six ports go fast. A sample allocation:

| Port | Use |
|---|---|
| 0 + 1 | Quadrature encoder (only if you really need it) |
| 2 | Intake beam break |
| 3 | Elevator bottom limit switch |
| 4 | Absolute encoder for the arm |
| 5 | Addressable LED data |

Anything more has to go elsewhere, mostly on CAN: CAN encoders, the limit-switch inputs on CAN
motor controllers, CAN sensors, or I2C sensors. WPILib also supports a REV Expansion Hub over
USB, which adds ports. Check whether it's legal in FRC.

### Re-terminating your cables
Old PWM, DIO and limit-switch cables with 0.1" housings won't latch into Molex SL ports.
- Buy pre-made Molex SL cables (some are in the Kickoff Kit), or re-terminate your own:
  - 3-pin housing **Molex 50579403**; 2-pin **50579402**; 4-pin **50579404**
  - Gold-plated female crimp terminals **Molex 16020103** (bag) or **16020087** (reel), for
    22–24 AWG. These are the standard-retention terminals Limelight's cable spec selects. The
    older alpha spec sheet lists high-retention 16021115/16021111, which are also gold.
  - Molex hand crimp tool **63811-8700**. One alpha team reported their existing PWM crimper
    "worked perfectly fine".
- **Use gold-plated terminals only.** SystemCore's pins are gold, and mixing gold with tin risks
  galvanic corrosion and worse signal quality.
- Leave about 12.7 mm (½") of free wire behind each housing before any heat shrink or tie, to
  reduce strain on the crimps.

---

## 6. I2C

- **2 ports**, 4-pin Molex SL, running at 100 kHz (400 kHz fast mode available).
- **Pinout:** pin 1 GND (black), pin 2 VCC 3.3 V (red), pin 3 **SDA** (blue), pin 4 **SCL**
  (yellow).
- **SDA and SCL are swapped compared with the roboRIO.** Existing cables such as REV's 4-wire
  I2C cable (used with the REV Color Sensor) need **SCL and SDA swapped**. The SystemCore pinout
  matches SparkFun Qwiic and the REV Control/Expansion Hub, and Limelight's spec includes
  ready-made Qwiic and JST adapter cables.
- I2C power comes from the same shared 1 A, 3.3 V supply as Smart I/O.

---

## 7. Robot Signal Light (RSL)

- Same approved lights as before: **Allen-Bradley 855PB-B12ME522** or **AndyMark am-3583**.
- 2-pin Molex SL port. SystemCore supplies **10 V, 100 mA max**. It flashes while enabled and
  stays solid while disabled. Follow the "10V / GND" label printed next to the port.
- The old roboRIO RSL plug won't fit, so make or buy a 2-pin SL cable.
- 2026 rule baseline (R709): at most two RSLs. They must be easily visible from 36 in. (91 cm)
  away on at least one side of the robot, plugged into the controller's RSL port, and flash in
  sync with it. Check the 2027 manual for the current wording.

---

## 8. USB ports

- **4× USB-A 3.0**, sharing **2 A** and 5 Gbps of bandwidth. Uses: USB cameras, a USB drive for
  logs (mounted at `/U`, `/V`, …), a CANivore, or a REV Expansion Hub.
- Known issue: on alpha and beta units, USB 3 drives can mount unreliably. The SystemcoreTesting
  known-issues table lists production units as not affected.
- **USB-C "LINK" port:** data only. It doesn't power SystemCore. A laptop tethered here reaches
  SystemCore at **172.26.0.1 (Windows)** or **172.27.0.1 (macOS/Linux)**.
  - **Alpha units:** if the USB-C cable is plugged in *before* power is applied, SystemCore boots
    into **flash (recovery) mode**. Connect the laptop after powering on.
  - **Beta units:** flash mode needs the **Config button** held down at power-on. FIRST says
    production units add the same Config button, so expect the same procedure. Confirm in the
    production instructions.
  - While tethered over USB, reaching devices on the radio's network (cameras, coprocessors)
    needs port forwarding.

---

## 9. Ethernet and the radio

- **One Ethernet port.** Connect it to the radio as you did the roboRIO. SystemCore gets
  **10.TE.AM.2** from the radio. 2026 rule baseline (R703): a **VH-109 v1.5** (blue holographic
  sticker) connects at its RIO port. A **VH-109 v1.0** needs a PoE injector or modified Ethernet
  cable, or else use an AUX port with its DIP switch off (the default).
- **Radio power is unchanged.** 2026 rule baseline (R616): the radio is powered by injection
  into its RIO port and/or by 12 V straight from the PD into the radio's 12 V terminals. If the
  radio is fed only through its 12 V terminals, the 2026 docs say the controller should connect
  through a PoE injector, a modified cable, or an AUX port. Recheck against the 2027 manual and
  wiring guide.
- SystemCore's **built-in Wi-Fi** (SSID `SYSTEMCORE`, at 172.30.0.1) is for the shop and
  off-season use. FRC matches still use the VH-109.

---

## 10. Built-in parts that affect mounting

- **IMU:** 3-axis gyro and accelerometer. It outputs a quaternion and fused robot yaw at
  400 Hz. Since the IMU is inside the
  controller, **mount SystemCore rigidly**, ideally flat and near the robot's center, and away
  from parts that vibrate. Software can be told the mounting orientation. Write down how it's
  mounted for the programmers.
- **OLED display:** shows the IP addresses, team number and hardware faults. Mount it where you
  can read it.
- **Status LEDs:**

  | LED | Meaning |
  |---|---|
  | Power (green) | Solid = powered. Off = no power **or reverse polarity**. |
  | Mode (yellow) | Solid = on and disabled. Blinking = enabled. |
  | Status (red) | Solid = hardware fault or brownout protection. Off = no faults. Check the display for details. |

- **Size and mounting:** 135.5 × 71.5 × 28.1 mm, 215 g, four **Ø4.5 mm** holes on a
  **128 × 64 mm** pattern. That pattern comes from Limelight's alpha drawing, so measure your
  actual unit before cutting a plate. It's a different footprint from the roboRIO, so plan a new
  mounting plate. Leave room around the connectors to press the latches.
- **M.2 slot:** keep the supplied cover installed.

---

## 11. Devices and features that no longer work

| Gone | What to use instead |
|---|---|
| Relays (Spike) | A PH/PCM solenoid channel or a motor controller (our suggestion, not an official replacement) |
| Analog outputs | None |
| SPI port and SPI IMUs (ADIS16448, ADIS16470, ADXRS450, ADXL345) | The onboard IMU or a CAN IMU |
| Analog gyros, built-in accelerometer | The onboard IMU |
| MXP port and MXP expansion boards | CAN, USB or I2C devices |
| Ultrasonic rangefinders (ping/echo), interrupts, analog triggers, DMA | Other sensor types |
| Servo power from the controller | REV Servo Power Module or Servo Hub |
| Nidec Dynamo brushless | Any supported CAN or PWM motor controller |

**Vendor firmware:** 2027 needs new firmware and libraries for CAN devices, such as Phoenix 6
26.70 firmware for CTRE devices with WPILib alpha-7. Vendor libraries built for alpha-6 or
earlier don't work with alpha-7. Plan time to update every device.

---

## 12. Migration checklist for our robot

1. **Power:** route SystemCore power from its own fused PD slot (10 A under 2026 rules) using
   the supplied Micro-Fit+ cable. Don't use a VRM or any regulator.
2. **Mounting:** make a new mounting plate for the 128 × 64 mm hole pattern. Mount the
   controller rigidly and flat for the IMU, with the display visible.
3. **CAN:** decide which devices go on which of the 5 buses. Fit Molex SL plugs (production) or
   ferrules (alpha). Put a terminator at the far end of every bus. Give the programmers the
   device-to-`CANPort` map.
4. **Smart I/O:** list every PWM, DIO and analog device. If there are more than 6, move some to
   CAN. Re-terminate the cables with Molex SL housings and gold terminals.
5. **Analog sensors:** check that every analog signal stays within **0–3.3 V**.
6. **Switches and sensors:** confirm whether your unit has pull-ups (production) or pull-downs
   (alpha), and wire switches to match.
7. **Servos:** add a REV Servo Power Module or Servo Hub.
8. **I2C:** swap SDA and SCL on any roboRIO-style I2C cables.
9. **RSL:** make a 2-pin Molex SL cable for the RSL.
10. **Radio:** Ethernet to the VH-109's RIO port. Power the radio per the 2027 rules.
11. **Firmware:** update all CAN devices and vendor libraries to 2027 versions.

---

## 13. Alpha vs. production: quick reference

| | Alpha units (2025–26 test) | Production units (2027) |
|---|---|---|
| Power input | Weidmuller push-in, 18 AWG ferrules (Micro-Fit+ Bridge also present: **never use both**) | **Micro-Fit+ Bridge port only**, cable included |
| CAN connectors | Weidmuller push-in, 22 AWG ferrules | **2-pin Molex SL** |
| Smart I/O digital input | 4.7 kΩ **pull-down** (switch to 3.3 V) | **Pull-up** (switch to GND, like the roboRIO) |
| Flash/recovery mode | USB-C plugged in before power-on | Config button added. On beta units, hold it at power-on. |
| Port labels | None on the case | Expected (beta units are labeled) |
| Known hardware issues | USB 3 drive mounting; analog readings drift | None listed yet |

## 14. Still to confirm at the 2027 Kickoff

- The 2027 game-manual rules for controller power (fuse size), radio power and RSL placement.
- WPILib's official SystemCore wiring guide. As of October 2026, the "Step 1: Building your
  Robot" wiring pages still show the roboRIO.
- What exactly is in the box: the type and length of the included power cable, and which Molex
  SL cables come in the Kickoff Kit.
- Whether the production brownout threshold is still 6.3 V, and whether teams can configure it.
- When CAN FD mode becomes available on the CAN buses.

---

## Sources

- Limelight: [SystemCore Specifications (alpha, June 2025, rev. Oct 2025)](https://downloads.limelightvision.io/documents/systemcore_specifications_june15_2025_alpha.pdf)
- Limelight: [FIRST Control System Cable Specifications, Rev. 1](https://downloads.limelightvision.io/documents/systemcore_motioncore_cable_specifications.pdf)
- FIRST: [Systemcore Pricing and Availability (Sept 23, 2026)](https://community.firstinspires.org/2026-systemcore-pricing-and-availability)
- FIRST: [Control System Update – FIRST Tech Challenge Edition (Dec 8, 2025)](https://community.firstinspires.org/control-system-update-first-tech-challenge-edition): the production power, CAN and pull-up changes
- FIRST: [Updates on the Future Robot Controller (Mar 19, 2025)](https://community.firstinspires.org/march-updates-on-the-future-robot-controller)
- WPILib: [SystemcoreTesting README](https://github.com/wpilibsuite/SystemcoreTesting/blob/main/README.md): powering, flash mode, roboRIO transition notes, known issues
- WPILib: [SystemcoreTesting product roadmap](https://github.com/wpilibsuite/SystemcoreTesting/blob/main/product-roadmap.md) and [CTR-Phoenix notes](https://github.com/wpilibsuite/SystemcoreTesting/blob/main/CTR-Phoenix.md)
- WPILib docs (2027): [Systemcore Introduction](https://docs.wpilib.org/en/latest/docs/software/systemcore-info/systemcore-introduction.html), [Hardware Component Overview](https://docs.wpilib.org/en/latest/docs/controls-overviews/control-system-hardware.html), [Removed Features](https://docs.wpilib.org/en/latest/docs/yearly-overview/removed-features.html), [IP Configurations](https://docs.wpilib.org/en/latest/docs/networking/networking-introduction/ip-configurations.html)
- WPILib alpha-7 HAL source: [`PortsInternal.hpp`](https://github.com/wpilibsuite/allwpilib/blob/v2027.0.0-alpha-7/hal/src/main/native/systemcore/PortsInternal.hpp), [`Encoder.cpp`](https://github.com/wpilibsuite/allwpilib/blob/v2027.0.0-alpha-7/hal/src/main/native/systemcore/Encoder.cpp)
- FIRST: [2026 FRC Inspection Checklist](https://firstfrc.blob.core.windows.net/frc2026/Manual/2026FRCInspectionChecklist.pdf): the 2026 baseline rules quoted above
- Chief Delphi: [FRC 1678 SystemCore Alpha Testing Thread](https://www.chiefdelphi.com/t/frc-1678-citrus-circuits-systemcore-alpha-testing-thread/506842): practical wiring notes and the pull-down workaround
- GitHub issue: [CAN FD not working (SystemcoreTesting #412)](https://github.com/wpilibsuite/SystemcoreTesting/issues/412): CAN FD status
- NI: [roboRIO 2.0 User Manual](https://www.ni.com/docs/en-US/bundle/roborio-20-umanual/page/umanual.html) and [roboRIO 2.0 Specifications](https://ni.com/docs/en-US/bundle/roborio-20-specs/page/specs.html): roboRIO port counts, 5 V PWM logic, 0–5 V analog
- WPILib docs (2026): [roboRIO Brownout and Understanding Current Draw](https://docs.wpilib.org/en/stable/docs/software/roborio-info/roborio-brownouts.html): roboRIO brownout stages
- REV: [Power Distribution Hub](https://docs.revrobotics.com/ion-control-system/pdh/overview), [Servo Hub](https://docs.revrobotics.com/rev-crossover-products/servo/servo-hub), [4-pin JST PH to roboRIO I2C Cable (REV-11-1729)](https://www.revrobotics.com/rev-11-1729/)
- CTRE: [Power Distribution Panel](https://store.ctr-electronics.com/power-distribution-panel): the CAN termination jumper
