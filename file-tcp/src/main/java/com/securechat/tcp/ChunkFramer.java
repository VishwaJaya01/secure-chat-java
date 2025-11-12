package com.securechat.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ChunkFramer {

    public static void writeChunk(DataOutputStream out, byte[] chunk) throws IOException {
        Checksum crc32 = new CRC32();
        crc32.update(chunk, 0, chunk.length);
        long checksum = crc32.getValue();

        out.writeInt(chunk.length);
        out.writeLong(checksum);
        out.write(chunk);
        out.flush();
    }

    public static byte[] readChunk(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            throw new IOException("Invalid chunk length: " + length);
        }
        long checksum = in.readLong();
        byte[] chunk = new byte[length];
        in.readFully(chunk);

        Checksum crc32 = new CRC32();
        crc32.update(chunk, 0, length);
        if (checksum != crc32.getValue()) {
            throw new IOException("Chunk checksum mismatch");
        }
        return chunk;
    }
}
