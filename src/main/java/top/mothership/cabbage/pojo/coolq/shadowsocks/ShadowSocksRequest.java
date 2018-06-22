package top.mothership.cabbage.pojo.coolq.shadowsocks;

import lombok.Data;
import top.mothership.cabbage.consts.OverallConsts;

/**
 * The type Shadow socks request.
 */
@Data
public class ShadowSocksRequest {
    /**
     * 用户 ID / 端口号 / 用户名（前后模糊匹配，要求 6 字以上）/ 邮箱（前后模糊匹配，要求 6 字以上）
     */
    private String user;
    /**
     * traffic / time
     */
    private String type;
    /**
     * 要添加的月/流量个数
     */
    private Integer number;
    private Integer confirm = 1;
    private Integer count;
    private Integer monthly;
    private String key = OverallConsts.CABBAGE_CONFIG.getString("ssCmdVerifyCode");
}
