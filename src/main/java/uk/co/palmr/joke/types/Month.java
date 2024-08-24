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
 * {@code Month} represents kdb+ month type, which is the number of months since Jan 2000.
 */
public class Month implements Comparable<Month> {
    /**
     * Number of months since Jan 2000
     */
    public int i;

    /**
     * Create a KDB+ representation of 'month' type from the q language
     * (a month value is the count of months since the beginning of the millennium.
     * Post-milieu is positive and pre is negative)
     *
     * @param x Number of months from millennium
     */
    public Month(int x) {
        i = x;
    }

    @Override
    public String toString() {
        int m = i + 24000;
        int y = m / 12;
        return i == NULL_INT ? "" : i2(y / 100) + i2(y % 100) + "-" + i2(1 + m % 12);
    }

    @Override
    public boolean equals(final Object o) {
        return ((o instanceof Month) && (((Month) o).i == i));
    }

    @Override
    public int hashCode() {
        return i;
    }

    @Override
    public int compareTo(Month m) {
        return i - m.i;
    }
}
