package top.mothership.cabbage.pojo.osu;

import lombok.Getter;
import lombok.Setter;


public class OsuFile {
    private Integer version;
    @Setter
    @Getter
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

}
