/*
 * Copyright (c) 1998-2017 Kx Systems In
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MonthTest {

    @Test
    public void testMonthToString() {
        final Month month = new Month(22);
        assertEquals("2001-11", month.toString());
    }

    @Test
    public void testNullMonthToString() {
        final Month month = new Month(Integer.MIN_VALUE);
        assertEquals("", month.toString());
    }

    @Test
    public void testMonthEquals() {
        Month mon1 = new Month(22);
        Month mon2 = new Month(22);
        Month mon3 = new Month(1);
        assertEquals(mon1, mon1);
        assertEquals(mon1, mon2);
        assertNotEquals(mon1, mon3);
        assertNotEquals(mon1, "test");
    }

    @Test
    public void testMonthHashCode() {
        Month mon1 = new Month(22);
        Month mon2 = new Month(22);
        Month mon3 = new Month(1);
        assertEquals(mon1.hashCode(), mon1.hashCode());
        assertEquals(mon1.hashCode(), mon2.hashCode());
        assertNotEquals(mon1.hashCode(), mon3.hashCode());
    }

    @Test
    public void testMonthCompareTo() {
        Month mon1 = new Month(22);
        Month mon2 = new Month(22);
        Month mon3 = new Month(1);
        assertEquals(0, mon1.compareTo(mon1));
        assertEquals(0, mon1.compareTo(mon2));
        assertEquals(21, mon1.compareTo(mon3));
    }
}
