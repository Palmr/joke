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

import static java.time.ZoneOffset.UTC;
import static uk.co.palmr.joke.types.DataType.Lambda;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import uk.co.palmr.joke.messages.KdbMessageHeader;
import uk.co.palmr.joke.types.*;

public class KdbProtocol {
  public static final byte NULL_BYTE = 0x00;

  /** null integer, i.e. 0Ni */
  public static final int NULL_INT = Integer.MIN_VALUE;

  /** null long, i.e. 0N */
  public static final long NULL_LONG = Long.MIN_VALUE;

  /** null float, i.e. 0Nf or 0n */
  public static final double NULL_FLOAT = Double.NaN;

  /** Representation of a null for a time atom within kdb */
  protected static final LocalTime NULL_LOCAL_TIME = LocalTime.ofNanoOfDay(1);

  protected static final int DAYS_BETWEEN_1970_2000 = 10_957;
  protected static final long SECONDS_BETWEEN_1970_2000 = 946_684_800L;
  protected static final long MILLS_IN_DAY = 86_400_000L;
  protected static final long MILLS_BETWEEN_1970_2000 = MILLS_IN_DAY * DAYS_BETWEEN_1970_2000;
  protected static final long NANOS_IN_SEC = 1_000_000_000L;
  protected static final long NANOS_IN_MS = NANOS_IN_SEC / 1_000;

  /** The character encoding to use when [de]-serializing strings. */
  private final Charset stringEncoding;

  private final boolean allowCompression;

  private int version = IpcVersion.KDB_IPC_VERSION;

  protected KdbProtocol(final Charset stringEncoding, final boolean allowCompression) {
    this.stringEncoding = stringEncoding;
    this.allowCompression = allowCompression;
  }

  protected void setVersion(final int version) {
    this.version = version;
  }

  /**
   * Serialize and write the data to the registered connection
   *
   * @param msgType type of the ipc message
   * @param msg object to serialize
   * @param kdbMessageHeader flyweight kdb message header
   * @param messageBuffer buffer to serialize data into
   */
  protected void serializeMessage(
      final MessageType msgType,
      final Object msg,
      final KdbMessageHeader kdbMessageHeader,
      final ByteBuffer messageBuffer)
      throws KdbException {
    kdbMessageHeader
        .setByteOrder(ByteOrder.BIG_ENDIAN)
        .setMessageType(msgType)
        .setCompressed(false)
        .setPad();

    messageBuffer.position(KdbMessageHeader.SIZE);
    serialize(msg, messageBuffer);

    kdbMessageHeader.setMessageSize(messageBuffer.position());

    if (allowCompression && messageBuffer.position() > 2000) {
      throw new UnsupportedOperationException("Not yet implemented compression");
      //            kdbMessageHeader.setIsCompressed(true);
      //            compress();
    }
  }

  protected Object deserialize(
      final KdbMessageHeader kdbMessageHeader, final ByteBuffer messageBuffer) throws KdbException {
    messageBuffer.order(kdbMessageHeader.getByteOrder());

    messageBuffer.position(KdbMessageHeader.SIZE);
    if (kdbMessageHeader.isCompressed()) {
      throw new UnsupportedOperationException("Not yet implemented compression");
      //            uncompress();
    }
    return deserializeResponseMessage(messageBuffer);
  }

