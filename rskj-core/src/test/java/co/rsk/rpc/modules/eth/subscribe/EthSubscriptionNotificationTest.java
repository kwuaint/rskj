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
package co.rsk.rpc.modules.eth.subscribe;

import co.rsk.jsonrpc.JsonRpcSerializer;
import org.ethereum.rpc.Web3;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EthSubscriptionNotificationTest {
    private static final String EMPTY_BLOCK_RESULT_JSON = "{\"number\":null,\"hash\":null,\"parentHash\":null,\"sha3Uncles\":null,\"logsBloom\":null,\"transactionsRoot\":null,\"stateRoot\":null,\"receiptsRoot\":null,\"miner\":null,\"difficulty\":null,\"totalDifficulty\":null,\"extraData\":null,\"size\":null,\"gasLimit\":null,\"gasUsed\":null,\"timestamp\":null,\"transactions\":null,\"uncles\":null,\"minimumGasPrice\":null}";

    private JsonRpcSerializer serializer = new JsonRpcSerializer();

    @Test
    public void basicRequest() throws IOException {
        SubscriptionId subscription = new SubscriptionId();
        EthSubscriptionNotification notification = new EthSubscriptionNotification(
                new EthSubscriptionParams(
                        subscription,
                        new Web3.BlockResult()
                )
        );

        String id = "0x" + Hex.toHexString(subscription.getId());
        String expected = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_subscription\",\"params\":{\"subscription\":\"" + id + "\",\"result\":" + EMPTY_BLOCK_RESULT_JSON + "}}";
        assertThat(serializer.serializeMessage(notification), is(expected));
    }
}