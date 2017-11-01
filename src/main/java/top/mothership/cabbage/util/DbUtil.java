package top.mothership.cabbage.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.pojo.Beatmap;
import top.mothership.cabbage.pojo.Score;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
@Component
public class DbUtil {
private final ApiUtil apiUtil;
    private final ScoreUtil scoreUtil;
    private Logger logger = LogManager.getLogger(this.getClass());
    @Autowired
    public DbUtil(ApiUtil apiUtil,ScoreUtil scoreUtil) {
        this.apiUtil = apiUtil;
        this.scoreUtil =scoreUtil;
    }

    //到底是把文件名传入构造方法，让一个util对象对应一个collection呢
    //还是提供方法，传入文件名，输出Beatmap集合呢
    public LinkedHashMap<String,List<Beatmap>> praseCollectionDB(String filename) throws IOException {
        DataInputStream reader  = new DataInputStream(new FileInputStream(filename));
        int version = readInt(reader);
        int count = readInt(reader);
        LinkedHashMap<String,List<Beatmap>> map = new LinkedHashMap<>(count);
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
    public LinkedHashMap<Beatmap,List<Score>> praseScoresDB(String filename) throws IOException {
        DataInputStream reader = new DataInputStream(new FileInputStream("D:\\scores.db"));
        int version = readInt(reader);
        int count = readInt(reader);
        LinkedHashMap<Beatmap, List<Score>> map = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            String md5 = readString(reader);
            Beatmap beatmap = apiUtil.getBeatmap(md5);
            int scoreCount = readInt(reader);
            List<Score> scores = new ArrayList<>(scoreCount);
            for (int i2 = 0; i2 < scoreCount; i2++) {
                Score score = new Score();
                byte mode = readByte(reader);
                int scoreVersion = readInt(reader);
                String mapMd5 = readString(reader);
                String username = readString(reader);
                String repMd5 = readString(reader);
                int count300 = readShort(reader);
                int count100 = readShort(reader);
                int count50 = readShort(reader);
                int countGeki = readShort(reader);
                int countKatu = readShort(reader);
                int countMiss = readShort(reader);
                int scoreValue = readInt(reader);
                int maxCombo = readInt(reader);
                boolean perfect = readBoolean(reader);
                int mods = readInt(reader);
//                String empty = readString(reader);
                long timestamps = readLong(reader);
                int size = readInt(reader);
                long onlineId = readLong(reader);
                LinkedHashMap<String,String> modsMap = scoreUtil.convertMOD(mods);
                score.setBeatmapId(Integer.valueOf(beatmap.getBeatmapId()));
                score.setCount50(count50);
                score.setCount100(count100);
                score.setCount300(count300);
                score.setCountGeki(countGeki);
                score.setCountKatu(countKatu);
                score.setCountMiss(countMiss);
                score.setDate(new Date(timestamps));
                score.setMaxCombo(maxCombo);
                score.setEnabledMods(mods);
                score.setScore((long) scoreValue);
                score.setUserName(username);
                if (perfect) {
                    score.setPerfect(1);
                } else {
                    score.setPerfect(0);
                }
                int noteCount = count50 + count100 + count300 + countMiss;
                float percent300 = (float) count300 / noteCount;
                float percent50 = (float) count50 / noteCount;

                if (percent300 < 0.7) {
                    score.setRank("D");
                } else if (percent300 <= 0.8) {
                    score.setRank("C");
                } else if (percent300 <= 0.85) {
                    score.setRank("B");
                }else if(percent300<1&&percent50<0.1){
                    if(modsMap.keySet().contains("HD")||modsMap.keySet().contains("FL")) {
                        score.setRank("SH");
                    }else{
                        score.setRank("S");
                    }
                }else{
                    if(modsMap.keySet().contains("HD")||modsMap.keySet().contains("FL")) {
                        score.setRank("XH");
                    }else{
                        score.setRank("X");
                    }
                }
                scores.add(score);
            }
            map.put(beatmap, scores);
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
