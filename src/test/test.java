import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class test {
    @Test
    public void Test() {
//        System.out.println("mp4".compareTo("mp5"));
//        System.out.println("mp3".compareTo("mp4"));
//        System.out.println("dev".compareTo("mp4"));
//        System.out.println("creep".compareTo("mp4chart"));
//        System.out.println("mp4chart".compareTo("mp5chart"));
        List<String> roles = Arrays.asList("mp5,mp4,mp4chart,mp5chart,dev,creep,mp3,mp5mc".split(","));
        Collections.reverse(roles);
        System.out.println(roles);
        roles = Arrays.asList("mp5,mp4,mp4chart,mp5chart,dev,creep,mp3,mp5mc".split(","));
        Collections.sort(roles);
        System.out.println(roles);
        roles = Arrays.asList("mp5,mp4,mp4chart,mp5chart,dev,creep,mp3,mp5mc".split(","));
        Collections.sort(roles);
        Collections.reverse(roles);
        System.out.println(roles);
    }
}