  /**
   * Write String to serialization buffer
   *
   * @param string String to serialize
   */
  protected void writeStringToBuffer(final String string, final ByteBuffer buffer) {
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
   * @param obj Object to serialize
   * @param messageBuffer buffer to serialize to
   */
  protected void serialize(final Object obj, final ByteBuffer messageBuffer) throws KdbException {
    final DataType type = DataType.getKdbType(obj);
    messageBuffer.put(type.getTypeCode());
    if (type.isAtom()) {
      switch (type) {
        case Boolean:
          serialize(((Boolean) obj).booleanValue(), messageBuffer);
          return;
        case UUID:
          serialize((UUID) obj, messageBuffer);
          return;
        case Byte:
          serialize(((Byte) obj).byteValue(), messageBuffer);
          return;
        case Short:
          serialize(((Short) obj).shortValue(), messageBuffer);
          return;
        case Integer:
          serialize(((Integer) obj).intValue(), messageBuffer);
          return;
        case Long:
          serialize(((Long) obj).longValue(), messageBuffer);
          return;
        case Float:
          serialize(((Float) obj).floatValue(), messageBuffer);
          return;
        case Double:
          serialize(((Double) obj).doubleValue(), messageBuffer);
          return;
        case Character:
          serialize(((Character) obj).charValue(), messageBuffer);
          return;
        case String:
          serialize((String) obj, messageBuffer);
          return;
        case Instant:
          serialize((Instant) obj, messageBuffer);
          return;
        case Month:
          serialize((Month) obj, messageBuffer);
          return;
        case LocalDate:
          serialize((LocalDate) obj, messageBuffer);
          return;
        case LocalDateTime:
          serialize((LocalDateTime) obj, messageBuffer);
          return;
        case Timespan:
          serialize((Timespan) obj, messageBuffer);
          return;
        case Minute:
          serialize((Minute) obj, messageBuffer);
          return;
        case Second:
          serialize((Second) obj, messageBuffer);
          return;
        case LocalTime:
          serialize((LocalTime) obj, messageBuffer);
          return;
      }
    }

    if (type == DataType.Dict) {
      final Dict r = (Dict) obj;
      serialize(r.keys(), messageBuffer);
      serialize(r.values(), messageBuffer);
      return;
    }

    messageBuffer.put(NULL_BYTE);
    if (type == DataType.Flip) {
      final Flip r = (Flip) obj;
      messageBuffer.put(DataType.Dict.getTypeCode());
      serialize(r.columnNames, messageBuffer);
      serialize(r.columns, messageBuffer);
      return;
    }

    final int numElements = elementCount(obj);

    serialize(numElements, messageBuffer);

    if (type == DataType.CharArray) {
      messageBuffer.put(new String((char[]) obj).getBytes(stringEncoding));
    } else if (type == DataType.ByteArray) {
      messageBuffer.put((byte[]) obj);
    } else if (type == DataType.ShortArray) {
      messageBuffer.asShortBuffer().put((short[]) obj);
      messageBuffer.position(messageBuffer.position() + numElements * Short.BYTES);
    } else if (type == DataType.IntArray) {
      messageBuffer.asIntBuffer().put((int[]) obj);
      messageBuffer.position(messageBuffer.position() + numElements * Integer.BYTES);
    } else if (type == DataType.LongArray) {
      messageBuffer.asLongBuffer().put((long[]) obj);
      messageBuffer.position(messageBuffer.position() + numElements * Long.BYTES);
    } else if (type == DataType.FloatArray) {
      messageBuffer.asFloatBuffer().put((float[]) obj);
      messageBuffer.position(messageBuffer.position() + numElements * Float.BYTES);
    } else if (type == DataType.DoubleArray) {
      messageBuffer.asDoubleBuffer().put((double[]) obj);
      messageBuffer.position(messageBuffer.position() + numElements * Double.BYTES);
    } else {
      for (int idx = 0; idx < numElements; idx++) {
        switch (type) {
          case List:
            serialize(((Object[]) obj)[idx], messageBuffer);
            break;
          case BooleanArray:
            serialize(((boolean[]) obj)[idx], messageBuffer);
            break;
          case UUIDArray:
            serialize(((UUID[]) obj)[idx], messageBuffer);
            break;
          case StringArray:
            serialize(((String[]) obj)[idx], messageBuffer);
            break;
          case InstantArray:
            serialize(((Instant[]) obj)[idx], messageBuffer);
            break;
          case MonthArray:
            serialize(((Month[]) obj)[idx], messageBuffer);
            break;
          case LocalDateArray:
            serialize(((LocalDate[]) obj)[idx], messageBuffer);
            break;
          case LocalDateTimeArray:
            serialize(((LocalDateTime[]) obj)[idx], messageBuffer);
            break;
          case TimespanArray:
            serialize(((Timespan[]) obj)[idx], messageBuffer);
            break;
          case MinuteArray:
            serialize(((Minute[]) obj)[idx], messageBuffer);
            break;
          case SecondArray:
            serialize(((Second[]) obj)[idx], messageBuffer);
            break;
          case LocalTimeArray:
            serialize(((LocalTime[]) obj)[idx], messageBuffer);
            break;
          default:
            throw new KdbException("Unhandled type: " + type);
        }
      }
    }
  }

  private void serialize(boolean bool, final ByteBuffer messageBuffer) {
    messageBuffer.put((byte) (bool ? 1 : 0));
  }

  private void serialize(UUID uuid, final ByteBuffer messageBuffer) {
    if (version < 3) {
      throw new RuntimeException("Guid not valid pre kdb+3.0");
    }

    serialize(uuid.getMostSignificantBits(), messageBuffer);
    serialize(uuid.getLeastSignificantBits(), messageBuffer);
  }

  private void serialize(byte b, final ByteBuffer messageBuffer) {
    messageBuffer.put(b);
  }

  private void serialize(short s, final ByteBuffer messageBuffer) {
    messageBuffer.putShort(s);
  }

  private void serialize(int i, final ByteBuffer messageBuffer) {
    messageBuffer.putInt(i);
  }

  private void serialize(long l, final ByteBuffer messageBuffer) {
    messageBuffer.putLong(l);
  }

  private void serialize(float f, final ByteBuffer messageBuffer) {
    messageBuffer.putInt(Float.floatToIntBits(f));
  }

  private void serialize(double d, final ByteBuffer messageBuffer) {
    messageBuffer.putLong(Double.doubleToLongBits(d));
  }

  private void serialize(char c, final ByteBuffer messageBuffer) {
    messageBuffer.put((byte) c);
  }

  private void serialize(String s, final ByteBuffer messageBuffer) {
    if (s != null) {
      byte[] encodedStringChars = s.getBytes(stringEncoding);
      for (int idx = 0;
          idx < encodedStringChars.length && encodedStringChars[idx] != NULL_BYTE;
          idx++) {
        messageBuffer.put(encodedStringChars[idx]);
      }
    }
    messageBuffer.put(NULL_BYTE);
  }

  private void serialize(Instant p, final ByteBuffer messageBuffer) {
    if (version < 1) {
      throw new RuntimeException("Instant not valid pre kdb+2.6");
    }
    messageBuffer.putLong(
        p == Instant.MIN
            ? NULL_LONG
            : 1000000 * (p.toEpochMilli() - MILLS_BETWEEN_1970_2000) + p.getNano() % 1000000);
  }

  private void serialize(Month m, final ByteBuffer messageBuffer) {
    messageBuffer.putInt(m.monthsSinceJan2000());
  }

  private void serialize(LocalDate d, final ByteBuffer messageBuffer) {
    if (d == LocalDate.MIN) {
      messageBuffer.putInt(NULL_INT);
      return;
    }
    long daysSince2000 = d.toEpochDay() - DAYS_BETWEEN_1970_2000;
    if (daysSince2000 < Integer.MIN_VALUE || daysSince2000 > Integer.MAX_VALUE) {
      throw new RuntimeException(
          "LocalDate epoch day since 2000 must be >= Integer.MIN_VALUE and <= Integer.MAX_VALUE");
    }
    messageBuffer.putInt((int) (daysSince2000));
  }

  private void serialize(LocalDateTime z, final ByteBuffer messageBuffer) {
    if (z == LocalDateTime.MIN) {
      serialize(NULL_FLOAT, messageBuffer);
      return;
    }

    long daysSince2000 = z.toLocalDate().toEpochDay() - DAYS_BETWEEN_1970_2000;
    long millisSince2000 =
        daysSince2000 * MILLS_IN_DAY + (z.toLocalTime().toNanoOfDay() / NANOS_IN_MS);
    serialize(millisSince2000 / (double) MILLS_IN_DAY, messageBuffer);
  }

  private void serialize(Timespan n, final ByteBuffer messageBuffer) {
    if (version < 1) {
      throw new RuntimeException("Timespan not valid pre kdb+2.6");
    }
    messageBuffer.putLong(n.nanosSinceMidnight());
  }

  private void serialize(Minute u, final ByteBuffer messageBuffer) {
    messageBuffer.putInt(u.minsSinceMidnight());
  }

  private void serialize(Second v, final ByteBuffer messageBuffer) {
    messageBuffer.putInt(v.secondsSinceMidnight());
  }

  private void serialize(LocalTime t, final ByteBuffer messageBuffer) {
    messageBuffer.putInt((t == NULL_LOCAL_TIME) ? NULL_INT : (int) (t.toNanoOfDay() / NANOS_IN_MS));
  }

  /**
   * Deserialize string from byte buffer
   *
   * @param messageBuffer incoming message buffer
   * @return Deserialized string using registered encoding
   */
  private String deserializeString(final ByteBuffer messageBuffer) {
    final var startPos = messageBuffer.position();
    while (messageBuffer.get() != NULL_BYTE) {
      // advance position to null terminator
    }
    final var endPos = messageBuffer.position();
    final var stringBytes = new byte[endPos - startPos - 1];
    messageBuffer.get(startPos, stringBytes);

    return (startPos == endPos - 1) ? "" : new String(stringBytes, stringEncoding);
  }

  /**
   * Deserializes the contents of the incoming message buffer
   *
   * @param messageBuffer incoming message buffer private @return deserialized object
   */
  protected Object deserializeResponseMessage(final ByteBuffer messageBuffer) throws KdbException {
    int i = 0;
    int n;
    DataType type = DataType.getKdbType(messageBuffer.get());
    if (type.isAtom())
      switch (type) {
        case Boolean:
          return deserializeBoolean(messageBuffer);
        case UUID:
          return deserializeUuid(messageBuffer);
        case Byte:
          return messageBuffer.get();
        case Short:
          return deserializeShort(messageBuffer);
        case Integer:
          return messageBuffer.getInt();
        case Long:
          return deserializeLong(messageBuffer);
        case Float:
          return deserializeFloat(messageBuffer);
        case Double:
          return deserializeDouble(messageBuffer);
        case Character:
          return deserializeChar(messageBuffer);
        case String:
          return deserializeString(messageBuffer);
        case Instant:
          return deserializeInstant(messageBuffer);
        case Month:
          return deserializeMonth(messageBuffer);
        case LocalDate:
          return deserializeLocalDate(messageBuffer);
        case LocalDateTime:
          return deserializeLocalDateTime(messageBuffer);
        case Timespan:
          return deserializeTimespan(messageBuffer);
        case Minute:
          return deserializeMinute(messageBuffer);
        case Second:
          return deserializeSecond(messageBuffer);
        case LocalTime:
          return deserializeLocalTime(messageBuffer);
        case Exception:
          throw new KdbException(deserializeString(messageBuffer));
      }
    if (type.getTypeCode() > DataType.Dict.getTypeCode()) {
      if (type == Lambda) {
        deserializeString(messageBuffer);
        return deserializeResponseMessage(messageBuffer);
      }
      if (type.getTypeCode() < DataType.Projection.getTypeCode()) {
        return messageBuffer.get() == 0 && type == DataType.UnaryPrimitive ? null : "func";
      }
      if (type.getTypeCode() > DataType.Composition.getTypeCode()) {
        deserializeResponseMessage(messageBuffer);
      } else {
        for (n = messageBuffer.getInt(); i < n; i++) {
          deserializeResponseMessage(messageBuffer);
        }
      }
      return "func";
    }
    if (type == DataType.Dict)
      return new Dict(
          deserializeResponseMessage(messageBuffer), deserializeResponseMessage(messageBuffer));

    messageBuffer.get();

    if (type == DataType.Flip) {
      return new Flip((Dict) deserializeResponseMessage(messageBuffer));
    }
    n = messageBuffer.getInt();
    switch (type) {
      case List:
        Object[] objArr = new Object[n];
        for (; i < n; i++) objArr[i] = deserializeResponseMessage(messageBuffer);
        return objArr;
      case BooleanArray:
        boolean[] boolArr = new boolean[n];
        for (; i < n; i++) boolArr[i] = deserializeBoolean(messageBuffer);
        return boolArr;
      case UUIDArray:
        UUID[] uuidArr = new UUID[n];
        for (; i < n; i++) uuidArr[i] = deserializeUuid(messageBuffer);
        return uuidArr;
      case ByteArray:
        byte[] byteArr = new byte[n];
        messageBuffer.get(byteArr);
        return byteArr;
      case ShortArray:
        short[] shortArr = new short[n];
        messageBuffer.asShortBuffer().get(shortArr);
        messageBuffer.position(messageBuffer.position() + n * Short.BYTES);
        return shortArr;
      case IntArray:
        int[] intArr = new int[n];
        messageBuffer.asIntBuffer().get(intArr);
        messageBuffer.position(messageBuffer.position() + n * Integer.BYTES);
        return intArr;
      case LongArray:
        long[] longArr = new long[n];
        messageBuffer.asLongBuffer().get(longArr);
        messageBuffer.position(messageBuffer.position() + n * Long.BYTES);
        return longArr;
      case FloatArray:
        float[] floatArr = new float[n];
        messageBuffer.asFloatBuffer().get(floatArr);
        messageBuffer.position(messageBuffer.position() + n * Float.BYTES);
        return floatArr;
      case DoubleArray:
        double[] doubleArr = new double[n];
        messageBuffer.asDoubleBuffer().get(doubleArr);
        messageBuffer.position(messageBuffer.position() + n * Double.BYTES);
        return doubleArr;
      case CharArray:
        char[] charArr =
            stringEncoding
                .decode(messageBuffer.slice(messageBuffer.position(), n))
                .toString()
                .toCharArray();
        messageBuffer.position(messageBuffer.position() + n);
        return charArr;
      case StringArray:
        String[] stringArr = new String[n];
        for (; i < n; i++) stringArr[i] = deserializeString(messageBuffer);
        return stringArr;
      case InstantArray:
        Instant[] timestampArr = new Instant[n];
        for (; i < n; i++) timestampArr[i] = deserializeInstant(messageBuffer);
        return timestampArr;
      case MonthArray:
        Month[] monthArr = new Month[n];
        for (; i < n; i++) monthArr[i] = deserializeMonth(messageBuffer);
        return monthArr;
      case LocalDateArray:
        LocalDate[] dateArr = new LocalDate[n];
        for (; i < n; i++) dateArr[i] = deserializeLocalDate(messageBuffer);
        return dateArr;
      case LocalDateTimeArray:
        LocalDateTime[] dateUtilArr = new LocalDateTime[n];
        for (; i < n; i++) dateUtilArr[i] = deserializeLocalDateTime(messageBuffer);
        return dateUtilArr;
      case TimespanArray:
        Timespan[] timespanArr = new Timespan[n];
        for (; i < n; i++) timespanArr[i] = deserializeTimespan(messageBuffer);
        return timespanArr;
      case MinuteArray:
        Minute[] minArr = new Minute[n];
        for (; i < n; i++) minArr[i] = deserializeMinute(messageBuffer);
        return minArr;
      case SecondArray:
        Second[] secArr = new Second[n];
        for (; i < n; i++) secArr[i] = deserializeSecond(messageBuffer);
        return secArr;
      case LocalTimeArray:
        LocalTime[] timeArr = new LocalTime[n];
        for (; i < n; i++) timeArr[i] = deserializeLocalTime(messageBuffer);
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
  private char deserializeChar(final ByteBuffer messageBuffer) {
    return (char) (messageBuffer.get() & 0xff);
  }

  /**
   * Deserialize boolean from byte buffer
   *
   * @return Deserialized boolean
   */
  private boolean deserializeBoolean(final ByteBuffer messageBuffer) {
    return 1 == messageBuffer.get();
  }

  /**
   * Deserialize short from byte buffer
   *
   * @return Deserialized short
   */
  private short deserializeShort(final ByteBuffer messageBuffer) {
    return messageBuffer.getShort();
  }

  /**
   * Deserialize UUID from byte buffer
   *
   * @return Deserialized UUID
   */
  private UUID deserializeUuid(final ByteBuffer messageBuffer) {
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
  private long deserializeLong(final ByteBuffer messageBuffer) {
    return messageBuffer.getLong();
  }

  /**
   * Deserialize float from byte buffer
   *
   * @return Deserialized float
   */
  private float deserializeFloat(final ByteBuffer messageBuffer) {
    return Float.intBitsToFloat(messageBuffer.getInt());
  }

  /**
   * Deserialize double from byte buffer
   *
   * @return Deserialized double
   */
  private double deserializeDouble(final ByteBuffer messageBuffer) {
    return Double.longBitsToDouble(messageBuffer.getLong());
  }

  /**
   * Deserialize Month from byte buffer
   *
   * @return Deserialized Month
   */
  private Month deserializeMonth(final ByteBuffer messageBuffer) {
    return new Month(messageBuffer.getInt());
  }

  /**
   * Deserialize Minute from byte buffer
   *
   * @return Deserialized Minute
   */
  private Minute deserializeMinute(final ByteBuffer messageBuffer) {
    return new Minute(messageBuffer.getInt());
  }

  /**
   * Deserialize Second from byte buffer
   *
   * @return Deserialized Second
   */
  private Second deserializeSecond(final ByteBuffer messageBuffer) {
    return new Second(messageBuffer.getInt());
  }

  /**
   * Deserialize Timespan from byte buffer
   *
   * @return Deserialized Timespan
   */
  private Timespan deserializeTimespan(final ByteBuffer messageBuffer) {
    return new Timespan(messageBuffer.getLong());
  }

  /**
   * Deserialize date from byte buffer
   *
   * @return Deserialized date
   */
  private LocalDate deserializeLocalDate(final ByteBuffer messageBuffer) {
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
  private LocalTime deserializeLocalTime(final ByteBuffer messageBuffer) {
    final int timeAsInt = messageBuffer.getInt();

    return (timeAsInt == NULL_INT
        ? NULL_LOCAL_TIME
        : LocalTime.ofNanoOfDay(timeAsInt * NANOS_IN_MS));
  }

  /**
   * Deserialize LocalDateTime from byte buffer
   *
   * @return Deserialized date
   */
  private LocalDateTime deserializeLocalDateTime(final ByteBuffer messageBuffer) {
    final double f = deserializeDouble(messageBuffer);
    if (Double.isNaN(f)) {
      return LocalDateTime.MIN;
    }
    final long millisSince2000 = Math.round(MILLS_IN_DAY * f);
    final long epochSecond = SECONDS_BETWEEN_1970_2000 + Math.floorDiv(millisSince2000, 1000L);
    final long nano = Math.floorMod(millisSince2000, 1000L) * NANOS_IN_MS;
    return LocalDateTime.ofEpochSecond(epochSecond, (int) nano, UTC);
  }

  /**
   * Deserialize Instant from byte buffer
   *
   * @return Deserialized timestamp
   */
  private Instant deserializeInstant(final ByteBuffer messageBuffer) {
    final long timeAsLong = messageBuffer.getLong();
    if (timeAsLong == NULL_LONG) {
      return Instant.MIN;
    }
    final long d = timeAsLong < 0 ? (timeAsLong + 1) / NANOS_IN_SEC - 1 : timeAsLong / NANOS_IN_SEC;
    return Instant.ofEpochMilli(MILLS_BETWEEN_1970_2000 + 1000 * d)
        .plusNanos((int) (timeAsLong - NANOS_IN_SEC * d));
  }

  private byte[] encodeString(final String string) {
    return string.getBytes(stringEncoding);
  }

  /**
   * A helper function used by nx which returns the number of elements in the supplied object (for
   * example: the number of keys in a Dict, the number of rows in a Flip, the length of the array if
   * its an array type)
   *
   * @param obj Object to be serialized
   * @return number of elements in an object.
   */
  private int elementCount(final Object obj) {
    if (obj instanceof Dict) {
      return elementCount(((Dict) obj).keys());
    }
    if (obj instanceof Flip) {
      return elementCount(((Flip) obj).columns[0]);
    }
    return obj instanceof char[]
        ? new String((char[]) obj).getBytes(stringEncoding).length
        : Array.getLength(obj);
  }
}
