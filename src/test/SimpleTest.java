import org.junit.Test;
import top.mothership.cabbage.manager.WebPageManager;
import top.mothership.cabbage.pojo.osu.Beatmap;

import java.io.IOException;

public class SimpleTest {
    @Test
    public void Test() throws IOException {
        Beatmap beatmap = new Beatmap();

    new WebPageManager().getBGBackup(beatmap);

//        String test = "osu file format v12\n" +
//                "\n" +
//                "[General]\n" +
//                "AudioFilename: 03 IMAGE -MATERIAL-2.mp3\n" +
//                "AudioLeadIn: 0\n" +
//                "PreviewTime: 310262\n" +
//                "Countdown: 0\n" +
//                "SampleSet: Soft\n" +
//                "StackLeniency: 0.7\n" +
//                "Mode: 0\n" +
//                "LetterboxInBreaks: 0\n" +
//                "WidescreenStoryboard: 0\n" +
//                "\n" +
//                "[Editor]\n" +
//                "DistanceSpacing: 1.3\n" +
//                "BeatDivisor: 4\n" +
//                "GridSize: 32\n" +
//                "\n" +
//                "[Metadata]\n" +
//                "Title:IMAGE -MATERIAL- <Version 0>\n" +
//                "TitleUnicode:IMAGE -MATERIAL- <Version 0>\n" +
//                "Artist:Tatsh\n" +
//                "ArtistUnicode:Tatsh\n" +
//                "Creator:Scorpiour\n" +
//                "Version:Scorpiour\n" +
//                "Source:\n" +
//                "Tags:Firce777 Reflec Beat\n" +
//                "BeatmapID:252238\n" +
//                "BeatmapSetID:93523\n" +
//                "\n" +
//                "[Difficulty]\n" +
//                "HPDrainRate:6\n" +
//                "CircleSize:4\n" +
//                "OverallDifficulty:8\n" +
//                "ApproachRate:10\n" +
//                "SliderMultiplier:1.8\n" +
//                "SliderTickRate:1\n" +
//                "\n" +
//                "[Events]\n" +
//                "//Background and Video events\n" +
//                "0,0,\"bg.jpg\",0,0\n" +
//                "//Break Periods\n" +
//                "2,49251,55985\n" +
//                "//Storyboard Layer 0 (Background)\n" +
//                "//Storyboard Layer 1 (Fail)\n" +
//                "//Storyboard Layer 2 (Pass)\n" +
//                "//Storyboard Layer 3 (Foreground)\n" +
//                "Sprite,Foreground,Centre,\"bg.jpg\",320,240\n" +
//                " C,0,0,6590,0,0,0\n" +
//                " S,0,0,384127,0.8\n" +
//                " C,0,6590,21359,0,0,0,128,128,128\n" +
//                " C,0,21359,55512,128,128,128,255,255,255\n" +
//                " C,0,55512,56666,255,255,255,128,128,128\n" +
//                " C,0,56666,114589,128,128,128\n" +
//                " C,0,114589,115512,128,128,128,64,64,64\n" +
//                " C,0,115512,117589,64,64,64,128,128,128\n" +
//                " C,0,117589,227204,128,128,128\n" +
//                " C,0,227204,227665,128,128,128,192,192,192\n" +
//                " C,0,227665,252702,192,192,192\n" +
//                " C,0,252702,258942,192,192,192,128,128,128\n" +
//                " C,0,258942,261073,128,128,128,0,0,0\n" +
//                " C,0,261073,263204,0,0,0\n" +
//                " C,0,263204,384127,128,128,128\n" +
//                " C,0,384127,388742,0,0,0\n" +
//                " C,0,388742,389665,0,0,0,255,255,255\n" +
//                "//Storyboard Sound Samples\n" +
//                "//Background Colour Transformations\n" +
//                "3,100,0,0,0\n" +
//                "\n" +
//                "[TimingPoints]\n" +
//                "6590,461.538461538462,4,2,1,6,1,0\n" +
//                "8436,-100,4,2,1,30,0,0\n" +
//                "8551,-100,4,2,1,6,0,0\n" +
//                "10282,-100,4,1,1,30,0,0\n" +
//                "10397,-100,4,2,1,10,0,0\n" +
//                "10744,-100,4,2,1,15,0,0\n" +
//                "11206,-100,4,2,1,20,0,0\n" +
//                "11667,-100,4,2,1,25,0,0\n" +
//                "12129,-100,4,2,1,30,0,0\n" +
//                "13513,-100,4,2,1,6,0,0\n" +
//                "13974,-100,4,2,1,30,0,0\n" +
//                "14090,-100,4,2,1,6,0,0\n" +
//                "15820,-100,4,2,1,30,0,0\n" +
//                "15936,-100,4,2,1,6,0,0\n" +
//                "17666,-100,4,2,1,30,0,0\n" +
//                "17782,-100,4,2,1,10,0,0\n" +
//                "18128,-100,4,2,1,15,0,0\n" +
//                "18590,-100,4,2,1,20,0,0\n" +
//                "19051,-100,4,2,1,25,0,0\n" +
//                "19513,-100,4,2,1,30,0,0\n" +
//                "20782,-100,4,2,1,6,0,0\n" +
//                "21243,-200,4,2,1,30,0,0\n" +
//                "43513,-200,4,2,1,15,0,0\n" +
//                "43974,-200,4,2,1,20,0,0\n" +
//                "44436,-200,4,2,1,25,0,0\n" +
//                "44897,-200,4,2,1,30,0,0\n" +
//                "45359,-200,4,2,1,35,0,0\n" +
//                "45820,-200,4,2,1,40,0,0\n" +
//                "46282,-200,4,2,1,45,0,0\n" +
//                "46743,-200,4,2,1,50,0,0\n" +
//                "49051,230.769230769231,4,2,1,5,1,0\n" +
//                "56377,-100,4,1,1,60,0,0\n" +
//                "70224,-100,4,1,1,55,0,0\n" +
//                "70512,-100,4,1,1,50,0,0\n" +
//                "70743,-100,4,1,1,45,0,0\n" +
//                "70974,-100,4,1,1,40,0,0\n" +
//                "71204,-100,4,1,1,60,0,0\n" +
//                "77377,-100,4,1,1,50,0,0\n" +
//                "78589,-100,4,1,1,60,0,0\n" +
//                "86897,-100,4,1,1,55,0,0\n" +
//                "87127,-100,4,1,1,50,0,0\n" +
//                "87358,-100,4,1,1,45,0,0\n" +
//                "87589,-100,4,1,1,40,0,0\n" +
//                "87820,-100,4,1,1,45,0,0\n" +
//                "88051,-100,4,1,1,50,0,0\n" +
//                "88281,-100,4,1,1,55,0,0\n" +
//                "88743,-100,4,1,1,60,0,1\n" +
//                "88801,-100,4,1,1,60,0,0\n" +
//                "96127,-100,4,1,1,55,0,0\n" +
//                "98897,-100,4,1,1,50,0,0\n" +
//                "99358,-100,4,1,1,45,0,0\n" +
//                "100281,-100,4,1,1,50,0,0\n" +
//                "100743,-100,4,1,1,55,0,0\n" +
//                "101204,-100,4,1,1,60,0,0\n" +
//                "103512,-100,4,1,1,55,0,0\n" +
//                "118281,-100,4,1,1,40,0,0\n" +
//                "118743,-100,4,1,1,45,0,0\n" +
//                "119204,-100,4,1,1,50,0,0\n" +
//                "119666,-100,4,1,1,55,0,0\n" +
//                "120127,-100,4,1,1,60,0,0\n" +
//                "130974,-100,4,1,1,40,0,0\n" +
//                "131204,-100,4,1,1,60,0,0\n" +
//                "133858,-100,4,1,1,50,0,0\n" +
//                "138531,-200,4,1,1,60,0,0\n" +
//                "150416,-200,4,1,1,55,0,0\n" +
//                "151281,-200,4,1,1,50,0,0\n" +
//                "152551,-200,4,1,1,49,0,0\n" +
//                "152608,-200,4,1,1,48,0,0\n" +
//                "152666,-200,4,1,1,47,0,0\n" +
//                "152724,-200,4,1,1,46,0,0\n" +
//                "152781,-200,4,1,1,45,0,0\n" +
//                "152839,-200,4,1,1,44,0,0\n" +
//                "152897,-200,4,1,1,43,0,0\n" +
//                "152954,-200,4,1,1,42,0,0\n" +
//                "153012,-200,4,1,1,41,0,0\n" +
//                "153070,-200,4,1,1,40,0,0\n" +
//                "153127,-200,4,1,1,39,0,0\n" +
//                "153185,-200,4,1,1,38,0,0\n" +
//                "153243,-200,4,1,1,37,0,0\n" +
//                "153301,-200,4,1,1,36,0,0\n" +
//                "153358,-200,4,1,1,35,0,0\n" +
//                "160685,-133.333333333333,4,1,1,40,0,0\n" +
//                "166280,-100,4,1,1,60,0,1\n" +
//                "166397,-100,4,1,1,60,0,0\n" +
//                "168993,-100,4,1,1,50,0,0\n" +
//                "169916,-100,4,1,1,60,0,0\n" +
//                "180070,-100,4,1,1,55,0,0\n" +
//                "180589,-100,4,1,1,50,0,0\n" +
//                "195012,-100,4,1,1,45,0,0\n" +
//                "195243,-100,4,1,1,40,0,0\n" +
//                "195474,-100,4,1,1,35,0,0\n" +
//                "196281,-100,4,1,1,40,0,0\n" +
//                "196743,-100,4,1,1,45,0,0\n" +
//                "197204,-100,4,1,1,50,0,0\n" +
//                "197666,-100,4,1,1,60,0,1\n" +
//                "209666,-100,4,1,1,40,0,1\n" +
//                "209897,-100,4,1,1,45,0,1\n" +
//                "210127,-100,4,1,1,50,0,1\n" +
//                "210358,-100,4,1,1,55,0,1\n" +
//                "210589,-100,4,1,1,60,0,1\n" +
//                "226743,-100,4,1,1,50,0,1\n" +
//                "226974,-100,4,1,1,40,0,1\n" +
//                "227204,461.538461538462,4,2,1,25,1,0\n" +
//                "251549,697.674418604651,4,2,1,20,1,0\n" +
//                "252702,714.285714285714,4,2,1,20,1,0\n" +
//                "253509,800,4,2,1,18,1,0\n" +
//                "254081,1132.07547169811,4,2,1,15,1,0\n" +
//                "255586,1200,4,2,1,10,1,0\n" +
//                "256408,1267.10000000001,4,2,1,8,1,0\n" +
//                "258906,2131.45714285714,4,2,2,6,1,0\n" +
//                "258906,-200,4,2,2,6,0,0\n" +
//                "263204,230.769230769231,4,1,1,60,1,1\n" +
//                "263261,-100,4,1,1,60,0,0\n" +
//                "270530,-100,4,1,1,40,0,0\n" +
//                "277973,-100,4,1,1,60,0,0\n" +
//                "294530,-100,4,1,1,40,0,0\n" +
//                "294819,-100,4,1,1,45,0,0\n" +
//                "295050,-100,4,1,1,50,0,0\n" +
//                "295280,-100,4,1,1,55,0,0\n" +
//                "295511,-100,4,1,1,55,0,1\n" +
//                "295627,-100,4,1,1,55,0,0\n" +
//                "302896,-100,4,1,1,50,0,0\n" +
//                "306127,-100,4,1,1,45,0,0\n" +
//                "307050,-100,4,1,1,50,0,0\n" +
//                "307511,-100,4,1,1,55,0,0\n" +
//                "308838,-100,4,1,1,35,0,0\n" +
//                "309127,-100,4,1,1,40,0,0\n" +
//                "309357,-100,4,1,1,45,0,0\n" +
//                "309588,-100,4,1,1,50,0,0\n" +
//                "309819,-100,4,1,1,55,0,0\n" +
//                "310280,-100,4,1,1,60,0,1\n" +
//                "322280,-100,4,1,1,40,0,1\n" +
//                "322511,-100,4,1,1,45,0,1\n" +
//                "322684,-100,4,1,1,50,0,1\n" +
//                "322973,-100,4,1,1,55,0,1\n" +
//                "323204,-100,4,1,1,60,0,1\n" +
//                "337050,-100,4,1,1,40,0,1\n" +
//                "337280,-100,4,1,1,45,0,1\n" +
//                "337511,-100,4,1,1,50,0,1\n" +
//                "337742,-100,4,1,1,55,0,1\n" +
//                "337973,-100,4,1,1,60,0,1\n" +
//                "356434,-100,4,1,1,60,0,0\n" +
//                "370338,-100,4,1,1,50,0,0\n" +
//                "371204,-100,4,1,1,60,0,0\n" +
//                "\n" +
//                "\n" +
//                "[Colours]\n" +
//                "Combo1 : 244,151,11\n" +
//                "Combo2 : 11,244,11\n" +
//                "Combo3 : 11,244,244\n" +
//                "Combo4 : 244,11,11\n" +
//                "\n" +
//                "[HitObjects]\n" +
//                "256,192,6590,12,0,7974,0:0:0:0:\n" +
//                "256,192,8436,5,4,0:1:0:0:\n" +
//                "256,192,8551,12,0,9820,0:0:0:0:\n" +
//                "256,192,10282,5,4,0:1:0:0:\n" +
//                "256,192,10397,12,0,13513,0:0:0:0:\n" +
//                "256,192,13974,5,4,0:1:0:0:\n" +
//                "256,192,14090,12,0,15359,0:0:0:0:\n" +
//                "256,192,15820,5,4,0:1:0:0:\n" +
//                "256,192,15936,12,0,17205,0:0:0:0:\n" +
//                "256,192,17666,5,4,0:1:0:0:\n" +
//                "215,217,384127,5,12,0:0:0:0:\n";
//        System.out.println(test.substring(test.indexOf("[HitObjects]")).matches("\\[HitObjects]\\n256,192,\\d*,12,0,\\d*,0:0:0:0:(.|\\r|\\n)*"));
//        System.out.println(new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson("{\"beatmapset_id\":\"796766\",\"beatmap_id\":\"1673919\",\"approved\":\"-2\",\"total_length\":\"128\",\"hit_length\":\"124\",\"version\":\"Dored's Hard\",\"file_md5\":\"8af6f7b0c47603fd3121b1e3f3d8a385\",\"diff_size\":\"3.5\",\"diff_overall\":\"6\",\"diff_approach\":\"7.5\",\"diff_drain\":\"5\",\"mode\":\"0\",\"approved_date\":null,\"last_update\":\"2018-09-13 04:48:46\",\"artist\":\"himmel\",\"title\":\"Empyrean\",\"creator\":\"Imouto koko\",\"creator_id\":\"7679162\",\"bpm\":\"175\",\"source\":\"Zyon \\u8f09\\u97f3\",\"tags\":\"heavenly trinity dynamix 84461810 papapa213 dored ametrin fushimi rio firika hanazawa kana vert suzuki_1112\",\"genre_id\":\"1\",\"language_id\":\"1\",\"favourite_count\":\"3\",\"playcount\":\"0\",\"passcount\":\"0\",\"max_combo\":\"683\",\"difficultyrating\":\"3.491027355194092\"}", Beatmap.class));
//        System.out.println(new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setDateFormat("yyyy-MM-dd HH:mm:ss").create().fromJson("{\"beatmapset_id\":\"13223\",\"beatmap_id\":\"53554\",\"approved\":\"2\",\"total_length\":\"428\",\"hit_length\":\"340\",\"version\":\"Extra Stage\",\"file_md5\":\"2f6b9a08fb595128073a7a8935572a6c\",\"diff_size\":\"4\",\"diff_overall\":\"9\",\"diff_approach\":\"9\",\"diff_drain\":\"8\",\"mode\":\"0\",\"approved_date\":\"2010-05-13 19:26:19\",\"last_update\":\"2010-05-13 19:14:44\",\"artist\":\"Demetori\",\"title\":\"Emotional Skyscraper ~ World's End\",\"creator\":\"happy30\",\"creator_id\":\"27767\",\"bpm\":\"178\",\"source\":\"Touhou\",\"tags\":\"hijiri byakuren touhou 12 cosmic mind nada upasana pundarika mekadon95 ignorethis 2012\",\"genre_id\":\"2\",\"language_id\":\"5\",\"favourite_count\":\"1355\",\"playcount\":\"3035630\",\"passcount\":\"251323\",\"max_combo\":\"2012\",\"difficultyrating\":\"4.99941873550415\"}", Beatmap.class));
//
//


//        UpdateOsuClientTasker tasker = new UpdateOsuClientTasker();
//        tasker.setWebPageManager(new WebPageManager());
//        tasker.updateOsuClient();
//        System.out.println(StringSimilarityUtil.calc("Delis' Insane","insane"));
//        System.out.println(StringSimilarityUtil.calc("Insane","insane"));
//        DefaultHttpClient client = new DefaultHttpClient();
//        HttpPost post = new HttpPost("https://osu.ppy.sh/forum/ucp.php?mode=login");
//        //添加请求头
//        java.util.List<NameValuePair> urlParameters = new ArrayList<>();
//        urlParameters.add(new BasicNameValuePair("autologin", "on"));
//        urlParameters.add(new BasicNameValuePair("login", "login"));
//        urlParameters.add(new BasicNameValuePair("username", Overall.CABBAGE_CONFIG.getString("accountForDL")));
//        urlParameters.add(new BasicNameValuePair("password", Overall.CABBAGE_CONFIG.getString("accountForDLPwd")));
//        try {
//            post.setEntity(new UrlEncodedFormEntity(urlParameters));
//            client.execute(post);
//        } catch (Exception ignored) { }
//        List<Cookie> cookies = client.getCookieStore().getCookies();
//        String cookie = "";
//        for (Cookie c : cookies) {
//            cookie = cookie.concat(c.getName()).concat("\n");
//        }
//        System.out.println(cookie);
//        System.out.println("——————————————————");
//        OkHttpClient CLIENT = new OkHttpClient().newBuilder()
//                .followRedirects(false)
//                .followSslRedirects(false)
//                .build();
//        RequestBody formBody = new FormBody.Builder()
//                .add("autologin", "on")
//                .add("login", "login")
//                .add("username", Overall.CABBAGE_CONFIG.getString("accountForDL"))
//                .add("password", Overall.CABBAGE_CONFIG.getString("accountForDLPwd"))
//                .build();
//        Request request = new Request.Builder()
//                .url("https://osu.ppy.sh/forum/ucp.php?mode=login")
//                .post(formBody)
//                .build();
//        StringBuilder cookie2 = new StringBuilder();
//        try (Response response = CLIENT.newCall(request).execute()) {
//            List<okhttp3.Cookie> cookies2 = okhttp3.Cookie.parseAll(request.url(), response.headers());
//            for (okhttp3.Cookie c : cookies2) {
//                cookie2.append(c.name()+"\n");
//            }
//        } catch (Exception ignored) { }
//        System.out.println(cookie2.toString());


//        String a = "123asd";
//        Instant s = Instant.now();
//        pattern ALL_NUMBER_SEARCH_KEYWORD = pattern.compile("^(\\d{1,7})$");
//        for (int i = 0; i < 100000; i++) {
//            try {
//                Integer in = Integer.valueOf(a);
//            } catch (Exception ignore) {
////239
//            }
////            ALL_NUMBER_SEARCH_KEYWORD.matcher(a).find();
////48
//        }
//        System.out.println(Duration.between(s, Instant.now()).toMillis());
//        User a = new User();
//        a.setBanned(false);
//        swap(a);
//        System.out.println(a.isBanned());

//        org.jsoup.nodes.Document doc = Jsoup.connect("https://syrin.me/pp+/api/user/2545898").timeout(10000).get();
//        System.out.println(doc);
    }

//    void swap(User a) {
//        a.setBanned(true);
//    }
}
