package top.mothership.cabbage.util.qq;

import org.springframework.stereotype.Component;
import top.mothership.cabbage.consts.Base64Consts;
import top.mothership.cabbage.consts.PatternConsts;
import top.mothership.cabbage.consts.TipConsts;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.CoolQ.Params;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * @author QHS
 */
@Component
public class ParamVerifyUtil {

    public Params statUserInfo(CqMsg cqMsg) {
        Params params = new Params();
        params.setVaild(true);
        String msg = cqMsg.getMessage();
        String dayRaw = null;
        String modeRaw = null;
        //预定义变量，day默认为1，这样才能默认和昨天的比较
        Integer day = 1;
        //mode预设为null
        Integer mode = null;

        // 首先尝试用带2个参数的正则去匹配，匹配失败则用带井号的匹配，再失败用带冒号的匹配

        Matcher m = PatternConsts.REG_CMD_REGEX_TWO_PARAMS.matcher(msg);
        if (m.find()) {
            modeRaw = m.group(4);
            dayRaw = m.group(3);
        } else {
            m = PatternConsts.REG_CMD_REGEX_TWO_PARAMS_REVERSE.matcher(msg);
            if (m.find()) {
                modeRaw = m.group(3);
                dayRaw = m.group(4);
            } else {
                //如果不是两个参数的
                m = PatternConsts.REG_CMD_REGEX_COLON_NUM_PARAM.matcher(msg);
                if (m.find()) {
                    modeRaw = m.group(3);
                } else {
                    //如果没有冒号
                    m = PatternConsts.REG_CMD_REGEX_SHARP_NUM_PARAM.matcher(msg);
                    if (m.find()) {
                        //如果是带井号的，尝试取出日期
                        dayRaw = m.group(3);
                    } else {
                        //如果井号和冒号都没有
                        m = PatternConsts.REG_CMD_REGEX.matcher(msg);
                        m.find();
                    }
                }
            }
        }
        if (dayRaw != null) {
            try {
                day = Integer.valueOf(dayRaw);
                if (day < 0) {
                    params.setVaild(false);
                    params.setResp("白菜不会预知未来。");
                }
                if (LocalDate.now().minusDays(day).isBefore(LocalDate.of(2007, 9, 16))) {
                    params.setVaild(false);
                    params.setResp("你要找史前时代的数据吗。");
                }
            } catch (java.lang.NumberFormatException e) {
                params.setVaild(false);
                params.setResp("假使这些完全……不能用的参数，你再给他传一遍，你等于……你也等于……你也有泽任吧？");
            }
        }
        if (modeRaw != null) {
            mode = convertModeStr(modeRaw);
            if (mode == null) {
                params.setResp(TipConsts.MODE_UNRECOGNIZED);
                params.setVaild(false);
            }
        }
        //子命令，查询的日期，模式，查询的对象（
        params.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
        params.setDay(day);
        params.setMode(mode);
        switch (params.getSubCommandLowCase()) {
            case "stat":
                params.setUsername(m.group(2));
                if ("白菜".equals(params.getUsername())) {
                    params.setResp("唉，没人疼没人爱，我是地里一颗小白菜。");
                    params.setVaild(false);
                }
                if (!PatternConsts.OSU_USER_NAME.matcher(params.getUsername()).find()) {
                    params.setResp(String.format(TipConsts.USERNAME_FORMAT_ERROR, params.getUsername()));
                    params.setVaild(false);
                }
                break;
            case "statme":
                if (!"".equals(m.group(2))) {
                    params.setVaild(false);
                    params.setResp(String.format(TipConsts.TARGET_CONFLICT, m.group(2)));
                }
                break;
            case "statu":
                m = PatternConsts.OSU_USER_ID.matcher(m.group(2));
                if (m.find()) {
                    params.setUserId(Integer.valueOf(m.group(2)));
                    if ("3".equals(params.getUserId())) {
                        params.setResp(TipConsts.QUERY_BANCHO_BOT);
                        params.setVaild(false);
                    }
                } else {
                    params.setVaild(false);
                    params.setResp(TipConsts.CMD_USERID_LIMITED);
                }
                break;
            default:
                break;
        }
        return params;
    }

