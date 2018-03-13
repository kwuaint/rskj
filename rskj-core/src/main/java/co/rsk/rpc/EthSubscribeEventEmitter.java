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
package co.rsk.rpc;

import co.rsk.core.Coin;
import co.rsk.jsonrpc.*;
import co.rsk.rpc.modules.eth.subscribe.EthSubscriptionNotification;
import co.rsk.rpc.modules.eth.subscribe.EthSubscriptionParams;
import co.rsk.rpc.modules.eth.subscribe.SubscriptionId;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.ethereum.core.Block;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.facade.Ethereum;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.rpc.TypeConverter;
import org.ethereum.rpc.Web3;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This manages subscriptions and emits events to interested clients.
 * Can only be used with the WebSockets transport.
 */
public class EthSubscribeEventEmitter {
    private final Map<SubscriptionId, MessageSender> subscriptions = new ConcurrentHashMap<>();
    private final JsonRpcSerializer jsonRpcSerializer;

    public EthSubscribeEventEmitter(Ethereum ethereum, JsonRpcSerializer jsonRpcSerializer) {
        ethereum.addListener(new EthereumListenerAdapter() {
            @Override
            public void onBlock(Block block, List<TransactionReceipt> receipts) {
                emit(block);
            }
        });
        this.jsonRpcSerializer = jsonRpcSerializer;
    }

    public SubscriptionId subscribe(Channel channel) {
        SubscriptionId subscriptionId = new SubscriptionId();
        subscriptions.put(subscriptionId, new MessageSender(channel));
        return subscriptionId;
    }

    /**
     * @return whether the unsubscription succeeded.
     */
    public boolean unsubscribe(SubscriptionId subscriptionId) {
        return subscriptions.remove(subscriptionId) != null;
    }

    private void emit(Block block) {
        subscriptions.forEach((SubscriptionId id, MessageSender sender) -> {
            Web3.BlockResult br = new Web3.BlockResult();
            br.number = TypeConverter.toJsonHex(block.getNumber());
            br.hash = block.getHashJsonString();
            br.parentHash = block.getParentHashJsonString();
            br.sha3Uncles= TypeConverter.toJsonHex(block.getUnclesHash());
            br.logsBloom = TypeConverter.toJsonHex(block.getLogBloom());
            br.transactionsRoot = TypeConverter.toJsonHex(block.getTxTrieRoot());
            br.stateRoot = TypeConverter.toJsonHex(block.getStateRoot());
            br.receiptsRoot = TypeConverter.toJsonHex(block.getReceiptsRoot());
            br.miner = TypeConverter.toJsonHex(block.getCoinbase().getBytes());
            br.difficulty = TypeConverter.toJsonHex(block.getDifficulty().getBytes());
            // sadly, this needs access to the blockchain
//            br.totalDifficulty = TypeConverter.toJsonHex(this.blockchain.getBlockStore().getTotalDifficultyForHash(block.getHash().getBytes()).asBigInteger());
            br.extraData = TypeConverter.toJsonHex(block.getExtraData());
            br.size = TypeConverter.toJsonHex(block.getEncoded().length);
            br.gasLimit = TypeConverter.toJsonHex(block.getGasLimit());
            Coin mgp = block.getMinimumGasPrice();
            br.minimumGasPrice = mgp != null ? mgp.asBigInteger().toString() : "";
            br.gasUsed = TypeConverter.toJsonHex(block.getGasUsed());
            br.timestamp = TypeConverter.toJsonHex(block.getTimestamp());

            // we're not going through the JsonRpc library, so we create the response by hand.
            EthSubscriptionNotification request = new EthSubscriptionNotification(
                    new EthSubscriptionParams(id, br)
            );

            try {
                sender.sendMessage(jsonRpcSerializer.serializeMessage(request));
            } catch (IOException e) {
                // TODO(mc)
            }
        });
    }

//      // TODO call this on client disconnect
//    public void unsubscribe(Channel channel) {
//        subscriptions.values().removeIf(channel::equals);
//    }

    private static class MessageSender {
        private final Channel channel;

        private MessageSender(Channel channel) {
            this.channel = channel;
        }

        private void sendMessage(String msg) {
            channel.writeAndFlush(new TextWebSocketFrame(msg));
        }
    }
}
