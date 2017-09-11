package top.mothership.cabbage.exception;

import java.io.IOException;

public class BgNotFoundException extends IOException{
    public BgNotFoundException(String msg){
        super(msg);
    }
}
