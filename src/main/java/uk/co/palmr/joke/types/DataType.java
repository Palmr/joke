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
package uk.co.palmr.joke.types;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <pre>
 * Basic datatypes
 * n   c   name      sz  literal            null inf SQL       Java      .Net
 * ------------------------------------------------------------------------------------
 * 0   *   list
 * 1   b   boolean   1   0b                                    Boolean   boolean
 * 2   g   guid      16                     0Ng                UUID      GUID
 * 4   x   byte      1   0x00                                  Byte      byte
 * 5   h   short     2   0h                 0Nh  0Wh smallint  Short     int16
 * 6   i   int       4   0i                 0Ni  0Wi int       Integer   int32
 * 7   j   long      8   0j                 0Nj  0Wj bigint    Long      int64
 * 0                  0N   0W
 * 8   e   real      4   0e                 0Ne  0We real      Float     single
 * 9   f   float     8   0.0                0n   0w  float     Double    double
 * 0f                 0Nf
 * 10  c   char      1   " "                " "                Character char
 * 11  s   symbol        `                  `        varchar
 * 12  p   timestamp 8   dateDtimespan      0Np  0Wp           Timestamp DateTime (RW)
 * 13  m   month     4   2000.01m           0Nm  0Wm
 * 14  d   date      4   2000.01.01         0Nd  0Wd date      Date
 * 15  z   datetime  8   dateTtime          0Nz  0wz timestamp Timestamp DateTime (RO)
 * 16  n   timespan  8   00:00:00.000000000 0Nn  0Wn           Timespan  TimeSpan
 * 17  u   minute    4   00:00              0Nu  0Wu
 * 18  v   second    4   00:00:00           0Nv  0Wv
 * 19  t   time      4   00:00:00.000       0Nt  0Wt time      Time      TimeSpan
 *
 * Columns:
 * n    short int returned by type and used for Cast, e.g. 9h$3
 * c    character used lower-case for Cast and upper-case for Tok and Load CSV
 * sz   size in bytes
 * inf  infinity (no math on temporal types); 0Wh is 32767h
 *
 * RO: read only; RW: read-write
 *
 * Other datatypes
 * 20-76   enums
 * 77      anymap                                      104  projection
 * 78-96   77+t – mapped list of lists of type t       105  composition
 * 97      nested sym enum                             106  f'
 * 98      table                                       107  f/
 * 99      dictionary                                  108  f\
 * 100     lambda                                      109  f':
 * 101     unary primitive                             110  f/:
 * 102     operator                                    111  f\:
 * 103     iterator                                    112  dynamic load
 * </pre>
 */
public enum DataType {
    /**
     * "number of bytes from type." A helper for `lengthOfObject`, to assist in calculating the number of bytes required
     * to serialize a particular type.
     */
    List(0, false, 0),
    Boolean(-1, true, 1),
    UUID(-2, true, 16),
    Byte(-4, true, 1),
    Short(-5, true, 2),
    Integer(-6, true, 4),
    Long(-7, true, 8),
    Float(-8, true, 4),
    Double(-9, true, 8),
    Character(-10, true, 1),
    String(-11, true, 0),
    Instant(-12, true, 8),
    Month(-13, true, 4),
    LocalDate(-14, true, 4),
    LocalDateTime(-15, true, 8),
    Timespan(-16, true, 8),
    Minute(-17, true, 4),
    Second(-18, true, 4),
    LocalTime(-19, true, 4),
    Exception(-128, true, 0),
    BooleanArray(1, false, 1),
    UUIDArray(2, false, 16),
    ByteArray(4, false, 1),
    ShortArray(5, false, 2),
    IntArray(6, false, 4),
    LongArray(7, false, 8),
    FloatArray(8, false, 4),
    DoubleArray(9, false, 8),
    CharArray(10, false, 1),
    StringArray(11, false, 0),
    InstantArray(12, false, 8),
    MonthArray(13, false, 4),
    LocalDateArray(14, false, 4),
    LocalDateTimeArray(15, false, 8),
    TimespanArray(16, false, 8),
    MinuteArray(17, false, 4),
    SecondArray(18, false, 4),
    LocalTimeArray(19, false, 4),
    Flip(98, false, 0),
    Dict(99, false, 0),
    Lambda(100, false, 0),
    UnaryPrimitive(101, false, 0),
    Operator(102, false, 0),
    Iterator(103, false, 0),
    Projection(104, false, 0),
    Composition(105, false, 0),
    Each(106, false, 0),
    Over(107, false, 0),
    Scan(108, false, 0),
    ParallelEach(109, false, 0),
    EachRight(110, false, 0),
    EachLeft(111, false, 0),
    dynamicLoad(112, false, 0);

