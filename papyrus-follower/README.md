# Papyrus Follower

A Fabric mod for Minecraft Java Edition **1.21.1** that adds THE GREAT PAPYRUS as a companion.
He is drawn as the camera-facing 2D overworld sprite (with a walk cycle for each direction)
instead of a blocky 3D model.

## Playing

| Action | Result |
| --- | --- |
| Get a **Papyrus Spawn Egg** (creative, Spawn Eggs tab) | Spawns a wild Papyrus |
| Right-click him with **Spaghetti** | Befriends him; he follows you, teleports to catch up, and fights what hurts you |
| Right-click with an **empty hand** | Toggle stay / follow |
| Right-click with Spaghetti when he's hurt | Heals him |

**Spaghetti** recipe (shapeless): 2 wheat, 1 beetroot, 1 bowl.

He has 40 health, hits for 5 damage and now and then says something in chat.

## Building

Requires JDK 21.

    ./gradlew build

The jar is written to `build/libs/papyrus-follower-<version>.jar`. Drop it in your `mods`
folder alongside [Fabric API](https://modrinth.com/mod/fabric-api) and Fabric Loader 0.16+.
The GitHub Actions workflow builds it on every push and uploads it as an artifact.

## Credits

Papyrus and the sprites are from *Undertale* by Toby Fox. This is an unofficial, non-commercial
fan project and is not affiliated with or endorsed by him. Code is MIT licensed (see the repo
`LICENSE`); the sprite artwork is not covered by that license.
