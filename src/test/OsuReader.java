

import java.io.*;
import java.nio.*;
import java.util.*;

public class OsuReader
{
    public static void main(String[] args) throws IOException
    {
//        String kind = args[0];
        String kind = "collection";
        OsuReader reader = new OsuReader("test");
        if (kind.equals("collection"))
        {
//            CollectionDB db = reader.readCollectionDB();
//            System.out.printf("Version: %d\n", db.version);
            List<CollectionItem> list = new ArrayList<>();
            for (CollectionItem item : list)
            {
                System.out.println();
                System.out.printf("Name: %s\n", item.name);
                for (String hash : item.md5Hashes)
                {
                    System.out.printf("  Hash: %s\n", hash);
                }
            }
        }
//        else if (kind.equals("scores"))
//        {
//            ScoresDB db = reader.readScoresDB();
//            System.out.printf("Version: %d", db.version);
//            for (Beatmap beatmap : db.beatmaps)
//            {
//                System.out.println("---");
//                System.out.printf("Beatmap hash: %s\n", beatmap.md5Hash);
//                for (Score score : beatmap.scores)
//                {
//                    System.out.println("  ---");
//                    System.out.printf("  Mode: %s (%d)\n", score.mode.name(), score.mode.byteValue);
//                    System.out.printf("  Version: %d\n", score.version);
//                    System.out.printf("  Beatmap MD5: %s\n", score.beatmapMd5Hash);
//                    System.out.printf("  Player name: %s\n", score.playerName);
//                    System.out.printf("  Replay MD5: %s\n", score.replayMd5Hash);
//                    System.out.printf("  Scores: %d / %d / %d / %d / %d / %d\n",
//                            score.numberOf300s, score.numberOf100s, score.numberOf50s, score.numberOfGekis,
//                            score.numberOfKatus, score.numberOfMisses);
//                    System.out.printf("  Replay score: %d\n", score.replayScore);
//                    System.out.printf("  Max combo: %d\n", score.maxCombo);
//                    System.out.printf("  Perfect combo: %s\n", score.perfectCombo ? "Yes" : "No");
//                    System.out.printf("  Mods used: %s\n", score.modsUsed);
//                    System.out.printf("  Timestamp: %s\n", score.timestamp);
//                }
//            }
//        }
    }

    private DataInputStream reader;

    public OsuReader(String filename) throws IOException
    {
        this(new FileInputStream("e:\\collection.db"));
    }

    public OsuReader(InputStream source)
    {
        this.reader = new DataInputStream(source);
    }

    // --- Primitive values ---

    public byte readByte() throws IOException
    {
        // 1 byte
        return this.reader.readByte();
    }

    public short readShort() throws IOException
    {
        // 2 bytes, little endian
        byte[] bytes = new byte[2];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }

