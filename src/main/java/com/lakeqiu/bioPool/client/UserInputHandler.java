package com.lakeqiu.bioPool.client;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * 负责接收服务器传递过来的信息并打印
 * @author lakeqiu
 */
public class UserInputHandler implements Runnable {
    private BufferedReader reader;

    public UserInputHandler(BufferedReader reader) {
        this.reader = reader;
    }

    @Override
    public void run() {
        try {

            String msg = null;
            while ((msg = reader.readLine()) != null) {
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
