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
package uk.co.palmr.joke;

import com.kx.c;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class OfficialDriverBenchmark {

  private static final int ARRAY_SIZE = 1_000;

  private c conn;

  private final long[] longArray = new long[ARRAY_SIZE];
  private final double[] doubleArray = new double[ARRAY_SIZE];
  private final String[] symbolArray = new String[ARRAY_SIZE];

  private byte[] longArrayBytes;
  private byte[] doubleArrayBytes;
  private byte[] symbolArrayBytes;

  @Setup
  public void setup() throws IOException {
    conn = new c();

    for (int i = 0; i < ARRAY_SIZE; i++) {
      longArray[i] = i;
      doubleArray[i] = i * 1.1;
      symbolArray[i] = "SYM" + i;
    }

    longArrayBytes = conn.serialize(1, longArray, false);
    doubleArrayBytes = conn.serialize(1, doubleArray, false);
    symbolArrayBytes = conn.serialize(1, symbolArray, false);
  }

  // --- serialize ---

  @Benchmark
  public byte[] serializeLongArray() throws IOException {
    return conn.serialize(1, longArray, false);
  }

  @Benchmark
  public byte[] serializeDoubleArray() throws IOException {
    return conn.serialize(1, doubleArray, false);
  }

  @Benchmark
  public byte[] serializeSymbolArray() throws IOException {
    return conn.serialize(1, symbolArray, false);
  }

  // --- deserialize ---

  @Benchmark
  public Object deserializeLongArray() throws IOException, c.KException {
    return conn.deserialize(longArrayBytes);
  }

  @Benchmark
  public Object deserializeDoubleArray() throws IOException, c.KException {
    return conn.deserialize(doubleArrayBytes);
  }

  @Benchmark
  public Object deserializeSymbolArray() throws IOException, c.KException {
    return conn.deserialize(symbolArrayBytes);
  }
}
