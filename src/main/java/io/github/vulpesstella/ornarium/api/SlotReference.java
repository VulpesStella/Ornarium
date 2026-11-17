package io.github.vulpesstella.ornarium.api;

import net.minecraft.util.ResourceLocation;

/** A stable slot address, using a zero-based index within the type. */
public final class SlotReference {
    public final ResourceLocation type;
    public final int index;

    public SlotReference(ResourceLocation type, int index) {
        if (type == null || index < 0) {
            throw new IllegalArgumentException("A slot requires a type and a non-negative index");
        }
        this.type = type;
        this.index = index;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SlotReference)) {
            return false;
        }
        SlotReference slot = (SlotReference) other;
        return type.equals(slot.type) && index == slot.index;
    }

    @Override public int hashCode() { return 31 * type.hashCode() + index; }
}
