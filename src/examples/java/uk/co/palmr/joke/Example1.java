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

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import uk.co.palmr.joke.types.Dict;
import uk.co.palmr.joke.types.Flip;

/**
 * Demonstrates connecting to KDB+ and working with the main result types.
 *
 * <p>Requires a running KDB+ process: {@code q -p 5010}
 */
public class Example1 {

  public static void main(String[] args) {
    // No password needed for a default q -p 5010 instance
    try (var client = new KdbClient("localhost", 5010, System.getProperty("user.name"), "")) {
      atomExamples(client);
      arrayExamples(client);
      tableExamples(client);
      dictExample(client);
      errorHandlingExample(client);
    } catch (KdbException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void atomExamples(KdbClient client) throws KdbException, IOException {
    System.out.println("=== Atoms ===");

    long intResult = (long) client.send("2 + 3");
    System.out.println("2 + 3 = " + intResult);

    double floatResult = (double) client.send("acos -1f");
    System.out.printf("acos -1f = %.6f%n", floatResult);

    boolean boolResult = (boolean) client.send("10 > 3");
    System.out.println("10 > 3 = " + boolResult);

    // KDB+ symbols deserialize to String
    String symbol = (String) client.send("`NYSE");
    System.out.println("symbol = " + symbol);

    // KDB+ char vectors (strings) deserialize to char[]
    char[] chars = (char[]) client.send("\"hello from kdb+\"");
    System.out.println("char vector = " + new String(chars));

    // KDB+ date deserializes to LocalDate
    LocalDate today = (LocalDate) client.send(".z.d");
    System.out.println("today = " + today);
  }

  private static void arrayExamples(KdbClient client) throws KdbException, IOException {
    System.out.println("\n=== Arrays ===");

    long[] longs = (long[]) client.send("1 2 3 4 5");
    System.out.println("long list = " + Arrays.toString(longs));

    double[] doubles = (double[]) client.send("1.1 2.2 3.3");
    System.out.println("float list = " + Arrays.toString(doubles));

    boolean[] bools = (boolean[]) client.send("0101b");
    System.out.println("bool list = " + Arrays.toString(bools));

    // Symbol lists deserialize to String[]
    String[] symbols = (String[]) client.send("`AAPL`GOOG`MSFT`AMZN");
    System.out.println("symbol list = " + Arrays.toString(symbols));
  }

  private static void tableExamples(KdbClient client) throws KdbException, IOException {
    System.out.println("\n=== Table (Flip) ===");

    client.send(
        "trades:([] sym:`AAPL`GOOG`AAPL`MSFT`GOOG;"
            + " side:`buy`sell`buy`buy`sell;"
            + " qty:100 200 150 300 250;"
            + " price:182.5 141.8 183.1 415.2 142.3)");

    // Full table scan
    Flip trades = (Flip) client.send("trades");
    String[] syms = (String[]) trades.at("sym");
    long[] qtys = (long[]) trades.at("qty");
    double[] prices = (double[]) trades.at("price");
    System.out.println("All trades:");
    for (int i = 0; i < syms.length; i++) {
      System.out.printf("  %-6s qty=%3d  price=%.2f%n", syms[i], qtys[i], prices[i]);
    }

    // Filtered query
    Flip buys = (Flip) client.send("select from trades where side=`buy");
    String[] buySyms = (String[]) buys.at("sym");
    System.out.println("Buy-side syms: " + Arrays.toString(buySyms));

    // Aggregation — 0! removes the key so the result is a plain Flip
    Flip vwap = (Flip) client.send("0!select vwap:qty wavg price by sym from trades");
    String[] vwapSyms = (String[]) vwap.at("sym");
    double[] vwapPrices = (double[]) vwap.at("vwap");
    System.out.println("VWAP by sym:");
    for (int i = 0; i < vwapSyms.length; i++) {
      System.out.printf("  %-6s %.4f%n", vwapSyms[i], vwapPrices[i]);
    }

    client.send("delete trades from `.");
  }

  private static void dictExample(KdbClient client) throws KdbException, IOException {
    System.out.println("\n=== Dictionary (Dict) ===");

    Dict stats = (Dict) client.send("`open`high`low`close!182.0 185.5 181.2 183.4");
    String[] keys = (String[]) stats.keys();
    double[] values = (double[]) stats.values();
    System.out.println("OHLC:");
    for (int i = 0; i < keys.length; i++) {
      System.out.printf("  %-6s %.2f%n", keys[i], values[i]);
    }
  }

  private static void errorHandlingExample(KdbClient client) throws IOException {
    System.out.println("\n=== Error Handling ===");

    // KDB+ evaluation errors surface as KdbException
    try {
      client.send("1 + `oops");
    } catch (KdbException e) {
      System.out.println("type error: " + e.getMessage());
    }

    try {
      client.send("select from nosuchtable");
    } catch (KdbException e) {
      System.out.println("undefined error: " + e.getMessage());
    }
  }
}
