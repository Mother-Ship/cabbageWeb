package top.mothership.cabbage.exception;

import java.io.IOException;

public class WebPageAccessException extends IOException {
    public WebPageAccessException(String msg) {
        super(msg);
    }
}