    public Params printBP(CqMsg cqMsg) {
        Params params = new Params();
        params.setVaild(true);
        String msg = cqMsg.getMessage();
        String modeRaw = null;
        Integer mode = null;
        //由于在Controller中用过正则区分，所以这里只可能有 是否有模式 两种情况
        Matcher m = PatternConsts.REG_CMD_REGEX_COLON_NUM_PARAM.matcher(msg);
        if (m.find()) {
            modeRaw = m.group(3);
        } else {
            //如果井号和冒号都没有
            m = PatternConsts.REG_CMD_REGEX.matcher(msg);
            m.find();
        }
        if (modeRaw != null) {
            mode = convertModeStr(modeRaw);
            if (mode == null) {
                params.setResp(TipConsts.MODE_UNRECOGNIZED);
                params.setVaild(false);
            }
        }
        params.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
        params.setMode(mode);
        switch (params.getSubCommandLowCase()) {
            case "bp":
                params.setText(false);
            case "bps":
                params.setText(true);
                params.setUsername(m.group(2));
                if ("白菜".equals(params.getUsername())) {
                    params.setResp("大白菜（学名：Brassica rapa pekinensis，异名Brassica campestris pekinensis或Brassica pekinensis）" +
                            "是一种原产于中国的蔬菜，又称“结球白菜”、“包心白菜”、“黄芽白”、“胶菜”等。(via 维基百科)");
                    params.setVaild(false);
                }
                break;
            case "bpu":
                params.setText(false);
            case "bpus":
                params.setText(true);
                m = PatternConsts.OSU_USER_ID.matcher(m.group(2));
                if (m.find()) {
                    params.setUserId(Integer.valueOf(m.group(2)));
                    if ("3".equals(params.getUserId())) {
                        params.setResp(TipConsts.QUERY_BANCHO_BOT);
                        params.setVaild(false);
                    }
                } else {
                    params.setVaild(false);
                    params.setResp(TipConsts.CMD_USERID_LIMITED);
                }
                break;
            case "mybps":
            case "bpmes":
                params.setText(true);
            case "bpme":
            case "mybp":
                if (!"".equals(m.group(2))) {
                    params.setVaild(false);
                    params.setResp(String.format(TipConsts.TARGET_CONFLICT, m.group(2)));
                }
                break;
            default:
                break;
        }

        return params;
    }

