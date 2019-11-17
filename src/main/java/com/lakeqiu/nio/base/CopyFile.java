package com.lakeqiu.nio.base;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author lakeqiu
 */
public class CopyFile {
    public static void main(String[] args) throws Exception {
        File source = new File("F:\\迅雷下载\\json.txt");
        File target = new File("F:\\迅雷下载\\json1.txt");
        /*NoBufferStreamCopy copy = new NoBufferStreamCopy();*/

        /*BufferStreamCopy copy = new BufferStreamCopy();*/

        /*NioBufferCopy copy = new NioBufferCopy();*/

        NioTransferCopy copy = new NioTransferCopy();
        copy.copy(source, target);
    }
}

interface CopyInter {
    /**
     * 拷贝文件
     * @param source 源文件
     * @param target 目标文件
     * @throws Exception
     */
    void copy(File source, File target) throws Exception;
}

/**
 * 阻塞IO没有缓冲区拷贝
 */
class NoBufferStreamCopy implements CopyInter {
    @Override
    public void copy(File source, File target) throws Exception {
        try (InputStream is = new FileInputStream(source);
        OutputStream os = new FileOutputStream(target)){
            int result;
            // 等于-1说明已经读完了
            while ((result = is.read()) != -1) {
                os.write(result);
            }
        }
    }
}

/**
 * BIO有缓冲区
 */
class BufferStreamCopy implements CopyInter {
    @Override
    public void copy(File source, File target) throws Exception {
        try (InputStream is = new FileInputStream(source);
             OutputStream os = new FileOutputStream(target)){
            // 构造缓冲容器
            byte[] buffer = new byte[1024];
            // 等于-1说明已经读完了
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
                // 刷新一下，确保缓冲容器里的内容全部写进去
                os.flush();
            }
        }
    }
}

class NioBufferCopy implements CopyInter {
    @Override
    public void copy(File source, File target) throws Exception {
        // 1、注意nio中是怎么获取通道的
        try (FileChannel fin = new FileInputStream(source).getChannel();
        FileChannel fout = new FileOutputStream(target).getChannel()) {
            // 2、建立缓冲buffer,默认读模式
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            // 3、这里有个优点，就是不用去弄长度了，channel有多少，读多少
            while (fin.read(buffer) != -1) {
                // 4、将buffer切换为写模式
                buffer.flip();
                // 5、因为write并不保证可以一次性读完所有数据，所以必须检查
                while (buffer.hasRemaining()) {
                    fout.write(buffer);
                }
                // 6、将buffer切换为读模式
                buffer.clear();
            }
        }
    }
}

class NioTransferCopy implements CopyInter {
    @Override
    public void copy(File source, File target) throws Exception {
        try (FileChannel fin = new FileInputStream(source).getChannel();
        FileChannel fout = new FileOutputStream(target).getChannel()) {
            long fileLength = 0L;
            // 由于transfer方法也是不能保证一个全部传输完，所以需要确认
            while (fileLength < fin.size()) {
                // 将源文件对应通道的数据传输到目的文件对应的通道（起始位置，要传输的长度，目的通道）
                // 传输到目的通道就是目的文件
                fileLength += fin.transferTo(0, fin.size(), fout);
            }
        }
    }
}
