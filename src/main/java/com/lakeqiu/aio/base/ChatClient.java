package com.lakeqiu.aio.base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author lakeqiu
 */
public class ChatClient {
    private final static String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final static Integer DEFAULT_SERVER_PORT = 8090;
    private AsynchronousSocketChannel socketChannel;

    private void start() {
        try {
            socketChannel = AsynchronousSocketChannel.open();
            // 这里是用get方式
            Future<Void> future = socketChannel.connect(new InetSocketAddress(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT));
            future.get();
            System.out.println("客户端已经连接服务器");
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                // 从控制套获取信息并发送给服务器
                String msg = consoleReader.readLine();
                byte[] msgBytes = msg.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(msgBytes);
                // 注意要等到其发送完给服务器
                Future<Integer> writeFuture = socketChannel.write(buffer);
                writeFuture.get();

                // 等待服务器返回的消息并打印在屏幕上
                buffer.flip();
                Future<Integer> readFuture = socketChannel.read(buffer);
                readFuture.get();
                String returnMsg = new String(buffer.array());
                System.out.println(returnMsg);
                buffer.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    private void close() {
        if (socketChannel != null) {
            try {
                socketChannel.close();
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
