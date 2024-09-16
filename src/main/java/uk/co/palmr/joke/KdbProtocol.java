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

import uk.co.palmr.joke.messages.KdbMessageHeader;
import uk.co.palmr.joke.types.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static uk.co.palmr.joke.types.DataType.Lambda;

public class KdbProtocol {
    public static final byte NULL_BYTE = 0x00;
    /**
     * null integer, i.e. 0Ni
     */
    public static final int NULL_INT = Integer.MIN_VALUE;
    /**
     * null long, i.e. 0N
     */
    public static final long NULL_LONG = Long.MIN_VALUE;
    /**
     * null float, i.e. 0Nf or 0n
     */
    public static final double NULL_FLOAT = Double.NaN;
    /**
     * Representation of a null for a time atom within kdb
     */
    protected static final LocalTime NULL_LOCAL_TIME = LocalTime.ofNanoOfDay(1);

    protected static final int DAYS_BETWEEN_1970_2000 = 10957;
    protected static final long MILLS_IN_DAY = 86400000L;
    protected static final long MILLS_BETWEEN_1970_2000 = MILLS_IN_DAY * DAYS_BETWEEN_1970_2000;
    protected static final long NANOS_IN_SEC = 1000000000L;

    /**
     * The character encoding to use when [de]-serializing strings.
     */
    private final String stringEncoding;

    private final boolean allowCompression;

    private int version = IpcVersion.KDB_IPC_VERSION;

    protected KdbProtocol(final String stringEncoding, final boolean allowCompression) {
        this.stringEncoding = stringEncoding;
        this.allowCompression = allowCompression;
    }

    protected void setVersion(final int version) {
        this.version = version;
    }

    /**
     * Serialize and write the data to the registered connection
     *
     * @param msgType          type of the ipc message
     * @param msg              object to serialise
     * @param kdbMessageHeader flyweight kdb message header
     * @param messageBuffer    buffer to serialise data into
     * @throws IOException should not throw
     */
    protected void serialiseMessage(final MessageType msgType,
                                    final Object msg,
                                    final KdbMessageHeader kdbMessageHeader,
                                    final ByteBuffer messageBuffer) throws IOException, KdbException {
        int length = KdbMessageHeader.SIZE + lengthOfObject(msg);

        messageBuffer.limit(length);

        kdbMessageHeader.setByteOrder(ByteOrder.BIG_ENDIAN)
                .setMessageType(msgType)
                .setMessageSize(length);

        messageBuffer.position(KdbMessageHeader.SIZE);
        serialise(msg, messageBuffer);
        if (allowCompression && messageBuffer.position() > 2000) {
            throw new UnsupportedEncodingException("Not yet implemented compression");
//            kdbMessageHeader.setIsCompressed(true);
//            compress();
        }
    }

    protected Object deserialize(final KdbMessageHeader kdbMessageHeader, final ByteBuffer messageBuffer) throws UnsupportedEncodingException, KdbException {
        messageBuffer.order(kdbMessageHeader.getByteOrder());

        messageBuffer.position(KdbMessageHeader.SIZE);
        if (kdbMessageHeader.isCompressed()) {
            throw new UnsupportedEncodingException("Not yet implemented compression");
//            uncompress();
        }
        return deserialiseResponseMessage(messageBuffer); // deserialize the message
    }

    /**
     * Write String to serialization buffer
     *
     * @param string String to serialize
     * @throws UnsupportedEncodingException If there is an issue with the registered encoding
     */
    protected void writeStringToBuffer(final String string, final ByteBuffer buffer) throws UnsupportedEncodingException {
        if (string != null && !string.isEmpty()) {
            final var stringBytes = encodeString(string);
            for (int idx = 0; idx < stringBytes.length && stringBytes[idx] != NULL_BYTE; idx++) {
                buffer.put(stringBytes[idx]);
            }
        }
        buffer.put(NULL_BYTE);
    }

