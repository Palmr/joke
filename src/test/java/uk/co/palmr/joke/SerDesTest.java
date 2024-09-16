/*
 * Copyright (c) 1998-2017 Kx Systems Inc.
 * Modifications copyright (C) 2024 Nick Palmer
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
package uk.co.palmr.joke;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.palmr.joke.messages.KdbMessageHeader;
import uk.co.palmr.joke.types.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.*;
import static uk.co.palmr.joke.KdbProtocol.DAYS_BETWEEN_1970_2000;


public class SerDesTest {
    private KdbProtocol kdbProtocol;
    private ByteBuffer buffer;

    @BeforeEach
    void setUp() {
        kdbProtocol = new KdbProtocol("ISO-8859-1", false);
        buffer = ByteBuffer.allocate(128);
    }

    private void assertSerDesAtom(final Object data) {
        try {
            kdbProtocol.serialise(data, buffer);

            buffer.position(0);

            assertEquals(data, kdbProtocol.deserialiseResponseMessage(buffer));
        } catch (UnsupportedEncodingException | KdbException e) {
            fail(e);
        }
    }

    private void assertSerDesArray(final Object[] data) {
        try {
            kdbProtocol.serialise(data, buffer);

            buffer.position(0);

            assertArrayEquals(data, (Object[]) kdbProtocol.deserialiseResponseMessage(buffer));
        } catch (UnsupportedEncodingException | KdbException e) {
            fail(e);
        }
    }

    @Test
    public void testSerializeDeserializeBool() {
        assertSerDesAtom(Boolean.TRUE);
    }

    @Test
    public void testSerializeDeserializeUUID() {
        assertSerDesAtom(UUID.randomUUID());
    }

    @Test
    public void testSerializeDeserializeUUIDFailsWithIpcVersion2() {
        kdbProtocol.setVersion(2);
        assertThrows(RuntimeException.class, () -> assertSerDesAtom(new UUID(0, 0)));
    }

    @Test
    public void testSerializeDeserializeByte() {
        assertSerDesAtom(Byte.MAX_VALUE);
    }

    @Test
    public void testSerializeDeserializeShort() {
        assertSerDesAtom(Short.MAX_VALUE);
    }

    @Test
    public void testSerializeDeserializeInteger() {
        assertSerDesAtom(Integer.MAX_VALUE);
    }

    @Test
    public void testSerializeDeserializeLong() {
        assertSerDesAtom(Long.MAX_VALUE);
    }

    @Test
    public void testSerializeDeserializeFloat() {
        assertSerDesAtom(Float.MAX_VALUE);
    }

    @Test
    public void testSerializeDeserializeDouble() {
        assertSerDesAtom(Double.MAX_VALUE);
    }

    @Test
    public void testSerializeDeserializeCharacter() {
        assertSerDesAtom('a');
    }

    @Test
    public void testSerializeDeserializeString() {
        assertSerDesAtom("Hello world!");
    }

    @Test
    public void testSerializeDeserializeLocalDateMax() {
        assertSerDesAtom(LocalDate.ofEpochDay(Integer.MAX_VALUE));
    }

    @Test
    public void testSerializeDeserializeLocalDateMin() {
        assertSerDesAtom(LocalDate.MIN);
    }

    @Test
    public void testSerializeDeserializeLocalDateOutOfRangeHigh() {
        assertThrows(RuntimeException.class, () -> assertSerDesAtom(LocalDate.MAX));
    }

    @Test
    public void testSerializeDeserializeLocalDateOutOfRangeLow() {
        assertThrows(RuntimeException.class, () -> assertSerDesAtom(LocalDate.ofEpochDay(Integer.MIN_VALUE - 1L - DAYS_BETWEEN_1970_2000)));
    }

    @Test
    public void testSerializeDeserializeLocalDateTime() {
        assertSerDesAtom(LocalDate.parse("2024-01-01").atStartOfDay());
    }

    @Test
    public void testSerializeDeserializeTime() {
        assertSerDesAtom(LocalTime.of(12, 10, 1, 1000000 * 5));
    }

    @Test
    public void testSerializeDeserializeInstant() {
        assertSerDesAtom(Instant.ofEpochMilli(55));
    }

    @Test
    public void testSerializeDeserializeInstantHigh() {
        assertSerDesAtom(Instant.ofEpochMilli(86400000L * 10957L + 10));
    }

    @Test
    public void testSerializeDeserializeInstantMin() {
        assertSerDesAtom(Instant.MIN);
    }

    @Test
    public void testSerializeDeserializeInstantIpcVersion0() {
        kdbProtocol.setVersion(0);
        assertThrows(RuntimeException.class, () -> assertSerDesAtom(Instant.MIN));
    }

    @Test
    public void testSerializeDeserializeTimespan() {
        assertSerDesAtom(new Timespan(java.util.TimeZone.getDefault()));
    }

    @Test
    public void testSerializeDeserializeTimespanIpcVersion0() {
        kdbProtocol.setVersion(0);
        assertThrows(RuntimeException.class, () -> assertSerDesAtom(new Timespan(java.util.TimeZone.getDefault())));
    }

    @Test
    public void testSerializeDeserializeMonth() {
        assertSerDesAtom(new Month(6));
    }

    @Test
    public void testSerializeDeserializeMinute() {
        assertSerDesAtom(new Minute(55));
    }

    @Test
    public void testSerializeDeserializeSecond() {
        assertSerDesAtom(new Second(12));
    }

    @Test
    public void testSerializeDeserializeObjectArray() {
        final Object[] input = new Object[2];
        input[0] = 77L;
        input[1] = 22;

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeBoolArray() {
        final Boolean[] input = new Boolean[2];
        input[0] = true;
        input[1] = false;

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeUUIDArray() {
        final UUID[] input = new UUID[2];
        input[0] = UUID.randomUUID();
        input[1] = UUID.randomUUID();

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeByteArray() {
        final Byte[] input = new Byte[2];
        input[0] = Byte.MIN_VALUE;
        input[1] = Byte.MAX_VALUE;

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeShortArray() {
        final Short[] input = new Short[2];
        input[0] = Short.MIN_VALUE;
        input[1] = Short.MAX_VALUE;

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeIntArray() {
        final Integer[] input = new Integer[2];
        input[0] = Integer.MIN_VALUE;
        input[1] = Integer.MAX_VALUE;

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeLongArray() {
        final Long[] input = new Long[2];
        input[0] = Long.MIN_VALUE;
        input[1] = Long.MAX_VALUE;

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeFloatArray() {
        final Float[] input = new Float[2];
        input[0] = Float.MIN_VALUE;
        input[1] = Float.MAX_VALUE;

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeDoubleArray() {
        final Double[] input = new Double[2];
        input[0] = Double.MIN_VALUE;
        input[1] = Double.MAX_VALUE;

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeCharArray() {
        final Character[] input = new Character[2];
        input[0] = 'a';
        input[1] = 'b';

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeStringArray() {
        final String[] input = new String[2];
        input[0] = "hello";
        input[1] = "world";

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeDateArray() {
        final LocalDate[] input = new LocalDate[2];
        input[0] = LocalDate.of(2024, 1, 1);
        input[1] = LocalDate.of(2024, 1, 2);

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeTimeArray() {
        final LocalTime[] input = new LocalTime[2];
        input[0] = LocalDateTime.ofInstant(Instant.ofEpochMilli(1), UTC).toLocalTime();
        input[1] = LocalDateTime.ofInstant(Instant.ofEpochMilli(2), UTC).toLocalTime();

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeInstantArray() {
        final Instant[] input = new Instant[2];
        input[0] = Instant.ofEpochMilli(1);
        input[1] = Instant.ofEpochMilli(2);

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeUtilDateArray() {
        final LocalDateTime[] input = new LocalDateTime[2];
        input[0] = LocalDate.parse("2024-01-01").atStartOfDay();
        input[1] = LocalDate.parse("2024-01-02").atStartOfDay();

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeTimespanArray() {
        final Timespan[] input = new Timespan[2];
        input[0] = new Timespan(1);
        input[1] = new Timespan(2);

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeMonthArray() {
        final Month[] input = new Month[2];
        input[0] = new Month(1);
        input[1] = new Month(2);

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeMinuteArray() {
        final Minute[] input = new Minute[2];
        input[0] = new Minute(1);
        input[1] = new Minute(2);

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeSecondArray() {
        final Second[] input = new Second[2];
        input[0] = new Second(1);
        input[1] = new Second(2);

        assertSerDesArray(input);
    }

    @Test
    public void testSerializeDeserializeDict() {
        final String[] x = new String[]{"Key1", "Key2"};
        final String[] y = new String[]{"Value1", "Value2"};
        final Dict input = new Dict(x, y);

        try {
            kdbProtocol.serialise(input, buffer);

            buffer.position(0);

            final Dict actual = (Dict) kdbProtocol.deserialiseResponseMessage(buffer);
            assertArrayEquals((String[]) input.x, (String[]) actual.x);
            assertArrayEquals((String[]) input.y, (String[]) actual.y);
        } catch (UnsupportedEncodingException | KdbException e) {
            fail(e);
        }
    }

    @Test
    public void testSerializeDeserializeFlip() {
        final String[] x = new String[]{"Key1"};
        final String[][] y = new String[][]{{"Value1", "Value2"}};
        final Flip input = new Flip(new Dict(x, y));

        try {
            kdbProtocol.serialise(input, buffer);

            buffer.position(0);

            final Flip actual = (Flip) kdbProtocol.deserialiseResponseMessage(buffer);
            assertArrayEquals(input.columns, actual.columns);
            assertArrayEquals(input.columnNames, actual.columnNames);
        } catch (UnsupportedEncodingException | KdbException e) {
            fail(e);
        }
    }

    @Test
    public void testDeserializeLittleEndInteger() throws KdbException, UnsupportedEncodingException {
        byte[] buff = {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0d, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xfa, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00};

        final var msgBuffer = ByteBuffer.wrap(buff);
        final var kdbMessageHeader = new KdbMessageHeader(msgBuffer);

        assertEquals(ByteOrder.LITTLE_ENDIAN, kdbMessageHeader.getByteOrder());
        assertEquals(1, kdbProtocol.deserialize(kdbMessageHeader, msgBuffer));
    }

    @Test
    public void testDeserializeLittleEndLong() throws KdbException, UnsupportedEncodingException {
        byte[] buff = {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x11, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xf9, (byte) 0x16, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

        final var msgBuffer = ByteBuffer.wrap(buff);
        final var kdbMessageHeader = new KdbMessageHeader(msgBuffer);

        assertEquals(ByteOrder.LITTLE_ENDIAN, kdbMessageHeader.getByteOrder());
        assertEquals(22L, kdbProtocol.deserialize(kdbMessageHeader, msgBuffer));
    }

    @Test
    public void testDeserializeEmptyTable() throws KdbException, UnsupportedEncodingException {
        // response from executing '([] name:(); iq:())'
        byte[] buff = {(byte) 0x01, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x2b, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x62, (byte) 0x00, (byte) 0x63, (byte) 0x0b, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x6e, (byte) 0x61, (byte) 0x6d, (byte) 0x65, (byte) 0x00, (byte) 0x69, (byte) 0x71, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        final var msgBuffer = ByteBuffer.wrap(buff);
        final var kdbMessageHeader = new KdbMessageHeader(msgBuffer);

        final var result = (Flip) kdbProtocol.deserialize(kdbMessageHeader, msgBuffer);

        final String[] x = new String[]{"name", "iq"};
        final String[][] y = new String[][]{{}, {}};
        final Dict dict = new Dict(x, y);
        final Flip flip = new Flip(dict);

        assertArrayEquals(result.columns, flip.columns);
        assertArrayEquals(result.columnNames, flip.columnNames);
    }

    @Test
    public void testStringLenZeroForNull() throws UnsupportedEncodingException {
        assertEquals(0, kdbProtocol.lengthOfEncodedString(null));
    }

    @Test
    public void testStringLen() throws UnsupportedEncodingException {
        assertEquals(12, kdbProtocol.lengthOfEncodedString("Hello world!"));
    }

    @Test
    public void testStringLenForNullTerminatedInput() throws UnsupportedEncodingException {
        assertEquals(2, kdbProtocol.lengthOfEncodedString(new String(new char[]{'H', 'i', 0x00, 'l', 'l', 'o', ' ', 'w', 'o', 'r', 'l', 'd', '!'})));
    }
}
