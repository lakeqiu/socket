package com.lakeqiu.aio.base;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * @author lakeqiu
 */
public class ChatServer {
    private final static String DEFAULT_HOST = "localhost";
    private final static Integer DEFAULT_PORT = 8090;
    private final static String QUIT = "quit";
    private AsynchronousServerSocketChannel serverSocketChannel;
    private Semaphore semaphore;
    private AcceptHandler acceptHandler;

    public ChatServer() {
        this.semaphore= new Semaphore(0);
        this.acceptHandler = new AcceptHandler();
    }

    private void start() throws IOException, InterruptedException {
        serverSocketChannel = AsynchronousServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(DEFAULT_PORT));
        System.out.println("服务器：服务器启动，监听端口[" + DEFAULT_PORT + "]");

        while (true) {
            // 异步开启线程等待客户端连接，因为主线程会继续执行下去，为了避免浪费资源，以及内存溢出
            // 我们这里调用CountDownLatch的await方法等待异步线程完成客户端的连接工作
            // 传入：要辅助的资源，完成后处理的类
            serverSocketChannel.accept(null, acceptHandler);
            // 使用这个当一个客户端监控，release释放许可证后
            // 主线程就可以进入等待下一个客户端的连接
            semaphore.acquire(1);
        }
    }

    /**
     *
     * <AsynchronousSocketChannel> 异步方法(即上面的accept方法)返回的结果
     * <CountDownLatch> 辅助的资源
     */
    private class AcceptHandler implements
            CompletionHandler<AsynchronousSocketChannel, Semaphore> {

        @Override
        public void completed(AsynchronousSocketChannel result, Semaphore attachment) {
            // 服务器与客户端的通道
            AsynchronousSocketChannel clientChannel = result;
            semaphore.release();

            // 如果用户通道不为空且还处于开启状态
            if (clientChannel != null && clientChannel.isOpen()){
                // 新建异步方法结果处理类
                ClientHandel clientHandel = new ClientHandel(clientChannel);

                ByteBuffer buffer = ByteBuffer.allocate(1024);
                // 辅助类
                Map<String, Object> info = new HashMap<>(2);
                // 辅助检查一下在Handler应该读还是写
                info.put("type", "read");
                info.put("buffer", buffer);
                // 由于read方法返回的是操作的字节个数，而不是buffer，所以我们将buffer塞进一个辅助类中
                // 在clientHandler要去读取客户端传过来的信息时，就可以拿到buffer
                clientChannel.read(buffer, info, clientHandel);

            }
        }

        @Override
        public void failed(Throwable exc, Semaphore attachment) {

        }
    }

    private class ClientHandel implements
            CompletionHandler<Integer, Map<String, Object>> {
        private AsynchronousSocketChannel clientChannel;
        private final static String READ = "read";
        private final static String WRITE = "write";

        public ClientHandel(AsynchronousSocketChannel socketChannel) {
            this.clientChannel = socketChannel;
        }

        @Override
        public void completed(Integer result, Map<String, Object> attachment) {
            Map<String, Object> info = attachment;
            String type = (String) info.get("type");
            // 客户端发送信息过来了，是可读事件
            if (READ.equals(type)) {
                ByteBuffer buffer = (ByteBuffer) info.get("buffer");
                // 翻转为读模式
                buffer.flip();
                info.put("type", "write");
                // 注意，每次都是异步调用
                clientChannel.write(buffer, info, this);
                buffer.clear();
            } else if (WRITE.equals(type)) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                // 辅助检查一下在Handler应该读还是写
                info.put("type", "read");
                info.put("buffer", buffer);
                // 异步调用read，在写事件完成后，就进行等待，等待客户端的下一次发送信息，然后读取
                clientChannel.read(buffer, info, this);
            }
        }

        @Override
        public void failed(Throwable exc, Map<String, Object> attachment) {

        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}
