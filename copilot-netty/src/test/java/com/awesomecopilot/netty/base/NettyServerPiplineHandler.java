package com.awesomecopilot.netty.base;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 自定义Handler需要继承netty规定好的某个HandlerAdapter(规范)
 *
 * <p>
 * Copyright: (C), 2020-09-02 10:32
 * <p>
 * <p>
 * Company: Sexy Uncle Inc.
 *
 * @author Rico Yu ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public class NettyServerPiplineHandler extends ChannelInboundHandlerAdapter {
	
	/**
	 * 读取客户端发送的数据
	 * @param ctx 上下文对象, 含有通道channel, 管道pipeline
	 * @param msg 就是客户端发送的数据
	 * @throws Exception
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		log.info("服务器读取线程 {}", Thread.currentThread().getName());
		Channel channel = ctx.channel();
		//本质是一个双向链接, 出站入站
		ChannelPipeline pipeline = ctx.pipeline();
		//将 msg 转成一个 ByteBuf，类似NIO 的 ByteBuffer
		ByteBuf buf = (ByteBuf)msg;
		log.info("客户端发送消息是:{}", buf.toString(CharsetUtil.UTF_8));
	}
	
	/**
	 * 数据读取完毕处理方法
	 * @param ctx
	 * @throws Exception
	 */
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ByteBuf buf = Unpooled.copiedBuffer("HelloClient".getBytes(UTF_8));
		ctx.writeAndFlush(buf);
	}
	
	/**
	 * 处理异常, 一般是需要关闭通道
	 * @param ctx
	 * @param cause
	 * @throws Exception
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.close();
	}
}
