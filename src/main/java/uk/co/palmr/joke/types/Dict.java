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

/**
 * {@code Dict} represents the kdb+ dictionary type, which is a mapping from a key list to a value list.
 * The two lists must have the same count.
 * An introduction can be found at <a href="https://code.kx.com/q4m3/5_Dictionaries/">https://code.kx.com/q4m3/5_Dictionaries/</a>
 */
public class Dict {
    /**
     * Dict keys
     */
    public Object x;
    /**
     * Dict values
     */
    public Object y;

    /**
     * Create a representation of the KDB+ dictionary type, which is a
     * mapping between keys and values
     *
     * @param keys Keys to store. Should be an array type when using multiple values.
     * @param vals Values to store. Index of each value should match the corresponding associated key.
     *             Should be an array type when using multiple values.
     */
    public Dict(Object keys, Object vals) {
        x = keys;
        y = vals;
    }
}
