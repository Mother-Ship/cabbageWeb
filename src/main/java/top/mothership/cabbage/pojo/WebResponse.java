package top.mothership.cabbage.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WebResponse<T> {
    private Integer code;
    private String status;
    private T data;
}
