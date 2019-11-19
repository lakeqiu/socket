package com.lakeqiu.nio.chatroom.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * @author lakeqiu
 */
public class ChatClient {
    private final static String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final static Integer DEFAULT_SERVER_PORT = 8090;
    private final static String QUIT = "quit";

    private String host;
    private Integer port;
    private SocketChannel socketChannel;
    private Selector selector;
    private ByteBuffer readerBuffer;
    private ByteBuffer writerBuffer;
    private Charset charset = StandardCharsets.UTF_8;

    public ChatClient() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    }

    public ChatClient(String host, Integer port) {
        this.host = host;
        this.port = port;
        this.readerBuffer = ByteBuffer.allocate(1024);
        this.writerBuffer = ByteBuffer.allocate(1024);
    }

    public void start() throws IOException {
        init();
        work();
    }

    /**
     * 初始化
     * @throws IOException
     */
    private void init() throws IOException {
        // 创建通道并设置为非阻塞
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        // 创建选择器并注册事件
        selector = Selector.open();
        // 这里是注册连接事件是因为在事件触发后，我们会在处理创建一个线程处理用户输入问题并注册一个READER事件
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        socketChannel.connect(new InetSocketAddress(host, port));
    }

    /**
     * 工作线程，在客户端连接服务器后，开启一个新的线程去负责监控用户控制台的输入，
     * 并发送给服务器。然后其自身负责监控通道是否有可读时间，如果有的话说明有其他用户
     * 发送消息，接收并打印在屏幕上
     * @throws IOException
     */
    private void work() throws IOException {
        while (true) {
            // 轮询查看是否有事件发生
            selector.select();
            Set<SelectionKey> selectionKeys = selector.keys();

            for (SelectionKey selectionKey : selectionKeys) {
                if (selectionKey.isConnectable()) {
                    System.out.println("客户端已经连接服务器");
                    // 注册一个READER事件
                    SocketChannel channel = (SocketChannel) selectionKey.channel();
                    channel.register(selector, SelectionKey.OP_READ);
                    // 创建一个线程去处理用户控制台输入问题并发送给服务器
                    new Thread(new UserInputHandler(this)).start();
                }
                // 处理可读事件
                if (selectionKey.isReadable()) {
                    readerBuffer.clear();
                    SocketChannel channel = (SocketChannel) selectionKey.channel();
                    while (channel.read(readerBuffer) > 0){
                        continue;
                    }
                    readerBuffer.flip();
                    String msg = String.valueOf(charset.decode(readerBuffer));
                    // 服务器异常
                    if (msg.isEmpty()) {
                        // 关闭连接
                        close();
                        System.out.println("服务器异常，关闭连接");
                    } else {
                        System.out.println(msg);
                    }
                }
            }
        }
    }

    protected void send(String msg) {
        // 如果信息为空，则不发送
        if (msg.isEmpty()) {
            return;
        }

        // 发送消息
        writerBuffer.clear();
        writerBuffer.put(charset.encode(msg));
        writerBuffer.flip();
        try {
            while (writerBuffer.hasRemaining()) {
                socketChannel.write(writerBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 如果是关闭口令，则关闭
        if (QUIT.equals(msg)) {
            close();
            System.out.println("关闭客户端成功");
        }
    }

    private void close() {
        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}
