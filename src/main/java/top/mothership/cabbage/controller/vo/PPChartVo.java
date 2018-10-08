package top.mothership.cabbage.controller.vo;

import lombok.Data;

import java.util.List;
@Data
public class PPChartVo {
    private List<String> xAxis;
    private List<Float> yAxis;
}
