package top.mothership.cabbage.pojo.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageQuery {
    private Integer pageSize = 50;
    private Integer pageIndex = 1;
    private Integer itemTotal;
}
