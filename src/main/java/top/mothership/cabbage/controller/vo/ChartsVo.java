package top.mothership.cabbage.controller.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChartsVo {
    private List<Integer> xAxis;
    private List<Integer> yAxis;
}
