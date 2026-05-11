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

/** {@code Month} represents kdb+ month type, which is the number of months since Jan 2000. */
public record Month(int monthsSinceJan2000) implements Comparable<Month> {
  public static final int MONTHS_IN_YEAR = 12;
  public static final int MONTHS_FROM_0_TO_JAN_2000 = 2000 * MONTHS_IN_YEAR;

  @Override
  public String toString() {
    int m = monthsSinceJan2000 + MONTHS_FROM_0_TO_JAN_2000;
    int y = m / MONTHS_IN_YEAR;
    return monthsSinceJan2000 == NULL_INT
        ? ""
        : i2(y / 100) + i2(y % 100) + "-" + i2(1 + m % MONTHS_IN_YEAR);
  }

  @Override
  public int compareTo(Month m) {
    return Integer.compare(monthsSinceJan2000, m.monthsSinceJan2000);
  }
}
