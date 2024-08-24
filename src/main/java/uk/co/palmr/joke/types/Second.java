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

import static uk.co.palmr.joke.KdbProtocol.NULL_INT;
import static uk.co.palmr.joke.NumberFormatter.i2;

/**
 * {@code Second} represents kdb+ second type, which is a point in time represented in seconds since midnight.
 */
public class Second implements Comparable<Second> {
    /**
     * Number of seconds since midnight.
     */
    public int i;

    /**
     * Create a KDB+ representation of 'second' type from the q language
     * (point in time represented in seconds since midnight)
     *
     * @param x Number of seconds since midnight
     */
    public Second(int x) {
        i = x;
    }

    @Override
    public String toString() {
        return i == NULL_INT ? "" : new Minute(i / 60).toString() + ':' + i2(i % 60);
    }

    @Override
    public boolean equals(final Object o) {
        return ((o instanceof Second) && (((Second) o).i == i));
    }

    @Override
    public int hashCode() {
        return i;
    }

    @Override
    public int compareTo(Second s) {
        return i - s.i;
    }
}
