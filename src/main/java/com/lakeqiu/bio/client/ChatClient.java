package com.lakeqiu.bio.client;

import java.io.*;
import java.net.Socket;
import java.nio.Buffer;

/**
 * 客户端，主要负责等待用户输入信息并发送给服务器
 * @author lakeqiu
 */
public class ChatClient {
    private final static String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final static Integer DEFAULT_SERVER_PORT = 8090;
    private final static String QUIT = "quit";

    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    private void work() throws IOException {
        // 获取向服务器的输出流
        // 获取控制台输入流
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String msg = consoleReader.readLine();
            // 发送给服务器
            writer.write(msg + "\n");
            writer.flush();
            if (QUIT.equals(msg)) {
                consoleReader.close();
                System.out.println("客户端：关闭客户端");
                break;
            }
        }
    }

    public void start() {
        try {
            this.socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
            // 获取流
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("客户端：已经连接到服务器");
            // 将接受信息的线程设置为守护线程
            Thread thread = new Thread(new UserInputHandler(reader));
            thread.setDaemon(true);
            thread.start();
            // 这是个阻塞方法，所以要最后开启
            work();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
               if (writer != null) {
                   writer.close();
               }
               if (reader != null) {
                   reader.close();
               }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}
