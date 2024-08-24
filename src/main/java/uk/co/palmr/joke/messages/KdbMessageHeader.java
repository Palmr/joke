/*
 * Copyright (c) 2024 Nick Palmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.co.palmr.joke.messages;

import uk.co.palmr.joke.types.MessageType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class KdbMessageHeader {
    public static final int SIZE = 8;
    public static final byte TRUE = (byte) 0x01;
    public static final byte FALSE = (byte) 0x00;

    private static final int OFFSET_IS_LITTLE_ENDIAN = 0;
    private static final int OFFSET_MESSAGE_TYPE = 1;
    private static final int OFFSET_IS_COMPRESSED = 2;
    private static final int OFFSET_MESSAGE_SIZE = 4;

    private final ByteBuffer buffer;

    public KdbMessageHeader(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteOrder getByteOrder() {
        return buffer.get(OFFSET_IS_LITTLE_ENDIAN) == TRUE
                ? ByteOrder.LITTLE_ENDIAN
                : ByteOrder.BIG_ENDIAN;
    }

    public KdbMessageHeader setByteOrder(final ByteOrder byteOrder) {
        buffer.order(byteOrder);
        buffer.put(OFFSET_IS_LITTLE_ENDIAN, byteOrder == ByteOrder.LITTLE_ENDIAN
                ? TRUE
                : FALSE);
        return this;
    }

    public MessageType getMessageType() {
        return switch (buffer.get(OFFSET_MESSAGE_TYPE)) {
            case 0 -> MessageType.async;
            case 1 -> MessageType.sync;
            case 2 -> MessageType.response;
            default -> throw new IllegalStateException("Unexpected value: " + buffer.get(1));
        };
    }

    public KdbMessageHeader setMessageType(final MessageType messageType) {
        buffer.put(OFFSET_MESSAGE_TYPE, messageType.getTypeCode());
        return this;
    }

    public boolean isCompressed() {
        return buffer.get(OFFSET_IS_COMPRESSED) == 1;
    }

    public KdbMessageHeader setCompressed(final boolean isCompressed) {
        buffer.put(OFFSET_IS_COMPRESSED, isCompressed
                ? TRUE
                : FALSE);
        return this;
    }

    public int getMessageSize() {
        return buffer.getInt(OFFSET_MESSAGE_SIZE);
    }

    public KdbMessageHeader setMessageSize(final int messageSize) {
        buffer.putInt(OFFSET_MESSAGE_SIZE, messageSize);
        return this;
    }
}
