package top.mothership.cabbage.pojo.osu.apiv2.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户成绩请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserScoresRequest {
    private String userId;        // 用户ID
    private String type;          // 成绩类型: "best" 或 "recent"
    private Integer limit;        // 返回结果数量限制 (可选)
    private Integer offset;       // 结果偏移量 (可选)
    private Boolean includeFails; // 是否包含失败成绩 (可选, 仅用于recent类型)
    private String mode;          // 游戏模式 (可选): "osu", "taiko", "fruits", "mania"
}