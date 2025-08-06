package top.mothership.cabbage.pojo.osu.apiv2.constant;

public class ApiV2Mode {
    public static final String MANIA = "mania";
    public static final String OSU = "osu";
    public static final String TAIKO = "taiko";
    public static final String FRUITS = "fruits";
    public static String fromInt(int value){
        switch (value){
            case 0:
                return OSU;
            case 1:
                return TAIKO;
            case 2:
                return FRUITS;
            case 3:
                return MANIA;
            default:
                return OSU;
        }
    }
}
