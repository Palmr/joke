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
package uk.co.palmr.joke.types;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataTypeTest {
    @Test
    public void testGetAtomType() {
        assertEquals(-1, DataType.getKdbType(Boolean.FALSE).getTypeCode());
        assertEquals(-2, DataType.getKdbType(new UUID(0, 0)).getTypeCode());
        assertEquals(-4, DataType.getKdbType(Byte.valueOf("1")).getTypeCode());
        assertEquals(-5, DataType.getKdbType(Short.valueOf("1")).getTypeCode());
        assertEquals(-6, DataType.getKdbType(1111).getTypeCode());
        assertEquals(-7, DataType.getKdbType(1111L).getTypeCode());
        assertEquals(-8, DataType.getKdbType(1.2f).getTypeCode());
        assertEquals(-9, DataType.getKdbType(1.2).getTypeCode());
        assertEquals(-10, DataType.getKdbType(' ').getTypeCode());
        assertEquals(-11, DataType.getKdbType("").getTypeCode());
        assertEquals(-14, DataType.getKdbType(LocalDate.MIN).getTypeCode());
        assertEquals(-19, DataType.getKdbType(LocalTime.MIN).getTypeCode());
        assertEquals(-12, DataType.getKdbType(Instant.MIN).getTypeCode());
        assertEquals(-15, DataType.getKdbType(LocalDateTime.MIN).getTypeCode());
        assertEquals(-16, DataType.getKdbType(new Timespan(Long.MIN_VALUE)).getTypeCode());
        assertEquals(-13, DataType.getKdbType(new Month(Integer.MIN_VALUE)).getTypeCode());
        assertEquals(-17, DataType.getKdbType(new Minute(Integer.MIN_VALUE)).getTypeCode());
        assertEquals(-18, DataType.getKdbType(new Second(Integer.MIN_VALUE)).getTypeCode());
    }

    @Test
    public void testGetType() {
        assertEquals(1, DataType.getKdbType(new boolean[2]).getTypeCode());
        assertEquals(2, DataType.getKdbType(new UUID[2]).getTypeCode());
        assertEquals(4, DataType.getKdbType(new byte[2]).getTypeCode());
        assertEquals(5, DataType.getKdbType(new short[2]).getTypeCode());
        assertEquals(6, DataType.getKdbType(new int[2]).getTypeCode());
        assertEquals(7, DataType.getKdbType(new long[2]).getTypeCode());
        assertEquals(8, DataType.getKdbType(new float[2]).getTypeCode());
        assertEquals(9, DataType.getKdbType(new double[2]).getTypeCode());
        assertEquals(10, DataType.getKdbType(new char[2]).getTypeCode());
        assertEquals(11, DataType.getKdbType(new String[2]).getTypeCode());
        assertEquals(14, DataType.getKdbType(new LocalDate[2]).getTypeCode());
        assertEquals(19, DataType.getKdbType(new LocalTime[2]).getTypeCode());
        assertEquals(12, DataType.getKdbType(new Instant[2]).getTypeCode());
        assertEquals(15, DataType.getKdbType(new LocalDateTime[2]).getTypeCode());
        assertEquals(16, DataType.getKdbType(new Timespan[2]).getTypeCode());
        assertEquals(13, DataType.getKdbType(new Month[2]).getTypeCode());
        assertEquals(17, DataType.getKdbType(new Minute[2]).getTypeCode());
        assertEquals(18, DataType.getKdbType(new Second[2]).getTypeCode());
        Dict dict = new Dict(new String[]{"Key"}, new String[][]{{"Value1", "Value2", "Value3"}});
        assertEquals(98, DataType.getKdbType(new Flip(dict)).getTypeCode());
        assertEquals(99, DataType.getKdbType(dict).getTypeCode());
    }

    @Test
    public void testGetUnknownType() {
        assertEquals(0, DataType.getKdbType(new StringBuffer()).getTypeCode());
    }

    @Test
    void testTypeByteSize() {
        final int[] bytesPerTypeId = {0, 1, 16, 0, 1, 2, 4, 8, 4, 8, 1, 0, 8, 4, 4, 8, 8, 4, 4, 4};
        for (int typeId = 0; typeId < bytesPerTypeId.length; typeId++) {
            final var kdbType = DataType.getKdbType((byte) typeId);
            if (kdbType != null) {
                assertEquals(bytesPerTypeId[typeId], kdbType.getAtomicByteSize(), "Byte size mismatch for type: " + kdbType);
            }
        }
    }
}