    /**
     * Serialize object in big endian format
     *
     * @param obj           Object to serialize
     * @param messageBuffer buffer to serialise to
     * @throws UnsupportedEncodingException If the named charset (encoding) is not supported
     */
    protected void serialise(final Object obj, final ByteBuffer messageBuffer) throws UnsupportedEncodingException, KdbException {
        final DataType type = DataType.getKdbType(obj);
        messageBuffer.put(type.getTypeCode());
        if (type.isAtom()) {
            switch (type) {
                case Boolean:
                    serialise(((Boolean) obj).booleanValue(), messageBuffer);
                    return;
                case UUID:
                    serialise((UUID) obj, messageBuffer);
                    return;
                case Byte:
                    serialise(((Byte) obj).byteValue(), messageBuffer);
                    return;
                case Short:
                    serialise(((Short) obj).shortValue(), messageBuffer);
                    return;
                case Integer:
                    serialise(((Integer) obj).intValue(), messageBuffer);
                    return;
                case Long:
                    serialise(((Long) obj).longValue(), messageBuffer);
                    return;
                case Float:
                    serialise(((Float) obj).floatValue(), messageBuffer);
                    return;
                case Double:
                    serialise(((Double) obj).doubleValue(), messageBuffer);
                    return;
                case Character:
                    serialise(((Character) obj).charValue(), messageBuffer);
                    return;
                case String:
                    serialise((String) obj, messageBuffer);
                    return;
                case Instant:
                    serialise((Instant) obj, messageBuffer);
                    return;
                case Month:
                    serialise((Month) obj, messageBuffer);
                    return;
                case LocalDate:
                    serialise((LocalDate) obj, messageBuffer);
                    return;
                case LocalDateTime:
                    serialise((LocalDateTime) obj, messageBuffer);
                    return;
                case Timespan:
                    serialise((Timespan) obj, messageBuffer);
                    return;
                case Minute:
                    serialise((Minute) obj, messageBuffer);
                    return;
                case Second:
                    serialise((Second) obj, messageBuffer);
                    return;
                case LocalTime:
                    serialise((LocalTime) obj, messageBuffer);
                    return;
            }
        }

        if (type == DataType.Dict) {
            final Dict r = (Dict) obj;
            serialise(r.x, messageBuffer);
            serialise(r.y, messageBuffer);
            return;
        }

        messageBuffer.put(NULL_BYTE);
        if (type == DataType.Flip) {
            final Flip r = (Flip) obj;
            messageBuffer.put(DataType.Dict.getTypeCode());
            serialise(r.columnNames, messageBuffer);
            serialise(r.columns, messageBuffer);
            return;
        }

        final int numElements = elementCount(obj);

        serialise(numElements, messageBuffer);

        if (type == DataType.CharArray) {
            byte[] b = new String((char[]) obj).getBytes(stringEncoding);
            for (final byte character : b) {
                serialise(character, messageBuffer);
            }
        } else {
            for (int idx = 0; idx < numElements; idx++) {
                switch (type) {
                    case List:
                        serialise(((Object[]) obj)[idx], messageBuffer);
                        break;
                    case BooleanArray:
                        serialise(((boolean[]) obj)[idx], messageBuffer);
                        break;
                    case UUIDArray:
                        serialise(((UUID[]) obj)[idx], messageBuffer);
                        break;
                    case ByteArray:
                        serialise(((byte[]) obj)[idx], messageBuffer);
                        break;
                    case ShortArray:
                        serialise(((short[]) obj)[idx], messageBuffer);
                        break;
                    case IntArray:
                        serialise(((int[]) obj)[idx], messageBuffer);
                        break;
                    case LongArray:
                        serialise(((long[]) obj)[idx], messageBuffer);
                        break;
                    case FloatArray:
                        serialise(((float[]) obj)[idx], messageBuffer);
                        break;
                    case DoubleArray:
                        serialise(((double[]) obj)[idx], messageBuffer);
                        break;
                    case StringArray:
                        serialise(((String[]) obj)[idx], messageBuffer);
                        break;
                    case InstantArray:
                        serialise(((Instant[]) obj)[idx], messageBuffer);
                        break;
                    case MonthArray:
                        serialise(((Month[]) obj)[idx], messageBuffer);
                        break;
                    case LocalDateArray:
                        serialise(((LocalDate[]) obj)[idx], messageBuffer);
                        break;
                    case LocalDateTimeArray:
                        serialise(((LocalDateTime[]) obj)[idx], messageBuffer);
                        break;
                    case TimespanArray:
                        serialise(((Timespan[]) obj)[idx], messageBuffer);
                        break;
                    case MinuteArray:
                        serialise(((Minute[]) obj)[idx], messageBuffer);
                        break;
                    case SecondArray:
                        serialise(((Second[]) obj)[idx], messageBuffer);
                        break;
                    case LocalTimeArray:
                        serialise(((LocalTime[]) obj)[idx], messageBuffer);
                        break;
                    default:
                        throw new KdbException("Unhandled type: " + type);
                }
            }
        }
    }

