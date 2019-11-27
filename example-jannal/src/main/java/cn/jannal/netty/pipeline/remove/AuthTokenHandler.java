package cn.jannal.netty.pipeline.remove;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class AuthTokenHandler extends SimpleChannelInboundHandler<ByteBuf> {


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (validateToken(msg)) {
            ctx.pipeline().remove(this);
        } else {
            ctx.close();
        }
    }

    boolean validateToken( ByteBuf msg) {
        return true;
    }
}
