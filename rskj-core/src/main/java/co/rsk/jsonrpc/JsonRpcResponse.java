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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * This the JsonRPC response DTO for JSON serialization purposes.
 */
public class JsonRpcResponse extends JsonRpcMessage {
    // TODO String, Number, or NULL
    private final int id;
    private final JsonRpcResultOrError resultOrError;

    public JsonRpcResponse(int id, JsonRpcResultOrError resultOrError) {
        super(JsonRpcVersion.V2_0);
        this.id = requireNonNegative(id);
        this.resultOrError = Objects.requireNonNull(resultOrError);
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public int getId() {
        return id;
    }

    @JsonAnyGetter
    @SuppressWarnings("unused")
    public Map<String, JsonRpcResultOrError> getResultOrError() {
        // we dynamically calculate the name of the property for the result or error
        String propertyName = resultOrError.isError() ? "error" : "result";
        return Collections.singletonMap(propertyName, resultOrError);
    }

    private static int requireNonNegative(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("JSON-RPC response id should be a positive number");
        }

        return id;
    }
}
