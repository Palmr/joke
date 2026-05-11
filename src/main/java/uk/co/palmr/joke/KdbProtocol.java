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
    switch (obj) {
      case Boolean b -> serialize((boolean) b, messageBuffer);
      case UUID u -> serialize(u, messageBuffer);
      case Byte b -> serialize((byte) b, messageBuffer);
      case Short s -> serialize((short) s, messageBuffer);
      case Integer i -> serialize((int) i, messageBuffer);
      case Long l -> serialize((long) l, messageBuffer);
      case Float f -> serialize((float) f, messageBuffer);
      case Double d -> serialize((double) d, messageBuffer);
      case Character c -> serialize((char) c, messageBuffer);
      case String s -> serialize(s, messageBuffer);
      case Instant p -> serialize(p, messageBuffer);
      case Month m -> serialize(m, messageBuffer);
      case LocalDate d -> serialize(d, messageBuffer);
      case LocalDateTime z -> serialize(z, messageBuffer);
      case Timespan n -> serialize(n, messageBuffer);
      case Minute u -> serialize(u, messageBuffer);
      case Second v -> serialize(v, messageBuffer);
      case LocalTime t -> serialize(t, messageBuffer);
      case Dict(var k, var v) -> {
        serialize(k, messageBuffer);
        serialize(v, messageBuffer);
      }
      case Flip f -> {
        messageBuffer.put(NULL_BYTE);
        messageBuffer.put(DataType.Dict.getTypeCode());
        serialize(f.columnNames(), messageBuffer);
        serialize(f.columns(), messageBuffer);
      }
      case char[] chars -> {
        messageBuffer.put(NULL_BYTE);
        var bytes = new String(chars).getBytes(stringEncoding);
        serialize(bytes.length, messageBuffer);
        messageBuffer.put(bytes);
      }
      case byte[] bytes -> {
        messageBuffer.put(NULL_BYTE);
        serialize(bytes.length, messageBuffer);
        messageBuffer.put(bytes);
      }
      case short[] shorts -> {
        messageBuffer.put(NULL_BYTE);
        serialize(shorts.length, messageBuffer);
        messageBuffer.asShortBuffer().put(shorts);
        messageBuffer.position(messageBuffer.position() + shorts.length * Short.BYTES);
      }
      case int[] ints -> {
        messageBuffer.put(NULL_BYTE);
        serialize(ints.length, messageBuffer);
        messageBuffer.asIntBuffer().put(ints);
        messageBuffer.position(messageBuffer.position() + ints.length * Integer.BYTES);
      }
      case long[] longs -> {
        messageBuffer.put(NULL_BYTE);
        serialize(longs.length, messageBuffer);
        messageBuffer.asLongBuffer().put(longs);
        messageBuffer.position(messageBuffer.position() + longs.length * Long.BYTES);
      }
      case float[] floats -> {
        messageBuffer.put(NULL_BYTE);
        serialize(floats.length, messageBuffer);
        messageBuffer.asFloatBuffer().put(floats);
        messageBuffer.position(messageBuffer.position() + floats.length * Float.BYTES);
      }
      case double[] doubles -> {
        messageBuffer.put(NULL_BYTE);
        serialize(doubles.length, messageBuffer);
        messageBuffer.asDoubleBuffer().put(doubles);
        messageBuffer.position(messageBuffer.position() + doubles.length * Double.BYTES);
      }
      case boolean[] bools -> {
        messageBuffer.put(NULL_BYTE);
        serialize(bools.length, messageBuffer);
        for (var b : bools) serialize(b, messageBuffer);
      }
      case UUID[] uuids -> {
        messageBuffer.put(NULL_BYTE);
        serialize(uuids.length, messageBuffer);
        for (var u : uuids) serialize(u, messageBuffer);
      }
      case String[] strings -> {
        messageBuffer.put(NULL_BYTE);
        serialize(strings.length, messageBuffer);
        for (var s : strings) serialize(s, messageBuffer);
      }
      case Instant[] instants -> {
        messageBuffer.put(NULL_BYTE);
        serialize(instants.length, messageBuffer);
        for (var p : instants) serialize(p, messageBuffer);
      }
      case Month[] months -> {
        messageBuffer.put(NULL_BYTE);
        serialize(months.length, messageBuffer);
        for (var m : months) serialize(m, messageBuffer);
      }
      case LocalDate[] dates -> {
        messageBuffer.put(NULL_BYTE);
        serialize(dates.length, messageBuffer);
        for (var d : dates) serialize(d, messageBuffer);
      }
      case LocalDateTime[] dateTimes -> {
        messageBuffer.put(NULL_BYTE);
        serialize(dateTimes.length, messageBuffer);
        for (var z : dateTimes) serialize(z, messageBuffer);
      }
      case Timespan[] timespans -> {
        messageBuffer.put(NULL_BYTE);
        serialize(timespans.length, messageBuffer);
        for (var n : timespans) serialize(n, messageBuffer);
      }
      case Minute[] minutes -> {
        messageBuffer.put(NULL_BYTE);
        serialize(minutes.length, messageBuffer);
        for (var u : minutes) serialize(u, messageBuffer);
      }
      case Second[] seconds -> {
        messageBuffer.put(NULL_BYTE);
        serialize(seconds.length, messageBuffer);
        for (var v : seconds) serialize(v, messageBuffer);
      }
      case LocalTime[] times -> {
        messageBuffer.put(NULL_BYTE);
        serialize(times.length, messageBuffer);
        for (var t : times) serialize(t, messageBuffer);
      }
      case Object[] objs -> {
        messageBuffer.put(NULL_BYTE);
        serialize(objs.length, messageBuffer);
        for (var o : objs) serialize(o, messageBuffer);
      }
      default -> throw new KdbException("Unhandled type: " + type);
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
    final var length = messageBuffer.position() - startPos - 1;
    return length == 0
        ? ""
        : new String(
            messageBuffer.array(), messageBuffer.arrayOffset() + startPos, length, stringEncoding);
  }

  /**
   * Deserializes the contents of the incoming message buffer
   *
   * @param messageBuffer incoming message buffer private @return deserialized object
   */
  protected Object deserializeResponseMessage(final ByteBuffer messageBuffer) throws KdbException {
    DataType type = DataType.getKdbType(messageBuffer.get());
    if (type.isAtom())
      return switch (type) {
        case Boolean -> deserializeBoolean(messageBuffer);
        case UUID -> deserializeUuid(messageBuffer);
        case Byte -> messageBuffer.get();
        case Short -> deserializeShort(messageBuffer);
        case Integer -> messageBuffer.getInt();
        case Long -> deserializeLong(messageBuffer);
        case Float -> deserializeFloat(messageBuffer);
        case Double -> deserializeDouble(messageBuffer);
        case Character -> deserializeChar(messageBuffer);
        case String -> deserializeString(messageBuffer);
        case Instant -> deserializeInstant(messageBuffer);
        case Month -> deserializeMonth(messageBuffer);
        case LocalDate -> deserializeLocalDate(messageBuffer);
        case LocalDateTime -> deserializeLocalDateTime(messageBuffer);
        case Timespan -> deserializeTimespan(messageBuffer);
        case Minute -> deserializeMinute(messageBuffer);
        case Second -> deserializeSecond(messageBuffer);
        case LocalTime -> deserializeLocalTime(messageBuffer);
        case Exception -> throw new KdbException(deserializeString(messageBuffer));
        default -> throw new IllegalStateException("Unexpected atom type: " + type);
      };
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
        for (var n = messageBuffer.getInt(); n > 0; n--) {
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
    var n = messageBuffer.getInt();
    return switch (type) {
      case List -> {
        var arr = new Object[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeResponseMessage(messageBuffer);
        yield arr;
      }
      case BooleanArray -> {
        var arr = new boolean[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeBoolean(messageBuffer);
        yield arr;
      }
      case UUIDArray -> {
        var arr = new UUID[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeUuid(messageBuffer);
        yield arr;
      }
      case ByteArray -> {
        var arr = new byte[n];
        messageBuffer.get(arr);
        yield arr;
      }
      case ShortArray -> {
        var arr = new short[n];
        messageBuffer.asShortBuffer().get(arr);
        messageBuffer.position(messageBuffer.position() + n * Short.BYTES);
        yield arr;
      }
      case IntArray -> {
        var arr = new int[n];
        messageBuffer.asIntBuffer().get(arr);
        messageBuffer.position(messageBuffer.position() + n * Integer.BYTES);
        yield arr;
      }
      case LongArray -> {
        var arr = new long[n];
        messageBuffer.asLongBuffer().get(arr);
        messageBuffer.position(messageBuffer.position() + n * Long.BYTES);
        yield arr;
      }
      case FloatArray -> {
        var arr = new float[n];
        messageBuffer.asFloatBuffer().get(arr);
        messageBuffer.position(messageBuffer.position() + n * Float.BYTES);
        yield arr;
      }
      case DoubleArray -> {
        var arr = new double[n];
        messageBuffer.asDoubleBuffer().get(arr);
        messageBuffer.position(messageBuffer.position() + n * Double.BYTES);
        yield arr;
      }
      case CharArray -> {
        var arr =
            stringEncoding
                .decode(messageBuffer.slice(messageBuffer.position(), n))
                .toString()
                .toCharArray();
        messageBuffer.position(messageBuffer.position() + n);
        yield arr;
      }
      case StringArray -> {
        var arr = new String[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeString(messageBuffer);
        yield arr;
      }
      case InstantArray -> {
        var arr = new Instant[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeInstant(messageBuffer);
        yield arr;
      }
      case MonthArray -> {
        var arr = new Month[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeMonth(messageBuffer);
        yield arr;
      }
      case LocalDateArray -> {
        var arr = new LocalDate[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeLocalDate(messageBuffer);
        yield arr;
      }
      case LocalDateTimeArray -> {
        var arr = new LocalDateTime[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeLocalDateTime(messageBuffer);
        yield arr;
      }
      case TimespanArray -> {
        var arr = new Timespan[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeTimespan(messageBuffer);
        yield arr;
      }
      case MinuteArray -> {
        var arr = new Minute[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeMinute(messageBuffer);
        yield arr;
      }
      case SecondArray -> {
        var arr = new Second[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeSecond(messageBuffer);
        yield arr;
      }
      case LocalTimeArray -> {
        var arr = new LocalTime[n];
        for (int j = 0; j < n; j++) arr[j] = deserializeLocalTime(messageBuffer);
        yield arr;
      }
      default -> null;
    };
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
}
