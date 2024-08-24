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
import java.util.Arrays;

public class Example1 {
    public static void main(String[] args) {
        try (var kdbClient = new KdbClient("localhost", 5010, System.getProperty("user.name"), "mypassword")) {
//            final var result = kdbClient.send("2+3");
//            final var result = kdbClient.send("10 100 1000 * (1 2 3;4 5 6;7 8)\n");

            kdbClient.send("t:([] c1:`a`b`c; c2:10 20 30; c3:1.1 2.2 3.3)");
            final var result = kdbClient.send("select from t where c2>15,c1 in `b`c");

            System.out.println("Result is:");
            if (result != null && result.getClass().isArray()) {
                System.out.println(Arrays.deepToString(((Object[])result)));
            }
            else {
                System.out.println(result);
            }
        } catch (KdbException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
