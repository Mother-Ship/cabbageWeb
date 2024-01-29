package top.mothership.cabbage.aspect;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.mothership.cabbage.constant.Overall;
import top.mothership.cabbage.enums.ParameterEnum;
import top.mothership.cabbage.constant.Tip;
import top.mothership.cabbage.manager.CqManager;
import top.mothership.cabbage.mapper.ResDAO;
import top.mothership.cabbage.constant.pattern.CQCodePattern;
import top.mothership.cabbage.constant.pattern.RegularPattern;
import top.mothership.cabbage.constant.pattern.SearchKeywordPattern;
import top.mothership.cabbage.pojo.coolq.Argument;
import top.mothership.cabbage.pojo.coolq.CqMsg;
import top.mothership.cabbage.pojo.osu.SearchParam;
import top.mothership.cabbage.util.osu.ScoreUtil;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;

@Component
@Aspect
@Order(2)
public class ParameterVerifyAspect {
    private final CqManager cqManager;
    private final ScoreUtil scoreUtil;
    private final ResDAO resDAO;
    private Logger logger = LogManager.getLogger(this.getClass());

    public ParameterVerifyAspect(CqManager cqManager, ScoreUtil scoreUtil, ResDAO resDAO) {
        this.cqManager = cqManager;
        this.scoreUtil = scoreUtil;
        this.resDAO = resDAO;
    }

    //当时为啥要指定CQ开头来着……
    @Pointcut("execution(* top.mothership.cabbage.service.*.*(top.mothership.cabbage.pojo.coolq.CqMsg,..))")
    private void regularService() {
    }


