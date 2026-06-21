# Simplet — Concept

A minimal Android launcher for a car head-unit tablet. It replaces the slow,
bloated stock home screen with a fast, single-purpose interface that does only
what's actually used in the car: **navigation** and **Bluetooth audio**.

## Background

- **Vehicle:** Hyundai, 2019. Tablet is an **aftermarket Android head unit**
  (not the original system).
- **Confirmed specs:** Android **10.0**, kernel **4.14.116**, Google **Play
  Store + Maps** present (so sideloading works). SoC/RAM not exposed in the
  unit's "about" screen; Android 10 + kernel 4.14.116 is consistent with
  current-gen units (likely Unisoc UIS7862-class, octa-core, ~4–6 GB RAM),
  which implies the lag is software bloat, not the CPU.
- **Problems with it today:** extremely slow, no Apple CarPlay, mostly used as
  a Bluetooth speaker.
- **The tablet has no internet of its own** — a phone provides connectivity.
- **Phone:** iPhone (used for hotspot + Bluetooth audio).

## Why not just replace the OS?

Considered and rejected as the primary path. These units weld the OS to cheap,
board-specific hardware. Flashing a different OS requires proprietary drivers
(touchscreen, audio amp/DSP, tuner, GPS, and the CAN-bus MCU for steering-wheel
buttons / reverse camera / ignition) that are essentially never published. High
risk of bricking and losing core functions. **Most of the slowness comes from
the manufacturer launcher and background bloat, not raw CPU** — so a lightweight
launcher plus debloating gets most of the benefit with none of the risk, and is
fully reversible.

## MVP scope

A minimal Android launcher set as the device's default Home, doing three things
and nothing else:

| Feature          | Detail                                                            |
| ---------------- | ----------------------------------------------------------------- |
| **Navigation**   | One big tile → OsmAnd (offline maps, no Google dependency).        |
| **Bluetooth audio** | Now-playing + play / pause / skip / volume from the iPhone.    |
| **Connectivity** | Keep WiFi on, auto-reconnect to the iPhone hotspot, show status.  |

The launcher itself stays tiny and runs no background services of its own.

## Locked decisions

- **Pure Android launcher** — no CarPlay. The iPhone is used only for hotspot +
  Bluetooth audio. (USB alone does not provide CarPlay; that would require a
  host app on the unit or an external box, both out of scope.)
- **Internet:** iPhone Personal Hotspot over **WiFi**. WiFi hotspot and
  Bluetooth audio run on different radios and coexist fine.
- **Positioning:** the **tablet's own built-in GPS**. Sharing internet does not
  share location. A cheap USB GPS dongle (u-blox-based) is the fallback if the
  internal GPS is weak. (Relaying the iPhone's GPS is impractical due to iOS
  sandboxing.)
- **Primary nav app:** **OsmAnd** (sideloaded APK, offline maps, no Google
  dependency). Chosen over Google Maps because Maps hard-depends on Play
  Services — the main RAM hog on this 1 GB-class unit — and can't be styled for
  Phase 2. Switching to OsmAnd let us remove the Google account and starve the
  Google stack.
- **Lockdown method:** to be decided at implementation time. Leading option:
  ADB debloat + register Simplet as **device owner** for Lock Task / kiosk mode
  (no root). A plain launcher app cannot force-stop other apps on its own.

## Caveats to keep in mind

1. **OsmAnd routing/search is less polished than Google's**, and live traffic is
   weak. Accepted trade-off for offline + no-Google + skinnable.
2. **Install OsmAnd before removing the Google account.** OsmAnd is sideloaded
   (APK from f-droid.org — the `OsmAnd~` build has unlimited offline maps free),
   so it needs no Play Store; maps download once over the hotspot, then offline.
3. **OsmAnd voice:** with Google TTS disabled, use a **recorded** voice so
   turn-by-turn still speaks.

## Phase 2 (deferred)

- **Custom "Minecraft-style" map skin — simple 2D only.** Decided: built as an
  **OsmAnd custom render style** (`.render.xml`), loaded on the device, fully
  offline. Nav app is now OsmAnd, which makes this possible (Google Maps can't
  be styled at all).
- Scope (kept deliberately small):
  - Flat **Minecraft palette** — green land, blue water, gray roads, dirt-brown
    paths.
  - **Blocky / pixel font** for labels (carries most of the Minecraft identity).
  - **Decluttered** — hide most POIs, transport, and extra labels; show route +
    essentials only. Lighter to render, which suits the low-RAM unit.
  - Optional later: pixel-art POI icons.
- **Out of scope:** a literal 3D voxel world (cubes, isometric blocks, textured
  terrain). That would require a custom map engine (MapLibre + custom sprites) —
  a separate, much larger project, not planned.

## Background-process control — reality

A launcher app alone cannot kill other apps' background processes on stock,
non-rooted Android. The levers that actually work:

1. **ADB debloat (no root, reversible):** `pm disable-user --user 0 <package>`
   to remove manufacturer launcher extras, app stores, weather widgets, etc.
2. **Device Owner + Lock Task / kiosk mode:** register Simplet as device owner
   via ADB (`dpm set-device-owner`) to whitelist apps and suppress the rest.
3. **Developer Options:** Background process limit → low/none; disable
   animations.
4. **Root:** freeze anything (e.g. Greenify-style). Highest risk, usually
   unnecessary once 1–3 are in place.

## Open items

- [x] **Tablet Android version** — Android 10.0 (kernel 4.14.116).
- [x] **Google Play Store / Play Services?** — yes (Play Store + Maps present).
- [x] **Can it sideload APKs?** — yes (has Play Store).
- [ ] SoC / RAM — not exposed in the unit's UI. Get RAM via Developer options →
      Memory; SoC via a CPU-Z / AIDA64 install. Nice to know, not blocking.

### Android 10 constraint

Regular apps **cannot programmatically toggle WiFi** on Android 10 (the
`setWifiEnabled` API was removed for non-system apps). So "auto-enable WiFi"
becomes: leave WiFi permanently on in the unit's settings and let the saved
hotspot auto-reconnect; Simplet shows status only. Full WiFi control returns if
Simplet is later made device owner. Target **minSdk 29** (Android 10).

## Architecture sketch (MVP)

- Standard Android launcher: an Activity declaring the `HOME` + `DEFAULT`
  intent-category filter, set as default Home.
- Simple full-screen grid of large tiles (Navigation, Music/BT, optionally Phone
  and Settings).
- Bluetooth media control via `MediaSession` / `MediaController` (needs
  Notification Access permission) to show now-playing and send AVRCP commands.
- WiFi auto-connect to the saved hotspot on boot (exact API depends on the
  tablet's Android version).
- No background services owned by Simplet; keep the APK small.
