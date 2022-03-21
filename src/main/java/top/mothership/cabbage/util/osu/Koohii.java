package top.mothership.cabbage.util.osu;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * pure java implementation of github.com/Francesco149/oppai-ng .
 *
 * <blockquote><pre>
 * this is meant to be a single file library that's as portable and
 * easy to set up as possible for java projects that need
 * pp/difficulty calculation.
 *
 * when running the test suite, speed is roughly equivalent to the C
 * implementation, but peak memory usage is almost 80 times higher.
 * if you are on a system with limited resources or you don't want
 * to spend time installing and setting up java, you can use the C
 * implementation which doesn't depend on any third party software.
 * -----------------------------------------------------------------
 * usage:
 * put Koohii.java in your project's folder
 * -----------------------------------------------------------------
 * import java.io.BufferedReader;
 * import java.io.InputStreamReader;
 *
 * class Example {
 *
 * public static void main(String[] args) throws java.io.IOException
 * {
 *     BufferedReader stdin =
 *         new BufferedReader(new InputStreamReader(System.in)
 *     );
 *
 *     Koohii.Map beatmap = new Koohii.Parser().map(stdin);
 *     Koohii.DiffCalc stars = new Koohii.DiffCalc().calc(beatmap);
 *     System.out.printf("%s stars\n", stars.total);
 *
 *     Koohii.PPv2 pp = Koohii.PPv2(
 *         stars.aim, stars.speed, beatmap
 *     );
 *
 *     System.out.printf("%s pp\n", pp.total);
 * }
 *
 * }
 * -----------------------------------------------------------------
 * javac Example.java
 * cat /path/to/file.osu | java Example
 * -----------------------------------------------------------------
 * this is free and unencumbered software released into the
 * public domain.
 *
 * refer to the attached UNLICENSE or http://unlicense.org/
 * </pre></blockquote>
 *
 * @author Franc[e]sco (lolisamurai@tfwno.gf)
 */
public final class Koohii {

    private Koohii() {}

    public static final int VERSION_MAJOR = 2;
    public static final int VERSION_MINOR = 0;
    public static final int VERSION_PATCH = 0;

    /** prints a message to stderr. */
    public static
    void info(String fmt, Object... args) {
        System.err.printf(fmt, args);
    }

    /* ------------------------------------------------------------- */
    /* math                                                          */

    /** 2D vector with double values */
    public static class Vector2
    {
        public double x = 0.0, y = 0.0;

        public Vector2() {}
        public Vector2(Vector2 other) { this(other.x, other.y); }
        public Vector2(double x, double y) { this.x = x; this.y = y; }

        public String toString() {
            return String.format("(%s, %s)", x, y);
        }

        /**
         * this -= other .
         * @return this
         */
        public Vector2 sub(Vector2 other)
        {
            x -= other.x; y -= other.y;
            return this;
        }

        /**
         * this *= value .
         * @return this
         */
        public Vector2 mul(double value)
        {
            x *= value; y *= value;
            return this;
        }

        /** length (magnitude) of the vector. */
        public double len() { return Math.sqrt(x * x + y * y); }

        /** dot product between two vectors, correlates with the angle */
        public double dot(Vector2 other) { return x * other.x + y * other.y; }
    }

    /* ------------------------------------------------------------- */
    /* beatmap utils                                                 */

    public static final int MODE_STD = 0;

    public static class Circle
    {
        public Vector2 pos = new Vector2();
        public String toString() { return pos.toString(); }
    }

    public static class Slider
    {
        public Vector2 pos = new Vector2();

        /** distance travelled by one repetition. */
        public double distance = 0.0;

        /** 1 = no repeats. */
        public int repetitions = 1;

        public String toString()
        {
            return String.format(
                    "{ pos=%s, distance=%s, repetitions=%d }",
                    pos, distance, repetitions
            );
        }
    }

    public static final int OBJ_CIRCLE = 1<<0;
    public static final int OBJ_SLIDER = 1<<1;
    public static final int OBJ_SPINNER = 1<<3;

    /** strain index for speed */
    public final static int DIFF_SPEED = 0;

    /** strain index for aim */
    public final static int DIFF_AIM = 1;

    public static class HitObject
    {
        /** start time in milliseconds. */
        public double time = 0.0;
        public int type = OBJ_CIRCLE;

        /** an instance of Circle or Slider or null. */
        public Object data = null;
        public Vector2 normpos =  new Vector2();
        public double angle = 0.0;
        public final double[] strains = new double[] { 0.0, 0.0 };
        public boolean is_single = false;
        public double delta_time = 0.0;
        public double d_distance = 0.0;

        /** string representation of the type bitmask. */
        public String typestr()
        {
            StringBuilder res = new StringBuilder();

            if ((type & OBJ_CIRCLE) != 0) res.append("circle | ");
            if ((type & OBJ_SLIDER) != 0) res.append("slider | ");
            if ((type & OBJ_SPINNER) != 0) res.append("spinner | ");

            String result = res.toString();
            return result.substring(0, result.length() - 3);
        }

        public String toString()
        {
            return String.format(
                    "{ time=%s, type=%s, data=%s, normpos=%s, " +
                            "strains=[ %s, %s ], is_single=%s }",
                    time, typestr(), data, normpos, strains[0], strains[1],
                    is_single
            );
        }
    }

    public static class Timing
    {
        /** start time in milliseconds. */
        public double time = 0.0;
        public double ms_per_beat = -100.0;

        /** if false, ms_per_beat is -100 * bpm_multiplier. */
        public boolean change = false;
    }

    /**
     * the bare minimum beatmap data for difficulty calculation.
     *
     * this object can be reused for multiple beatmaps without
     * re-allocation by simply calling reset()
     */
    public static class Map
    {
        public int format_version;
        public int mode;
        public String title, title_unicode;
        public String artist, artist_unicode;

        /** mapper name. */
        public String creator;

        /** difficulty name. */
        public String version;

        public int ncircles, nsliders, nspinners;
        public float hp, cs, od, ar;
        public float sv, tick_rate;

        public final ArrayList<HitObject> objects =
                new ArrayList<HitObject>(512);

        public final ArrayList<Timing> tpoints =
                new ArrayList<Timing>(32);

        public Map() { reset(); }

