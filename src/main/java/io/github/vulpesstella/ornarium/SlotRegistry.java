package io.github.vulpesstella.ornarium;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.vulpesstella.ornarium.api.IOrnarium;
import io.github.vulpesstella.ornarium.api.OrnariumTypes;
import io.github.vulpesstella.ornarium.api.SlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.oredict.OreDictionary;

public final class SlotRegistry {
    private static final Map<ResourceLocation, SlotType> TYPES = new LinkedHashMap<ResourceLocation, SlotType>();
    private static final Map<ResourceLocation, Integer> REQUESTS = new LinkedHashMap<ResourceLocation, Integer>();
    private static SlotLayout layout;

    static {
        builtin(OrnariumTypes.AMULET, 0, 1, "ornariumAmulet");
        builtin(OrnariumTypes.RING, 1, 2, "ornariumRing");
        builtin(OrnariumTypes.BELT, 2, 1, "ornariumBelt");
    }

    private SlotRegistry() {}

    private static void builtin(ResourceLocation id, int order, int count, String oreName) {
        ResourceLocation icon = new ResourceLocation("ornarium", "textures/gui/slots/" + id.getResourcePath() + ".png");
        register(new SlotType(id, "ornarium.slot." + id.getResourcePath(), "ornarium.group.accessories",
            icon, order, oreName));
        request(id, count);
    }

    public static void register(SlotType type) {
        checkOpen();
        if (type == null || type.id == null || !validId(type.id.toString())
            || !validKey(type.nameKey) || !validKey(type.groupKey)
            || (type.icon != null && !validId(type.icon.toString()))
            || (type.oreName != null && !validKey(type.oreName))) {
            throw new IllegalArgumentException("Invalid Ornarium slot definition");
        }
        if (TYPES.containsKey(type.id)) {
            throw new IllegalArgumentException("Duplicate Ornarium slot type: " + type.id);
        }
        TYPES.put(type.id, type);
    }

    public static void request(ResourceLocation type, int count) {
        checkOpen();
        if (type == null || count < 0 || count > SlotLayout.MAX_SLOTS) {
            throw new IllegalArgumentException("Invalid Ornarium slot request: " + type + " x " + count);
        }
        Integer existing = REQUESTS.get(type);
        REQUESTS.put(type, existing == null ? count : Math.max(existing, count));
    }

    static void freeze(Configuration config) {
        for (ResourceLocation id : REQUESTS.keySet()) {
            if (!TYPES.containsKey(id)) {
                throw new IllegalArgumentException("Requested unregistered Ornarium slot type: " + id);
            }
        }
        List<SlotLayout.Entry> entries = new ArrayList<SlotLayout.Entry>();
        for (SlotType type : TYPES.values()) {
            Integer requested = REQUESTS.get(type.id);
            int override = config.get("slots", type.id.toString(), -1,
                "Set to -1 to use mod defaults, or 0 to disable this type.").getInt();
            int count = override == -1 ? (requested == null ? 0 : requested) : override;
            if (count < 0 || count > SlotLayout.MAX_SLOTS) {
                throw new IllegalArgumentException("Invalid Ornarium slot count for " + type.id + ": " + count);
            }
            if (count > 0) {
                entries.add(new SlotLayout.Entry(type, count));
            }
        }
        layout = new SlotLayout(entries);
        if (config.hasChanged()) {
            config.save();
        }
    }

    public static SlotLayout layout() {
        if (layout == null) {
            throw new IllegalStateException("Ornarium slots are available after mod initialization");
        }
        return layout;
    }

    public static List<SlotType> matchingTypes(ItemStack stack) {
        Collection<ResourceLocation> declared = stack.getItem() instanceof IOrnarium
            ? ((IOrnarium) stack.getItem()).getOrnariumTypes(stack) : null;
        List<SlotType> matches = new ArrayList<SlotType>();
        for (SlotType type : TYPES.values()) {
            if (matches(type, stack, declared)) {
                matches.add(type);
            }
        }
        return matches;
    }

    static boolean matches(SlotType type, ItemStack stack, Collection<ResourceLocation> declared) {
        if (declared != null && declared.contains(type.id)) {
            return true;
        }
        if (type.oreName != null) {
            for (ItemStack entry : OreDictionary.getOres(type.oreName)) {
                if (OreDictionary.itemMatches(entry, stack, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void checkOpen() {
        if (layout != null) {
            throw new IllegalStateException("The Ornarium slot registry is frozen");
        }
    }

    static boolean validId(String value) {
        return value.length() <= 64 && value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }

    static boolean validKey(String value) {
        return value != null && value.length() <= 64 && value.matches("[A-Za-z0-9_.-]+");
    }
}