    private static final Map<Byte, DataType> typeCodeLookup = new HashMap<>();

    static {
        for (final DataType dt : values()) {
            typeCodeLookup.put(dt.typeCode, dt);
        }
    }

    private final byte typeCode;
    private final boolean isAtom;
    private final int atomicByteSize;

    DataType(final int typeCode, final boolean isAtom, final int atomicByteSize) {
        this.typeCode = (byte) typeCode;
        this.isAtom = isAtom;
        this.atomicByteSize = atomicByteSize;
    }

    /**
     * Gets the numeric type of the supplied object used in kdb+ (distict supported data types in KDB+ can be identified by a numeric).&nbsp;
     * See data type reference <a href="https://code.kx.com/q/basics/datatypes/">https://code.kx.com/q/basics/datatypes/</a>.
     * For example, an object of type java.lang.Integer provides a numeric type of -6.
     *
     * @param x Object to get the numeric type of
     * @return kdb+ type number for an object
     */
    public static DataType getKdbType(final Object x) {
        if (x instanceof Boolean)
            return Boolean;
        if (x instanceof java.util.UUID)
            return UUID;
        if (x instanceof Byte)
            return Byte;
        if (x instanceof Short)
            return Short;
        if (x instanceof Integer)
            return Integer;
        if (x instanceof Long)
            return Long;
        if (x instanceof Float)
            return Float;
        if (x instanceof Double)
            return Double;
        if (x instanceof Character)
            return Character;
        if (x instanceof String)
            return String;
        if (x instanceof java.time.LocalDate)
            return LocalDate;
        if (x instanceof java.time.LocalTime)
            return LocalTime;
        if (x instanceof java.time.Instant)
            return Instant;
        if (x instanceof java.time.LocalDateTime)
            return LocalDateTime;
        if (x instanceof Timespan)
            return Timespan;
        if (x instanceof Month)
            return Month;
        if (x instanceof Minute)
            return Minute;
        if (x instanceof Second)
            return Second;
        if (x instanceof boolean[])
            return BooleanArray;
        if (x instanceof UUID[])
            return UUIDArray;
        if (x instanceof byte[])
            return ByteArray;
        if (x instanceof short[])
            return ShortArray;
        if (x instanceof int[])
            return IntArray;
        if (x instanceof long[])
            return LongArray;
        if (x instanceof float[])
            return FloatArray;
        if (x instanceof double[])
            return DoubleArray;
        if (x instanceof char[])
            return CharArray;
        if (x instanceof String[])
            return StringArray;
        if (x instanceof LocalDate[])
            return LocalDateArray;
        if (x instanceof LocalTime[])
            return LocalTimeArray;
        if (x instanceof Instant[])
            return InstantArray;
        if (x instanceof LocalDateTime[])
            return LocalDateTimeArray;
        if (x instanceof Timespan[])
            return TimespanArray;
        if (x instanceof Month[])
            return MonthArray;
        if (x instanceof Minute[])
            return MinuteArray;
        if (x instanceof Second[])
            return SecondArray;
        if (x instanceof Flip)
            return Flip;
        if (x instanceof Dict)
            return Dict;
        return List;
    }

    public static DataType getKdbType(final byte typeCode) {
        return typeCodeLookup.get(typeCode);
    }

    public byte getTypeCode() {
        return typeCode;
    }

    public boolean isAtom() {
        return isAtom;
    }

    public int getAtomicByteSize() {
        return atomicByteSize;
    }
}
