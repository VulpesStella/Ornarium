package io.github.vulpesstella.ornarium;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.vulpesstella.ornarium.api.SlotReference;
import io.github.vulpesstella.ornarium.api.SlotType;
import net.minecraft.util.ResourceLocation;

public final class SlotLayout {
    public static final int MAX_SLOTS = 256;
    private static final int MAX_TYPES = 64;
    private static final int PAGE_SIZE = 27;
    public static final SlotLayout EMPTY = new SlotLayout(Collections.<Entry>emptyList());

    private final List<Entry> entries;
    private final List<SlotReference> slots = new ArrayList<SlotReference>();
    private final List<SlotType> types = new ArrayList<SlotType>();
    private final Map<SlotReference, Integer> indices = new HashMap<SlotReference, Integer>();
    private final List<Page> pages = new ArrayList<Page>();

    SlotLayout(List<Entry> entries) {
        if (entries.size() > MAX_TYPES) {
            throw new IllegalArgumentException("Ornarium supports up to " + MAX_TYPES + " active slot types");
        }
        this.entries = new ArrayList<Entry>(entries);
        Collections.sort(this.entries, new Comparator<Entry>() {
            @Override public int compare(Entry left, Entry right) {
                int group = left.type.groupKey.compareTo(right.type.groupKey);
                if (group != 0) {
                    return group;
                }
                int order = Integer.compare(left.type.order, right.type.order);
                return order != 0 ? order : left.type.id.toString().compareTo(right.type.id.toString());
            }
        });
        Page page = null;
        for (Entry entry : this.entries) {
            if (entry.count <= 0 || entry.count > MAX_SLOTS - slots.size()) {
                throw new IllegalArgumentException("Ornarium supports up to " + MAX_SLOTS + " active slots");
            }
            for (int index = 0; index < entry.count; index++) {
                SlotReference slot = new SlotReference(entry.type.id, index);
                if (indices.put(slot, slots.size()) != null) {
                    throw new IllegalArgumentException("Duplicate Ornarium slot: " + entry.type.id);
                }
                if (page == null || page.size == PAGE_SIZE || !page.groupKey.equals(entry.type.groupKey)) {
                    page = new Page(entry.type.groupKey, slots.size());
                    pages.add(page);
                }
                page.size++;
                slots.add(slot);
                types.add(entry.type);
            }
        }
        if (pages.isEmpty()) {
            pages.add(new Page("ornarium.group.accessories", 0));
        }
    }

    public int size() { return slots.size(); }
    public SlotReference slot(int index) { return slots.get(index); }
    public SlotType type(int index) { return types.get(index); }
    public int indexOf(SlotReference slot) {
        Integer index = indices.get(slot);
        return index == null ? -1 : index;
    }
    public int pageCount() { return pages.size(); }
    public Page page(int index) { return pages.get(index); }

    public int pageOf(int slot) {
        for (int page = 0; page < pages.size(); page++) {
            Page candidate = pages.get(page);
            if (slot >= candidate.start && slot < candidate.start + candidate.size) {
                return page;
            }
        }
        throw new IndexOutOfBoundsException("Slot " + slot);
    }

    void write(DataOutputStream output) throws IOException {
        output.writeByte(entries.size());
        for (Entry entry : entries) {
            SlotType type = entry.type;
            output.writeUTF(type.id.toString());
            output.writeUTF(type.nameKey);
            output.writeUTF(type.groupKey);
            output.writeUTF(type.icon == null ? "" : type.icon.toString());
            output.writeUTF(type.oreName == null ? "" : type.oreName);
            output.writeInt(type.order);
            output.writeShort(entry.count);
        }
    }

    public static SlotLayout read(DataInputStream input) throws IOException {
        int types = input.readUnsignedByte();
        if (types > MAX_TYPES) {
            throw new IOException("Too many Ornarium slot types");
        }
        List<Entry> entries = new ArrayList<Entry>();
        for (int index = 0; index < types; index++) {
            String id = input.readUTF();
            String name = input.readUTF();
            String group = input.readUTF();
            String icon = input.readUTF();
            String ore = input.readUTF();
            if (!SlotRegistry.validId(id) || !SlotRegistry.validKey(name) || !SlotRegistry.validKey(group)
                || (!icon.isEmpty() && !SlotRegistry.validId(icon))
                || (!ore.isEmpty() && !SlotRegistry.validKey(ore))) {
                throw new IOException("Invalid Ornarium slot definition");
            }
            SlotType type = new SlotType(new ResourceLocation(id), name, group,
                icon.isEmpty() ? null : new ResourceLocation(icon), input.readInt(), ore.isEmpty() ? null : ore);
            entries.add(new Entry(type, input.readUnsignedShort()));
        }
        if (input.available() != 0) {
            throw new IOException("Trailing Ornarium layout data");
        }
        try {
            return new SlotLayout(entries);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid Ornarium slot layout", exception);
        }
    }

    static final class Entry {
        final SlotType type;
        final int count;

        Entry(SlotType type, int count) {
            this.type = type;
            this.count = count;
        }
    }

    public static final class Page {
        public final String groupKey;
        public final int start;
        private int size;

        private Page(String groupKey, int start) {
            this.groupKey = groupKey;
            this.start = start;
        }

        public int size() { return size; }
    }
}