    @Around("regularService() && args(cqMsg,..)")
    public Object regularCommand(ProceedingJoinPoint pjp, CqMsg cqMsg) throws Throwable {
        switch (cqMsg.getPostType()) {
            case "message":
                //只处理消息，事件直接放行
                switch (cqMsg.getMessageType()) {
                    //只处理私聊 群 讨论组消息，其他消息不予处理
                    case "group":
                    case "discuss":
                    case "private":
                        Argument argument = new Argument();
                        String msg = cqMsg.getMessage();
                        cqMsg.setArgument(argument);
                        if (cqMsg.getRequired() == null && cqMsg.getOptional() == null) {
                            //如果命令没有指定required和optional，直接让它执行
                            //2018-2-27 16:09:47漏掉了argument，在这里也需要一个argument（
                            Matcher m = RegularPattern.REG_CMD_REGEX.matcher(msg);
                            if (m.find()) {
                                argument.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
                                cqMsg.setArgument(argument);
                            }
                            if ("sudo".equals(argument.getSubCommandLowCase())) {
                                m = RegularPattern.ADMIN_CMD_REGEX.matcher(msg);
                                m.find();
                                argument.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
                                cqMsg.setArgument(argument);
                            }
                            return pjp.proceed(new Object[]{cqMsg});
                        }
                        if (cqMsg.getRequired() == null) {
                            cqMsg.setRequired(Overall.EMPTY_PARAMETER_LIST);
                        }
                        if (cqMsg.getOptional() == null) {
                            cqMsg.setOptional(Overall.EMPTY_PARAMETER_LIST);
                        }
                        int argumentCount = 0;
                        String firstParam = null;
                        String secondParam = null;
                        String thirdParam = null;
                        //预定义变量，day默认为1，这样才能默认和昨天的比较
                        Integer day = 1;
                        //mode预设为null
                        //2018-2-27 09:42:45 由于各个命令 未指定Mode的时候表现不同，所以不能预设为0
                        Integer mode = null;
                        Integer num = null;


                        Matcher m = RegularPattern.REG_CMD_REGEX.matcher(msg);
                        if(!m.find()){
                            //对BP命令做的一个Patch（那个命令分两层，第二层会重新进入该方法，此时会报不匹配错误）
                            return null;
                        }
                        //取出子命令
                        argument.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
                        if ("sudo".equals(argument.getSubCommandLowCase())) {
                            m = RegularPattern.ADMIN_CMD_REGEX.matcher(msg);
                            m.find();
                            argument.setSubCommandLowCase(m.group(1).toLowerCase(Locale.CHINA));
                            cqMsg.setArgument(argument);
                        }
                        if (!"".equals(m.group(2))) {
                            firstParam = m.group(2);
                            //取出井号和冒号的位置
                            int indexOfSharp = firstParam.indexOf("#");
                            int indexOfColon = -1;
                            if (firstParam.contains(":")) {
                                indexOfColon = firstParam.indexOf(":");
                            }
                            if (firstParam.contains("：")) {
                                indexOfColon = firstParam.indexOf("：");
                            }
                            //进行分割
                            String[] args = firstParam.split("[#:：]");
                            argumentCount = args.length;
                            if ("".equals(args[0])) {
                                argumentCount--;
                            }

                            for (int i = 0; i < args.length; i++) {
                                if (args[i].endsWith(" ")) {
                                    args[i] = args[i].substring(0, args[i].length() - 1);
                                }
                                switch (i) {
                                    case 0:
                                        firstParam = args[i];
                                        break;
                                    case 1:
                                        if (indexOfColon < indexOfSharp) {
                                            //如果参数段里冒号在前，那列表里的第二个是冒号开头的三号参数
                                            if (indexOfColon == -1) {
                                                //如果只指定了井号，那这个还是2号参数
                                                secondParam = args[i];
                                            } else {
                                                thirdParam = args[i];
                                            }
                                        } else {
                                            if (indexOfSharp == -1) {
                                                //如果只指定了冒号，那这个还是三号参数
                                                thirdParam = args[i];
                                            } else {
                                                secondParam = args[i];
                                            }
                                        }
                                        break;
                                    case 2:
                                        if (indexOfColon < indexOfSharp) {
                                            //如果参数段里冒号在前，那参数表的第三个参数是井号开头的二号参数
                                            secondParam = args[i];
                                        } else {
                                            //如果有三个参数，肯定两个符号，进行比较即可
                                            thirdParam = args[i];
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                            //解析结束后，如果没有对应的分隔符，后面的参数会是默认值null
                        }
                        if ("bg".equals(argument.getSubCommandLowCase())) {
                            //由于URL里的冒号问题
                            argumentCount--;
                        }
                        if (Arrays.asList(cqMsg.getRequired()).contains(ParameterEnum.AT)) {
                            //对@中CQ码的冒号与分隔符冲突的特殊处理
                            argumentCount--;
                            firstParam += ":";
                            firstParam += thirdParam;
                        }
                        //确保实参数目不小于最小形参数目，也不大于最大形参数目
                        if (argumentCount < cqMsg.getRequired().length) {
                            cqMsg.setMessage(String.format(Tip.ARGUMENTS_LESS_THAN_PARAMETERS, Arrays.toString(cqMsg.getRequired()), argumentCount));
                            cqManager.sendMsg(cqMsg);
                            return null;
                        }
                        int maxArgumentCount = cqMsg.getRequired().length + cqMsg.getOptional().length;
                        if (Arrays.binarySearch(cqMsg.getRequired(), ParameterEnum.SEARCH_PARAM) > -1) {
                            //由于Search命令用的正则不一样，允许单独包含模式，所以这里+1
                            maxArgumentCount++;
                        }
                        if (argumentCount > maxArgumentCount) {
                            cqMsg.setMessage(String.format(Tip.ARGUMENTS_MORE_THAN_PARAMETERS, Arrays.toString(cqMsg.getRequired()), Arrays.toString(cqMsg.getOptional()), argumentCount));
                            cqManager.sendMsg(cqMsg);
                            return null;
                        }

                        //判断是否需要以文字形式输出
                        Matcher text = RegularPattern.TEXT_VERSION_COMMAND.matcher(argument.getSubCommandLowCase());
                        argument.setText(text.find());
                        Matcher legalParamMatcher;
                        //如果指定了必选参数
                        for (ParameterEnum p : cqMsg.getRequired()) {
                            //对所有必须参数进行检查
                            switch (p) {
                                case QQ:
                                    //add命令的必须参数 QQ是第二个参数
                                    String qqRaw;
                                    if (secondParam != null) {
                                        qqRaw = secondParam;
                                    } else {
                                        //sudo checkq 用的
                                        qqRaw = firstParam;
                                    }
                                    legalParamMatcher = RegularPattern.QQ.matcher(qqRaw);
                                    if (legalParamMatcher.find()) {
                                        argument.setQq(Long.valueOf(qqRaw));
                                    } else {
                                        cqMsg.setMessage(String.format(Tip.FORMAT_ERROR, qqRaw, "QQ号"));
                                        cqManager.sendMsg(cqMsg);
                                        return null;
                                    }
                                    break;
                                case USER_ID:
                                    legalParamMatcher = RegularPattern.OSU_USER_ID.matcher(firstParam);
                                    if (legalParamMatcher.find()) {
                                        argument.setUserId(Integer.valueOf(firstParam));
                                    } else {
                                        cqMsg.setMessage(String.format(Tip.FORMAT_ERROR, firstParam, "osu!uid"));
                                        cqManager.sendMsg(cqMsg);
                                        return null;
                                    }
                                    if (argument.getUserId() == 3) {
                                        cqMsg.setMessage(Tip.QUERY_BANCHO_BOT);
                                        cqManager.sendMsg(cqMsg);
                                        return null;
                                    }
                                    break;
                                case USERNAME:
                                    legalParamMatcher = RegularPattern.OSU_USER_NAME.matcher(firstParam);
                                    if (legalParamMatcher.find() || "白菜".equals(firstParam)) {
                                        //2018-2-27 09:40:11这里把彩蛋放过去，在各个命令的方法里具体处理
                                        argument.setUsername(firstParam);
                                    } else {
                                        cqMsg.setMessage(String.format(Tip.FORMAT_ERROR, firstParam, "osu!用户名"));
                                        cqManager.sendMsg(cqMsg);
                                        return null;
                                    }
                                    if (argument.getUsername().toLowerCase().equals("banchobot")) {
                                        cqMsg.setMessage(Tip.QUERY_BANCHO_BOT);
                                        cqManager.sendMsg(cqMsg);
                                        return null;
                                    }
                                    break;
                                case MODE:
                                    //!mode xxx，是参数1
                                    mode = convertModeStrToInteger(firstParam);
                                    if (mode == null) {
                                        //兼容备选参数
                                        if (thirdParam != null) {
                                            //兼容带空格的
                                            if (thirdParam.endsWith(" ")) {
                                                thirdParam = thirdParam.substring(0, thirdParam.length() - 1);
                                            }
                                            mode = convertModeStrToInteger(thirdParam);
                                        }
                                    }
                                    if (mode == null) {
                                        cqMsg.setMessage(String.format(Tip.FORMAT_ERROR, firstParam, "osu!游戏模式"));
                                        cqManager.sendMsg(cqMsg);
                                        return null;
                                    }
                                    argument.setMode(mode);
                                    break;
                                case FILENAME:
                                    //没必要做验证
                                    argument.setFileName(firstParam);
                                    break;
                                case URL:
                                    //由于连接中的冒号与其他参数处理冲突，这里单独处理msg
                                    String url = msg.substring(msg.indexOf(":") + 1);
                                    Matcher urlMatcher = RegularPattern.URL.matcher(url);
                                    if (urlMatcher.find()) {
                                        argument.setUrl(url);
                                    } else {
                                        cqMsg.setMessage(String.format(Tip.FORMAT_ERROR, url, "URL"));
                                        cqManager.sendMsg(cqMsg);
                                        return null;
                                    }
                                    break;
                                case ROLE:
                                    //如果是未指定usernames和username的（既不是add/del也不是钦点命令），并且指定了第一个参数，就使用第一个参数
                                    if (argument.getUsernames() == null && argument.getUsername() == null && firstParam != null) {
                                        //兼容两种参数
                                        argument.setRole(firstParam);
                                    } else {
                                        argument.setRole(thirdParam);
                                    }
                                    //以后可以考虑大规模重构，把用户组抽出来做一个表
                                    break;
                                case USERNAME_LIST:
                                    if (firstParam != null) {
                                        String[] usernames = firstParam.split(",");
                                        argument.setUsernames(Arrays.asList(usernames));
                                    }
                                    break;
                                case AT:
                                    //AT的逻辑不一样，检查第一参数+第三参数（CQ码有冒号）
                                    Matcher atMatcher = CQCodePattern.AT.matcher(firstParam);
                                    if (atMatcher.find()) {
                                        //如果是艾特qq
                                        if ("all".equals(atMatcher.group(1))) {
                                            //艾特全员改成-1
                                            argument.setQq(-1L);
                                        } else {
                                            argument.setQq(Long.valueOf(atMatcher.group(1)));
                                        }
                                    } else {
                                        //也兼容直接输入qq
                                        Matcher qqMatcher = RegularPattern.QQ.matcher(firstParam);
                                        if (qqMatcher.find()) {
                                            argument.setQq(Long.valueOf(firstParam));
                                        } else {
                                            cqMsg.setMessage(String.format(Tip.FORMAT_ERROR, firstParam, "QQ号"));
                                            cqManager.sendMsg(cqMsg);
                                            return null;
                                        }
                                    }
                                    break;
                                case FLAG:
                                    //懒得验证了
                                    argument.setFlag(firstParam);
                                    break;
                                case SEARCH_PARAM:
                                    SearchParam searchParam = genSearchParam(cqMsg);
                                    if(searchParam ==null){
                                        return null;
                                    }
                                    argument.setSearchParam(searchParam);
                                    break;
                                case BEATMAP_ID:
                                    argument.setBeatmapId(Integer.valueOf(secondParam));
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
                                    try {
                                        if (secondParam != null) {
                                            day = Integer.valueOf(secondParam);
                                        }
                                        if ("afk".equals(argument.getSubCommandLowCase()) && firstParam != null) {
                                            //兼容!sudo afk 180:mp5
                                            day = Integer.valueOf(firstParam);
                                        }
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

                                    argument.setDay(day);
                                    break;
                                case HOUR:

                                    Matcher sleepMatcher = RegularPattern.SLEEP_REGEX.matcher(msg);
                                    if (!sleepMatcher.find()) {
                                        //sleep专用正则，sleep前面加东西不工作
                                        return null;
                                    }
                                    if (firstParam != null) {
                                        Long hour;
                                        try {
                                            hour = Long.valueOf(firstParam);
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
                                    if (secondParam != null) {
                                        Matcher bpNumMatcher = RegularPattern.BPNUM.matcher(secondParam);
                                        if (bpNumMatcher.find()) {
                                            num = Integer.valueOf(secondParam);
                                            if (num <= 0 || num > 100) {
                                                cqMsg.setMessage("其他人看不到的东西，白菜也看不到啦。");
                                                cqManager.sendMsg(cqMsg);

                                                return null;
                                            }
                                        } else {
                                            cqMsg.setMessage("[CQ:record,file=base64://" + Base64.getEncoder().encodeToString((byte[]) resDAO.getResource("ay_ay_ay.wav")) + "]");
                                            cqManager.sendMsg(cqMsg);
                                            return null;
                                        }
                                        argument.setNum(num);
                                    }
                                    break;
                                case MODE:
                                    //!stat :xxx，是上面取的thirdParam
                                    if (thirdParam != null) {
                                        mode = convertModeStrToInteger(thirdParam);
                                        if (mode == null) {
                                            cqMsg.setMessage(String.format(Tip.FORMAT_ERROR, thirdParam, "osu!游戏模式"));
                                            cqManager.sendMsg(cqMsg);
                                            return null;
                                        }
                                        argument.setMode(mode);
                                    }
                                    break;
                                case ROLE:
                                    argument.setRole(thirdParam);
                                    //以后可以考虑大规模重构，把用户组抽出来做一个表
                                    break;
                                case SECOND:
                                    //我这个参数比较特殊
                                    String[] args = firstParam.split(" ");
                                    if (args.length > 1) {
                                        argument.setSecond(Integer.valueOf(args[1]));
                                    } else {
                                        argument.setSecond(600);
                                    }
                                    break;
                                case GROUPID:
                                    //群号和QQ试用同一个正则
                                    legalParamMatcher = RegularPattern.QQ.matcher(firstParam);
                                    if (legalParamMatcher.find()) {
                                        argument.setGroupId(Long.valueOf(firstParam));
                                    } else {
                                        cqMsg.setMessage(String.format(Tip.FORMAT_ERROR, firstParam, "群号"));
                                        cqManager.sendMsg(cqMsg);
                                        return null;
                                    }
                                    break;
                                case QQ:
                                    //可选项QQ参数只有在钦点里用到
                                    legalParamMatcher = RegularPattern.QQ.matcher(secondParam);
                                    if (legalParamMatcher.find() || "0".equals(secondParam)) {
                                        argument.setQq(Long.valueOf(secondParam));
                                    } else {
                                        cqMsg.setMessage(String.format(Tip.FORMAT_ERROR, secondParam, "QQ号"));
                                        cqManager.sendMsg(cqMsg);
                                        return null;
                                    }


                                    break;
                                case BOUND:
                                    argument.setBound(firstParam);
                                    //以后可以考虑大规模重构，把用户组抽出来做一个表
                                default:
                                    break;
                            }
                        }

                        break;

                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return pjp.proceed(new Object[]{cqMsg});
    }

    private SearchParam genSearchParam(CqMsg cqMsg) {
        String msg = cqMsg.getMessage();
        Argument argument = cqMsg.getArgument();
        SearchParam searchParam = new SearchParam();
        Integer modsNum = null;
        String mods = "None";
        Integer mode;
        String keyword = null;
        String scoreString = null;
        Double ar = null;
        Double od = null;
        Double cs = null;
        Double hp = null;
        boolean keywordFound = false;
        //先从字符串结尾的mod开始检测
        Matcher getKeyWordAndMod = SearchKeywordPattern.MOD.matcher(msg);
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
            //如果检测出来就去掉
            msg = msg.replace("+" + mods, "");
        }
        //再检测是否指定了成绩字符串

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
            msg = msg.replace(scoreString, "");
            msg = msg.replaceAll("[《<>》]", "");
        }
        //最后检测是否指定了模式（如果先检测，会把后面的文字也计算进去）
        getKeyWordAndMod = SearchKeywordPattern.MODE.matcher(msg);
        if (getKeyWordAndMod.find()) {
            keyword = getKeyWordAndMod.group(2);
            keywordFound = true;
            mode = convertModeStrToInteger(getKeyWordAndMod.group(3));
            if (mode == null) {
                logger.debug(getKeyWordAndMod.group(3));
                cqMsg.setMessage(String.format(Tip.FORMAT_ERROR, getKeyWordAndMod.group(3), "osu!游戏模式"));
                cqManager.sendMsg(cqMsg);
                return null;
            }
            argument.setMode(mode);
        } else {
            argument.setMode(0);
        }


        if (!keywordFound) {
            //这种情况，三个参数都没有指定
            Matcher m = RegularPattern.REG_CMD_REGEX.matcher(msg);
            m.find();
            if ("sudo".equals(argument.getSubCommandLowCase())) {
                m = RegularPattern.ADMIN_CMD_REGEX.matcher(msg);
                m.find();
            }
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
            return searchParam;
        }
        //新格式(咕)

        //比较菜，手动补齐参数
        if (!keyword.contains("-")) {
            //如果没有横杠，手动补齐
            keyword = "-" + keyword;
        }
        if (!(keyword.endsWith("]") || keyword.endsWith(")") || keyword.endsWith("}")
                || keyword.endsWith("】") || keyword.endsWith("）")
        )) {
            //如果圆括号 方括号 花括号都没有
            keyword += "[](){}";
        }
        if (keyword.endsWith("]") || keyword.endsWith("】")) {
            //如果有方括号
            keyword += "(){}";
        }
        if (keyword.endsWith(")") || keyword.endsWith("）")) {
            //如果有圆括号
            keyword += "{}";
        }
        Matcher getArtistTitleEtc = SearchKeywordPattern.KETWORD.matcher(keyword);
        if (!getArtistTitleEtc.find()) {
            cqMsg.setMessage("搜索格式：艺术家-歌曲标题[难度名](麻婆名){AR9.0OD9.0CS9.0HP9.0}:osu!std<98acc 1x100 2x50 3xmiss 4cb> +MOD双字母简称。\n" +
                    "所有参数都可以省略(但横线、方括号和圆括号不能省略)，方括号 圆括号和四维的小数点支持全/半角；四维顺序必须按AR OD CS HP排列。");
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
            return searchParam;
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
            case "osu!std":
            case "泡泡":
                return 0;
            case "1":
            case "太鼓":
            case "taiko":
            case "o!taiko":
            case "t":
            case "打鼓":
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
            case "接屎":
                return 2;
            case "3":
            case "osu!mania":
            case "mania":
            case "骂娘":
            case "钢琴":
            case "o!m":
            case "m":
            case "下落":
            case "下落式":
                return 3;
            default:
                return null;

        }

    }
}
