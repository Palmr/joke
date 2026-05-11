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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
import uk.co.palmr.joke.messages.KdbMessageHeader;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class SerDesBenchmark extends KdbProtocol {

  private static final int ARRAY_SIZE = 1_000;

  private final ByteBuffer serializeBuffer = ByteBuffer.allocate(64 * 1024);

  private final long[] longArray = new long[ARRAY_SIZE];
  private final double[] doubleArray = new double[ARRAY_SIZE];
  private final String[] symbolArray = new String[ARRAY_SIZE];

  private ByteBuffer longArrayBuffer;
  private ByteBuffer doubleArrayBuffer;
  private ByteBuffer symbolArrayBuffer;

  public SerDesBenchmark() {
    super(StandardCharsets.ISO_8859_1, false);
  }

  @Setup
  public void setup() throws KdbException {
    for (int i = 0; i < ARRAY_SIZE; i++) {
      longArray[i] = i;
      doubleArray[i] = i * 1.1;
      symbolArray[i] = "SYM" + i;
    }

    longArrayBuffer = preserialized(longArray);
    doubleArrayBuffer = preserialized(doubleArray);
    symbolArrayBuffer = preserialized(symbolArray);
  }

  private ByteBuffer preserialized(Object obj) throws KdbException {
    var buf = ByteBuffer.allocate(64 * 1024);
    buf.position(KdbMessageHeader.SIZE);
    serialize(obj, buf);
    return buf;
  }

  // --- serialize ---

  @Benchmark
  public void serializeLongArray() throws KdbException {
    serializeBuffer.clear();
    serializeBuffer.position(KdbMessageHeader.SIZE);
    serialize(longArray, serializeBuffer);
  }

  @Benchmark
  public void serializeDoubleArray() throws KdbException {
    serializeBuffer.clear();
    serializeBuffer.position(KdbMessageHeader.SIZE);
    serialize(doubleArray, serializeBuffer);
  }

  @Benchmark
  public void serializeSymbolArray() throws KdbException {
    serializeBuffer.clear();
    serializeBuffer.position(KdbMessageHeader.SIZE);
    serialize(symbolArray, serializeBuffer);
  }

  // --- deserialize ---

  @Benchmark
  public Object deserializeLongArray() throws KdbException {
    longArrayBuffer.position(KdbMessageHeader.SIZE);
    return deserializeResponseMessage(longArrayBuffer);
  }

  @Benchmark
  public Object deserializeDoubleArray() throws KdbException {
    doubleArrayBuffer.position(KdbMessageHeader.SIZE);
    return deserializeResponseMessage(doubleArrayBuffer);
  }

  @Benchmark
  public Object deserializeSymbolArray() throws KdbException {
    symbolArrayBuffer.position(KdbMessageHeader.SIZE);
    return deserializeResponseMessage(symbolArrayBuffer);
  }
}
