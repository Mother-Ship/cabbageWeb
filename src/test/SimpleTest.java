import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

public class SimpleTest {
    @Test
    public void Test() {
        String a = "123asd";
        Instant s = Instant.now();
        Pattern ALL_NUMBER_SEARCH_KEYWORD = Pattern.compile("^(\\d{1,7})$");
        for (int i = 0; i < 100000; i++) {
            try {
                Integer in = Integer.valueOf(a);
            } catch (Exception ignore) {
//239
            }
//            ALL_NUMBER_SEARCH_KEYWORD.matcher(a).find();
//48
        }
        System.out.println(Duration.between(s, Instant.now()).toMillis());
    }
}
