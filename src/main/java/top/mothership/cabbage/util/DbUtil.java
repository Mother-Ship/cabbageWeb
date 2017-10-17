package top.mothership.cabbage.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.Beatmap;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
@Component
public class DbUtil {
private final ApiUtil apiUtil;
    private Logger logger = LogManager.getLogger(this.getClass());
    @Autowired
    public DbUtil(ApiUtil apiUtil) {
        this.apiUtil = apiUtil;
    }

    //到底是把文件名传入构造方法，让一个util对象对应一个collection呢
    //还是提供方法，传入文件名，输出Beatmap集合呢
    public Map<String,List<Beatmap>> praseCollectionDB(String filename) throws IOException {
        DataInputStream reader  = new DataInputStream(new FileInputStream(filename));
        int version = readInt(reader);
        int count = readInt(reader);
        Map<String,List<Beatmap>> map = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++)
        {
            String name = readString(reader);
            int count2 = readInt(reader);
            List<Beatmap> md5Hashes = new ArrayList<>(count);
            logger.info("开始解析收藏夹"+name);
            for (int i2 = 0; i2 < count2; i2++)
            {
                String md5Hash = readString(reader);
                Beatmap beatmap = apiUtil.getBeatmap(md5Hash);
                md5Hashes.add(beatmap);
            }

            map.put(name,md5Hashes);
        }
        return map;
    }
    private byte readByte(DataInputStream reader) throws IOException
    {
        // 1 byte
        return reader.readByte();
    }

    private short readShort(DataInputStream reader) throws IOException
    {
        // 2 bytes, little endian
        byte[] bytes = new byte[2];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }

    private int readInt(DataInputStream reader) throws IOException
    {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    private long readLong(DataInputStream reader) throws IOException
    {
        // 8 bytes, little endian
        byte[] bytes = new byte[8];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getLong();
    }

    private int readULEB128(DataInputStream reader) throws IOException
    {
        // variable bytes, little endian
        // MSB says if there will be more bytes. If cleared,
        // that byte is the last.
        int value = 0;
        for (int shift = 0; shift < 32; shift += 7)
        {
            byte b = reader.readByte();
            value |= ((int) b & 0x7F) << shift;

            if (b >= 0) return value; // MSB is zero. End of value.
        }
        throw new IOException("ULEB128 too large");
    }

    private float readSingle(DataInputStream reader) throws IOException
    {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
       reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getFloat();
    }

    private double readDouble(DataInputStream reader) throws IOException
    {
        // 8 bytes little endian
        byte[] bytes = new byte[8];
        reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getDouble();
    }

    private boolean readBoolean(DataInputStream reader) throws IOException
    {
        // 1 byte, zero = false, non-zero = true
        return reader.readBoolean();
    }

    private String readString(DataInputStream reader) throws IOException
    {
        // variable length
        // 00 = empty string
        // 0B <length> <char>* = normal string
        // <length> is encoded as an LEB, and is the byte length of the rest.
        // <char>* is encoded as UTF8, and is the string content.
        byte kind = reader.readByte();
        if (kind == 0) return "";
        if (kind != 11)
        {
            throw new IOException(String.format("String format error: Expected 0x0B or 0x00, found 0x%02X", (int) kind & 0xFF));
        }
        int length = readULEB128(reader);
        if (length == 0) return "";
        byte[] utf8bytes = new byte[length];
        reader.readFully(utf8bytes);
        return new String(utf8bytes, "UTF-8");
    }

    private Date readDate(DataInputStream reader) throws IOException
    {
        long ticks = readLong(reader);
        long TICKS_AT_EPOCH = 621355968000000000L;
        long TICKS_PER_MILLISECOND = 10000;

        return new Date((ticks - TICKS_AT_EPOCH)/TICKS_PER_MILLISECOND);
    }
}