        /** clears the instance so that it can be reused. */
        public void reset()
        {
            title = title_unicode =
                    artist = artist_unicode =
                            creator =
                                    version = "";

            ncircles = nsliders = nspinners = 0;
            hp = cs = od = ar = 5.0f;
            sv = tick_rate = 1.0f;

            objects.clear(); tpoints.clear();
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            for (HitObject obj : objects)
            {
                sb.append(obj);
                sb.append(", ");
            }

            String objs_str = sb.toString();

            sb.setLength(0);

            for (Timing t : tpoints)
            {
                sb.append(t);
                sb.append(", ");
            }

            String timing_str = sb.toString();

            return String.format(
                    "beatmap { mode=%d, title=%s, title_unicode=%s, " +
                            "artist=%s, artist_unicode=%s, creator=%s, " +
                            "version=%s, ncircles=%d, nsliders=%d, nspinners=%d," +
                            " hp=%s, cs=%s, od=%s, ar=%s, sv=%s, tick_rate=%s, " +
                            "tpoints=[ %s ], objects=[ %s ] }",
                    mode, title, title_unicode, artist, artist_unicode,
                    creator, version, ncircles, nsliders, nspinners, hp,
                    cs, od, ar, sv, tick_rate, timing_str, objs_str
            );
        }

        public int max_combo()
        {
            int res = 0;
            int tindex = -1;
            double tnext = Double.NEGATIVE_INFINITY;
            double px_per_beat = 0.0;

            for (HitObject obj : objects)
            {
                if ((obj.type & OBJ_SLIDER) == 0)
                {
                    /* non-sliders add 1 combo */
                    ++res;
                    continue;
                }

            /* keep track of the current timing point without
            looping through all of them for every object */
                while (obj.time >= tnext)
                {
                    ++tindex;

                    if (tpoints.size() > tindex + 1) {
                        tnext = tpoints.get(tindex + 1).time;
                    } else {
                        tnext = Double.POSITIVE_INFINITY;
                    }

                    Timing t = tpoints.get(tindex);

                    double sv_multiplier = 1.0;

                    if (!t.change && t.ms_per_beat < 0) {
                        sv_multiplier = -100.0 / t.ms_per_beat;
                    }

                    px_per_beat = sv * 100.0 * sv_multiplier;
                    if (format_version < 8) {
                        px_per_beat /= sv_multiplier;
                    }
                }

                /* slider, we need to calculate slider ticks */
                Slider sl = (Slider)obj.data;

                double num_beats =
                        (sl.distance * sl.repetitions) / px_per_beat;

                int ticks = (int)
                        Math.ceil(
                                (num_beats - 0.1) / sl.repetitions * tick_rate
                        );

                --ticks;
                ticks *= sl.repetitions;
                ticks += sl.repetitions + 1;

                res += Math.max(0, ticks);
            }

            return res;
        }
    }

    /* ------------------------------------------------------------- */
    /* beatmap parser                                                */

/* note: I just let parser throw built-in exceptions instead of
error checking stuff because it's as good as making my own
exception since you can check lastline/lastpos when you catch */

    public static class Parser
    {
        /** last line touched. */
        public String lastline;

        /** last line number touched. */
        public int nline;

        /** last token touched. */
        public String lastpos;

        /** true if the parsing completed successfully. */
        public boolean done;

        /**
         * the parsed beatmap will be stored in this object.
         * willl persist throughout reset() calls and will be reused by
         * subsequent parse calls until changed.
         * @see Parser#reset
         */
        public Map beatmap = null;

        private String section; /* current section */
        private boolean ar_found = false;

        public Parser() { reset(); }

        private void reset()
        {
            lastline = lastpos = section = "";
            nline = 0;
            done = false;
            if (beatmap != null) {
                beatmap.reset();
            }
        }

        public String toString()
        {
            return String.format(
                    "in line %d\n%s\n> %s", nline, lastline, lastpos
            );
        }

        private void warn(String fmt, Object... args)
        {
            info("W: ");
            info(fmt, args);
            info("\n%s\n", this);
        }

        /**
         * trims v, sets lastpos to it and returns trimmed v.
         * should be used to access any string that can make the parser
         * fail
         */
        private String setlastpos(String v)
        {
            v = v.trim();
            lastpos = v;
            return v;
        }

        private String[] property()
        {
            String[] split = lastline.split(":", 2);
            split[0] = setlastpos(split[0]);
            if (split.length > 1) {
                split[1] = setlastpos(split[1]);
            }
        /* why does java have such inconsistent naming? ArrayList
        length is .size(), normal array length is .length, string
        length is .length(). why do I have to look up documentation
        for stuff that should have the same interface? */
            return split;
        }

        private void metadata()
        {
            String[] p = property();

            if (p[0].equals("Title")) {
                beatmap.title = p[1];
            }
            else if (p[0].equals("TitleUnicode")) {
                beatmap.title_unicode = p[1];
            }
            else if (p[0].equals("Artist")) {
                beatmap.artist = p[1];
            }
            else if (p[0].equals("ArtistUnicode")) {
                beatmap.artist_unicode = p[1];
            }
            else if (p[0].equals("Creator")) {
                beatmap.creator = p[1];
            }
            else if (p[0].equals("Version")) {
                beatmap.version = p[1];
            }
        }

        private void general()
        {
            String[] p = property();

            if (p[0].equals("Mode"))
            {
                beatmap.mode = Integer.parseInt(setlastpos(p[1]));

                if (beatmap.mode != MODE_STD)
                {
                    throw new UnsupportedOperationException(
                            "this gamemode is not yet supported"
                    );
                }
            }
        }

        private void difficulty()
        {
            String[] p = property();

            /* what's up with the redundant Float.parseFloat ?_? */
            if (p[0].equals("CircleSize")) {
                beatmap.cs = Float.parseFloat(setlastpos(p[1]));
            }
            else if (p[0].equals("OverallDifficulty")) {
                beatmap.od = Float.parseFloat(setlastpos(p[1]));
            }
            else if (p[0].equals("ApproachRate")) {
                beatmap.ar = Float.parseFloat(setlastpos(p[1]));
                ar_found = true;
            }
            else if (p[0].equals("HPDrainRate")) {
                beatmap.hp = Float.parseFloat(setlastpos(p[1]));
            }
            else if (p[0].equals("SliderMultiplier")) {
                beatmap.sv = Float.parseFloat(setlastpos(p[1]));
            }
            else if (p[0].equals("SliderTickRate")) {
                beatmap.tick_rate = Float.parseFloat(setlastpos(p[1]));
            }
        }

