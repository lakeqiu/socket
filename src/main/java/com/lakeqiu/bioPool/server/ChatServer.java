package com.lakeqiu.bioPool.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 负责接收请求并转发
 * @author lakeqiu
 */
public class ChatServer {
    /**
     *  1、默认监听端口
     *  2、客户端退出命令
     *  3、存储用户信息map，<标识用户端口，输出流>
     *  4、服务器socket
     */
    private final static Integer DEFAULT_PORT = 8090;
    protected final static String QUIT = "quit";
    private final Map<Integer, Writer> clientMap;
    private ServerSocket serverSocket;


    private ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(20, 50, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(100), runnable -> new Thread(runnable));


    public ChatServer() {
        this.clientMap = new HashMap<>();
    }

    /**
     * 用户连接服务器
     * 将用户添加到用户列表中
     * @param socket
     * @throws IOException
     */
    public synchronized void addClient(Socket socket) throws IOException {
        if (null != socket) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            clientMap.put(port, writer);
            System.out.println("服务器：用户[" + port + "]连接到服务器");
        }
    }

    /**
     * 用户主动断开连接，关闭这个socket
     * @param socket
     * @throws IOException
     */
    public synchronized void removeClient(Socket socket) throws IOException {
        if (null != socket) {
            int port = socket.getPort();
            if (clientMap.containsKey(port)) {
                // 关闭了write，内部的流jdk也会帮我们关闭
                clientMap.get(port).close();
            }
            // 用户已经断开连接，从用户列表中移除
            clientMap.remove(port);
            System.out.println("服务器：用户[" + port + "]断开连接");
        }
    }

    /**
     * 向其他用户转发这个用户的发言
     * @param socket 这个用户
     * @param msg 发言
     * @throws IOException
     */
    public void forwardMsg(Socket socket, String msg) throws IOException {
        /*clientMap.entrySet().stream().filter(k -> k.getKey() != socket.getPort())
                .forEach(m -> m.getValue().write(msg));*/
        for (Map.Entry<Integer, Writer> entry : clientMap.entrySet()) {
            if (!entry.getKey().equals(socket.getPort())) {
                Writer writer = entry.getValue();
                writer.write(socket.getPort() + ":" + msg + "\n");
                writer.flush();
            }
        }
    }

    private void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                System.out.println("服务器：服务器已经关闭");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("服务器：服务器已经启动，监听端口[" + DEFAULT_PORT + "]");
            while (true) {
                Socket accept = serverSocket.accept();
                // 将用户传递给Handler线程
                poolExecutor.submit(new ChatHandler(this, accept));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }



}
