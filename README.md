# Ornarium

Ornarium brings the classic Baubles equipment layout to Minecraft 1.6.4 with Forge. It adds one amulet slot, two ring slots and one belt slot for items supplied by compatible mods.

## Play

Install the release JAR in the `mods` directory of both the client and the server. Ornarium targets Forge 9.11.1.1345.

Press **B** in game to open the accessory inventory. You can change the key in Controls. Click to move items between slots or shift-click to equip or remove an accessory. Use the arrows to browse additional pages. Shift-click searches all pages for a compatible empty slot. Equipped items are saved with the player and follow the world's `keepInventory` rule.

Server operators can set slot counts in `config/ornarium.cfg`. Use `-1` for mod defaults or `0` to disable a type. When a configuration change displaces an equipped item, Ornarium tries the first empty inventory slot, then the first empty Ender Chest slot, then drops the item beside the player.

## Integrate

Add the Ornarium development JAR to your compile classpath and declare `required-after:ornarium` in your Forge mod dependencies. Implement `io.github.vulpesstella.ornarium.api.IOrnarium` on an item and return a `Collection<ResourceLocation>` from `getOrnariumTypes(ItemStack)`. `OrnariumTypes` provides the built-in IDs. The item controls whether it can be equipped and removed, and receives equip, unequip and worn-tick callbacks on the logical server. Use the worn-tick callback for behavior that must continue after loading or respawning. Permission checks should be side-effect free and may run on either side during container interaction.

Register a custom `SlotType` with `OrnariumApi.registerSlotType` during Forge pre-initialization or initialization. Its definition supplies the ID, translated name and group, optional icon, order and optional Ore Dictionary entry. Use `OrnariumApi.requestSlots(id, count)` to request default capacity. The largest request for a type wins, subject to server configuration. A matching Ore Dictionary entry allows ordinary items to be equipped without implementing `IOrnarium`. The built-in types use `ornariumAmulet`, `ornariumRing` and `ornariumBelt`.

Read equipped items on the server with `OrnariumApi.getEquipped(player, slot)`. A `SlotReference` pairs a type ID with a zero-based index within that type. To support right-click equip, call `OrnariumApi.tryEquip(player, stack)` from your item's right-click handler. On success, it consumes one item from the supplied stack.

To draw an equipped item on a player, register an `IOrnariumRenderer` for the item with `OrnariumRenderers.register` from your client proxy. The renderer receives an appearance stack containing the item and metadata, and a context with the player, partial tick, slot type ID, and index within that type. Call `context.attachTo(Anchor.BODY)` or use another body anchor before drawing your model in local coordinates. This hook runs in the third-person player render pass.

## Build

Use JDK 17 to run the Gradle wrapper, JDK 8 to compile, and JDK 7 for the target API. Set `JDK7` and `JDK8` to the corresponding installation directories and pass `-Pjava7Home=<JDK 7 path>` to the wrapper.

Run `./gradlew build` (or `gradlew.bat build` on Windows) with that option. Use the JAR without the `-dev` suffix from `build/libs` for installation.

## Credits

Ornarium is inspired by [Baubles by Azanor](https://github.com/Azanor/Baubles). [Baubles MITE](https://modrinth.com/mod/baubles-mite) provides a separate implementation for FishModLoader.