        private void timing()
        {
            String[] s = lastline.split(",");

            if (s.length > 8) {
                warn("timing point with trailing values");
            }

            Timing t = new Timing();
            t.time = Double.parseDouble(setlastpos(s[0]));
            t.ms_per_beat = Double.parseDouble(setlastpos(s[1]));

            if (s.length >= 7) {
                t.change = !s[6].trim().equals("0");
            }

            beatmap.tpoints.add(t);
        }

        private void objects()
        {
            String[] s = lastline.split(",");

            if (s.length > 11) {
                warn("object with trailing values");
            }

            HitObject obj = new HitObject();
            obj.time = Double.parseDouble(setlastpos(s[2]));
            obj.type = Integer.parseInt(setlastpos(s[3]));

            if ((obj.type & OBJ_CIRCLE) != 0)
            {
                ++beatmap.ncircles;
                Circle c = new Circle();
                c.pos.x = Double.parseDouble(setlastpos(s[0]));
                c.pos.y = Double.parseDouble(setlastpos(s[1]));
                obj.data = c;
            }

            else if ((obj.type & OBJ_SPINNER) != 0) {
                ++beatmap.nspinners;
            }

            else if ((obj.type & OBJ_SLIDER) != 0)
            {
                ++beatmap.nsliders;
                Slider sli = new Slider();
                sli.pos.x = Double.parseDouble(setlastpos(s[0]));
                sli.pos.y = Double.parseDouble(setlastpos(s[1]));
                sli.repetitions = Integer.parseInt(setlastpos(s[6]));
                sli.distance = Double.parseDouble(setlastpos(s[7]));
                obj.data = sli;
            }

            beatmap.objects.add(obj);
        }

        /**
         * calls reset() on beatmap and parses a osu file into it.
         * if beatmap is null, it will be initialized to a new Map
         * @return this.beatmap
         * @throws IOException
         */
        public Map map(BufferedReader reader) throws IOException
        {
            String line = null;

            if (beatmap == null) {
                beatmap = new Map();
            }

            reset();

            while ((line = reader.readLine()) != null)
            {
                lastline = line;
                ++nline;

                /* comments (according to lazer) */
                if (line.startsWith(" ") || line.startsWith("_")) {
                    continue;
                }

                line = lastline = line.trim();
                if (line.length() <= 0) {
                    continue;
                }

                /* c++ style comments */
                if (line.startsWith("//")) {
                    continue;
                }

                /* [SectionName] */
                if (line.startsWith("[")) {
                    section = line.substring(1, line.length() - 1);
                    continue;
                }

                try
                {
                    if (section.equals("Metadata"))
                        metadata();
                    else if (section.equals("General"))
                        general();
                    else if (section.equals("Difficulty"))
                        difficulty();
                    else if (section.equals("TimingPoints"))
                        timing();
                    else if (section.equals("HitObjects"))
                        objects();
                    else {
                        int fmt_index = line.indexOf("file format v");
                        if (fmt_index < 0) {
                            continue;
                        }

                        beatmap.format_version = Integer.parseInt(
                                line.substring(fmt_index + 13)
                        );
                    }
                }

                catch (NumberFormatException e) {
                    warn("ignoring line with bad number");
                } catch (ArrayIndexOutOfBoundsException e) {
                    warn("ignoring malformed line");
                }
            }

            if (!ar_found) {
                beatmap.ar = beatmap.od;
            }

            done = true;
            return beatmap;
        }

        /**
         * sets beatmap and returns map(reader)
         * @return this.beatmap
         * @throws IOException
         */
        public Map map(BufferedReader reader, Map beatmap)
                throws IOException
        {
            this.beatmap = beatmap;
            return map(reader);
        }
    }

    /* ------------------------------------------------------------- */
    /* mods utils                                                    */

    public static final int MODS_NOMOD = 0;

    public static final int MODS_NF = 1<<0;
    public static final int MODS_EZ = 1<<1;
    public static final int MODS_TOUCH_DEVICE = 1<<2;
    public static final int MODS_TD = MODS_TOUCH_DEVICE;
    public static final int MODS_HD = 1<<3;
    public static final int MODS_HR = 1<<4;
    public static final int MODS_DT = 1<<6;
    public static final int MODS_HT = 1<<8;
    public static final int MODS_NC = 1<<9;
    public static final int MODS_FL = 1<<10;
    public static final int MODS_SO = 1<<12;

    public static final int MODS_SPEED_CHANGING =
            MODS_DT | MODS_HT | MODS_NC;

    public static final int MODS_MAP_CHANGING =
            MODS_HR | MODS_EZ | MODS_SPEED_CHANGING;

    /** @return a string representation of the mods, such as HDDT */
    public static
    String mods_str(int mods)
    {
        StringBuilder sb = new StringBuilder();

        if ((mods & MODS_NF) != 0) {
            sb.append("NF");
        }

        if ((mods & MODS_EZ) != 0) {
            sb.append("EZ");
        }

        if ((mods & MODS_TOUCH_DEVICE) != 0) {
            sb.append("TD");
        }

        if ((mods & MODS_HD) != 0) {
            sb.append("HD");
        }

        if ((mods & MODS_HR) != 0) {
            sb.append("HR");
        }

        if ((mods & MODS_NC) != 0) {
            sb.append("NC");
        } else if ((mods & MODS_DT) != 0) {
            sb.append("DT");
        }

        if ((mods & MODS_HT) != 0) {
            sb.append("HT");
        }

        if ((mods & MODS_FL) != 0) {
            sb.append("FL");
        }

        if ((mods & MODS_SO) != 0) {
            sb.append("SO");
        }

        return sb.toString();
    }

    /** @return mod bitmask from the string representation */
    public static
    int mods_from_str(String str)
    {
        int mask = 0;

        while (str.length() > 0)
        {
            if (str.startsWith("NF")) mask |= MODS_NF;
            else if (str.startsWith("EZ")) mask |= MODS_EZ;
            else if (str.startsWith("TD")) mask |= MODS_TOUCH_DEVICE;
            else if (str.startsWith("HD")) mask |= MODS_HD;
            else if (str.startsWith("HR")) mask |= MODS_HR;
            else if (str.startsWith("DT")) mask |= MODS_DT;
            else if (str.startsWith("HT")) mask |= MODS_HT;
            else if (str.startsWith("NC")) mask |= MODS_NC;
            else if (str.startsWith("FL")) mask |= MODS_FL;
            else if (str.startsWith("SO")) mask |= MODS_SO;
            else {
                str = str.substring(1);
                continue;
            }
            str = str.substring(2);
        }

        return mask;
    }

    /**
     * beatmap stats with mods applied.
     * should be populated with the base beatmap stats and passed to
     * mods_apply which will modify the stats for the given mods
     */
    public static class MapStats
    {
        float ar, od, cs, hp;

