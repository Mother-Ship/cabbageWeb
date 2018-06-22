package top.mothership.cabbage.enums;

public enum  TournamentTypeEnum {
    单败淘汰赛(MatchTypeEnum.class),
    双败淘汰赛(DoubleEliminationMatchTypeEnum.class),
    瑞士制(MatchTypeEnum.class),
    循环赛(MatchTypeEnum.class);
    private Class matchType;

    TournamentTypeEnum(Class matchType) {
        this.matchType = matchType;
    }
}
