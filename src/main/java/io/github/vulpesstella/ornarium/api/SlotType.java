package io.github.vulpesstella.ornarium.api;

import net.minecraft.util.ResourceLocation;

/** Describes a kind of equipment slot. The server assigns its capacity separately. */
public final class SlotType {
    public final ResourceLocation id;
    public final String nameKey;
    public final String groupKey;
    public final ResourceLocation icon;
    public final int order;
    public final String oreName;

    public SlotType(ResourceLocation id, String nameKey, String groupKey, ResourceLocation icon,
        int order, String oreName) {
        this.id = id;
        this.nameKey = nameKey;
        this.groupKey = groupKey;
        this.icon = icon;
        this.order = order;
        this.oreName = oreName;
    }
}
