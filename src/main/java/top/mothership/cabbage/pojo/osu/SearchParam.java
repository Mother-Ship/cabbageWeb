package top.mothership.cabbage.pojo.osu;

import lombok.Data;

@Data
public class SearchParam {
    private String artist;
    private String title;
    private String diffName;
    private String mapper;
    private Double ar;
    private Double od;
    private Double cs;
    private Double hp;
    private Integer mods;
    private String modsString;
}
