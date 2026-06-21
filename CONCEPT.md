# Simplet — Concept

A minimal Android launcher for a car head-unit tablet. It replaces the slow,
bloated stock home screen with a fast, single-purpose interface that does only
what's actually used in the car: **navigation** and **Bluetooth audio**.

## Background

- **Vehicle:** Hyundai, 2019. Tablet is an **aftermarket Android head unit**
  (not the original system).
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
| **Navigation**   | One big tile → Google Maps (with offline areas pre-downloaded).   |
| **Bluetooth audio** | Now-playing + play / pause / skip / volume from the iPhone.    |
| **Connectivity** | Auto-enable WiFi and reconnect to the iPhone hotspot on boot.     |

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
- **Primary nav app:** **Google Maps** with offline areas downloaded.
- **Lockdown method:** to be decided at implementation time. Leading option:
  ADB debloat + register Simplet as **device owner** for Lock Task / kiosk mode
  (no root). A plain launcher app cannot force-stop other apps on its own.

## Caveats to keep in mind

1. **Google Maps needs Google Play Services.** Most of these units have the Play
   Store, but some cheap ones do not. If absent, fall back to OsmAnd. **This is
   the #1 thing to verify on the tablet.**
2. **Offline Google Maps is limited:** offline areas expire (~15–30 days, need a
   refresh over the hotspot), no live traffic offline, limited rerouting. Good
   for known routes; the hotspot covers the rest.
3. **The Phase-2 skin will not be a reskin of Google Maps** (Maps can't be
   styled). It will be a separate styled map view.

## Phase 2 (deferred)

- **Custom "Minecraft-style" map skin.** Requires a styling engine, not Waze or
  Google Maps. Candidate paths:
  - **MapLibre / Mapbox** — vector tiles + a custom style JSON; most freedom for
    a blocky, pixelated aesthetic.
  - **OsmAnd** — OpenStreetMap-based, custom render styles, fully offline.
- Sub-scope to decide later: cosmetic "follow-me" styled map (small) vs. full
  turn-by-turn navigation with the skin baked in (large).

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

## Open items / needed before scaffolding

- [ ] **Tablet Android version** (Settings → About → Android version).
- [ ] **Does it have the Google Play Store / Play Services?**
- [ ] **Can it sideload APKs?**
- [ ] SoC / RAM (nice to know for performance expectations).

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
