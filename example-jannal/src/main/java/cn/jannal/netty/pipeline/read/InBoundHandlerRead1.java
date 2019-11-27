package cn.jannal.netty.pipeline.read;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class InBoundHandlerRead1 extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("channelActive 1");
        ctx.channel().pipeline().fireChannelRead("Hello!");
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        System.out.println("channelRead 1");
        System.out.println(msg);
        ctx.fireChannelRead("world!");
    }
}
