package com.lakeqiu.nio.chatroom.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

/**
 * @author lakeqiu
 */
public class ChatServer {
    /**
     * 1、默认监听端口
     * 2、退出口令
     * 3、服务器channel
     * 4、选择器
     * 5、端口
     * 6、默认buffer长度
     * 7、负责读的buffer
     * 8、负责写的buffer
     * （分成两个buffer是为了方便操作，免去了模式切换的麻烦）
     */
    private final static int DEFAULT_PORT = 8090;
    private final static String QUIT = "quit";
    private ServerSocketChannel socketChannel;
    private Selector selector;
    private int port;
    private int bufferLength = 1024;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;


    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
        readBuffer = ByteBuffer.allocate(bufferLength);
        writeBuffer = ByteBuffer.allocate(bufferLength);
    }

    public void start() {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化服务器
     * @throws IOException
     */
    private void init() throws IOException {
        // 创建服务器通道
        socketChannel = ServerSocketChannel.open();
        // 默认是阻塞通道，将其改为非阻塞通道
        socketChannel.configureBlocking(false);
        // 绑定监听端口
        socketChannel.socket().bind(new InetSocketAddress(port));
        // 创建选择器
        selector = Selector.open();
        // 将服务器通道注册到选择器上，触发事件为ACCEPT
        socketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务器：服务器已经启动，开始监听端口[" + port + "]");
    }

    private void work() throws IOException {
        // 需要不断地查询
        while (true) {
            // select本身是阻塞的，其会等到其所监听的通道有其关心的事件触发
            // 其才会返回触发的事件个数
            selector.select();
            // select返回了，有事件触发了
            // 返回触发事件的集合
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (SelectionKey key : selectionKeys) {
                // 处理被触发的事件
                
            }
        }
    }
}