        /**
         * speed multiplier / music rate.
         * this doesn't need to be initialized before calling mods_apply
         */
        float speed;
    }

    private static final double OD0_MS = 80;
    private static final double OD10_MS = 20;
    private static final double AR0_MS = 1800.0;
    private static final double AR5_MS = 1200.0;
    private static final double AR10_MS = 450.0;

    private static final double OD_MS_STEP = (OD0_MS - OD10_MS) / 10.0;
    private static final double AR_MS_STEP1 = (AR0_MS - AR5_MS) / 5.0;
    private static final double AR_MS_STEP2 = (AR5_MS - AR10_MS) / 5.0;

    private static final int APPLY_AR = 1<<0;
    private static final int APPLY_OD = 1<<1;
    private static final int APPLY_CS = 1<<2;
    private static final int APPLY_HP = 1<<3;

    /**
     * applies mods to mapstats.
     *
     * <blockquote><pre>
     * Koohii.MapStats mapstats = new Koohii.MapStats();
     * mapstats.ar = 9;
     * Koohii.mods_apply(Koohii.MODS_DT, mapstats, Koohii.APPLY_AR);
     * // mapstats.ar is now 10.33, mapstats.speed is 1.5
     * </pre></blockquote>
     *
     * @param mapstats the base beatmap stats
     * @param flags bitmask that specifies which stats to modify. only
     *              the stats specified here need to be initialized in
     *              mapstats.
     * @return mapstats
     * @see MapStats
     */
    public static
    MapStats mods_apply(int mods, MapStats mapstats, int flags)
    {
        mapstats.speed = 1.0f;

        if ((mods & MODS_MAP_CHANGING) == 0) {
            return mapstats;
        }

        if ((mods & (MODS_DT | MODS_NC)) != 0) {
            mapstats.speed = 1.5f;
        }

        if ((mods & MODS_HT) != 0) {
            mapstats.speed *= 0.75f;
        }

        float od_ar_hp_multiplier = 1.0f;

        if ((mods & MODS_HR) != 0) {
            od_ar_hp_multiplier = 1.4f;
        }

        if ((mods & MODS_EZ) != 0) {
            od_ar_hp_multiplier *= 0.5f;
        }

        if ((flags & APPLY_AR) != 0)
        {
            mapstats.ar *= od_ar_hp_multiplier;

            /* convert AR into milliseconds window */
            double arms = mapstats.ar < 5.0f ?
                    AR0_MS - AR_MS_STEP1 * mapstats.ar
                    : AR5_MS - AR_MS_STEP2 * (mapstats.ar - 5.0f);

        /* stats must be capped to 0-10 before HT/DT which brings
        them to a range of -4.42->11.08 for OD and -5->11 for AR */
            arms = Math.min(AR0_MS, Math.max(AR10_MS, arms));
            arms /= mapstats.speed;

            mapstats.ar = (float)(
                    arms > AR5_MS ?
                            (AR0_MS - arms) / AR_MS_STEP1
                            : 5.0 + (AR5_MS - arms) / AR_MS_STEP2
            );
        }

        if ((flags & APPLY_OD) != 0)
        {
            mapstats.od *= od_ar_hp_multiplier;
            double odms = OD0_MS - Math.ceil(OD_MS_STEP * mapstats.od);
            odms = Math.min(OD0_MS, Math.max(OD10_MS, odms));
            odms /= mapstats.speed;
            mapstats.od = (float)((OD0_MS - odms) / OD_MS_STEP);
        }

        if ((flags & APPLY_CS) != 0)
        {
            if ((mods & MODS_HR) != 0) {
                mapstats.cs *= 1.3f;
            }

            if ((mods & MODS_EZ) != 0) {
                mapstats.cs *= 0.5f;
            }

            mapstats.cs = Math.min(10.0f, mapstats.cs);
        }

        if ((flags & APPLY_HP) != 0)
        {
            mapstats.hp =
                    Math.min(10.0f, mapstats.hp * od_ar_hp_multiplier);
        }

        return mapstats;
    }

    /* ------------------------------------------------------------- */
    /* difficulty calculator                                         */

    /**
     * arbitrary thresholds to determine when a stream is spaced
     * enough that it becomes hard to alternate.
     */
    private final static double SINGLE_SPACING = 125.0;

    /** strain decay per interval. */
    private final static double[] DECAY_BASE = { 0.3, 0.15 };

    /** balances speed and aim. */
    private final static double[] WEIGHT_SCALING = { 1400.0, 26.25 };

    /**
     * max strains are weighted from highest to lowest, this is how
     * much the weight decays.
     */
    private final static double DECAY_WEIGHT = 0.9;

    /**
     * strains are calculated by analyzing the map in chunks and taking
     * the peak strains in each chunk. this is the length of a strain
     * interval in milliseconds
     */
    private final static double STRAIN_STEP = 400.0;

    /** non-normalized diameter where the small circle buff starts. */
    private final static double CIRCLESIZE_BUFF_THRESHOLD = 30.0;

    /** global stars multiplier. */
    private final static double STAR_SCALING_FACTOR = 0.0675;

    /** in osu! pixels */
    private final static double PLAYFIELD_WIDTH = 512.0,
            PLAYFIELD_HEIGHT = 384.0;

    private final static Vector2 PLAYFIELD_CENTER = new Vector2(
            PLAYFIELD_WIDTH / 2.0, PLAYFIELD_HEIGHT / 2.0
    );

    /**
     * 50% of the difference between aim and speed is added to total
     * star rating to compensate for aim/speed only maps
     */
    private final static double EXTREME_SCALING_FACTOR = 0.5;

    private final static double MIN_SPEED_BONUS = 75.0;
    private final static double MAX_SPEED_BONUS = 45.0;
    private final static double ANGLE_BONUS_SCALE = 90.0;
    private final static double AIM_TIMING_THRESHOLD = 107;
    private final static double SPEED_ANGLE_BONUS_BEGIN = 5 * Math.PI / 6;
    private final static double AIM_ANGLE_BONUS_BEGIN = Math.PI / 3;

