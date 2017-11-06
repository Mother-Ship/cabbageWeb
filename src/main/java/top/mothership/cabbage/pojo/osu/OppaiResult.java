package top.mothership.cabbage.pojo.osu;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by QHS on 2017/9/11.
 */
@Data
@AllArgsConstructor
public class OppaiResult {
    private String oppaiVersion;
    private int code;
    private String errstr;
    private String artist;
    private String artistUnicode;
    private String title;
    private String titleUnicode;
    private String creator;
    private String version;
    private String modsStr;
    private int mods;
    private double od;
    private double ar;
    private double cs;
    private double hp;
    private int combo;
    private int maxCombo;
    private int numCircles;
    private int numSliders;
    private int numSpinners;
    private int misses;
    private int scoreVersion;
    private double stars;
    private double speedStars;
    private double aimStars;
    private int nsingles;
    private int nsinglesThreshold;
    private double aimPp;
    private double speedPp;
    private double accPp;
    private double pp;
}
