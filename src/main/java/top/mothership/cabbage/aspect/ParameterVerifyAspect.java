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
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.consts.ParameterEnum;
import top.mothership.cabbage.consts.TipConsts;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.pojo.CoolQ.Argument;
import top.mothership.cabbage.pojo.CoolQ.CqMsg;
import top.mothership.cabbage.pojo.osu.SearchParam;
import top.mothership.cabbage.util.osu.ScoreUtil;

import java.time.LocalDate;
import java.util.Arrays;
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
        switch (cqMsg.getMessageType()) {
            //只处理私聊 群 讨论组消息，其他消息不予处理
            case "group":
            case "discuss":
            case "private":
                Argument argument = new Argument();
                String msg = cqMsg.getMessage();

                if (cqMsg.getRequired() == null && cqMsg.getOptional() == null) {
                    //如果命令没有指定required和optional，直接让它执行
                    //2018-2-27 16:09:47漏掉了argument，在这里也需要一个argument（
                    Matcher m = RegularPattern.REG_CMD_REGEX.matcher(msg);
                    m.find();
                    argument.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
                    cqMsg.setArgument(argument);
                    return pjp.proceed(new Object[]{cqMsg});
                }
                if (cqMsg.getRequired() == null) {
                    cqMsg.setRequired(OverallConsts.EMPTY_PARAMETER_LIST);
                }
                if (cqMsg.getOptional() == null) {
                    cqMsg.setOptional(OverallConsts.EMPTY_PARAMETER_LIST);
                }
                int argumentCount = 0;
                String numRaw = null;
                String modeRaw = null;
                //预定义变量，day默认为1，这样才能默认和昨天的比较
                Integer day = 1;
                //mode预设为null
                //2018-2-27 09:42:45 由于各个命令 未指定Mode的时候表现不同，所以不能预设为0
                Integer mode = null;
                Integer num = null;
                String targetRaw = null;

                Matcher m = RegularPattern.REG_CMD_REGEX_TWO_PARAMS.matcher(msg);

                if (m.find()) {
                    modeRaw = m.group(4);
                    numRaw = m.group(3);
                    if (!"".equals(m.group(2))) {
                        //如果空格后有东西，那就表示有一个参数
                        argumentCount = 3;
                    } else {
                        argumentCount = 2;
                    }
                } else {
                    m = RegularPattern.REG_CMD_REGEX_TWO_PARAMS_REVERSE.matcher(msg);
                    if (m.find()) {
                        modeRaw = m.group(3);
                        numRaw = m.group(4);
                        if (!"".equals(m.group(2))) {
                            //如果空格后有东西，那就表示有一个参数
                            argumentCount = 3;
                        } else {
                            argumentCount = 2;
                        }
                    } else {
                        //如果不是两个参数的
                        m = RegularPattern.REG_CMD_REGEX_COLON_NUM_PARAM.matcher(msg);
                        if (m.find()) {
                            modeRaw = m.group(3);
                            if (!"".equals(m.group(2))) {
                                //如果空格后有东西，那就表示有一个参数
                                argumentCount = 2;
                            } else {
                                argumentCount = 1;
                            }
                        } else {
                            //如果没有冒号
                            m = RegularPattern.REG_CMD_REGEX_SHARP_NUM_PARAM.matcher(msg);
                            if (m.find()) {
                                //如果是带井号的，尝试取出日期
                                numRaw = m.group(3);
                                if (!"".equals(m.group(2))) {
                                    //如果空格后有东西，那就表示有一个参数
                                    argumentCount = 2;
                                } else {
                                    argumentCount = 1;
                                }
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
                targetRaw = m.group(2);
                //确保实参数目不小于最小形参数目，也不大于最大形参数目
                if (argumentCount < cqMsg.getRequired().length) {
                    cqMsg.setMessage(String.format(TipConsts.ARGUMENTS_LESS_THAN_PARAMETERS, argumentCount, cqMsg.getRequired().length));
                    cqManager.sendMsg(cqMsg);
                    return null;
                }
                int maxArgumentCount = cqMsg.getRequired().length + cqMsg.getOptional().length;
                if (Arrays.binarySearch(cqMsg.getRequired(), ParameterEnum.SEARCHPARAM) > -1) {
                    //由于Search命令用的正则不一样，允许单独包含模式，所以这里+1
                    maxArgumentCount++;
                }
                if (argumentCount > maxArgumentCount) {

                    cqMsg.setMessage(String.format(TipConsts.ARGUMENTS_MORE_THAN_PARAMETERS, argumentCount, cqMsg.getRequired().length + cqMsg.getOptional().length));
                    cqManager.sendMsg(cqMsg);
                    return null;
                }
                //取出子命令
                argument.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
                //判断是否需要以文字形式输出
                Matcher text = RegularPattern.TEXT_VERSION_COMMAND.matcher(argument.getSubCommandLowCase());
                argument.setText(text.find());
                Matcher legalParamMatcher;
                //如果指定了必选参数
                for (ParameterEnum p : cqMsg.getRequired()) {
                    //对所有必须参数进行检查
                    switch (p) {
                        case QQ:
                            //只有add命令需要QQ，而QQ是参数2
                            legalParamMatcher = RegularPattern.QQ.matcher(m.group(3));
                            if (legalParamMatcher.find()) {
                                argument.setQq(Long.valueOf(m.group(3)));
                            } else {
                                cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, m.group(3), "QQ号"));
                                cqManager.sendMsg(cqMsg);
                                return null;
                            }
                            break;
                        case USERID:
                            if (targetRaw.endsWith(" ")) {
                                targetRaw = targetRaw.substring(0, targetRaw.length() - 1);
                            }
                            legalParamMatcher = RegularPattern.OSU_USER_ID.matcher(targetRaw);
                            if (legalParamMatcher.find()) {
                                argument.setUserId(Integer.valueOf(targetRaw));
                            } else {
                                cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, targetRaw, "osu!uid"));
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
                            if (targetRaw.endsWith(" ")) {
                                targetRaw = targetRaw.substring(0, targetRaw.length() - 1);
                            }
                            legalParamMatcher = RegularPattern.OSU_USER_NAME.matcher(targetRaw);
                            if (legalParamMatcher.find() || "白菜".equals(targetRaw)) {
                                //2018-2-27 09:40:11这里把彩蛋放过去，在各个命令的方法里具体处理
                                argument.setUsername(targetRaw);
                            } else {
                                cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, targetRaw, "osu!用户名"));
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
                            mode = convertModeStrToInteger(targetRaw);
                            if (mode == null) {
                                cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, m.group(2), "osu!游戏模式"));
                                cqManager.sendMsg(cqMsg);
                                return null;
                            }
                            argument.setMode(mode);
                            break;
                        case SEARCHPARAM:
                            SearchParam searchParam = new SearchParam();
                            Integer modsNum = null;
                            String mods = "None";
                            String keyword = null;
                            String scoreString = null;
                            Double ar = null;
                            Double od = null;
                            Double cs = null;
                            Double hp = null;
                            boolean keywordFound = false;
                            //先检测是否指定了模式
                            Matcher getKeyWordAndMod = SearchKeywordPattern.MODE.matcher(msg);
                            if (getKeyWordAndMod.find()) {
                                keyword = getKeyWordAndMod.group(2);
                                keywordFound = true;
                                mode = convertModeStrToInteger(getKeyWordAndMod.group(3));
                                if (mode == null) {
                                    cqMsg.setMessage(String.format(TipConsts.FORMAT_ERROR, getKeyWordAndMod.group(3), "osu!游戏模式"));
                                    cqManager.sendMsg(cqMsg);
                                    return null;
                                }
                                argument.setMode(mode);
                            } else {
                                argument.setMode(0);
                            }
                            //再检测是否有PP计算参数
                            //兼容koohii 加默认值
                            searchParam.setMaxCombo(-1);
                            searchParam.setCount50(0);
                            searchParam.setCount100(0);
                            searchParam.setCountMiss(0);
                            getKeyWordAndMod = SearchKeywordPattern.PP_CALC.matcher(msg);
                            if (getKeyWordAndMod.find()) {
                                if (!keywordFound) {
                                    keyword = getKeyWordAndMod.group(2);
                                    keywordFound = true;
                                }

                                scoreString = getKeyWordAndMod.group(3);
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

                            //最后检测是否指定了mod
                            getKeyWordAndMod = SearchKeywordPattern.MOD.matcher(msg);
                            if (getKeyWordAndMod.find()) {
                                if (!keywordFound) {
                                    keyword = getKeyWordAndMod.group(2);
                                    keywordFound = true;
                                }
                                mods = getKeyWordAndMod.group(3);
                                modsNum = scoreUtil.reverseConvertMod(mods);
                                //如果字符串解析出错，会返回null，因此这里用null值来判断输入格式
                                if (modsNum == null) {
                                    cqMsg.setMessage("请使用MOD的双字母缩写，不需要任何分隔符。" +
                                            "\n接受的Mod有：NF EZ TD HD HR SD DT HT NC FL SO PF。");
                                    cqManager.sendMsg(cqMsg);
                                    return null;
                                }
                            }
                            if (!keywordFound) {
                                //这种情况，三个参数都没有指定
                                keyword = m.group(2);
                            }

                            //如果mode不是主模式，而且命令是search
                            if (!argument.getMode().equals(0) && "search".equals(argument.getSubCommandLowCase())) {
                                cqMsg.setMessage("由于oppai不支持其他模式，因此白菜也只有主模式支持!search命令。");
                                cqManager.sendMsg(cqMsg);
                                return null;
                            }

                            searchParam.setMods(modsNum);
                            searchParam.setModsString(mods);


                            if (keyword.endsWith(" ")) {
                                keyword = keyword.substring(0, keyword.length() - 1);
                            }
                            Matcher allNumberKeyword = SearchKeywordPattern.ALL_NUMBER_SEARCH_KEYWORD.matcher(keyword);
                            if (allNumberKeyword.find()) {
                                searchParam.setBeatmapId(Integer.valueOf(allNumberKeyword.group(1)));
                                argument.setSearchParam(searchParam);
                                break;
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
                            if (keyword.endsWith("]")) {
                                //如果有方括号
                                keyword += "(){}";
                            }
                            if (keyword.endsWith(")")) {
                                //如果有圆括号
                                keyword += "{}";
                            }
                            Matcher getArtistTitleEtc = SearchKeywordPattern.KETWORD.matcher(keyword);
                            if (!getArtistTitleEtc.find()) {
                                cqMsg.setMessage("请使用艺术家-歌曲标题[难度名](麻婆名){AR9.0OD9.0CS9.0HP9.0}:osu!std<> +MOD双字母简称 的格式。\n" +
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
                                    Matcher getFourDimens = SearchKeywordPattern.FOUR_DIMENSIONS_REGEX.matcher(fourDimensions);
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


                //如果有指定可选参数
                for (ParameterEnum p : cqMsg.getOptional()) {
                    //这里是可选参数
                    switch (p) {
                        case DAY:
                            if (numRaw != null) {

                                if (numRaw.endsWith(" ")) {
                                    numRaw = numRaw.substring(0, numRaw.length() - 1);
                                }
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
                            Matcher sleepMatcher = RegularPattern.SLEEP_REGEX.matcher(msg);
                            if (!sleepMatcher.find()) {
                                //sleep专用正则，sleep前面加东西不工作
                                return null;
                            }
                            if (numRaw != null) {
                                if (numRaw.endsWith(" ")) {
                                    numRaw = numRaw.substring(0, numRaw.length() - 1);
                                }
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
                            } else {
                                argument.setHour(6L);
                            }
                            break;
                        case NUM:
                            if (numRaw != null) {
                                if (numRaw.endsWith(" ")) {
                                    numRaw = numRaw.substring(0, numRaw.length() - 1);
                                }
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
                                //兼容带空格的
                                if (modeRaw.endsWith(" ")) {
                                    modeRaw = modeRaw.substring(0, modeRaw.length() - 1);
                                }
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
                cqMsg.setArgument(argument);
                return pjp.proceed(new Object[]{cqMsg});
            default:
                return null;
        }

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
