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
package uk.co.palmr.joke;

import java.text.DecimalFormat;

public class NumberFormatter {
    /**
     * Creates a string from int with left padding of 0s, if less than 2 digits
     * @param i Integer to convert to string
     * @return String representation of int with zero padding
     */
    public static String i2(int i){
        return new DecimalFormat("00").format(i);
    }

    /**
     * Creates a string from int with left padding of 0s, if less than 9 digits
     * @param i Integer to convert to string
     * @return String representation of int with zero padding
     */
    public static String i9(int i){
        return new DecimalFormat("000000000").format(i);
    }
}
