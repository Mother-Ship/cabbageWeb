package top.mothership.cabbage.pojo.autogen;

public class CbMatchTeam {
    private Integer id;

    private Long tournamentId;

    private String teamName;

    private Integer teamLeader;

    private Integer teamPoint;

    private Float teamToken;

    private byte[] teamAvatar;

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

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName == null ? null : teamName.trim();
    }

    public Integer getTeamLeader() {
        return teamLeader;
    }

    public void setTeamLeader(Integer teamLeader) {
        this.teamLeader = teamLeader;
    }

    public Integer getTeamPoint() {
        return teamPoint;
    }

    public void setTeamPoint(Integer teamPoint) {
        this.teamPoint = teamPoint;
    }

    public Float getTeamToken() {
        return teamToken;
    }

    public void setTeamToken(Float teamToken) {
        this.teamToken = teamToken;
    }

    public byte[] getTeamAvatar() {
        return teamAvatar;
    }

    public void setTeamAvatar(byte[] teamAvatar) {
        this.teamAvatar = teamAvatar;
    }
}