/*
 * This file is part of RskJ
 * Copyright (C) 2018 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package co.rsk.rpc.netty;

import co.rsk.jsonrpc.*;
import co.rsk.rpc.EthSubscribeEventEmitter;
import co.rsk.rpc.modules.RskJsonRpcRequest;
import co.rsk.rpc.modules.RskJsonRpcRequestVisitor;
import co.rsk.rpc.modules.eth.subscribe.EthSubscribeRequest;
import co.rsk.rpc.modules.eth.subscribe.EthUnsubscribeRequest;
import co.rsk.rpc.modules.eth.subscribe.EthSubscribeTypes;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This listens to incoming eth_subscribe commands and adds listeners.
 * Note that we have to deserialize the request by hand because the JSON RPC library doesn't expose the deserialized
 * objects. Once you call {@link com.googlecode.jsonrpc4j.JsonRpcBasicServer#handleRequest}, the request is out of your
 * hands and you only get an {@link java.io.OutputStream} back.
 * Eventually, we might want to replace the jsonrpc4j library to make things easier.
 */
public class EthPubSubHandler
        extends SimpleChannelInboundHandler<ByteBufHolder>
        implements RskJsonRpcRequestVisitor {
    private static final Logger LOGGER = LoggerFactory.getLogger("jsonrpc");

    private final EthSubscribeEventEmitter emitter;
    private final JsonRpcSerializer serializer;

    public EthPubSubHandler(EthSubscribeEventEmitter emitter, JsonRpcSerializer serializer) {
        this.emitter = emitter;
        this.serializer = serializer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBufHolder msg) {
        // intercept eth_(un)subscribe commands
        try {
            RskJsonRpcRequest request = serializer.deserializeRequest(
                    new ByteBufInputStream(msg.copy().content())
            );

            // TODO(mc) we should support the ModuleDescription method filters
            JsonRpcResultOrError resultOrError = request.accept(this, ctx);
            JsonRpcResponse response = new JsonRpcResponse(request.getId(), resultOrError);
            ctx.writeAndFlush(new TextWebSocketFrame(serializer.serializeMessage(response)));
            return;
        } catch (IOException e) {
            LOGGER.trace("Not a known or valid JsonRpcRequest");
        }

        // delegate to the next handler (JsonRpcWeb3ServerHandler)
        ctx.fireChannelRead(msg.retain());
    }

    @Override
    public JsonRpcResultOrError visit(EthUnsubscribeRequest request, ChannelHandlerContext ctx) {
        boolean unsubscribed = emitter.unsubscribe(request.getParams().getSubscriptionId());
        return new JsonRpcBooleanResult(unsubscribed);
    }

    @Override
    public JsonRpcResultOrError visit(EthSubscribeRequest request, ChannelHandlerContext ctx) {
        EthSubscribeTypes subscribeType = request.getParams().getSubscription();
        switch (subscribeType) {
            case NEW_HEADS:
                return emitter.subscribe(ctx.channel());
            default:
                LOGGER.error("Subscription type {} is not implemented", subscribeType);
                return new JsonRpcInternalError();
        }
    }
}
