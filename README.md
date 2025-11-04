# GlowingBlocks Plugin

A Paper plugin that provides commands and features to use the **GlowingEntities API** by SkytAsul. Make blocks and NPCs glow with custom colors using simple commands!

**Compatible with Paper 1.17 - 1.21+**

> **Note:** This plugin uses the [GlowingEntities API](https://github.com/SkytAsul/GlowingEntities) by SkytAsul as a dependency.

## Features

- Make blocks glow with custom colors
- **🌈 RAINBOW MODE** - Animated color-changing glow!
- **🌍 Global visibility** - Everyone sees the glow OR private (only you)
- **💾 Persistent storage** - Saves to `saves.yml`, survives restarts!
- **Works with player heads** (perfect for custom chests/decorations!)
- Per-player glowing effects (only you see the glow)
- 16 different colors available + rainbow animation
- Easy-to-use commands
- Automatic head owner detection
- No ProtocolLib required
- No external dependencies

## Installation

1. Download `glowingblocks-1.0.0.jar`
2. Place it in your server's `plugins/` folder
3. Restart or reload your server
4. Done!

**No additional dependencies required** - the GlowingEntities API is included in the JAR.

## Commands

### Make Blocks Glow

```
/glowblock [color|rainbow] [global]  - Make the block you're looking at glow
/gb [color|rainbow] [global]         - Short alias for glowblock

Examples:
/glowblock RED              - Make block glow red (private - only you see it)
/glowblock BLUE             - Make block glow blue (private)
/glowblock RAINBOW          - Animated rainbow glow (private)
/glowblock GOLD global      - Make block glow gold (everyone sees it)
/glowblock RAINBOW global   - Animated rainbow (everyone sees it)
/gb YELLOW                  - Short command works too!
```

### Remove Glow

```
/unglowblock             - Remove glow from the block you're looking at
/ugb                     - Short alias for unglowblock
```

### Admin Commands

```
/glowingentities help    - Show help message
/glowingentities version - Show plugin version
/ge list                 - Show count of glowing blocks
/ge save                 - Manually save all glowing blocks
/ge reload               - Reload saves.yml
/ge help                 - Short alias
```

## Available Colors

- RED
- BLUE
- GREEN
- YELLOW
- AQUA
- GOLD
- DARK_RED
- DARK_BLUE
- DARK_GREEN
- DARK_AQUA
- DARK_PURPLE
- LIGHT_PURPLE
- BLACK
- DARK_GRAY
- GRAY
- WHITE
- **RAINBOW** - Animated color cycling through all colors!

## Permissions

```yaml
glowingblocks.glowblock     - Allow making blocks glow (default: op)
glowingblocks.unglowblock   - Allow removing glow from blocks (default: op)
glowingblocks.glownpc       - Allow making NPCs glow (default: op)
glowingblocks.unglownpc     - Allow removing glow from NPCs (default: op)
glowingblocks.admin         - Admin commands (default: op)
```

## Usage Examples

1. **Make a block glow red (private):**
   - Look at the block
   - Type `/glowblock RED`
   - Only you will see the glow

2. **Make a player head (chest) glow with rainbow animation:**
   - Look at the player head
   - Type `/glowblock RAINBOW`
   - Plugin will show: "Player Head is now glowing with RAINBOW color!"
   - Plugin will display the head owner's name
   - Colors will cycle automatically every second!

3. **Make a block glow gold for everyone (global):**
   - Look at the block
   - Type `/glowblock GOLD global`
   - All players on the server will see the gold glow
   - Saved to `saves.yml` - survives server restarts!

4. **Create a global rainbow animation:**
   - Look at the block
   - Type `/glowblock RAINBOW global`
   - Everyone sees the animated rainbow effect!

5. **Change block color to blue:**
   - Look at the same block
   - Type `/glowblock BLUE`

6. **Remove the glow:**
   - Look at the block
   - Type `/unglowblock`

## How It Works

This plugin uses fake invisible entities and Minecraft's team system to create the glowing effect:
- **Regular blocks:** Uses invisible Shulker entity (1x1 block)
- **Player heads:** Uses invisible tiny Slime entity (0.52 blocks - **perfect fit for heads!**)

### Private vs Global Glowing

- **Private mode (default):** The glow is only visible to you - other players won't see it unless they also use the command
- **Global mode:** When you add `global` to the command, everyone on the server sees the glow immediately

### Rainbow Animation

Rainbow mode cycles through 7 colors automatically:
- RED → GOLD → YELLOW → GREEN → AQUA → BLUE → LIGHT_PURPLE → (repeat)
- Changes color every second (20 ticks)
- Works for both private and global glowing blocks

### Persistence System

All glowing blocks are automatically saved to `plugins/GlowingBlocks/saves.yml`:
- Saves location, color, animation state, and visibility mode
- Automatically loads on server start
- Global blocks reappear for all players after restart
- Private blocks reappear for the owner when they rejoin

Tiny slimes (size 1) are ideal for player heads because they're almost exactly the same size as a head block (0.5 blocks), making the glowing effect look natural and precise!

## Requirements

- **Paper** server (or Paper-based fork like Purpur, Pufferfish)
- **NOT compatible** with Spigot or Bukkit (Paper-only)
- Minecraft version 1.17 or higher
- Java 17 or higher

## Building from Source

```bash
mvn clean package
```

The compiled JAR will be in `target/glowingblocks-1.0.0.jar`

## Credits

This plugin uses the **GlowingEntities API** by **SkytAsul**.

- Original API: https://github.com/SkytAsul/GlowingEntities
- API License: MIT
- Plugin Author: TremeQ

## License

MIT License - See LICENSE file for details
