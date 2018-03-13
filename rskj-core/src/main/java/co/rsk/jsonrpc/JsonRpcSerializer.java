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
package co.rsk.jsonrpc;

import co.rsk.rpc.modules.RskJsonRpcRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * This exposes basic JSON-RPC serialization methods.
 */
public class JsonRpcSerializer {
    private final ObjectMapper mapper = new ObjectMapper();

    // TODO add overload with stream

    // TODO(mc) IOException is too generic
    public String serializeMessage(JsonRpcMessage message) throws IOException {
        return mapper.writeValueAsString(message);
    }

    // TODO(mc) IOException is too generic
    public RskJsonRpcRequest deserializeRequest(InputStream source) throws IOException {
        return mapper.readValue(source, RskJsonRpcRequest.class);
    }
}
