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

import static org.junit.jupiter.api.Assertions.*;

class FlipTest {
    @Test
    public void testFlipConstructor() {
        String[] x = new String[]{"Key"};
        String[][] y = new String[][]{{"Value1", "Value2", "Value3"}};
        Dict dict = new Dict(x, y);
        Flip flip = new Flip(dict);
        assertArrayEquals(x, flip.columnNames);
        assertArrayEquals(y, flip.columns);
    }

    @Test
    public void testFlipColumnPosition() {
        String[] x = new String[]{"Key"};
        String[][] y = new String[][]{{"Value1", "Value2", "Value3"}};
        Dict dict = new Dict(x, y);
        Flip flip = new Flip(dict);
        assertEquals(y[0], flip.at("Key"));
    }

    @Test
    public void testFlipUnknownColumn() {
        String[] x = new String[]{"Key"};
        String[][] y = new String[][]{{"Value1", "Value2", "Value3"}};
        Dict dict = new Dict(x, y);
        Flip flip = new Flip(dict);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> flip.at("RUBBISH"));
    }
}