    private static
    double d_spacing_weight(int type, double distance, double delta_time,
                            double prev_distance, double prev_delta_time, double angle)
    {
        double strain_time = Math.max(delta_time, 50.0);
        double prev_strain_time = Math.max(prev_delta_time, 50.0);
        double angle_bonus;
        switch (type)
        {
            case DIFF_AIM: {
                double result = 0.0;
                if (!Double.isNaN(angle) && angle > AIM_ANGLE_BONUS_BEGIN) {
                    angle_bonus = Math.sqrt(
                            Math.max(prev_distance - ANGLE_BONUS_SCALE, 0.0) *
                                    Math.pow(Math.sin(angle - AIM_ANGLE_BONUS_BEGIN), 2.0) *
                                    Math.max(distance - ANGLE_BONUS_SCALE, 0.0)
                    );
                    result = (
                            1.5 * Math.pow(Math.max(0.0, angle_bonus), 0.99) /
                                    Math.max(AIM_TIMING_THRESHOLD, prev_strain_time)
                    );
                }
                double weighted_distance = Math.pow(distance, 0.99);
                return Math.max(result +
                                weighted_distance /
                                        Math.max(AIM_TIMING_THRESHOLD, strain_time),
                        weighted_distance / strain_time);
            }

            case DIFF_SPEED: {
                distance = Math.min(distance, SINGLE_SPACING);
                delta_time = Math.max(delta_time, MAX_SPEED_BONUS);
                double speed_bonus = 1.0;
                if (delta_time < MIN_SPEED_BONUS) {
                    speed_bonus +=
                            Math.pow((MIN_SPEED_BONUS - delta_time) / 40.0, 2);
                }
                angle_bonus = 1.0;
                if (!Double.isNaN(angle) && angle < SPEED_ANGLE_BONUS_BEGIN) {
                    double s = Math.sin(1.5 * (SPEED_ANGLE_BONUS_BEGIN - angle));
                    angle_bonus += Math.pow(s, 2) / 3.57;
                    if (angle < Math.PI / 2.0) {
                        angle_bonus = 1.28;
                        if (distance < ANGLE_BONUS_SCALE && angle < Math.PI / 4.0) {
                            angle_bonus += (1.0 - angle_bonus) *
                                    Math.min((ANGLE_BONUS_SCALE - distance) / 10.0, 1.0);
                        } else if (distance < ANGLE_BONUS_SCALE) {
                            angle_bonus += (1.0 - angle_bonus) *
                                    Math.min((ANGLE_BONUS_SCALE - distance) / 10.0, 1.0) *
                                    Math.sin((Math.PI / 2.0 - angle) * 4.0 / Math.PI);
                        }
                    }
                }
                return (
                        (1 + (speed_bonus - 1) * 0.75) * angle_bonus *
                                (0.95 + speed_bonus * Math.pow(distance / SINGLE_SPACING, 3.5))
                ) / strain_time;
            }
        }

        throw new UnsupportedOperationException(
                "this difficulty type does not exist"
        );
    }

    /**
     * calculates the strain for one difficulty type and stores it in
     * obj. this assumes that normpos is already computed.
     * this also sets is_single if type is DIFF_SPEED
     */
    private static
    void d_strain(int type, HitObject obj, HitObject prev,
                  double speed_mul)
    {
        double value = 0.0;
        double time_elapsed = (obj.time - prev.time) / speed_mul;
        double decay =
                Math.pow(DECAY_BASE[type], time_elapsed / 1000.0);

        obj.delta_time = time_elapsed;

        /* this implementation doesn't account for sliders */
        if ((obj.type & (OBJ_SLIDER | OBJ_CIRCLE)) != 0)
        {
            double distance =
                    new Vector2(obj.normpos).sub(prev.normpos).len();
            obj.d_distance = distance;

            if (type == DIFF_SPEED) {
                obj.is_single = distance > SINGLE_SPACING;
            }

            value = d_spacing_weight(type, distance, time_elapsed,
                    prev.d_distance, prev.delta_time, obj.angle);
            value *= WEIGHT_SCALING[type];
        }

        obj.strains[type] = prev.strains[type] * decay + value;
    }

    /**
     * difficulty calculator, can be reused in subsequent calc() calls.
     */
    public static class DiffCalc
    {
        /** star rating. */
        public double total;

        /** aim stars. */
        public double aim;

        /** aim difficulty (used to calc length bonus) */
        public double aim_difficulty;

        /** aim length bonus (unused at the moment) */
        public double aim_length_bonus;

        /** speed stars. */
        public double speed;

        /** speed difficulty (used to calc length bonus) */
        public double speed_difficulty;

        /** speed length bonus (unused at the moment) */
        public double speed_length_bonus;

        /**
         * number of notes that are considered singletaps by the
         * difficulty calculator.
         */
        public int nsingles;

        /**
         * number of taps slower or equal to the singletap threshold
         * value.
         */
        public int nsingles_threshold;

        /**
         * the beatmap we want to calculate the difficulty for.
         * must be set or passed to calc() explicitly.
         * persists across calc() calls unless it's changed or explicity
         * passed to calc()
         * @see DiffCalc#calc(Koohii.Map, int, double)
         * @see DiffCalc#calc(Koohii.Map, int)
         * @see DiffCalc#calc(Koohii.Map)
         */
        public Map beatmap = null;

        private double speed_mul;
        private final ArrayList<Double> strains =
                new ArrayList<Double>(512);

        public DiffCalc() { reset(); }

        /** sets up the instance for re-use by resetting fields. */
        private void reset()
        {
            total = aim = speed = 0.0;
            nsingles = nsingles_threshold = 0;
            speed_mul = 1.0;
        }

        public String toString()
        {
            return String.format("%s stars (%s aim, %s speed)",
                    total, aim, speed);
        }

        private static double length_bonus(double stars, double difficulty) {
            return (
                    0.32 + 0.5 *
                            (Math.log10(difficulty + stars) - Math.log10(stars))
            );
        }

        private class DiffValues
        {
            public double difficulty, total;

            public DiffValues(double difficulty, double total) {
                this.difficulty = difficulty;
                this.total = total;
            }
        };

