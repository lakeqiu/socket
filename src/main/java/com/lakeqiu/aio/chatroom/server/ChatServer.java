package com.lakeqiu.aio.chatroom.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

/**
 * @author lakeqiu
 */
public class ChatServer {
    private final static String HOST = "localhost";
    private final static Integer DEFAULT_PORT = 8090;
    private final static String QUIT = "quit";
    private final static Integer BUFFER_LENGTH = 1024;
    private final static Integer THREAD_POOL_SIZE = 8;

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverSocketChannel;
    private Charset charset = StandardCharsets.UTF_8;
    private int port;
    private AcceptHandler acceptHandler;
    private Semaphore semaphore;
    private Queue<AsynchronousSocketChannel> queue;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
        this.semaphore = new Semaphore(0);
        this.acceptHandler = new AcceptHandler();
        this.queue = new LinkedBlockingDeque<>();
    }

    public void start() {
        try {
            init();
            work();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            close();
        }

    }

    private void close() {
        try {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() throws IOException {
        // 创建线程池，指定线程池作为group
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        channelGroup = AsynchronousChannelGroup.withThreadPool(threadPool);
        // 指定group
        serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
        serverSocketChannel.bind(new InetSocketAddress(port));
    }

    private void work() throws InterruptedException {
        while (true) {
            serverSocketChannel.accept(semaphore, acceptHandler);
            semaphore.acquire(1);
        }
    }

    private class AcceptHandler implements
            CompletionHandler<AsynchronousSocketChannel, Semaphore> {
        @Override
        public void completed(AsynchronousSocketChannel result, Semaphore attachment) {
            AsynchronousSocketChannel clientChannel = result;
            semaphore.release();

            if (clientChannel != null && clientChannel.isOpen()) {
                queue.add(clientChannel);
                ClientHandler clientHandler = new ClientHandler(clientChannel);
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_LENGTH);
                // 虽然是同一个buffer，但是意义不同，第一是让系统把用户发送的信息写到buffer中
                // 第二个是一个辅助，帮助异步调用成功后，将用户的消息发送给其他用户
                clientChannel.read(buffer, buffer, clientHandler);
            }
        }

        @Override
        public void failed(Throwable exc, Semaphore attachment) {

        }
    }

    private class ClientHandler implements
            CompletionHandler<Integer, ByteBuffer> {
        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, ByteBuffer attachment) {
            ByteBuffer buffer = attachment;
            if (buffer != null) {
                if (result <= 0){
                    // 客户端异常
                } else {
                    // 获取信息
                    buffer.flip();
                    String msg = String.valueOf(charset.decode(buffer));
                    System.out.println(msg);
                    buffer.clear();

                    // 转发给其他客户端
                    for (AsynchronousSocketChannel socketChannel : queue) {
                        if (!socketChannel.equals(clientChannel)) {
                            buffer.put(charset.encode(msg));
                            buffer.flip();
                            socketChannel.write(buffer);
                            buffer.clear();
                        }
                    }
                    clientChannel.read(buffer, buffer, this);
                }
            }
        }

        @Override
        public void failed(Throwable exc, ByteBuffer attachment) {

        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}
