# Simplet — Tablet Debloat Checklist

Tailored to this unit's installed apps (Android 10, ~1.1 GB RAM, navigation =
**OsmAnd**). Goal: cut background processes and reclaim RAM.

## Golden rules

- **Disable, don't uninstall** system apps — reversible via
  Settings → Apps → ⋮ *Show system* → app → *Enable*.
- **Work in small batches and test after each** — Bluetooth audio, reverse
  camera, radio.
- **Install OsmAnd FIRST** (from the USB stick) before removing the Google
  account, so nav is working before the Play Store goes away.

## Recommended order

1. Install OsmAnd from the USB stick; set a **recorded** voice; confirm it
   navigates.
2. Disable the "Disable now" list.
3. Disable Google Maps + Play Store.
4. Settings → Accounts → **remove the Google account** (quiets Play
   Services/Store).
5. Re-check Developer options → Memory; confirm BT audio + reverse camera +
   radio still work.

## ✅ Disable now (safe)

- [ ] Youtube
- [ ] Chrome
- [ ] Calendar
- [ ] Exchange services
- [ ] Google partner setup
- [ ] Sound recorder
- [ ] Tlink5 — phone-mirroring app, not used

## ✅ Disable after OsmAnd works

- [ ] Maps (Google Maps)
- [ ] Google play store — if it offers *Disable*; otherwise it goes dormant
  after the account is removed

## ⚠️ Conditional

- [ ] Speech recognition and synthesis from Google (TTS) — OK to disable, but
  set OsmAnd to a **recorded** voice so directions still speak
- [ ] Files vs File manager — keep **one** to install APKs from the USB stick;
  disable the redundant one (if "Files" = Files by Google, disable that one)
- [ ] Gallery / Video / Localmusic — optional, tiny RAM gain; disable only if
  never used

## ⚠️ The RAM hog — special handling

- **Google Play Services** — do **not** hard-disable on-device (risky, often
  greyed, can cause error loops). **Starve it**: remove the Google account.
  Revisit a full disable only if network-ADB becomes available.

## ⛔ Keep — do NOT touch

- Backcar (reverse camera)
- Bluetooth (audio)
- Canbus2 (steering controls / vehicle data)
- Car settings
- Original car (factory / car system)
- Radio
- Settings
- Sound effects (audio DSP / EQ)
- Launcher (until Simplet replaces it)
- Gboard (your **only** keyboard — disabling = no typing)
- Downloads (download-manager plumbing)
- Theme (tied to the current launcher)
