package top.mothership.cabbage.pojo.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageForm {
    private String pageSize;
    private String pageIndex;
    private String itemTotal;
}
