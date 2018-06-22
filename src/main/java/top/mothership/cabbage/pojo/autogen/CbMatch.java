package top.mothership.cabbage.pojo.autogen;

import java.util.Date;

public class CbMatch {
    private Integer id;

    private Long tournamentId;

    private Long blueTeamId;

    private Long redTeamId;

    private Integer blueTeamScore;

    private Integer redTeamScore;

    private Integer matchType;

    private Long mappoolId;

    private String liveAddress;

    private String recordAddress;

    private Date startTime;

    private Boolean timeConfirmed;

    private String mpLink;

    private Integer referee;

    private Boolean ended;

    private String note;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }

    public Long getBlueTeamId() {
        return blueTeamId;
    }

    public void setBlueTeamId(Long blueTeamId) {
        this.blueTeamId = blueTeamId;
    }

    public Long getRedTeamId() {
        return redTeamId;
    }

    public void setRedTeamId(Long redTeamId) {
        this.redTeamId = redTeamId;
    }

    public Integer getBlueTeamScore() {
        return blueTeamScore;
    }

    public void setBlueTeamScore(Integer blueTeamScore) {
        this.blueTeamScore = blueTeamScore;
    }

    public Integer getRedTeamScore() {
        return redTeamScore;
    }

    public void setRedTeamScore(Integer redTeamScore) {
        this.redTeamScore = redTeamScore;
    }

    public Integer getMatchType() {
        return matchType;
    }

    public void setMatchType(Integer matchType) {
        this.matchType = matchType;
    }

    public Long getMappoolId() {
        return mappoolId;
    }

    public void setMappoolId(Long mappoolId) {
        this.mappoolId = mappoolId;
    }

    public String getLiveAddress() {
        return liveAddress;
    }

    public void setLiveAddress(String liveAddress) {
        this.liveAddress = liveAddress == null ? null : liveAddress.trim();
    }

    public String getRecordAddress() {
        return recordAddress;
    }

    public void setRecordAddress(String recordAddress) {
        this.recordAddress = recordAddress == null ? null : recordAddress.trim();
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Boolean getTimeConfirmed() {
        return timeConfirmed;
    }

    public void setTimeConfirmed(Boolean timeConfirmed) {
        this.timeConfirmed = timeConfirmed;
    }

    public String getMpLink() {
        return mpLink;
    }

    public void setMpLink(String mpLink) {
        this.mpLink = mpLink == null ? null : mpLink.trim();
    }

    public Integer getReferee() {
        return referee;
    }

    public void setReferee(Integer referee) {
        this.referee = referee;
    }

    public Boolean getEnded() {
        return ended;
    }

    public void setEnded(Boolean ended) {
        this.ended = ended;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? null : note.trim();
    }
}