        private DiffValues calc_individual(int type)
        {
            strains.clear();

            double strain_step = STRAIN_STEP * speed_mul;
            /* the first object doesn't generate a strain
             * so we begin with an incremented interval end */
            double interval_end = (
                    Math.ceil(beatmap.objects.get(0).time / strain_step)
                            * strain_step
            );
            double max_strain = 0.0;

            /* calculate all strains */
            for (int i = 0; i < beatmap.objects.size(); ++i)
            {
                HitObject obj = beatmap.objects.get(i);
                HitObject prev = i > 0 ?
                        beatmap.objects.get(i - 1) : null;

                if (prev != null) {
                    d_strain(type, obj, prev, speed_mul);
                }

                while (obj.time > interval_end)
                {
                    /* add max strain for this interval */
                    strains.add(max_strain);

                    if (prev != null)
                    {
                    /* decay last object's strains until the next
                    interval and use that as the initial max
                    strain */
                        double decay = Math.pow(DECAY_BASE[type],
                                (interval_end - prev.time) / 1000.0);

                        max_strain = prev.strains[type] * decay;
                    } else {
                        max_strain = 0.0;
                    }

                    interval_end += strain_step;
                }

                max_strain = Math.max(max_strain, obj.strains[type]);
            }

            /* don't forget to add the last strain interval */
            strains.add(max_strain);

            /* weigh the top strains sorted from highest to lowest */
            double weight = 1.0;
            double total = 0.0;
            double difficulty = 0.0;

            Collections.sort(strains, Collections.reverseOrder());

            for (Double strain : strains)
            {
                total += Math.pow(strain, 1.2);
                difficulty += strain * weight;
                weight *= DECAY_WEIGHT;
            }

            return new DiffValues(difficulty, total);
        }

        /**
         * default value for singletap_threshold.
         * @see DiffCalc#calc
         */
        public final static double DEFAULT_SINGLETAP_THRESHOLD = 125.0;

        /**
         * calculates beatmap difficulty and stores it in total, aim,
         * speed, nsingles, nsingles_speed fields.
         * @param singletap_threshold the smallest milliseconds interval
         *        that will be considered singletappable. for example,
         *        125ms is 240 1/2 singletaps ((60000 / 240) / 2)
         * @return self
         */
        public DiffCalc calc(int mods, double singletap_threshold)
        {
            reset();

            MapStats mapstats = new MapStats();
            mapstats.cs = beatmap.cs;
            mods_apply(mods, mapstats, APPLY_CS);
            speed_mul = mapstats.speed;

            double radius = (PLAYFIELD_WIDTH / 16.0) *
                    (1.0 - 0.7 * (mapstats.cs - 5.0) / 5.0);

        /* positions are normalized on circle radius so that we can
        calc as if everything was the same circlesize */
            double scaling_factor = 52.0 / radius;

            if (radius < CIRCLESIZE_BUFF_THRESHOLD)
            {
                scaling_factor *= 1.0 +
                        Math.min(CIRCLESIZE_BUFF_THRESHOLD - radius, 5.0) / 50.0;
            }

            Vector2 normalized_center =
                    new Vector2(PLAYFIELD_CENTER).mul(scaling_factor);

            HitObject prev1 = null;
            HitObject prev2 = null;
            int i = 0;

            /* calculate normalized positions */
            for (HitObject obj : beatmap.objects)
            {
                if ((obj.type & OBJ_SPINNER) != 0) {
                    obj.normpos = new Vector2(normalized_center);
                }

                else
                {
                    Vector2 pos;

                    if ((obj.type & OBJ_SLIDER) != 0) {
                        pos = ((Slider)obj.data).pos;
                    }

                    else if ((obj.type & OBJ_CIRCLE) != 0) {
                        pos = ((Circle)obj.data).pos;
                    }

                    else
                    {
                        info(
                                "W: unknown object type %08X\n",
                                obj.type
                        );
                        pos = new Vector2();
                    }

                    obj.normpos = new Vector2(pos).mul(scaling_factor);
                }

                if (i >= 2) {
                    Vector2 v1 = new Vector2(prev2.normpos).sub(prev1.normpos);
                    Vector2 v2 = new Vector2(obj.normpos).sub(prev1.normpos);
                    double dot = v1.dot(v2);
                    double det = v1.x * v2.y - v1.y * v2.x;
                    obj.angle = Math.abs(Math.atan2(det, dot));
                } else {
                    obj.angle = Double.NaN;
                }

                prev2 = prev1;
                prev1 = obj;
                ++i;
            }

            /* speed and aim stars */

            DiffValues aimvals = calc_individual(DIFF_AIM);
            aim = aimvals.difficulty;
            aim_difficulty = aimvals.total;
            aim_length_bonus = length_bonus(aim, aim_difficulty);

            DiffValues speedvals = calc_individual(DIFF_SPEED);
            speed = speedvals.difficulty;
            speed_difficulty = speedvals.total;
            speed_length_bonus = length_bonus(speed, speed_difficulty);

            aim = Math.sqrt(aim) * STAR_SCALING_FACTOR;
            speed = Math.sqrt(speed) * STAR_SCALING_FACTOR;
            if ((mods & MODS_TOUCH_DEVICE) != 0) {
                aim = Math.pow(aim, 0.8);
            }

            /* total stars */
            total = aim + speed +
                    Math.abs(speed - aim) * EXTREME_SCALING_FACTOR;

            /* singletap stats */
            for (i = 1; i < beatmap.objects.size(); ++i)
            {
                HitObject prev = beatmap.objects.get(i - 1);
                HitObject obj = beatmap.objects.get(i);

                if (obj.is_single) {
                    ++nsingles;
                }

                if ((obj.type & (OBJ_CIRCLE | OBJ_SLIDER)) == 0) {
                    continue;
                }

                double interval = (obj.time - prev.time) / speed_mul;

                if (interval >= singletap_threshold) {
                    ++nsingles_threshold;
                }
            }

            return this;
        }

        /**
         * @return calc(mods, DEFAULT_SINGLETAP_THRESHOLD)
         * @see DiffCalc#calc(int, double)
         * @see DiffCalc#DEFAULT_SINGLETAP_THRESHOLD
         */
        public DiffCalc calc(int mods) {
            return calc(mods, DEFAULT_SINGLETAP_THRESHOLD);
        }

        /**
         * @return calc(MODS_NOMOD, DEFAULT_SINGLETAP_THRESHOLD)
         * @see DiffCalc#calc(int, double)
         * @see DiffCalc#DEFAULT_SINGLETAP_THRESHOLD
         */
        public DiffCalc calc() {
            return calc(MODS_NOMOD, DEFAULT_SINGLETAP_THRESHOLD);
        }

        /**
         * sets beatmap field and calls
         * calc(mods, singletap_threshold).
         * @see DiffCalc#calc(int, double)
         */
        public DiffCalc calc(Map beatmap, int mods,
                             double singletap_threshold)
        {
            this.beatmap = beatmap;
            return calc(mods, singletap_threshold);
        }

        /**
         * sets beatmap field and calls
         * calc(mods, DEFAULT_SINGLETAP_THRESHOLD).
         * @see DiffCalc#calc(int, double)
         * @see DiffCalc#DEFAULT_SINGLETAP_THRESHOLD
         */
        public DiffCalc calc(Map beatmap, int mods) {
            return calc(beatmap, mods, DEFAULT_SINGLETAP_THRESHOLD);
        }

