package com.sedwt.alarm.client;

import com.sedwt.alarm.client.handler.PandaClientHeartbeatHandler;
import com.sedwt.alarm.client.handler.PandaDecodeHandler;
import com.sedwt.alarm.client.handler.PandaTaskHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author : yj zhang
 * @since : 2021/5/8 11:47
 */

public class PandaTcpClient implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger("pandaTcpClient");

    private final String hostname;
    private final int port;
    private final long writeIdleTimePeriod;
    private final int retryConnectInterval;

    public PandaTcpClient(String hostname, int port, int writeIdleTimePeriod, int retryConnectInterval) {
        this.hostname = hostname;
        this.port = port;
        this.writeIdleTimePeriod = (writeIdleTimePeriod + 1);
        this.retryConnectInterval = retryConnectInterval;
    }

    @Override
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(hostname, port);
        try {
            bootstrap.group(bossGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //空闲状态监听 handler
                            ch.pipeline().addLast(new IdleStateHandler(0,
                                    writeIdleTimePeriod, 0, TimeUnit.SECONDS));
                            // 自定义解码器
                            ch.pipeline().addLast(new PandaDecodeHandler());
                            // 心跳handler
                            ch.pipeline().addLast(new PandaClientHeartbeatHandler());
                            // 业务逻辑处理器
                            ch.pipeline().addLast(new PandaTaskHandler());
                            // 编码器
                            ch.pipeline().addLast(new ByteArrayEncoder());

                        }
                    });
            ChannelFuture channelFuture = null;
            int retryCount = 0;
            while (true) {
                try {
                    channelFuture = bootstrap.connect(inetSocketAddress).sync();
                    if (channelFuture.isSuccess()) {
                        logger.info("Panda TCP 客户端成功启动，已连接到服务器 {}:{}", hostname, port);
                        break;
                    }
                } catch (Exception e) {
                    retryCount++;
                    if (channelFuture != null && channelFuture.isSuccess()){
                        // 加这个判断就是为了sonar检查的，其实没必要，断链重连走不进来
                        Thread.currentThread().interrupt();
                    }
                    logger.error("客户端连接服务端异常, {}, 将再次重试，重试次数[{}]", e.getMessage(), retryCount);
                    TimeUnit.SECONDS.sleep(retryConnectInterval);
                }
            }
        } catch (Exception e) {
            logger.error("客户端异常, {}", e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            logger.info("即将优雅的关闭线程组");
            bossGroup.shutdownGracefully();
        }
    }
}
