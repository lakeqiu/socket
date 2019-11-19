package com.lakeqiu.nio.chatroom.server;

import sun.awt.AWTCharset;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
     * 9、默认统一编码
     */
    private final static int DEFAULT_PORT = 8090;
    private final static String QUIT = "quit\n";
    private ServerSocketChannel socketChannel;
    private Selector selector;
    private int port;
    private int bufferLength = 1024;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private Charset charset = StandardCharsets.UTF_8;


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
            work();
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
                if (key.isAcceptable()) {
                    acceptAffair(key);
                }else if (key.isReadable()) {
                    sendMsgAffair(key);
                }

            }
            // 记得将事件集合情况，不然下一轮会包含上一轮的事件
            selectionKeys.clear();
        }
    }

    /**
     * 客户端连接事件
     */
    private void acceptAffair(SelectionKey selectionKey) throws IOException {
        // 获取服务器socket
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        // 获取与客户端连接socket并设置为非阻塞式
        SocketChannel accept = serverSocketChannel.accept();
        accept.configureBlocking(false);
        // 注册事件
        accept.register(selector, SelectionKey.OP_READ);
        System.out.println("服务器：客户端[" + accept.socket().getPort() + "]已经连接");
    }

    /**
     * 客户端发送消息事件
     */
    private void sendMsgAffair(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        socketChannel.configureBlocking(false);
        // 读取客户端发来的消息
        String msg = readMsg(socketChannel);
        // 客户端异常
        if (msg.isEmpty()) {
            // 关闭事件与通道，并通知selector更新
            selectionKey.cancel();
            selector.wakeup();

            System.out.println("服务器：用户[" + socketChannel.socket().getPort() + "]异常断开连接");
        }else {
            // 用户断开连接
            if (QUIT.equals(msg)) {
                msg = "已经退出聊天室";
                selectionKey.cancel();
                selector.wakeup();
                System.out.println("服务器：用户[" + socketChannel.socket().getPort() + "]已经断开连接");
            }
            // 转发信息
            forwardMsg(socketChannel, msg);
        }
    }

    /**
     * 读取客户端发送过来的信息
     * @param socketChannel
     * @return
     * @throws IOException
     */
    private String readMsg(SocketChannel socketChannel) throws IOException {
        // 防止readBuffer里有信息，先清零,也是切换为读模式
        readBuffer.clear();
        // 注意与FileChannel判断读完没有的区别，FileChannel是=-1
        // 这个是只要能读出字节就继续读，直到不能读取为止
        while (socketChannel.read(readBuffer) > 0) {
            continue;
        }
        // 将buffer切换为写模式
        readBuffer.flip();
        return String.valueOf(charset.decode(readBuffer));
    }

    private void forwardMsg(SocketChannel socketChannel, String msg) throws IOException {
        // 获取所有selector监听的所有事件通道
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey key : keys) {
           Channel channel = key.channel();
           // 如果是监听客户端连接的通道，则不发送请求
           if (channel instanceof ServerSocketChannel) {
               continue;
           }
           // 如果该通道有效并且不是发送此消息的通道
           if (key.isValid() && !socketChannel.equals(channel)) {
                // 切换为读模式情况信息，往里面将信息装进去再切换为写模式
                writeBuffer.clear();
                writeBuffer.put(charset.encode(socketChannel.socket().getPort() + ":" + msg));
                writeBuffer.flip();
                // 注意要读完
                while (writeBuffer.hasRemaining()) {
                    ((SocketChannel)channel).write(writeBuffer);
                }
           }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}
