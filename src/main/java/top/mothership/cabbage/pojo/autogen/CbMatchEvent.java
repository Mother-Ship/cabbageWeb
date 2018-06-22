package top.mothership.cabbage.pojo.autogen;

public class CbMatchEvent {
    private Integer id;

    private Long matchId;

    private Integer eventType;

    private Integer operatorTeam;

    private Long beatmapId;

    private Integer winner;

    private Integer playerId;

    private Long score;

    private Integer rollPoint;

    private String note;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Integer getEventType() {
        return eventType;
    }

    public void setEventType(Integer eventType) {
        this.eventType = eventType;
    }

    public Integer getOperatorTeam() {
        return operatorTeam;
    }

    public void setOperatorTeam(Integer operatorTeam) {
        this.operatorTeam = operatorTeam;
    }

    public Long getBeatmapId() {
        return beatmapId;
    }

    public void setBeatmapId(Long beatmapId) {
        this.beatmapId = beatmapId;
    }

    public Integer getWinner() {
        return winner;
    }

    public void setWinner(Integer winner) {
        this.winner = winner;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public Long getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }

    public Integer getRollPoint() {
        return rollPoint;
    }

    public void setRollPoint(Integer rollPoint) {
        this.rollPoint = rollPoint;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? null : note.trim();
    }
}