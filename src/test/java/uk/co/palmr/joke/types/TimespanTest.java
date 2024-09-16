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

class TimespanTest {
    @Test
    public void testTimespanToString() {
        Timespan mon = new Timespan(22);
        assertEquals("00:00:00.000000022", mon.toString());
        mon = new Timespan(-22);
        assertEquals("-00:00:00.000000022", mon.toString());
        mon = new Timespan(0);
        assertEquals("00:00:00.000000000", mon.toString());
        mon = new Timespan(86400000000000L);
        assertEquals("1D00:00:00.000000000", mon.toString());
        mon = new Timespan(Long.MIN_VALUE);
        assertEquals("", mon.toString());
    }

    @Test
    public void testTimespanEquals() {
        Timespan mon1 = new Timespan(22);
        Timespan mon2 = new Timespan(22);
        Timespan mon3 = new Timespan();
        assertEquals(mon1, mon1);
        assertEquals(mon1, mon2);
        assertNotEquals(mon1, mon3);
        assertNotEquals(mon1, "test");
    }

    @Test
    public void testTimespanHashCode() {
        Timespan mon1 = new Timespan(22);
        Timespan mon2 = new Timespan(22);
        Timespan mon3 = new Timespan();
        assertEquals(mon1.hashCode(), mon1.hashCode());
        assertEquals(mon1.hashCode(), mon2.hashCode());
        assertNotEquals(mon1.hashCode(), mon3.hashCode());
    }

    @Test
    public void testTimespanCompareTo() {
        Timespan mon1 = new Timespan(22);
        Timespan mon2 = new Timespan(22);
        Timespan mon3 = new Timespan(1);
        Timespan mon4 = new Timespan(-1);
        assertEquals(0, mon1.compareTo(mon1));
        assertEquals(0, mon1.compareTo(mon2));
        assertEquals(1, mon1.compareTo(mon3));
        assertEquals(-1, mon4.compareTo(mon1));
    }
}
