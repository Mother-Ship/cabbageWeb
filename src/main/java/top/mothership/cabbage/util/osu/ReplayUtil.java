package top.mothership.cabbage.util.osu;

import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.util.Pair;
import top.mothership.cabbage.util.qq.ImgUtil;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ReplayUtil {

    private static Logger logger = LogManager.getLogger(ReplayUtil.class);
    @SneakyThrows
    public static String getLifePoint(byte[] data) {
        DataInputStream reader = new DataInputStream(new ByteArrayInputStream(data));
        logger.info(readByte(reader));
        logger.info(readInt(reader));
        logger.info(readString(reader));
        logger.info(readString(reader));
        logger.info(readString(reader));
        logger.info(readShort(reader));
        logger.info(readShort(reader));
        logger.info(readShort(reader));
        logger.info(readShort(reader));
        logger.info(readShort(reader));
        logger.info(readShort(reader));
        logger.info(readInt(reader));
        logger.info(readShort(reader));
        logger.info(readByte(reader));
        logger.info(readInt(reader));
        String lifePoint = readString(reader);
        logger.info(lifePoint);
        return lifePoint;
    }
    private static byte readByte(DataInputStream reader) throws IOException {
        // 1 byte
        return reader.readByte();
    }
    private static short readShort(DataInputStream reader) throws IOException {
        // 2 bytes, little endian
        byte[] bytes = new byte[2];
        reader.readFully(bytes);
        //读出两个字节，并且用ByteBuffer转换成short
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }
    private static int readInt(DataInputStream reader) throws IOException {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }
    private static String readString(DataInputStream reader) throws IOException {
        // variable length
        // 00 = empty string
        // 0B <length> <char>* = normal string
        // <length> is encoded as an LEB, and is the byte length of the rest.
        // <char>* is encoded as UTF8, and is the string content.
        byte kind = reader.readByte();
        if (kind == 0) {
            return "";
        }
        if (kind != 11) {
            throw new IOException(String.format("String format error: Expected 0x0B or 0x00, found 0x%02X", (int) kind & 0xFF));
        }
        int length = readULEB128(reader);
        if (length == 0) {
            return "";
        }
        byte[] utf8bytes = new byte[length];
        reader.readFully(utf8bytes);
        return new String(utf8bytes, StandardCharsets.UTF_8);
    }
    private static long readLong(DataInputStream reader) throws IOException {
        // 8 bytes, little endian
        byte[] bytes = new byte[8];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getLong();
    }
    private static int readULEB128(DataInputStream reader) throws IOException {
        // variable bytes, little endian
        // MSB says if there will be more bytes. If cleared,
        // that byte is the last.
        int value = 0;
        for (int shift = 0; shift < 32; shift += 7) {
            byte b = reader.readByte();
            value |= ((int) b & 0x7F) << shift;
            //value = value|((int) b & 0x7F)<<shift
            if (b >= 0) return value; // MSB is zero. End of value.
        }
        throw new IOException("ULEB128 too large");
    }

}
