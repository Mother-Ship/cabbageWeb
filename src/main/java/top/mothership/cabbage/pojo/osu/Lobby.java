package top.mothership.cabbage.pojo.osu;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
@Data
public class Lobby {
    private Match match;
    private List<Game> games;
}
