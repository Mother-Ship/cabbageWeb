package top.mothership.cabbage.pojo.osu;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
    private Integer beatmapId;
    private Integer countMiss;
    private Integer count100;
    private Integer count50;
    private Integer maxCombo;
    private Double acc;
    public String toOsuDirectSearchString() {
        StringBuilder query = new StringBuilder();

        query.append("[");

        if (artist != null && !artist.isEmpty()) {
            query.append(String.format("artist = \"%s\"", artist));
        }

        if (ar != null) {
            appendAnd(query);
            query.append(String.format("beatmaps.ar = %s", ar));
        }

        if (mapper != null && !mapper.isEmpty()) {
            appendAnd(query);
            query.append(String.format("creator = \"%s\"", mapper));
        }

        if (od != null) {
            appendAnd(query);
            query.append(String.format("beatmaps.accuracy = %s", od));
        }

        if (cs != null) {
            appendAnd(query);
            query.append(String.format("beatmaps.cs = %s", cs));
        }

        if (hp != null) {
            appendAnd(query);
            query.append(String.format("beatmaps.drain = %s", hp));
        }

        query.append("]");

        if (title != null && !title.isEmpty()) {
            query.append(title);
        }

        return query.toString();
    }

    private void appendAnd(StringBuilder query) {
        if (query.length() > 1) { // 确保不是第一个条件
            query.append(" AND ");
        }
    }
    @Override
    public String toString() {
        return "{" +
                "艺术家='" + artist + '\'' +
                ", 标题='" + title + '\'' +
                ", 难度名='" + diffName + '\'' +
                ", 作者='" + mapper + '\'' +
                ", 谱面id=" + beatmapId +
                '}';
    }
}