    private void serialise(boolean bool, final ByteBuffer messageBuffer) {
        messageBuffer.put((byte) (bool ? 1 : 0));
    }

    private void serialise(UUID uuid, final ByteBuffer messageBuffer) {
        if (version < 3) {
            throw new RuntimeException("Guid not valid pre kdb+3.0");
        }

        serialise(uuid.getMostSignificantBits(), messageBuffer);
        serialise(uuid.getLeastSignificantBits(), messageBuffer);
    }

    private void serialise(byte b, final ByteBuffer messageBuffer) {
        messageBuffer.put(b);
    }

    private void serialise(short s, final ByteBuffer messageBuffer) {
        messageBuffer.putShort(s);
    }

    private void serialise(int i, final ByteBuffer messageBuffer) {
        messageBuffer.putInt(i);
    }

    private void serialise(long l, final ByteBuffer messageBuffer) {
        messageBuffer.putLong(l);
    }

    private void serialise(float f, final ByteBuffer messageBuffer) {
        messageBuffer.putInt(Float.floatToIntBits(f));
    }

    private void serialise(double d, final ByteBuffer messageBuffer) {
        messageBuffer.putLong(Double.doubleToLongBits(d));
    }

    private void serialise(char c, final ByteBuffer messageBuffer) {
        messageBuffer.put((byte) c);
    }

    private void serialise(String s, final ByteBuffer messageBuffer) throws UnsupportedEncodingException {
        if (s != null) {
            byte[] encodedStringChars = s.getBytes(stringEncoding);
            for (int idx = 0; idx < encodedStringChars.length && encodedStringChars[idx] != NULL_BYTE; idx++) {
                messageBuffer.put(encodedStringChars[idx]);
            }
        }
        messageBuffer.put(NULL_BYTE);
    }

    private void serialise(Instant p, final ByteBuffer messageBuffer) {
        if (version < 1) {
            throw new RuntimeException("Instant not valid pre kdb+2.6");
        }
        messageBuffer.putLong(p == Instant.MIN
                ? NULL_LONG
                : 1000000 * (p.toEpochMilli() - MILLS_BETWEEN_1970_2000) + p.getNano() % 1000000);
    }

    private void serialise(Month m, final ByteBuffer messageBuffer) {
        messageBuffer.putInt(m.i);
    }

    private void serialise(LocalDate d, final ByteBuffer messageBuffer) {
        if (d == LocalDate.MIN) {
            messageBuffer.putInt(NULL_INT);
            return;
        }
        long daysSince2000 = d.toEpochDay() - DAYS_BETWEEN_1970_2000;
        if (daysSince2000 < Integer.MIN_VALUE || daysSince2000 > Integer.MAX_VALUE) {
            throw new RuntimeException("LocalDate epoch day since 2000 must be >= Integer.MIN_VALUE and <= Integer.MAX_VALUE");
        }
        messageBuffer.putInt((int) (daysSince2000));
    }

