package top.mothership.cabbage.pojo.osu.apiv2;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthCredentials {

    private String accessToken;
    private long expiresIn;
    private LocalDateTime createdAt;

    public boolean isTokenExpired() {
        if (accessToken == null || createdAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(createdAt.plusSeconds(expiresIn - 300)); // 5分钟缓冲
    }
}