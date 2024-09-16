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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SecondTest {
    @Test
    public void testSecondToString() {
        Second mon = new Second(22);
        assertEquals("00:00:22", mon.toString());
        mon = new Second(Integer.MIN_VALUE);
        assertEquals("", mon.toString());
    }

    @Test
    public void testSecondEquals() {
        Second mon1 = new Second(22);
        Second mon2 = new Second(22);
        Second mon3 = new Second(1);
        assertEquals(mon1, mon1);
        assertEquals(mon1, mon2);
        assertNotEquals(mon1, mon3);
        assertNotEquals(mon1, "test");
    }

    @Test
    public void testSecondHashCode() {
        Second mon1 = new Second(22);
        Second mon2 = new Second(22);
        Second mon3 = new Second(1);
        assertEquals(mon1.hashCode(), mon1.hashCode());
        assertEquals(mon1.hashCode(), mon2.hashCode());
        assertNotEquals(mon1.hashCode(), mon3.hashCode());
    }

    @Test
    public void testSecondCompareTo() {
        Second mon1 = new Second(22);
        Second mon2 = new Second(22);
        Second mon3 = new Second(1);
        assertEquals(0, mon1.compareTo(mon1));
        assertEquals(0, mon1.compareTo(mon2));
        assertEquals(21, mon1.compareTo(mon3));
    }

}