    private void serialise(LocalDateTime z, final ByteBuffer messageBuffer) {
        serialise(z == LocalDateTime.MIN
                        ? NULL_FLOAT
                        : (z.toInstant(UTC).toEpochMilli() - MILLS_BETWEEN_1970_2000) / 8.64e7,
                messageBuffer);
    }

    private void serialise(Timespan n, final ByteBuffer messageBuffer) {
        if (version < 1) {
            throw new RuntimeException("Timespan not valid pre kdb+2.6");
        }
        messageBuffer.putLong(n.j);
    }

    private void serialise(Minute u, final ByteBuffer messageBuffer) {
        messageBuffer.putInt(u.i);
    }

    private void serialise(Second v, final ByteBuffer messageBuffer) {
        messageBuffer.putInt(v.i);
    }

    private void serialise(LocalTime t, final ByteBuffer messageBuffer) {
        messageBuffer.putInt((t == NULL_LOCAL_TIME)
                ? NULL_INT
                : (int) ((t.toSecondOfDay() * 1000 + t.getNano() / 1000000) % MILLS_IN_DAY));
    }

    /**
     * Deserialize string from byte buffer
     *
     * @param messageBuffer incoming message buffer
     * @return Deserialized string using registered encoding
     */
    private String deserializeString(final ByteBuffer messageBuffer) throws UnsupportedEncodingException {
        final var startPos = messageBuffer.position();
        while (messageBuffer.get() != NULL_BYTE) {
            Thread.onSpinWait();
        }
        final var endPos = messageBuffer.position();
        final var stringBytes = new byte[endPos - startPos - 1];
        messageBuffer.get(startPos, stringBytes);

        return (startPos == endPos - 1)
                ? ""
                : new String(stringBytes, stringEncoding);
    }

