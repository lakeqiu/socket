package com.lakeqiu.base;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lakeqiu
 */
public class Server {
    /**
     * 默认端口
     * 服务器socket
     */
    private final static int DEFAULT_PORT = 8090;
    private int port;
    private ServerSocket serverSocket;
    private volatile boolean flag = true;

    public Server(int port) {
       this.port = port;
    }

    public Server() {
        this(DEFAULT_PORT);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(8090);
            System.out.println("服务器 --> 启动服务器，开始监听端口[" + port + "]");
            while (true) {
                work();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null == serverSocket) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void work() throws IOException {
        // 等待客户端来连接，阻塞式调用
        Socket accept = serverSocket.accept();
        System.out.println("服务器 --> 客户端[" + accept.getPort() + "]已经连接");

        // 创建流接收客户端传过来的信息
        BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
        // 创建流传输消息给客户端
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(accept.getOutputStream()));
        String msg = null;
        while ((msg = reader.readLine()) != null) {

            // 如果客户端那么出了什么问题，导致读取时连接断了，那么msg就会为空

            System.out.println("服务器 --> 接收到客户端[" + accept.getPort() + "]信息：" + msg);
            // 给客户端回信息
            writer.write("服务器接收成功\n");
            // buffer有缓冲区，将其flush一下，保证数据都发送出去
            writer.flush();

            // 如果客户端发送关闭连接，那就关闭
            if ("shut".equals(msg)) {
                System.out.println("服务器 --> 客户端断开连接");
                break;
            }
        }

        writer.close();
        reader.close();

    }

    public void shutdown() {
        this.flag = false;
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
