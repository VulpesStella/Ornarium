# Ornarium

Ornarium brings the classic Baubles equipment layout to Minecraft 1.6.4 with Forge. It adds one amulet slot, two ring slots and one belt slot for items supplied by compatible mods.

## Play

Install the release JAR in the `mods` directory of both the client and the server. Ornarium targets Forge 9.11.1.1345.

Press **B** in game to open the accessory inventory. You can change the key in Controls. Click to move items between slots or shift-click to equip or remove an accessory. Equipped items are saved with the player. On death they follow the world's `keepInventory` rule.

## Integrate

Add the Ornarium development JAR to your compile classpath and declare `required-after:ornarium` in your Forge mod dependencies. Implement `io.github.vulpesstella.ornarium.api.IOrnarium` on an item and return an `OrnariumTypes` value from `getOrnariumType(ItemStack)`. The item controls whether it can be equipped and removed, and receives equip, unequip and worn-tick callbacks on the logical server. Use the worn-tick callback for behavior that must continue after loading or respawning. Permission checks should be side-effect free and may run on either side during container interaction.

Read equipped items on the server with `OrnariumApi.getEquipped`. To support right-click equip, call `OrnariumApi.tryEquip(player, stack)` from your item's right-click handler. On success, it consumes one item from the supplied stack.

## Build

Use JDK 17 to run the Gradle wrapper, JDK 8 to compile, and JDK 7 for the target API. Set `JDK7` and `JDK8` to the corresponding installation directories and pass `-Pjava7Home=<JDK 7 path>` to the wrapper.

Run `./gradlew build` (or `gradlew.bat build` on Windows) with that option. Use the JAR without the `-dev` suffix from `build/libs` for installation.

## Credits

Ornarium is inspired by [Baubles by Azanor](https://github.com/Azanor/Baubles). [Baubles MITE](https://modrinth.com/mod/baubles-mite) provides a separate implementation for FishModLoader.
