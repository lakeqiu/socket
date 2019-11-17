package com.lakeqiu.base;

import java.io.*;
import java.net.Socket;

/**
 * @author lakeqiu
 */
public class Client {
    private final static int DEFAULT_SERVER_PORT = 8090;
    private final static String DEFAULT_SERVER_HOST = "127.0.0.1";
    private int serverPort;
    private String serveHost;
    private Socket socket;

    private volatile boolean flag = true;

    public Client(int serverPort, String serveHost) {
        this.serverPort = serverPort;
        this.serveHost = serveHost;
    }

    public Client() {
        this(DEFAULT_SERVER_PORT, DEFAULT_SERVER_HOST);
    }

    public void start() {
        try {
            socket = new Socket(serveHost, serverPort);
            System.out.println("客户端已经启动");
            work();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void work() throws IOException {
        BufferedReader reader;
        BufferedWriter writer;
        BufferedReader consoleReader = null;
        // 创建IO流
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();
        reader = new BufferedReader(new InputStreamReader(inputStream));
        writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        while (flag) {
            // 等待控制台输人信息
            System.out.print("客户端：");
            consoleReader = new BufferedReader(new InputStreamReader(System.in));
            // 读取信息并发送到服务器
            String msg = consoleReader.readLine();
            writer.write(msg + "\n");
            writer.flush();
            if ("shut".equals(msg)) {
                System.out.println("关闭客户端");
                break;
            }
            // 获取服务器返回的信息并打印
            String returnMsg = reader.readLine();
            System.out.println("客户端 --> 接收到" + returnMsg);


        }
        consoleReader.close();
        writer.close();
        reader.close();
    }


    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