    public Params printSpecifiedBP(CqMsg cqMsg) {
        Params params = new Params();
        params.setVaild(true);
        String msg = cqMsg.getMessage();
        String numRaw = null;
        String modeRaw = null;
        //预定义变量，day默认为1，这样才能默认和昨天的比较
        Integer num = 0;
        //mode预设为null
        Integer mode = null;
        params.setText(true);

        // 首先尝试用带2个参数的正则去匹配，匹配失败则用带井号的匹配，再失败用带冒号的匹配

        Matcher m = PatternConsts.REG_CMD_REGEX_TWO_PARAMS.matcher(msg);
        if (m.find()) {
            modeRaw = m.group(4);
            numRaw = m.group(3);
        } else {
            m = PatternConsts.REG_CMD_REGEX_TWO_PARAMS_REVERSE.matcher(msg);
            if (m.find()) {
                modeRaw = m.group(3);
                numRaw = m.group(4);
            } else {
                //如果不是两个参数的
                m = PatternConsts.REG_CMD_REGEX_COLON_NUM_PARAM.matcher(msg);
                if (m.find()) {
                    modeRaw = m.group(3);
                } else {
                    //如果没有冒号
                    m = PatternConsts.REG_CMD_REGEX_SHARP_NUM_PARAM.matcher(msg);
                    if (m.find()) {
                        //如果是带井号的，尝试取出日期
                        numRaw = m.group(3);
                    } else {
                        //如果井号和冒号都没有
                        m = PatternConsts.REG_CMD_REGEX.matcher(msg);
                        m.find();
                    }
                }
            }
        }
        if (numRaw != null) {
            try {
                num = Integer.valueOf(numRaw);
                if (num < 0 || num > 100) {
                    params.setResp("其他人看不到的东西，白菜也看不到啦。");
                    params.setVaild(false);
                }
            } catch (java.lang.NumberFormatException e) {
                params.setVaild(false);
                params.setResp("[CQ:record,file=base64://" + Base64Consts.AYA_YA_YA + "]");
            }
        }
        if (modeRaw != null) {
            mode = convertModeStr(modeRaw);
            if (mode == null) {
                params.setResp(TipConsts.MODE_UNRECOGNIZED);
                params.setVaild(false);
            }
        }
        //子命令，查询的日期，模式，查询的对象（
        params.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
        params.setNum(num);
        params.setMode(mode);
        switch (params.getSubCommandLowCase()) {

            case "bp":
                params.setText(false);
                //两者共用逻辑，这里不加break
            case "bps":
                params.setUsername(m.group(2));
                if ("白菜".equals(params.getUsername())) {
                    params.setResp("大白菜（学名：Brassica rapa pekinensis，异名Brassica campestris pekinensis或Brassica pekinensis）" +
                            "是一种原产于中国的蔬菜，又称“结球白菜”、“包心白菜”、“黄芽白”、“胶菜”等。(via 维基百科)");
                    params.setVaild(false);
                }
                if (!PatternConsts.OSU_USER_NAME.matcher(params.getUsername()).find()) {
                    params.setResp(String.format(TipConsts.USERNAME_FORMAT_ERROR, params.getUsername()));
                    params.setVaild(false);
                }
                break;
            case "bpu":
                params.setText(false);
            case "bpus":
                m = PatternConsts.OSU_USER_ID.matcher(m.group(2));
                if (m.find()) {
                    params.setUserId(Integer.valueOf(m.group(2)));
                    if ("3".equals(params.getUserId())) {
                        params.setResp(TipConsts.QUERY_BANCHO_BOT);
                        params.setVaild(false);
                    }
                } else {
                    params.setVaild(false);
                    params.setResp(TipConsts.CMD_USERID_LIMITED);
                }
                break;
            case "bpme":
            case "mybp":
                params.setText(false);
            case "mybps":
            case "bpmes":
                if (!"".equals(m.group(2))) {
                    params.setVaild(false);
                    params.setResp(String.format(TipConsts.TARGET_CONFLICT, m.group(2)));
                }
                break;
            default:
                break;
        }
        return params;
    }

    public Params setId(CqMsg cqMsg) {
        Params params = new Params();
        String msg = cqMsg.getMessage();
        String modeRaw = null;
        Integer mode = null;
        Matcher m = PatternConsts.REG_CMD_REGEX_COLON_NUM_PARAM.matcher(msg);
        if (m.find()) {
            modeRaw = m.group(3);
        } else {
            m = PatternConsts.REG_CMD_REGEX.matcher(cqMsg.getMessage());
            m.find();
        }
        if (modeRaw != null) {
            mode = convertModeStr(modeRaw);
            if (mode == null) {
                params.setResp(TipConsts.MODE_UNRECOGNIZED);
                params.setVaild(false);
            }
        }


    }

    private Integer convertModeStr(String mode) {
        switch (mode.toLowerCase(Locale.CHINA)) {
            case "0":
            case "std":
            case "standard":
            case "主模式":
            case "戳泡泡":
            case "屙屎":
            case "o!std":
                return 0;
            case "1":
            case "太鼓":
            case "taiko":
            case "o!taiko":
                return 1;
            case "2":
            case "catch the beat":
            case "catchthebeat":
            case "ctb":
            case "接水果":
            case "接翔":
            case "fruit":
            case "艹他爸":
                return 2;
            case "3":
            case "osu!mania":
            case "mania":
            case "骂娘":
            case "钢琴":
            case "o!m":
                return 3;
            default:
                return null;

        }

    }
}
