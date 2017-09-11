package top.mothership.cabbage.exception;

import java.io.IOException;

public class ApiAccessExceptiion extends IOException{
    public ApiAccessExceptiion(String msg) {
        super(msg);
    }
}