        /**
         * sets beatmap field and calls
         * calc(MODS_NOMOD, DEFAULT_SINGLETAP_THRESHOLD).
         * @see DiffCalc#calc(int, double)
         * @see DiffCalc#DEFAULT_SINGLETAP_THRESHOLD
         */
        public DiffCalc calc(Map beatmap)
        {
            return calc(beatmap, MODS_NOMOD,
                    DEFAULT_SINGLETAP_THRESHOLD);
        }
    }

    /* ------------------------------------------------------------- */
    /* acc calc                                                      */

    public static class Accuracy
    {
        public int n300 = 0, n100 = 0, n50 = 0, nmisses = 0;

        public Accuracy() {}

        /**
         * @param n300 the number of 300s, if -1 it will be calculated
         *             from the object count in Accuracy#value(int).
         */
        public Accuracy(int n300, int n100, int n50, int nmisses)
        {
            this.n300 = n300;
            this.n100 = n100;
            this.n50 = n50;
            this.nmisses = nmisses;
        }

        /**
         * calls Accuracy(-1, n100, n50, nmisses) .
         * @see Accuracy#Accuracy(int, int, int, int)
         */
        public Accuracy(int n100, int n50, int nmisses) {
            this(-1, n100, n50, nmisses);
        }

        /**
         * calls Accuracy(-1, n100, n50, 0) .
         * @see Accuracy#Accuracy(int, int, int, int)
         */
        public Accuracy(int n100, int n50) {
            this(-1, n100, n50, 0);
        }

        /**
         * calls Accuracy(-1, n100, 0, 0) .
         * @see Accuracy#Accuracy(int, int, int, int)
         */
        public Accuracy(int n100) {
            this(-1, n100, 0, 0);
        }

        /**
         * rounds to the closest amount of 300s, 100s, 50s for a given
         * accuracy percentage.
         * @param nobjects the total number of hits (n300 + n100 + n50 +
         *        nmisses)
         */
        public Accuracy(double acc_percent, int nobjects, int nmisses)
        {
            nmisses = Math.min(nobjects, nmisses);
            int max300 = nobjects - nmisses;

            double maxacc =
                    new Accuracy(max300, 0, 0, nmisses).value() * 100.0;

            acc_percent = Math.max(0.0, Math.min(maxacc, acc_percent));

            /* just some black magic maths from wolfram alpha */
            n100 = (int)
                    Math.round(
                            -3.0 *
                                    ((acc_percent * 0.01 - 1.0) * nobjects + nmisses) *
                                    0.5
                    );

            if (n100 > max300)
            {
                /* acc lower than all 100s, use 50s */
                n100 = 0;

                n50 = (int)
                        Math.round(
                                -6.0 *
                                        ((acc_percent * 0.01 - 1.0) * nobjects +
                                                nmisses) * 0.5
                        );

                n50 = Math.min(max300, n50);
            }

            n300 = nobjects - n100 - n50 - nmisses;
        }

        /**
         * @param nobjects the total number of hits (n300 + n100 + n50 +
         *                 nmiss). if -1, n300 must have been set and
         *                 will be used to deduce this value.
         * @return the accuracy value (0.0-1.0)
         */
        public double value(int nobjects)
        {
            if (nobjects < 0 && n300 < 0)
            {
                throw new IllegalArgumentException(
                        "either nobjects or n300 must be specified"
                );
            }

            int n300_ = n300 > 0 ? n300 :
                    nobjects - n100 - n50 - nmisses;

            if (nobjects < 0) {
                nobjects = n300_ + n100 + n50 + nmisses;
            }

            double res = (n50 * 50.0 + n100 * 100.0 + n300_ * 300.0) /
                    (nobjects * 300.0);

            return Math.max(0, Math.min(res, 1.0));
        }

        /**
         * calls value(-1) .
         * @see Accuracy#value(int)
         */
        public double value() {
            return value(-1);
        }
    }

    /* ------------------------------------------------------------- */
    /* pp calc                                                       */

    /* base pp value for stars, used internally by ppv2 */
    private static
    double pp_base(double stars)
    {
        return Math.pow(5.0 * Math.max(1.0, stars / 0.0675) - 4.0, 3.0)
                / 100000.0;
    }

    /**
     * parameters to be passed to PPv2.
     * aim_stars, speed_stars, max_combo, nsliders, ncircles, nobjects,
     * base_ar, base_od are required.
     * @see PPv2#PPv2(Koohii.PPv2Parameters)
     */
    public static class PPv2Parameters
    {
        /**
         * if not null, max_combo, nsliders, ncircles, nobjects,
         * base_ar, base_od will be obtained from this beatmap.
         */
        public Map beatmap = null;

        public double aim_stars = 0.0;
        public double speed_stars = 0.0;
        public int max_combo = 0;
        public int nsliders = 0, ncircles = 0, nobjects = 0;

        /** the base AR (before applying mods). */
        public float base_ar = 5.0f;

        /** the base OD (before applying mods). */
        public float base_od = 5.0f;

        /** gamemode. */
        public int mode = MODE_STD;

        /** the mods bitmask, same as osu! api, see MODS_* constants */
        public int mods = MODS_NOMOD;

        /**
         * the maximum combo achieved, if -1 it will default to
         * max_combo - nmiss .
         */
        public int combo = -1;

        /**
         * number of 300s, if -1 it will default to
         * nobjects - n100 - n50 - nmiss .
         */
        public int n300 = -1;
        public int n100 = 0, n50 = 0, nmiss = 0;

        /** scorev1 (1) or scorev2 (2). */
        public int score_version = 1;
    }

    public static class PPv2
    {
        public double total, aim, speed, acc;
        public Accuracy computed_accuracy;

