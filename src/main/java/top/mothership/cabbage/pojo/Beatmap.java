package top.mothership.cabbage.pojo;

public class Beatmap {
    private Integer approved;


    private String artist;
    private String title;
    private String version;
    private String creator;

    public String getCreator() {
        return creator;
    }

    public Integer getApproved() {
        return approved;
    }

    public void setApproved(Integer approved) {
        this.approved = approved;
    }
    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getArtist() {

        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
