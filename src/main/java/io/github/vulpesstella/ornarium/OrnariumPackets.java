package io.github.vulpesstella.ornarium;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.network.packet.Packet250CustomPayload;

public final class OrnariumPackets {
    public static final byte OPEN = 0;
    public static final byte APPEARANCE = 1;
    public static final byte LAYOUT = 2;
    public static final byte PAGE = 3;

    private OrnariumPackets() {}

    public static Packet250CustomPayload open() {
        return new Packet250CustomPayload(Ornarium.CHANNEL, new byte[] { OPEN });
    }

    public static Packet250CustomPayload page(final int windowId, final int page) {
        return encode(PAGE, new Writer() {
            @Override public void write(DataOutputStream output) throws IOException {
                output.writeByte(windowId);
                output.writeShort(page);
            }
        });
    }

    static Packet250CustomPayload encode(byte type, Writer writer) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        try {
            output.writeByte(type);
            writer.write(output);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot encode an Ornarium packet", exception);
        }
        return new Packet250CustomPayload(Ornarium.CHANNEL, bytes.toByteArray());
    }

    interface Writer {
        void write(DataOutputStream output) throws IOException;
    }
}
