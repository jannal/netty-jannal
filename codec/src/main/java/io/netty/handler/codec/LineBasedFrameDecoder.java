/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ByteProcessor;

import java.util.List;

/**
 * A decoder that splits the received {@link ByteBuf}s on line endings.
 * <p>
 * Both {@code "\n"} and {@code "\r\n"} are handled.
 * <p>
 * The byte stream is expected to be in UTF-8 character encoding or ASCII. The current implementation
 * uses direct {@code byte} to {@code char} cast and then compares that {@code char} to a few low range
 * ASCII characters like {@code '\n'} or {@code '\r'}. UTF-8 is not using low range [0..0x7F]
 * byte values for multibyte codepoint representations therefore fully supported by this implementation.
 * <p>
 * For a more general delimiter-based decoder, see {@link DelimiterBasedFrameDecoder}.
 */
public class LineBasedFrameDecoder extends ByteToMessageDecoder {

    /** Maximum length of a frame we're willing to decode.  */
    // 消息的最大长度
    private final int maxLength;
    /** Whether or not to throw an exception as soon as we exceed maxLength. */
    // 超出最大长度是否抛出异常
    private final boolean failFast;
    // 是否跳过分隔符
    private final boolean stripDelimiter;

    /** True if we're discarding input because we're already over maxLength.  */
    private boolean discarding;
    // 丢弃字节的长度
    private int discardedBytes;

    /** Last scan position. */
    // 偏移量
    private int offset;

    /**
     * Creates a new decoder.
     * @param maxLength  the maximum length of the decoded frame.
     *                   A {@link TooLongFrameException} is thrown if
     *                   the length of the frame exceeds this value.
     */
    public LineBasedFrameDecoder(final int maxLength) {
        this(maxLength, true, false);
    }

    /**
     * Creates a new decoder.
     * @param maxLength  the maximum length of the decoded frame.
     *                   A {@link TooLongFrameException} is thrown if
     *                   the length of the frame exceeds this value.
     * @param stripDelimiter  whether the decoded frame should strip out the
     *                        delimiter or not
     * @param failFast  If <tt>true</tt>, a {@link TooLongFrameException} is
     *                  thrown as soon as the decoder notices the length of the
     *                  frame will exceed <tt>maxFrameLength</tt> regardless of
     *                  whether the entire frame has been read.
     *                  If <tt>false</tt>, a {@link TooLongFrameException} is
     *                  thrown after the entire frame that exceeds
     *                  <tt>maxFrameLength</tt> has been read.
     */
    public LineBasedFrameDecoder(final int maxLength, final boolean stripDelimiter, final boolean failFast) {
        this.maxLength = maxLength;
        this.failFast = failFast;
        this.stripDelimiter = stripDelimiter;
    }

    @Override
    protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    /**
     * Create a frame out of the {@link ByteBuf} and return it.
     *
     * @param   ctx             the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param   buffer          the {@link ByteBuf} from which to read data
     * @return  frame           the {@link ByteBuf} which represent the frame or {@code null} if no frame could
     *                          be created.
     */
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        // 查找行尾，返回\n或者\r的位置，假设消息为 "abcedf\r\n"  整体length=8  那么eol=6
        final int eol = findEndOfLine(buffer);
        // 正常情况discarding=false
        if (!discarding) {
            // 存在结尾
            if (eol >= 0) {
                final ByteBuf frame;
                // 结尾位置 - 读指针位置  = 有效数据长度
                final int length = eol - buffer.readerIndex();
                // 如果当前索引下指向的是\r证明是以\r\n开头的   结尾数据占两个字节否则占用一个
                final int delimLength = buffer.getByte(eol) == '\r'? 2 : 1;
                // 如果有效数据长度大于预设长度进行报错和丢弃
                if (length > maxLength) {
                    // 重新指定读指针的位置
                    buffer.readerIndex(eol + delimLength);
                    // 进行报错
                    fail(ctx, length);
                    return null;
                }
                // 是否丢弃换行符
                if (stripDelimiter) {
                    // 如果设置为丢弃 则从当前都指针位置切片 并增加引用计数
                    // 相当于调用了readSlice().retain()
                    frame = buffer.readRetainedSlice(length);
                    // 跳过换行符  \n跳过一个  \r\n跳过两个
                    buffer.skipBytes(delimLength);
                } else {
                    // 从当前的读指针位置读取  有效数据长度+换行数据长度的位置
                    frame = buffer.readRetainedSlice(length + delimLength);
                }
                // 返回这个切片
                return frame;
            } else {
                // 当前有效数据
                final int length = buffer.readableBytes();
                if (length > maxLength) {
                    discardedBytes = length;
                    buffer.readerIndex(buffer.writerIndex());
                    discarding = true;
                    offset = 0;
                    if (failFast) {
                        fail(ctx, "over " + discardedBytes);
                    }
                }
                // 不做处理
                return null;
            }
        } else {
            if (eol >= 0) {
                // discardedBytes是之前记录需要丢弃的字节数   [eol - buffer.readerIndex()]是分隔符前面的字节数
                final int length = discardedBytes + eol - buffer.readerIndex();
                // 分隔符长度
                final int delimLength = buffer.getByte(eol) == '\r'? 2 : 1;
                // 丢弃分隔符之前的字节（包括分隔符)
                buffer.readerIndex(eol + delimLength);
                // 重置标记变量
                discardedBytes = 0;
                discarding = false;
                // 抛异常
                if (!failFast) {
                    fail(ctx, length);
                }
            } else {
                // 没找到分隔符
                // 增加discardedBytes的值 然后继续丢弃字节
                discardedBytes += buffer.readableBytes();
                buffer.readerIndex(buffer.writerIndex());
                // We skip everything in the buffer, we need to set the offset to 0 again.
                offset = 0;
            }
            return null;
        }
    }

    private void fail(final ChannelHandlerContext ctx, int length) {
        fail(ctx, String.valueOf(length));
    }

    private void fail(final ChannelHandlerContext ctx, String length) {
        ctx.fireExceptionCaught(
                new TooLongFrameException(
                        "frame length (" + length + ") exceeds the allowed maximum (" + maxLength + ')'));
    }

    /**
     * Returns the index in the buffer of the end of line found.
     * Returns -1 if no end of line was found in the buffer.
     */
    private int findEndOfLine(final ByteBuf buffer) {
        // 获取可读字节的长度
        int totalLength = buffer.readableBytes();
        // 从当前读指针+偏移量的位置查询到最大可读数-偏移量的位置  查询\n
        int i = buffer.forEachByte(buffer.readerIndex() + offset, totalLength - offset, ByteProcessor.FIND_LF);
        // 如果存在\n
        if (i >= 0) {
            // 初始化偏移量
            offset = 0;
            // 如果换行符的前一位是\r 就返回\r的位置
            if (i > 0 && buffer.getByte(i - 1) == '\r') {
                i--;
            }
        } else {
            // 没查询到就记录一个当前的偏移量将偏移量移动到数据末尾等待下一次数据过来再读
            offset = totalLength;
        }
        return i;
    }
}
