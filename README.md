# TFC Auto Forging with Whitelist

[中文版](README_CN.md)

A client-side mod that enhances the auto-forging experience in TerraFirmaCraft: TNG.  
Based on [TFC Auto Forging](https://github.com/Eternal130/TFCAutoForging) by Eternal130.

## Features

1. **Smart Auto-Forging** — automatically detects and executes the optimal hammer steps.
2. **Tap-to-Activate Mode** — a middle ground between OFF and FULL AUTO.  
   The mod waits for you to manually strike the first step, then takes over.
3. **Target Item Whitelist** — specify which recipe outputs get auto-forged.  
   Whitelisted items can use different policies than non-whitelisted ones.
4. **Forging Speed Control** — choose from Extreme / Fast / Moderate / Safe.
5. **Forging Tips** — highlights the recommended next button on the anvil GUI.
6. **In-game Settings GUI** — press **Ctrl+F** to open. View your backpack, right-click items to add to the whitelist, toggle policies and speed instantly.

## Key Bindings

| Key | Function |
|-----|----------|
| **F** | Toggle auto-forging ON/OFF |
| **Ctrl+F** | Open settings panel |
| **G** | Toggle forging tip highlight |

## Settings Panel (Ctrl+F)

- **Inside Whitelist** — Policy for whitelisted recipes: Tap to Activate / Full Auto
- **Outside Whitelist** — Policy for non-whitelisted recipes: Never / Tap to Activate / Full Auto
- **Speed** — Forging interval: Extreme (instant) / Fast / Moderate / Safe (~1s)
- **3×3 Whitelist grid** — right-click backpack items to add; left-click to pick up/swap; right-click slot to remove
- All changes save instantly. Press ESC to close.

## Installation

Two versions are available:

### Minecraft 1.20.1 (Forge)
- Requires: **Forge 47.x** + **TFC TNG 3.x for 1.20.1**
- Download: `tfcafww-tfctng-1.20.1-0.3.jar`

### Minecraft 1.21.1 (NeoForge)
- Requires: **NeoForge 21.1.x** + **TFC TNG for 1.21.1**
- Download: `tfcafww-tfctng-1.21.1-0.3.jar`

This is a client-only mod. Servers do not need to install it.

## Credits

- Original mod by [Eternal130](https://github.com/Eternal130/TFCAutoForging)
- Whitelist, tap mode, speed control, and GUI redesign by [SaloninusBlue](https://github.com/SaloninusBlue)

## License

GPL-3.0