    /**
     * Deserializes the contents of the incoming message buffer
     *
     * @param messageBuffer incoming message buffer
     *                      private @return deserialised object
     */
    protected Object deserialiseResponseMessage(final ByteBuffer messageBuffer) throws UnsupportedEncodingException, KdbException {
        int i = 0;
        int n;
        DataType type = DataType.getKdbType(messageBuffer.get());
        if (type.isAtom())
            switch (type) {
                case Boolean:
                    return deserialiseBoolean(messageBuffer);
                case UUID:
                    return deserialiseUuid(messageBuffer);
                case Byte:
                    return messageBuffer.get();
                case Short:
                    return deserialiseShort(messageBuffer);
                case Integer:
                    return messageBuffer.getInt();
                case Long:
                    return deserialiseLong(messageBuffer);
                case Float:
                    return deserialiseFloat(messageBuffer);
                case Double:
                    return deserialiseDouble(messageBuffer);
                case Character:
                    return deserialiseChar(messageBuffer);
                case String:
                    return deserializeString(messageBuffer);
                case Instant:
                    return deserialiseInstant(messageBuffer);
                case Month:
                    return deserialiseMonth(messageBuffer);
                case LocalDate:
                    return deserialiseLocalDate(messageBuffer);
                case LocalDateTime:
                    return deserialiseLocalDateTime(messageBuffer);
                case Timespan:
                    return deserialiseTimespan(messageBuffer);
                case Minute:
                    return deserialiseMinute(messageBuffer);
                case Second:
                    return deserialiseSecond(messageBuffer);
                case LocalTime:
                    return deserialiseLocalTime(messageBuffer);
                case Exception:
                    throw new KdbException(deserializeString(messageBuffer));
            }
        if (type.getTypeCode() > 99) {
            if (type == Lambda) {
                deserializeString(messageBuffer);
                return deserialiseResponseMessage(messageBuffer);
            }
            if (type.getTypeCode() < 104) {
                return messageBuffer.get() == 0 && type.getTypeCode() == 101
                        ? null
                        : "func";
            }
            if (type.getTypeCode() > 105) {
                deserialiseResponseMessage(messageBuffer);
            } else {
                for (n = messageBuffer.getInt(); i < n; i++) {
                    deserialiseResponseMessage(messageBuffer);
                }
            }
            return "func";
        }
        if (type == DataType.Dict)
            return new Dict(deserialiseResponseMessage(messageBuffer), deserialiseResponseMessage(messageBuffer));

        messageBuffer.get();

        if (type == DataType.Flip) {
            return new Flip((Dict) deserialiseResponseMessage(messageBuffer));
        }
        n = messageBuffer.getInt();
        switch (type) {
            case List:
                Object[] objArr = new Object[n];
                for (; i < n; i++)
                    objArr[i] = deserialiseResponseMessage(messageBuffer);
                return objArr;
            case BooleanArray:
                boolean[] boolArr = new boolean[n];
                for (; i < n; i++)
                    boolArr[i] = deserialiseBoolean(messageBuffer);
                return boolArr;
            case UUIDArray:
                UUID[] uuidArr = new UUID[n];
                for (; i < n; i++)
                    uuidArr[i] = deserialiseUuid(messageBuffer);
                return uuidArr;
            case ByteArray:
                byte[] byteArr = new byte[n];
                for (; i < n; i++)
                    byteArr[i] = messageBuffer.get();
                return byteArr;
            case ShortArray:
                short[] shortArr = new short[n];
                for (; i < n; i++)
                    shortArr[i] = deserialiseShort(messageBuffer);
                return shortArr;
            case IntArray:
                int[] intArr = new int[n];
                for (; i < n; i++)
                    intArr[i] = messageBuffer.getInt();
                return intArr;
            case LongArray:
                long[] longArr = new long[n];
                for (; i < n; i++)
                    longArr[i] = deserialiseLong(messageBuffer);
                return longArr;
            case FloatArray:
                float[] floatArr = new float[n];
                for (; i < n; i++)
                    floatArr[i] = deserialiseFloat(messageBuffer);
                return floatArr;
            case DoubleArray:
                double[] doubleArr = new double[n];
                for (; i < n; i++)
                    doubleArr[i] = deserialiseDouble(messageBuffer);
                return doubleArr;
            case CharArray:
                char[] charArr = Charset.forName(stringEncoding).decode(messageBuffer.slice(messageBuffer.position(), n)).toString().toCharArray();
                messageBuffer.position(messageBuffer.position() + n);
                return charArr;
            case StringArray:
                String[] stringArr = new String[n];
                for (; i < n; i++)
                    stringArr[i] = deserializeString(messageBuffer);
                return stringArr;
            case InstantArray:
                Instant[] timestampArr = new Instant[n];
                for (; i < n; i++)
                    timestampArr[i] = deserialiseInstant(messageBuffer);
                return timestampArr;
            case MonthArray:
                Month[] monthArr = new Month[n];
                for (; i < n; i++)
                    monthArr[i] = deserialiseMonth(messageBuffer);
                return monthArr;
            case LocalDateArray:
                LocalDate[] dateArr = new LocalDate[n];
                for (; i < n; i++)
                    dateArr[i] = deserialiseLocalDate(messageBuffer);
                return dateArr;
            case LocalDateTimeArray:
                LocalDateTime[] dateUtilArr = new LocalDateTime[n];
                for (; i < n; i++)
                    dateUtilArr[i] = deserialiseLocalDateTime(messageBuffer);
                return dateUtilArr;
            case TimespanArray:
                Timespan[] timespanArr = new Timespan[n];
                for (; i < n; i++)
                    timespanArr[i] = deserialiseTimespan(messageBuffer);
                return timespanArr;
            case MinuteArray:
                Minute[] minArr = new Minute[n];
                for (; i < n; i++)
                    minArr[i] = deserialiseMinute(messageBuffer);
                return minArr;
            case SecondArray:
                Second[] secArr = new Second[n];
                for (; i < n; i++)
                    secArr[i] = deserialiseSecond(messageBuffer);
                return secArr;
            case LocalTimeArray:
                LocalTime[] timeArr = new LocalTime[n];
                for (; i < n; i++)
                    timeArr[i] = deserialiseLocalTime(messageBuffer);
                return timeArr;
            default:
                // do nothing, let it return null
        }
        return null;
    }

