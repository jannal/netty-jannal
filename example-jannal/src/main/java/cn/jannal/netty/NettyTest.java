package cn.jannal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.SystemPropertyUtil;
import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

/**
 * @author jannal
 **/
public class NettyTest {

    @Test
    public void testThreadNum() {
        int DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
        //16
        System.out.println(DEFAULT_EVENT_LOOP_THREADS);
    }

    @Test
    public void testArray() {
        System.out.println(1024 << 1);
    }


    @Test
    public void testNioEventLoopGroup() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        LockSupport.park();
    }

    @Test
    public void testPooledByteBufAllocator() {
        ByteBufAllocator alloc = PooledByteBufAllocator.DEFAULT;
        //tiny规格内存分配 会变成大于等于16的整数倍的数：这里254 会规格化为256
        ByteBuf byteBuf = alloc.directBuffer(254);
        //读写bytebuf
        byteBuf.writeInt(126);
        System.out.println(byteBuf.readInt());
        //内存释放
        byteBuf.release();
    }

    @Test
    public void testFastThreadLocal() {
        FastThreadLocal<String> fastThreadLocal = new FastThreadLocal<>();
        fastThreadLocal.set("hello");
        //输出hello
        System.out.println(fastThreadLocal.get());
        fastThreadLocal.remove();
        //输出null
        System.out.println(fastThreadLocal.get());
    }


}
