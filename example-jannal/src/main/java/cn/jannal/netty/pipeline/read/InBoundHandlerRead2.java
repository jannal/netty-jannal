package cn.jannal.netty.pipeline.read;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class InBoundHandlerRead2 extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        System.out.println("channelRead 2");
        System.out.println(msg);
        ctx.fireChannelRead(msg);
    }
}
