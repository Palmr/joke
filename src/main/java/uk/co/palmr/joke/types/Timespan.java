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

import static uk.co.palmr.joke.KdbProtocol.NULL_LONG;
import static uk.co.palmr.joke.NumberFormatter.i2;
import static uk.co.palmr.joke.NumberFormatter.i9;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * {@code Timespan} represents kdb+ timestamp type, which is a point in time represented in
 * nanoseconds since midnight.
 */
public record Timespan(long nanosSinceMidnight) implements Comparable<Timespan> {
  /** Constructs {@code Timespan} using current time since midnight and default timezone. */
  public Timespan() {
    this(TimeZone.getDefault());
  }

  /**
   * Constructs {@code Timespan} using current time since midnight and the supplied timezone.
   *
   * @param tz {@code TimeZone} to use for deriving midnight.
   */
  public Timespan(TimeZone tz) {
    this(nanosFromMidnight(tz));
  }

  private static long nanosFromMidnight(TimeZone tz) {
    Calendar c = Calendar.getInstance(tz);
    long now = c.getTimeInMillis();
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return (now - c.getTimeInMillis()) * 1000000L;
  }

  @Override
  public String toString() {
    if (nanosSinceMidnight == NULL_LONG) return "";
    String s = nanosSinceMidnight < 0 ? "-" : "";
    long jj = nanosSinceMidnight < 0 ? -nanosSinceMidnight : nanosSinceMidnight;
    int d = ((int) (jj / 86400000000000L));
    if (d != 0) s += d + "D";
    return s
        + i2((int) ((jj % 86400000000000L) / 3600000000000L))
        + ":"
        + i2((int) ((jj % 3600000000000L) / 60000000000L))
        + ":"
        + i2((int) ((jj % 60000000000L) / 1000000000L))
        + "."
        + i9((int) (jj % 1000000000L));
  }

  @Override
  public int compareTo(Timespan t) {
    return Long.compare(nanosSinceMidnight, t.nanosSinceMidnight);
  }
}
