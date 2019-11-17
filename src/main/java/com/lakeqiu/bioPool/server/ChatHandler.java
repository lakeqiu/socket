package com.lakeqiu.bioPool.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * 负责真正与客户进行通信
 * @author lakeqiu
 */
public class ChatHandler implements Runnable {
    private ChatServer server;
    private Socket socket;

    public ChatHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // 将用户加入用户列表中
            server.addClient(socket);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg = null;
            while ((msg = reader.readLine()) != null) {
                System.out.println("服务器：客户端[" + socket.getPort() + "]发送了：" + msg);
                if (ChatServer.QUIT.equals(msg)) {
                    // 关闭与用户的连接
                    server.removeClient(socket);
                    // 上面的知识关闭输出流，输入流也要关闭
                    reader.close();
                    break;
                }

                server.forwardMsg(socket,msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