        /**
         * calculates ppv2, results are stored in total, aim, speed,
         * acc, acc_percent.
         * @see PPv2Parameters
         */
        private PPv2(double aim_stars, double speed_stars,
                     int max_combo, int nsliders, int ncircles, int nobjects,
                     float base_ar, float base_od, int mode, int mods,
                     int combo, int n300, int n100, int n50, int nmiss,
                     int score_version, Map beatmap)
        {
            if (beatmap != null)
            {
                mode = beatmap.mode;
                base_ar = beatmap.ar;
                base_od = beatmap.od;
                max_combo = beatmap.max_combo();
                nsliders = beatmap.nsliders;
                ncircles = beatmap.ncircles;
                nobjects = beatmap.objects.size();
            }

            if (mode != MODE_STD)
            {
                throw new UnsupportedOperationException(
                        "this gamemode is not yet supported"
                );
            }

            if (max_combo <= 0)
            {
                info("W: max_combo <= 0, changing to 1\n");
                max_combo = 1;
            }

            if (combo < 0) {
                combo = max_combo - nmiss;
            }

            if (n300 < 0) {
                n300 = nobjects - n100 - n50 - nmiss;
            }

            /* accuracy -------------------------------------------- */
            computed_accuracy = new Accuracy(n300, n100, n50, nmiss);
            double accuracy = computed_accuracy.value();
            double real_acc = accuracy;

        /* scorev1 ignores sliders since they are free 300s
        and for some reason also ignores spinners */
            int nspinners = nobjects - nsliders - ncircles;

            switch (score_version)
            {
                case 1:
                    real_acc = new Accuracy(n300 - nsliders - nspinners,
                            n100, n50, nmiss).value();

                    real_acc = Math.max(0.0, real_acc);
                    break;

                case 2:
                    ncircles = nobjects;
                    break;

                default:
                    throw new UnsupportedOperationException(
                            String.format("unsupported scorev%d",
                                    score_version)
                    );
            }

            /* global values --------------------------------------- */
            double nobjects_over_2k = nobjects / 2000.0;

            double length_bonus = 0.95 + 0.4 *
                    Math.min(1.0, nobjects_over_2k);

            if (nobjects > 2000) {
                length_bonus += Math.log10(nobjects_over_2k) * 0.5;
            }

            double miss_penality_aim =
                    0.97 * Math.pow(1 - Math.pow((double)nmiss / nobjects, 0.775), nmiss);
            double miss_penality_speed =
                    0.97 * Math.pow(1 - Math.pow((double)nmiss / nobjects, 0.775), Math.pow(nmiss, 0.875));
            double combo_break = Math.pow(combo, 0.8) /
                    Math.pow(max_combo, 0.8);

            /* calculate stats with mods */
            MapStats mapstats = new MapStats();
            mapstats.ar = base_ar;
            mapstats.od = base_od;
            mods_apply(mods, mapstats, APPLY_AR | APPLY_OD);

            /* ar bonus -------------------------------------------- */
            double ar_bonus = 0.0;

            if (mapstats.ar > 10.33) {
                ar_bonus += 0.4 * (mapstats.ar - 10.33);
            }

            else if (mapstats.ar < 8.0) {
                ar_bonus +=  0.1 * (8.0 - mapstats.ar);
            }

            /* aim pp ---------------------------------------------- */
            aim = pp_base(aim_stars);
            aim *= length_bonus;
            if (nmiss > 0) {
                aim *= miss_penality_aim;
            }
            aim *= combo_break;
            aim *= 1.0 + Math.min(ar_bonus, ar_bonus * (nobjects / 1000.0));

            double hd_bonus = 1.0;
            if ((mods & MODS_HD) != 0) {
                hd_bonus *= 1.0 + 0.04 * (12.0 - mapstats.ar);
            }
            aim *= hd_bonus;

            if ((mods & MODS_FL) != 0) {
                double fl_bonus = 1.0 + 0.35 * Math.min(1.0, nobjects / 200.0);
                if (nobjects > 200) {
                    fl_bonus += 0.3 * Math.min(1.0, (nobjects - 200) / 300.0);
                }
                if (nobjects > 500) {
                    fl_bonus += (nobjects - 500) / 1200.0;
                }
                aim *= fl_bonus;
            }

            double acc_bonus = 0.5 + accuracy / 2.0;
            double od_squared = mapstats.od * mapstats.od;
            double od_bonus = 0.98 + od_squared / 2500.0;

            aim *= acc_bonus;
            aim *= od_bonus;

            /* speed pp -------------------------------------------- */
            speed = pp_base(speed_stars);
            speed *= length_bonus;
            if (nmiss > 0) {
                speed *= miss_penality_speed;
            }
            speed *= combo_break;
            if (mapstats.ar > 10.33) {
                speed *= 1.0 + Math.min(ar_bonus, ar_bonus * (nobjects / 1000.0));
            }
            speed *= hd_bonus;

            /* similar to aim acc and od bonus */
            speed *= (0.95 + od_squared / 750.0) *
                    Math.pow(accuracy, (14.5 - Math.max(mapstats.od, 8)) / 2);
            speed *= Math.pow(0.98, n50 < nobjects / 500.0 ? 0.00 : n50 - nobjects / 500.0);

            /* acc pp ---------------------------------------------- */
            acc = Math.pow(1.52163, mapstats.od) *
                    Math.pow(real_acc, 24.0) * 2.83;

            acc *= Math.min(1.15, Math.pow(ncircles / 1000.0, 0.3));

            if ((mods & MODS_HD) != 0) {
                acc *= 1.08;
            }

            if ((mods & MODS_FL) != 0) {
                acc *= 1.02;
            }

            /* total pp -------------------------------------------- */
            double final_multiplier = 1.12;

            if ((mods & MODS_NF) != 0) {
                final_multiplier *= Math.max(0.9, 1.0 - 0.2 * nmiss);
            }

            if ((mods & MODS_SO) != 0) {
                final_multiplier *= 1.0 - Math.pow((double)nspinners / nobjects, 0.85);
            }

            total = Math.pow(
                    Math.pow(aim, 1.1) + Math.pow(speed, 1.1) +
                            Math.pow(acc, 1.1),
                    1.0 / 1.1
            ) * final_multiplier;
        }

        /** @see PPv2Parameters */
        public PPv2(PPv2Parameters p)
        {
            this(p.aim_stars, p.speed_stars, p.max_combo, p.nsliders,
                    p.ncircles, p.nobjects, p.base_ar, p.base_od, p.mode,
                    p.mods, p.combo, p.n300, p.n100, p.n50, p.nmiss,
                    p.score_version, p.beatmap);
        }

        /**
         * simplest possible call, calculates ppv2 for SS scorev1.
         * @see PPv2#PPv2(Koohii.PPv2Parameters)
         */
        public PPv2(double aim_stars, double speed_stars, Map b)
        {
            this(aim_stars, speed_stars, -1, b.nsliders, b.ncircles,
                    b.objects.size(), b.ar, b.od, b.mode, MODS_NOMOD, -1,
                    -1, 0, 0, 0, 1, b);
        }
    }

} /* public final class Koohii */
