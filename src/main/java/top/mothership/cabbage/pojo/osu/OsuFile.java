package top.mothership.cabbage.pojo.osu;

public class OsuFile {
    private Integer version;
    private String bgName;
    private String audioFilename;
    private Integer audioLeadin;
    private Integer previewTime;
    private Boolean countdown;
    private String sampleSet;
    private String stackLeniency;
    private Integer mode;
    private Boolean letterboxInBreaks;
    private Boolean wideScreenStoryBoard;

    public String getBgName() {
        return bgName;
    }

    public void setBgName(String bgName) {
        this.bgName = bgName;
    }
}