    public int readInt() throws IOException
    {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    public long readLong() throws IOException
    {
        // 8 bytes, little endian
        byte[] bytes = new byte[8];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getLong();
    }

    public int readULEB128() throws IOException
    {
        // variable bytes, little endian
        // MSB says if there will be more bytes. If cleared,
        // that byte is the last.
        int value = 0;
        for (int shift = 0; shift < 32; shift += 7)
        {
            byte b = this.reader.readByte();
            value |= ((int) b & 0x7F) << shift;

            if (b >= 0) return value; // MSB is zero. End of value.
        }
        throw new IOException("ULEB128 too large");
    }

    public float readSingle() throws IOException
    {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getFloat();
    }

    public double readDouble() throws IOException
    {
        // 8 bytes little endian
        byte[] bytes = new byte[8];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getDouble();
    }

    public boolean readBoolean() throws IOException
    {
        // 1 byte, zero = false, non-zero = true
        return this.reader.readBoolean();
    }

    public String readString() throws IOException
    {
        // variable length
        // 00 = empty string
        // 0B <length> <char>* = normal string
        // <length> is encoded as an LEB, and is the byte length of the rest.
        // <char>* is encoded as UTF8, and is the string content.
        byte kind = this.reader.readByte();
        if (kind == 0) return "";
        if (kind != 11)
        {
            throw new IOException(String.format("String format error: Expected 0x0B or 0x00, found 0x%02X", (int) kind & 0xFF));
        }
        int length = readULEB128();
        if (length == 0) return "";
        byte[] utf8bytes = new byte[length];
        this.reader.readFully(utf8bytes);
        return new String(utf8bytes, "UTF-8");
    }

    public Date readDate() throws IOException
    {
        long ticks = readLong();
        long TICKS_AT_EPOCH = 621355968000000000L;
        long TICKS_PER_MILLISECOND = 10000;

        return new Date((ticks - TICKS_AT_EPOCH)/TICKS_PER_MILLISECOND);
    }

    // --- Composite structures ---

    public CollectionDB readCollectionDB() throws IOException
    {
        CollectionDB result = new CollectionDB();
        result.version = readInt();
        int count = readInt();
        result.collections = new ArrayList<CollectionItem>(count);
        for (int i = 0; i < count; i++)
        {
            CollectionItem item = readCollectionItem();
            result.collections.add(item);
        }
        return result;
    }

    public CollectionItem readCollectionItem() throws IOException
    {
        CollectionItem item = new CollectionItem();
        item.name = readString();
        int count = readInt();
        item.md5Hashes = new ArrayList<String>(count);
        for (int i = 0; i < count; i++)
        {
            String md5Hash = readString();
            item.md5Hashes.add(md5Hash);
        }
        return item;
    }

//    public ScoresDB readScoresDB() throws IOException
//    {
//        ScoresDB result = new ScoresDB();
//        result.version = readInt();
//        int count = readInt();
//        result.beatmaps = new ArrayList<Beatmap>(count);
//        for (int i = 0; i < count; i++)
//        {
//            Beatmap beatmap = readBeatmap();
//            result.beatmaps.add(beatmap);
//        }
//        return result;
//    }

//    public Beatmap readBeatmap() throws IOException
//    {
//        Beatmap result = new Beatmap();
//        result.md5Hash = readString();
//        int count = readInt();
//        result.scores = new ArrayList<Score>();
//        for (int i = 0; i < count; i++)
//        {
//            Score score = readScore();
//            result.scores.add(score);
//        }
//        return result;
//    }

//    public Score readScore() throws IOException
//    {
//        Score result = new Score();
//        result.mode = GameplayMode.valueOf(readByte());
//        result.version = readInt();
//        result.beatmapMd5Hash = readString();
//        result.playerName = readString();
//        result.replayMd5Hash = readString();
//        result.numberOf300s = readShort();
//        result.numberOf100s = readShort();
//        result.numberOf50s = readShort();
//        result.numberOfGekis = readShort();
//        result.numberOfKatus = readShort();
//        result.numberOfMisses = readShort();
//        result.replayScore = readInt();
//        result.maxCombo = readShort();
//        result.perfectCombo = readBoolean();
//        result.modsUsed = OsuMod.valueOf(readInt());
//        result.unknown1 = readString();
//        result.timestamp = readDate();
//        result.unknown2 = readInt();
//        result.unknown3 = readInt();
//        result.unknown4 = readInt();
//        return result;
//    }

    public class CollectionDB
    {
        public int version; // 20150203
        public List<CollectionItem> collections;
    }

    public class CollectionItem
    {
        public String name;
        public List<String> md5Hashes;
    }

//    public class ScoresDB
//    {
//        public int version; // 20150204
//        public List<Beatmap> beatmaps;
//    }

    public class Beatmap
    {
        public String md5Hash;
        public List<Score> scores;
    }

    public enum GameplayMode
    {
        OsuStandard((byte) 0),
        Taiko((byte) 1),
        CTB((byte) 2),
        Mania((byte) 3);

        public final byte byteValue;

        private GameplayMode(byte byteValue)
        {
            this.byteValue = byteValue;
        }

        public static GameplayMode valueOf(byte byteValue)
        {
            for (GameplayMode item : values())
            {
                if (item.byteValue == byteValue) return item;
            }
            throw new IllegalArgumentException("byteValue");
        }
    }

    public enum OsuMod
    {
        NoFail(1),
        Easy(2),
        NoVideo(4),
        Hidden(8),
        HardRock(16),
        SuddenDeath(32),
        DoubleTime(64),
        Relax(128),
        HalfTime(256),
        Nightcore(512),
        Flashlight(1024),
        Autoplay(2048),
        SpunOut(4096),
        Relax2(8192),
        Perfect(16384),
        Key4(32768),
        Key5(65536),
        Key6(131072),
        Key7(262144),
        Key8(524288),
        keyMod(1015808),
        FadeIn(1048576),
        Random(2097152),
        LastMod(4194304);

        public final int bit;

        private OsuMod(int bit)
        {
            this.bit = bit;
        }

        public static EnumSet<OsuMod> valueOf(int bits)
        {
            EnumSet<OsuMod> result = EnumSet.noneOf(OsuMod.class);
            for (OsuMod flag : OsuMod.values())
            {
                if ((bits & flag.bit) == flag.bit)
                {
                    result.add(flag);
                }
            }
            return result;
        }
    }

    public class Score
    {
        public GameplayMode mode;
        public int version; // 20150203
        public String beatmapMd5Hash;
        public String playerName;
        public String replayMd5Hash;
        public short numberOf300s;
        public short numberOf100s;
        public short numberOf50s;
        public short numberOfGekis;
        public short numberOfKatus;
        public short numberOfMisses;
        public int replayScore;
        public short maxCombo;
        public boolean perfectCombo;
        public EnumSet<OsuMod> modsUsed;
        public String unknown1;
        public Date timestamp;
        public int unknown2;
        public int unknown3;
        public int unknown4;
    }
}