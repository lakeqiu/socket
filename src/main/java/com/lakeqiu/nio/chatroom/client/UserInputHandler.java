package com.lakeqiu.nio.chatroom.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 负责监控控制台信息并发送给服务器
 * @author lakeqiu
 */
public class UserInputHandler implements Runnable {
    private ChatClient chatClient;

    public UserInputHandler(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        try {

            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            String msg = null;
            while ((msg = consoleReader.readLine()) != null) {
                chatClient.send(msg);
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}