    /**
     * Deserialize char from byte buffer
     *
     * @return Deserialized char
     */
    private char deserialiseChar(final ByteBuffer messageBuffer) {
        return (char) (messageBuffer.get() & 0xff);
    }

    /**
     * Deserialize boolean from byte buffer
     *
     * @return Deserialized boolean
     */
    private boolean deserialiseBoolean(final ByteBuffer messageBuffer) {
        return 1 == messageBuffer.get();
    }

    /**
     * Deserialize short from byte buffer
     *
     * @return Deserialized short
     */
    private short deserialiseShort(final ByteBuffer messageBuffer) {
        return messageBuffer.getShort();
    }

    /**
     * Deserialize UUID from byte buffer
     *
     * @return Deserialized UUID
     */
    private UUID deserialiseUuid(final ByteBuffer messageBuffer) {
        final ByteOrder originalOrder = messageBuffer.order();

        messageBuffer.order(ByteOrder.BIG_ENDIAN);
        UUID g = new UUID(messageBuffer.getLong(), messageBuffer.getLong());

        messageBuffer.order(originalOrder);
        return g;
    }

    /**
     * Deserialize long from byte buffer
     *
     * @return Deserialized long
     */
    private long deserialiseLong(final ByteBuffer messageBuffer) {
        return messageBuffer.getLong();
    }

    /**
     * Deserialize float from byte buffer
     *
     * @return Deserialized float
     */
    private float deserialiseFloat(final ByteBuffer messageBuffer) {
        return Float.intBitsToFloat(messageBuffer.getInt());
    }

    /**
     * Deserialize double from byte buffer
     *
     * @return Deserialized double
     */
    private double deserialiseDouble(final ByteBuffer messageBuffer) {
        return Double.longBitsToDouble(messageBuffer.getLong());
    }

    /**
     * Deserialize Month from byte buffer
     *
     * @return Deserialized Month
     */
    private Month deserialiseMonth(final ByteBuffer messageBuffer) {
        return new Month(messageBuffer.getInt());
    }

    /**
     * Deserialize Minute from byte buffer
     *
     * @return Deserialized Minute
     */
    private Minute deserialiseMinute(final ByteBuffer messageBuffer) {
        return new Minute(messageBuffer.getInt());
    }

    /**
     * Deserialize Second from byte buffer
     *
     * @return Deserialized Second
     */
    private Second deserialiseSecond(final ByteBuffer messageBuffer) {
        return new Second(messageBuffer.getInt());
    }

    /**
     * Deserialize Timespan from byte buffer
     *
     * @return Deserialized Timespan
     */
    private Timespan deserialiseTimespan(final ByteBuffer messageBuffer) {
        return new Timespan(messageBuffer.getLong());
    }

    /**
     * Deserialize date from byte buffer
     *
     * @return Deserialized date
     */
    private LocalDate deserialiseLocalDate(final ByteBuffer messageBuffer) {
        final int dateAsInt = messageBuffer.getInt();
        return (dateAsInt == NULL_INT
                ? LocalDate.MIN
                : LocalDate.ofEpochDay(DAYS_BETWEEN_1970_2000 + dateAsInt));
    }

