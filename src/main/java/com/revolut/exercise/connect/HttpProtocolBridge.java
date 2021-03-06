package com.revolut.exercise.connect;


import org.apache.log4j.Logger;

import com.revolut.exercise.Context;
import com.revolut.exercise.protocol.Link;
import com.revolut.exercise.protocol.ProtocolConfiguration;
import com.revolut.exercise.protocol.ProtocolHandler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public class HttpProtocolBridge extends ChannelHandlerAdapter {

	private static final Logger LOGGER = Logger.getLogger(HttpProtocolBridge.class);
	
	private final ProtocolConfiguration protocolConfiguration;
	private final Context context;
	
	public HttpProtocolBridge(ProtocolConfiguration protocolConfiguration, Context context) {
		this.protocolConfiguration = protocolConfiguration;
		this.context = context;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		FullHttpRequest request = (FullHttpRequest) msg;
		String content = request.content().toString(CharsetUtil.UTF_8);
		LOGGER.info("Received request: " + request + (content.length() > 0 ? "\n" + content : ""));
		FullHttpResponse response = null;
		if (request.decoderResult().isSuccess()) {
			String uri = request.uri();
			HttpMethod httpMethod = request.method();
			Link link = new Link(uri, null, httpMethod);
			ProtocolHandler handler = protocolConfiguration.getHandlers().get(link);
			if (handler != null) {
				try {
					String json = handler.handle(link, content, context);
					response = createHttpResponse(HttpResponseStatus.OK, json);
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					response = createHttpResponse(HttpResponseStatus.BAD_REQUEST, e.getMessage());
				}
			} else {
				LOGGER.info("Unmapped link: " + link);
				response = createHttpResponse(HttpResponseStatus.BAD_REQUEST, "Unrecognized URI or HTTP method");
			}
		} else {
			LOGGER.info("Malformed request");
			response = createHttpResponse(HttpResponseStatus.BAD_REQUEST,
					request.decoderResult().cause().getMessage());
		}
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
		content = response.content().toString(CharsetUtil.UTF_8);
		LOGGER.info("Sending response: " + response + (content.length() > 0 ? "\n" + content : ""));
		ctx.writeAndFlush(response);
		if (!HttpHeaderUtil.isKeepAlive(request) || response.status() == HttpResponseStatus.BAD_REQUEST) {
			ctx.close();
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOGGER.error(cause.getMessage(), cause);
		ctx.close();
	}
	
	private static FullHttpResponse createHttpResponse(HttpResponseStatus status, String content) {
		return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
				content == null ? Unpooled.buffer(0) : Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
	}
}
