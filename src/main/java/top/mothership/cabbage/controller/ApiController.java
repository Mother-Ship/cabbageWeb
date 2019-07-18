package top.mothership.cabbage.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import top.mothership.cabbage.consts.OverallConsts;
import top.mothership.cabbage.controller.vo.ChartsVo;
import top.mothership.cabbage.controller.vo.PPChartVo;
import top.mothership.cabbage.manager.ApiManager;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.mapper.RedisDAO;
import top.mothership.cabbage.mapper.UserDAO;
import top.mothership.cabbage.mapper.UserInfoDAO;
import top.mothership.cabbage.pojo.User;
import top.mothership.cabbage.pojo.WebResponse;
import top.mothership.cabbage.pojo.coolq.osu.Userinfo;
import top.mothership.cabbage.service.CqServiceImpl;
import top.mothership.cabbage.service.UserServiceImpl;
import top.mothership.cabbage.util.osu.UserUtil;
import top.mothership.cabbage.util.qq.ImgUtil;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = {"application/json;charset=UTF-8"})
public class ApiController {
    private final UserInfoDAO userInfoDAO;
    private final ApiManager apiManager;
    private final ImgUtil imgUtil;
    private final UserDAO userDAO;
    private final UserUtil userUtil;
    private final WebPageManager webPageManager;
    private final RedisDAO redisDAO;
    private Logger logger = LogManager.getLogger(this.getClass());

    @Autowired
    public ApiController( UserInfoDAO userInfoDAO, ApiManager apiManager, ImgUtil imgUtil, UserDAO userDAO, UserUtil userUtil, WebPageManager webPageManager, RedisDAO redisDAO) {
        this.userInfoDAO = userInfoDAO;
        this.apiManager = apiManager;
        this.imgUtil = imgUtil;
        this.userDAO = userDAO;
        this.userUtil = userUtil;
        this.webPageManager = webPageManager;
        this.redisDAO = redisDAO;
    }


    private static int[][] multiple(int[][] a) {
        int[][] c = new int[1200][1200];
        for (int i = 0; i < 1200; i++) {
            for (int j = 0; j < 1200; j++) {
                for (int k = 0; k < 1200; k++) {
                    c[i][j] += a[i][k] * a[k][j];
                }
            }
        }
        return c;
    }