    /**
     * Deserialize time from byte buffer
     *
     * @return Deserialized time
     */
    private LocalTime deserialiseLocalTime(final ByteBuffer messageBuffer) {
        final int timeAsInt = messageBuffer.getInt();
        return (timeAsInt == NULL_INT
                ? NULL_LOCAL_TIME
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(timeAsInt), UTC).toLocalTime());
    }

    /**
     * Deserialize LocalDateTime from byte buffer
     *
     * @return Deserialized date
     */
    private LocalDateTime deserialiseLocalDateTime(final ByteBuffer messageBuffer) {
        final double f = deserialiseDouble(messageBuffer);
        if (Double.isNaN(f)) {
            return LocalDateTime.MIN;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(MILLS_BETWEEN_1970_2000 + Math.round(8.64e7 * f)), UTC);
    }

    /**
     * Deserialize Instant from byte buffer
     *
     * @return Deserialized timestamp
     */
    private Instant deserialiseInstant(final ByteBuffer messageBuffer) {
        final long timeAsLong = messageBuffer.getLong();
        if (timeAsLong == NULL_LONG) {
            return Instant.MIN;
        }
        final long d = timeAsLong < 0
                ? (timeAsLong + 1) / NANOS_IN_SEC - 1
                : timeAsLong / NANOS_IN_SEC;
        return Instant.ofEpochMilli(MILLS_BETWEEN_1970_2000 + 1000 * d).plusNanos((int) (timeAsLong - NANOS_IN_SEC * d));
    }

    /**
     * A helper function for nx, calculates the number of bytes which would be required to serialize the supplied string.
     *
     * @param string String to be serialized
     * @return number of bytes required to serialise a string
     * @throws UnsupportedEncodingException If the named charset is not supported
     */
    protected int lengthOfEncodedString(final String string) throws UnsupportedEncodingException {
        if (string == null) {
            return 0;
        }

        int nullTerminatorPosition;
        if (-1 < (nullTerminatorPosition = string.indexOf(0x00))) {
            return string.substring(0, nullTerminatorPosition).getBytes(stringEncoding).length;
        } else {
            return string.getBytes(stringEncoding).length;
        }
    }

    private byte[] encodeString(final String string) throws UnsupportedEncodingException {
        return string.getBytes(stringEncoding);
    }

    /**
     * Calculates the number of bytes which would be required to serialize the supplied object.
     *
     * @param obj Object to be serialized
     * @return number of bytes required to serialise an object.
     * @throws UnsupportedEncodingException If the named charset is not supported
     */
    private int lengthOfObject(final Object obj) throws UnsupportedEncodingException {
        DataType type = DataType.getKdbType(obj);
        if (type == DataType.Dict) {
            return 1 + lengthOfObject(((Dict) obj).x) + lengthOfObject(((Dict) obj).y);
        }
        if (type == DataType.Flip) {
            return 3 + lengthOfObject(((Flip) obj).columnNames) + lengthOfObject(((Flip) obj).columns);
        }
        if (type.isAtom()) {
            return type == DataType.String
                    ? 2 + lengthOfEncodedString((String) obj)
                    : 1 + type.getAtomicByteSize();
        }

        int numBytes = 6;
        int numElements = elementCount(obj);
        if (type == DataType.List || type == DataType.StringArray) {
            for (int idx = 0; idx < numElements; ++idx)
                numBytes +=
                        type == DataType.List
                                ? lengthOfObject(((Object[]) obj)[idx])
                                : 1 + lengthOfEncodedString(((String[]) obj)[idx]);
        } else {
            numBytes += numElements * type.getAtomicByteSize();
        }
        return numBytes;
    }

    /**
     * A helper function used by nx which returns the number of elements in the supplied object
     * (for example: the number of keys in a Dict, the number of rows in a Flip,
     * the length of the array if its an array type)
     *
     * @param obj Object to be serialized
     * @return number of elements in an object.
     * @throws UnsupportedEncodingException If the named charset is not supported
     */
    private int elementCount(final Object obj) throws UnsupportedEncodingException {
        if (obj instanceof Dict) {
            return elementCount(((Dict) obj).x);
        }
        if (obj instanceof Flip) {
            return elementCount(((Flip) obj).columns[0]);
        }
        return obj instanceof char[]
                ? new String((char[]) obj).getBytes(stringEncoding).length
                : Array.getLength(obj);
    }
}
