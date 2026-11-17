package io.github.vulpesstella.ornarium.api.client;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.item.Item;

/** Register from the client proxy during mod initialization. */
public final class OrnariumRenderers {
    private static final Map<Item, IOrnariumRenderer> RENDERERS =
        new IdentityHashMap<Item, IOrnariumRenderer>();

    private OrnariumRenderers() {}

    public static void register(Item item, IOrnariumRenderer renderer) {
        if (item == null || renderer == null) {
            throw new IllegalArgumentException("Item and renderer are required");
        }
        if (RENDERERS.containsKey(item)) {
            throw new IllegalArgumentException("An Ornarium renderer is already registered for item " + item.itemID);
        }
        RENDERERS.put(item, renderer);
    }

    public static IOrnariumRenderer get(Item item) {
        return RENDERERS.get(item);
    }
}
