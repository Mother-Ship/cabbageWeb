package top.mothership.cabbage.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.Pattern.RegularPattern;
import top.mothership.cabbage.Pattern.SearchKeywordPattern;
import top.mothership.cabbage.consts.Base64Consts;
import top.mothership.cabbage.consts.ParameterEnum;
import top.mothership.cabbage.consts.TipConsts;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.pojo.CoolQ.Argument;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.osu.SearchParam;
import top.mothership.cabbage.util.osu.ScoreUtil;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;

@Component
@Aspect
@Order(2)
public class ParameterVerifyAspect {
    private final CqManager cqManager;
    private final ScoreUtil scoreUtil;

    public ParameterVerifyAspect(CqManager cqManager, ScoreUtil scoreUtil) {
        this.cqManager = cqManager;
        this.scoreUtil = scoreUtil;
    }

    @Pointcut("execution(* top.mothership.cabbage.serviceImpl.CqServiceImpl.*(top.mothership.cabbage.pojo.CoolQ.CqMsg,..))")
    private void regularService() {
    }

    @Pointcut("execution(* top.mothership.cabbage.serviceImpl.CqServiceImpl.*(top.mothership.cabbage.pojo.CoolQ.CqMsg,..))")
    private void adminService() {
    }

    @Around("regularService() && args(cqMsg,..)")
    public Object regularCommandParamVerify(ProceedingJoinPoint pjp, CqMsg cqMsg) throws Throwable {
        //TODO 分开判断optional和required
        if (cqMsg.getRequired() == null && cqMsg.getOptional() == null) {
            //如果命令没有指定required和optional，直接让它执行
            return pjp.proceed(new Object[]{cqMsg});
        }
        int argumentCount = 0;
        String numRaw = null;
        String modeRaw = null;
        //预定义变量，day默认为1，这样才能默认和昨天的比较
        Integer day = 1;
        //mode预设为null
        Integer mode = null;
        Integer num = null;
        Argument argument = new Argument();
        String msg = cqMsg.getMessage();


        Matcher m = RegularPattern.REG_CMD_REGEX_TWO_PARAMS.matcher(msg);
        if (m.find()) {
            modeRaw = m.group(4);
            numRaw = m.group(3);
            argumentCount = 3;
        } else {
            m = RegularPattern.REG_CMD_REGEX_TWO_PARAMS_REVERSE.matcher(msg);
            if (m.find()) {
                modeRaw = m.group(3);
                numRaw = m.group(4);
                argumentCount = 3;
            } else {
                //如果不是两个参数的
                m = RegularPattern.REG_CMD_REGEX_COLON_NUM_PARAM.matcher(msg);
                if (m.find()) {
                    modeRaw = m.group(3);
                    argumentCount = 2;
                } else {
                    //如果没有冒号
                    m = RegularPattern.REG_CMD_REGEX_SHARP_NUM_PARAM.matcher(msg);
                    if (m.find()) {
                        //如果是带井号的，尝试取出日期
                        numRaw = m.group(3);
                        argumentCount = 2;
                    } else {
                        //如果井号和冒号都没有
                        m = RegularPattern.REG_CMD_REGEX.matcher(msg);
                        m.find();
                        if (!"".equals(m.group(2))) {
                            //如果空格后有东西，那就表示有一个参数
                            argumentCount = 1;
                        }
                    }
                }
            }
        }
        //确保实参数目不小于最小形参数目，也不大于最大形参数目
        if (cqMsg.getRequired().length < argumentCount) {
            cqMsg.setMessage(String.format(TipConsts.ARGUMENTS_LESS_THAN_PARAMETERS, argumentCount, cqMsg.getRequired().length));
            cqManager.sendMsg(cqMsg);
            return null;
        }
        if (argumentCount > cqMsg.getRequired().length + cqMsg.getOptional().length) {
            cqMsg.setMessage(String.format(TipConsts.ARGUMENTS_MORE_THAN_PARAMETERS, argumentCount, cqMsg.getRequired().length + cqMsg.getOptional().length));
            cqManager.sendMsg(cqMsg);
            return null;
        }
        //取出子命令
        argument.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
        //判断是否需要以文字形式输出
        m = RegularPattern.TEXT_VERSION_COMMAND.matcher(argument.getSubCommandLowCase());
        argument.setText(m.find());
        if (cqMsg.getRequired() != null) {
            //如果指定了必选参数
            for (ParameterEnum p : cqMsg.getRequired()) {
                //对所有必须参数进行检查
                switch (p) {
                    case QQ:
                        //只有add命令需要QQ，而QQ是参数2
                        m = RegularPattern.QQ.matcher(m.group(3));
                        if (m.find()) {
                            argument.setQq(Long.valueOf(m.group(3)));
                        } else {
                            cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, m.group(3), "QQ号"));
                            cqManager.sendMsg(cqMsg);
                            return null;
                        }
                        break;
                    case USERID:
                        m = RegularPattern.OSU_USER_ID.matcher(m.group(2));
                        if (m.find()) {
                            argument.setUserId(Integer.valueOf(m.group(2)));
                        } else {
                            cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, m.group(2), "osu!uid"));
                            cqManager.sendMsg(cqMsg);
                            return null;
                        }
                        if (argument.getUserId() == 3) {
                            cqMsg.setMessage(TipConsts.QUERY_BANCHO_BOT);
                            cqManager.sendMsg(cqMsg);
                            return null;
                        }
                        break;
                    case USERNAME:
                        m = RegularPattern.OSU_USER_NAME.matcher(m.group(2));
                        if (m.find()) {
                            argument.setUsername(m.group(2));
                        } else {
                            cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, m.group(2), "osu!用户名"));
                            cqManager.sendMsg(cqMsg);
                            return null;
                        }
                        if (argument.getUsername().toLowerCase().equals("banchobot")) {
                            cqMsg.setMessage(TipConsts.QUERY_BANCHO_BOT);
                            cqManager.sendMsg(cqMsg);
                            return null;
                        }
                        break;
                    case MODE:
                        //!mode xxx，是参数1
                        mode = convertModeStrToInteger(m.group(2));
                        if (mode == null) {
                            cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, m.group(2), "osu!游戏模式"));
                            cqManager.sendMsg(cqMsg);
                            return null;
                        }
                        argument.setMode(mode);
                        break;
                    case SEARCHPARAM:
                        SearchParam searchParam = new SearchParam();
                        Matcher getKeyWordAndMod = SearchKeywordPattern.OSU_SEARCH_MOD_AND_ACC_ETC.matcher(msg);
                        Integer modsNum;
                        String mods = "None";
                        String keyword;
                        String scoreString = null;
                        Double ar = null;
                        Double od = null;
                        Double cs = null;
                        Double hp = null;
                        if (getKeyWordAndMod.find()) {
                            mods = getKeyWordAndMod.group(4);
                            scoreString = getKeyWordAndMod.group(3);
                        } else {
                            getKeyWordAndMod = SearchKeywordPattern.OSU_SEARCH_MOD_ONLY.matcher(msg);
                            if (getKeyWordAndMod.find()) {
                                mods = getKeyWordAndMod.group(3);
                            } else {
                                //未指定mod的情况下，mods和modnum依然为null
                                getKeyWordAndMod = RegularPattern.REG_CMD_REGEX.matcher(cqMsg.getMessage());
                                getKeyWordAndMod.find();
                            }
                        }

                        modsNum = scoreUtil.reverseConvertMod(mods);
                        //如果字符串解析出错，会返回null，因此这里用null值来判断输入格式
                        if (modsNum == null) {
                            cqMsg.setMessage("请使用MOD的双字母缩写，不需要任何分隔符。" +
                                    "\n接受的Mod有：NF EZ TD HD HR SD DT HT NC FL SO PF。");
                            cqManager.sendMsg(cqMsg);
                            return null;
                        }
                        searchParam.setMods(modsNum);
                        searchParam.setModsString(mods);
                        if (scoreString != null) {
                            String[] scoreParams = scoreString.split(" ");
                            for (String s : scoreParams) {
                                Matcher getScoreParams = SearchKeywordPattern.KEYWORD_ACC.matcher(s);
                                if (getScoreParams.find()) {
                                    searchParam.setAcc(Double.valueOf(getScoreParams.group(1)));
                                }
                                getScoreParams = SearchKeywordPattern.KEYWORD_COMBO.matcher(s);
                                if (getScoreParams.find()) {
                                    searchParam.setMaxCombo(Integer.valueOf(getScoreParams.group(1)));
                                }
                                getScoreParams = SearchKeywordPattern.KEYWORD_COUNT_50.matcher(s);
                                if (getScoreParams.find()) {
                                    searchParam.setCount50(Integer.valueOf(getScoreParams.group(1)));
                                }
                                getScoreParams = SearchKeywordPattern.KEYWORD_COUNT_100.matcher(s);
                                if (getScoreParams.find()) {
                                    searchParam.setCount100(Integer.valueOf(getScoreParams.group(1)));
                                }
                                getScoreParams = SearchKeywordPattern.KEYWORD_MISS.matcher(s);
                                if (getScoreParams.find()) {
                                    searchParam.setCountMiss(Integer.valueOf(getScoreParams.group(1)));
                                }
                            }
                        }
                        keyword = getKeyWordAndMod.group(2);
                        if (keyword.endsWith(" ")) {
                            keyword = keyword.substring(0, keyword.length() - 1);
                        }
                        Matcher allNumberKeyword = SearchKeywordPattern.ALL_NUMBER_SEARCH_KEYWORD.matcher(keyword);
                        if (allNumberKeyword.find()) {
                            searchParam.setBeatmapId(Integer.valueOf(allNumberKeyword.group(1)));
                            return searchParam;
                        }
                        //新格式(咕)

                        //比较菜，手动补齐参数
                        if (!keyword.contains("-")) {
                            //如果没有横杠，手动补齐
                            keyword = "-" + keyword;
                        }
                        if (!(keyword.endsWith("]") || keyword.endsWith(")") || keyword.endsWith("}"))) {
                            //如果圆括号 方括号 花括号都没有
                            keyword += "[](){}";
                        }
                        if (keyword.endsWith("]"))
                            //如果有方括号
                            keyword += "(){}";
                        if (keyword.endsWith(")"))
                            //如果有圆括号
                            keyword += "{}";
                        Matcher getArtistTitleEtc = SearchKeywordPattern.OSU_SEARCH_KETWORD.matcher(keyword);
                        if (!getArtistTitleEtc.find()) {
                            cqMsg.setMessage("请使用艺术家-歌曲标题[难度名](麻婆名){AR9.0OD9.0CS9.0HP9.0} +MOD双字母简称 的格式。\n" +
                                    "所有参数都可以省略(但横线、方括号和圆括号不能省略)，四维顺序必须按AR OD CS HP排列。");
                            cqManager.sendMsg(cqMsg);
                            return null;
                        } else {
                            //没啥办法……手动处理吧，这个正则管不了了，去掉可能存在的空格
                            String artist = getArtistTitleEtc.group(1);
                            if (artist.endsWith(" ")) {
                                artist = artist.substring(0, artist.length() - 1);
                            }
                            String title = getArtistTitleEtc.group(2);
                            if (title.startsWith(" ")) {
                                title = title.substring(1);
                            }
                            if (title.endsWith(" ")) {
                                title = title.substring(0, title.length() - 1);
                            }
                            searchParam.setArtist(artist);
                            searchParam.setTitle(title);
                            searchParam.setDiffName(getArtistTitleEtc.group(3));
                            searchParam.setMapper(getArtistTitleEtc.group(4));
                            //处理四维字符串
                            String fourDimensions = getArtistTitleEtc.group(5);
                            if (!"".equals(fourDimensions)) {
                                Matcher getFourDimens = SearchKeywordPattern.OSU_SEARCH_FOUR_DEMENSIONS_REGEX.matcher(fourDimensions);
                                getFourDimens.find();
                                if (getFourDimens.group(1) != null) {
                                    ar = Double.valueOf(getFourDimens.group(1));
                                }
                                if (getFourDimens.group(2) != null) {
                                    od = Double.valueOf(getFourDimens.group(2));
                                }
                                if (getFourDimens.group(3) != null) {
                                    cs = Double.valueOf(getFourDimens.group(3));
                                }
                                if (getFourDimens.group(4) != null) {
                                    hp = Double.valueOf(getFourDimens.group(4));
                                }

                            }
                            searchParam.setAr(ar);
                            searchParam.setOd(od);
                            searchParam.setCs(cs);
                            searchParam.setHp(hp);

                        }
                        argument.setSearchParam(searchParam);
                        break;
                    default:
                        break;
                }
            }
        }
        if (cqMsg.getOptional() != null) {
            //如果有指定可选参数
            for (ParameterEnum p : cqMsg.getOptional()) {
                //这里是可选参数
                switch (p) {
                    case DAY:
                        if (numRaw != null) {
                            try {
                                day = Integer.valueOf(numRaw);
                                if (day < 0) {
                                    cqMsg.setMessage("白菜不会预知未来。");
                                    cqManager.sendMsg(cqMsg);
                                    return null;
                                }
                                if (LocalDate.now().minusDays(day).isBefore(LocalDate.of(2007, 9, 16))) {
                                    cqMsg.setMessage("你要找史前时代的数据吗。");
                                    cqManager.sendMsg(cqMsg);
                                    return null;
                                }
                            } catch (java.lang.NumberFormatException e) {
                                cqMsg.setMessage("假使这些完全……不能用的参数，你再给他传一遍，你等于……你也等于……你也有泽任吧？");
                                cqManager.sendMsg(cqMsg);
                                return null;
                            }
                        }
                        argument.setDay(day);
                        break;
                    case HOUR:
                        if (numRaw != null) {
                            Long hour;
                            try {
                                hour = Long.valueOf(numRaw);
                            } catch (java.lang.Exception e) {
                                hour = 6L;
                            }
                            if (hour > 13) {
                                hour = 13L;
                            }
                            if (hour == 0) {
                                if (cqMsg.getUserId() == 546748348) {
                                    hour = 720L;
                                } else {
                                    hour = 6L;
                                }
                            }
                            if (hour < 0) {
                                hour = 6L;
                            }
                            argument.setHour(hour);
                        }
                        break;
                    case NUM:
                        if (numRaw != null) {
                            try {
                                num = Integer.valueOf(numRaw);
                                if (num < 0 || num > 100) {
                                    cqMsg.setMessage("其他人看不到的东西，白菜也看不到啦。");
                                    cqManager.sendMsg(cqMsg);
                                    return null;
                                }
                            } catch (java.lang.NumberFormatException e) {
                                cqMsg.setMessage("[CQ:record,file=base64://" + Base64Consts.AYA_YA_YA + "]");
                                cqManager.sendMsg(cqMsg);
                                return null;
                            }
                            argument.setNum(num);
                        }
                        break;
                    case MODE:
                        //!stat :xxx，是上面取的modeRaw
                        if (modeRaw != null) {
                            mode = convertModeStrToInteger(modeRaw);
                            if (mode == null) {
                                cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, modeRaw, "osu!游戏模式"));
                                cqManager.sendMsg(cqMsg);
                                return null;
                            }
                            argument.setMode(mode);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        cqMsg.setArgument(argument);
        return pjp.proceed(new Object[]{cqMsg});
    }

    private Integer convertModeStrToInteger(String mode) {
        switch (mode.toLowerCase(Locale.CHINA)) {
            case "0":
            case "std":
            case "standard":
            case "主模式":
            case "戳泡泡":
            case "屙屎":
            case "o!std":
            case "s":
                return 0;
            case "1":
            case "太鼓":
            case "taiko":
            case "o!taiko":
            case "t":
                return 1;
            case "2":
            case "catch the beat":
            case "catchthebeat":
            case "ctb":
            case "接水果":
            case "接翔":
            case "fruit":
            case "艹他爸":
            case "c":
                return 2;
            case "3":
            case "osu!mania":
            case "mania":
            case "骂娘":
            case "钢琴":
            case "o!m":
            case "m":
                return 3;
            default:
                return null;

        }

    }
}
