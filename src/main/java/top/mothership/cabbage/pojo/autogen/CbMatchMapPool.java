package top.mothership.cabbage.pojo.autogen;

public class CbMatchMapPool {
    private Integer id;

    private String tournamentId;

    private Integer mappoolType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId == null ? null : tournamentId.trim();
    }

    public Integer getMappoolType() {
        return mappoolType;
    }

    public void setMappoolType(Integer mappoolType) {
        this.mappoolType = mappoolType;
    }
}