    @RequestMapping(value = "/code", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public String getCode() {
        return null;
    }

    @RequestMapping(value = "/userinfo/{uid}", method = RequestMethod.GET)
    public String userInfo(@PathVariable Integer uid,
                           @RequestParam("start") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate start,
                           @RequestParam("limit") Integer limit,
                           @RequestParam(value = "mode", required = false) Integer mode) {
        User user = userDAO.getUser(null, uid);
        if (user != null && mode == null) {
            //如果没有指定模式，而且用户
            mode = user.getMode();
        }
        //去osu api验证用户名是否存在
        Userinfo now = apiManager.getUser(mode, uid);
        if (now == null) {
            return new Gson().toJson(new WebResponse<>(1, "user not found", null));
        }
        //取到最接近8.29的那条记录
        Userinfo earliest = userInfoDAO.getNearestUserInfo(mode, now.getUserId(), LocalDate.of(2017, 8, 29));
        if (earliest == null) {
            return new Gson().toJson(new WebResponse<>(2, "user not registered", null));
        }
        //判断传入日期是否比最早的记录早，是则返回错误+最早的记录时间
        if (start.isBefore(earliest.getQueryDate())) {
            return new Gson().toJson(new WebResponse<>(3, "start date is too early", earliest.getQueryDate()));
        }
        //判断最早的记录+传入的天数是否比今天晚
        //这里不必-1，否则传入当天日期会返回0+null
        if (start.plusDays(limit).isAfter(LocalDate.now())) {
            return new Gson().toJson(new WebResponse<>(4, "end date is too late", null));
        }
        List<Userinfo> list = new ArrayList<>();
        for (long i = 0; i < limit; i++) {
            Userinfo tmp = userInfoDAO.getUserInfo(mode, now.getUserId(), start.plusDays(i));
            list.add(tmp);
        }

        return new Gson().toJson(new WebResponse<>(0, "ok", list));
    }

    @RequestMapping(value = "/user/qq/{qq}", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public String userRole(@PathVariable Long qq) {
        User user = userDAO.getUser(qq, null);
        if (user == null) {
            return new Gson().toJson(new WebResponse<>(1, "user not found", null));
        } else if (user.isBanned()) {
            return new Gson().toJson(new WebResponse<>(2, "user is banned", user));
        } else {
            return new Gson().toJson(new WebResponse<>(0, "success", user));
        }
    }

    @RequestMapping(value = "/stat/{uid}", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public void getStat(HttpServletResponse response, @PathVariable Integer uid, @RequestParam(value = "mode", defaultValue = "0", required = false) Integer mode) {
        String role;
        Integer scoreRank;
        User user = userDAO.getUser(null, uid);
        Userinfo userFromAPI = apiManager.getUser(mode, uid);
        Userinfo userInDB = null;
        boolean near = false;
        int day = 1;
        if (user == null) {
            if (userFromAPI == null) {
                return;
            } else {
                logger.info("玩家" + userFromAPI.getUserName() + "初次使用本机器人，开始登记");
                userUtil.registerUser(userFromAPI.getUserId(), mode, 0L, OverallConsts.DEFAULT_ROLE);
                userInDB = userFromAPI;
            }
            role = "creep";
        } else if (user.isBanned()) {
            //当数据库查到该玩家，并且被ban时，从数据库里取出最新的一份userinfo伪造
            userFromAPI = userInfoDAO.getNearestUserInfo(mode, user.getUserId(), LocalDate.now());
            //尝试补上当前用户名
            if (user.getCurrentUname() != null) {
                userFromAPI.setUserName(user.getCurrentUname());
            } else {
                List<String> list = new GsonBuilder().create().fromJson(user.getLegacyUname(), new TypeToken<List<String>>() {
                }.getType());
                if (list.size() > 0) {
                    userFromAPI.setUserName(list.get(0));
                } else {
                    userFromAPI.setUserName(String.valueOf(user.getUserId()));
                }
            }
            List<String> list = userUtil.sortRoles(user.getRole());
            role = list.get(0);
            day = 0;
            mode = user.getMode();
        } else {
            List<String> list = userUtil.sortRoles(user.getRole());
            role = list.get(0);
            userInDB = redisDAO.get(uid, mode);
            if (userInDB == null) {
                userInDB = userInfoDAO.getUserInfo(mode, userFromAPI.getUserId(), LocalDate.now().minusDays(day));
                if (userInDB == null) {
                    userInDB = userInfoDAO.getNearestUserInfo(mode, userFromAPI.getUserId(), LocalDate.now().minusDays(day));
                    near = true;
                }
            }
            mode = user.getMode();
        }


        //获取score rank
        scoreRank = webPageManager.getRank(userFromAPI.getRankedScore(), 1, 2000);
        String result = imgUtil.drawUserInfo(userFromAPI, userInDB, role, day, near, scoreRank, mode);
        byte[] bytes = Base64.getDecoder().decode(result);
        response.setContentType("image/png");
        try (InputStream in = new ByteArrayInputStream(bytes);
             OutputStream out = response.getOutputStream()) {
            BufferedImage img = ImageIO.read(in);
            BufferedImage img2 = new BufferedImage(600, 288, BufferedImage.TYPE_INT_RGB);
            img2.getGraphics().drawImage(img.getScaledInstance(600, 288, Image.SCALE_SMOOTH), 0, 0, null);
            ImageIO.write(img2, "png", new MemoryCacheImageOutputStream(out));
            out.write(bytes);
        } catch (IOException ignore) {

        }
    }

    @RequestMapping(value = "/userinfo/nearest/{uid}", method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost")
    public String nearestUserInfo(@PathVariable Integer uid, @RequestParam(value = "mode", defaultValue = "0", required = false) Integer mode) {
        Userinfo userInDB = redisDAO.get(uid, mode);
        if (userInDB == null) {
            userInDB = userInfoDAO.getNearestUserInfo(mode, uid, LocalDate.now().minusDays(1));
            if (userInDB == null) {
                userInDB = apiManager.getUser(mode, uid);
                userUtil.registerUser(userInDB.getUserId(), mode, 0L, OverallConsts.DEFAULT_ROLE);
                return new Gson().toJson(new WebResponse<>(1, "user not registered", null));
            }
        }
        return new Gson().toJson(new WebResponse<>(0, "ok", userInDB));
    }

    /**
     * 多音提的需求：绘制一个图表，给定某个PP段，横坐标是PP/TTH/RS/TTS在一个月的增长量，纵坐标是玩家人数
     *
     * @return 返回应该有两个数组
     */
    @RequestMapping(value = "/chart/{criteria}", method = RequestMethod.GET)
    @CrossOrigin
    public String domenCherry(@PathVariable String criteria,
                              @RequestParam("ppMin") Integer ppMin,
                              @RequestParam("ppMax") Integer ppMax,
                              @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                              @RequestParam(value = "start", defaultValue = "0", required = false) Integer start,
                              @RequestParam(value = "end", defaultValue = "0", required = false) Integer end,
                              @RequestParam(value = "grainSize", defaultValue = "0", required = false) Integer grainSize
    ) {
        if (startDate.isAfter(LocalDate.now())) {
            return new Gson().toJson(new WebResponse<>(4, "end date is too late", null));
        }
        ChartsVo vo = new ChartsVo();

        List<Userinfo> a = userInfoDAO.getStdUserRegisteredInOneMonth(ppMin, ppMax, startDate);

        List<Integer> uidList = new ArrayList<>(a.size());
        for (Userinfo userinfo : a) {
            uidList.add(userinfo.getUserId());
        }
        List<Userinfo> nowUserinfoList = userInfoDAO.batchGetNowUserinfo(uidList, startDate);
        Map<Integer, Userinfo> map = new HashMap<>(uidList.size());
        for (int k = 0; k < uidList.size(); k++) {
            map.put(a.get(k).getUserId(), a.get(k));
        }
        //把每个用户信息的PC TTH RS设为他一个月内的差值
        for (int k = 0; k < uidList.size(); k++) {
            Userinfo userinfo = nowUserinfoList.get(k);
            Userinfo i = map.get(userinfo.getUserId());
            userinfo.setPlayCount(userinfo.getPlayCount() - i.getPlayCount());
            userinfo.setCount50(userinfo.getCount50() - i.getCount50());
            userinfo.setCount100(userinfo.getCount100() - i.getCount100());
            userinfo.setCount300(userinfo.getCount300() - i.getCount300());
            userinfo.setRankedScore(userinfo.getRankedScore() - i.getRankedScore());
        }
        List<Long> xAxis = new ArrayList<>(500);

        vo.setXAxis(xAxis);

        //直接给a排序
        quickSort(nowUserinfoList, 0, nowUserinfoList.size() - 1, criteria);
        Userinfo max = nowUserinfoList.get(nowUserinfoList.size() - 1);
        Userinfo min = nowUserinfoList.get(0);
         long  maxValue ;
        switch (criteria) {
            case "tth":
                //划下X轴
                if(end.equals(0)) {
                    maxValue = max.getCount100() + max.getCount300() + max.getCount50() - min.getCount50() - min.getCount300() - min.getCount100();
                }else{
                    maxValue = end;
                }
                if(grainSize.equals(0)) grainSize = 10000;
                for (long i = start; i <= maxValue; i += grainSize) {
                    xAxis.add(i);
                }
                Integer[] yAxisRaw = new Integer[xAxis.size() - 1];
                vo.setYAxis(Arrays.asList(yAxisRaw));
//                划下Y轴
                for (int i = 1; i < xAxis.size(); i++) {
                    yAxisRaw[i - 1] = 0;
                    for (Userinfo userinfo : nowUserinfoList) {
                        int tth = userinfo.getCount100() + userinfo.getCount300() + userinfo.getCount50();
                        if (tth >= xAxis.get(i - 1) && tth < xAxis.get(i)) {
                            yAxisRaw[i - 1]++;
                        }
                    }
                }
                break;
            case "pc":
                if(end.equals(0)){
                    maxValue = max.getPlayCount() - min.getPlayCount();
                }else{
                    maxValue = end;
                }
                if(grainSize.equals(0)) grainSize = 20;
                for (long i = start; i <= maxValue; i += grainSize) {
                    xAxis.add(i);
                }
                yAxisRaw = new Integer[xAxis.size() - 1];
                vo.setYAxis(Arrays.asList(yAxisRaw));
//                划下Y轴
                for (int i = 1; i < xAxis.size(); i++) {
                    yAxisRaw[i - 1] = 0;
                    for (Userinfo userinfo : nowUserinfoList) {
                        int tth = userinfo.getPlayCount();
                        if (tth >= xAxis.get(i - 1) && tth < xAxis.get(i)) {
                            yAxisRaw[i - 1]++;
                        }
                    }
                }
                break;
            case "rs":
                if(end.equals(0)) {
                    maxValue = max.getRankedScore() - min.getRankedScore();
                }else{
                    maxValue = end;
                }
                if(grainSize.equals(0)) grainSize = 1000000;
                for (long i = start; i <  maxValue; i +=grainSize) {
                    xAxis.add(i);
                }
                yAxisRaw = new Integer[xAxis.size() - 1];
                vo.setYAxis(Arrays.asList(yAxisRaw));
//                划下Y轴
                for (int i = 1; i < xAxis.size(); i++) {
                    yAxisRaw[i - 1] = 0;
                    for (Userinfo userinfo : nowUserinfoList) {
                        long tth = userinfo.getRankedScore();
                        if (tth >= xAxis.get(i - 1) && tth < xAxis.get(i)) {
                            yAxisRaw[i - 1]++;
                        }
                    }
                }
            case "tts":
                if(end.equals(0)) {
                    maxValue = max.getTotalScore() - min.getTotalScore();
                }else{
                    maxValue = end;
                }
                if(grainSize.equals(0)) grainSize = 10000000;
                for (long i = start; i <  maxValue; i += grainSize) {
                    xAxis.add(i);
                }
                yAxisRaw = new Integer[xAxis.size() - 1];
                vo.setYAxis(Arrays.asList(yAxisRaw));
//                划下Y轴
                for (int i = 1; i < xAxis.size(); i++) {
                    yAxisRaw[i - 1] = 0;
                    for (Userinfo userinfo : nowUserinfoList) {
                        long tth = userinfo.getTotalScore();
                        if (tth >= xAxis.get(i - 1) && tth < xAxis.get(i)) {
                            yAxisRaw[i - 1]++;
                        }
                    }
                }
                break;
        }
        return new Gson().toJson(new WebResponse<>(0, "success", vo));
    }

    /**
     * 盒子提的需求：PP曲线图
     */
    @RequestMapping(value = "/pp_chart.php", method = RequestMethod.GET)
    @CrossOrigin
    public String kongouHikari(@RequestParam("id") String id,@RequestParam("mode") Integer mode) {
        Userinfo userinfo = apiManager.getUser(0,id);
        List<Userinfo> list = userInfoDAO.listUserInfoByUserIdAndMode(userinfo.getUserId(),mode);
        PPChartVo vo = new PPChartVo();
        List<String> xAxis = new ArrayList<>(list.size());
        List<Float> yAxis = new ArrayList<>(list.size());


        for (Userinfo userinfo1 : list) {
            xAxis.add(userinfo1.getQueryDate().toString());
            yAxis.add(userinfo1.getPpRaw());
        }
        vo.setXAxis(xAxis);
        vo.setYAxis(yAxis);
        return new Gson().toJson(new WebResponse<>(0, "success", vo));
    }

    public void quickSort(List<Userinfo> arr, int start, int end, String criteria) {
        int i = start, j = end;//设置两头两个指针
        Userinfo pivot = arr.get(start);//选第一个元素为基准

        while (i <= j) {
            switch (criteria) {
                case "tth":
                    while ((i < arr.size()) && (
                            arr.get(i).getCount300() + arr.get(i).getCount50() + arr.get(i).getCount100()
                                    < pivot.getCount300() + pivot.getCount100() + pivot.getCount50())) {
                        i++;
                    }
                    while ((j > 0) && (arr.get(j).getCount300() + arr.get(j).getCount50() + arr.get(j).getCount100()
                            > pivot.getCount300() + pivot.getCount100() + pivot.getCount50())) {
                        j--;
                    }

                    break;
                case "pc":
                    while ((i < arr.size()) && (
                            arr.get(i).getPlayCount()
                                    < pivot.getPlayCount())) {
                        i++;
                    }
                    while ((j > 0) && (arr.get(j).getPlayCount()
                            > pivot.getPlayCount())) {
                        j--;
                    }
                    break;
                case "rs":
                    while ((i < arr.size()) && (
                            arr.get(i).getRankedScore()
                                    < pivot.getRankedScore())) {
                        i++;
                    }
                    while ((j > 0) && (arr.get(i).getRankedScore()
                            > pivot.getRankedScore())) {
                        j--;
                    }
                    break;
                case "tts":
                    while ((i < arr.size()) && (
                            arr.get(i).getTotalScore()
                                    < pivot.getTotalScore())) {
                        i++;
                    }
                    while ((j > 0) && (arr.get(i).getTotalScore()
                            > pivot.getTotalScore())) {
                        j--;
                    }
                    break;
            }
            if (i <= j) {
                swap(arr, i, j);
                i++;
                j--;
            }
        }
        if (start < j)
            quickSort(arr, start, j, criteria);
        if (i < end)
            quickSort(arr, i, end, criteria);
    }

    //交换两个元素
    public void swap(List<Userinfo> arr, int i, int j) {
        Userinfo temp = arr.get(i);
        arr.set(i, arr.get(j));
        arr.set(j, temp);
    }
//    @RequestMapping(value = "/upload",method = RequestMethod.POST)
//    @CrossOrigin(origins = "http://localhost")
//    public String upload(@RequestParam(value = "myfile") MultipartFile file) throws Exception {
//        String path = "C:\\coolq Pro\\data\\image\\resource\\db\\";
//        String fileName = file.getOriginalFilename();
//        String resp;
//
//        if(!fileName.matches("(.*).db$")){
//            logger.warn("检测到文件扩展名错误的文件上传请求：文件名为"+fileName);
//            //返回字符串给JS进行弹窗
//            return "文件类型错误";
////            throw new Exception("类型错误");
//        }
//        File targetFile = new File(path, fileName);
//        if (!targetFile.exists()){
//            targetFile.mkdirs();
//            // 保存
//            resp = "服务端消息：上传成功";
//        }else{
//            resp = "服务端消息：覆盖成功";
//        }
//        try {
//            logger.info("检测到文件上传请求：文件名为"+fileName);
//            file.transferTo(targetFile);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//        return resp;
//    }
}