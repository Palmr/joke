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

import java.util.Arrays;

/**
 * {@code Flip} represents a kdb+ table (an array of column names, and an array of arrays containing the column data).
 * q tables are column-oriented, in contrast to the row-oriented tables in relational databases.
 * An introduction can be found at <a href="https://code.kx.com/q4m3/8_Tables/">https://code.kx.com/q4m3/8_Tables/</a>
 */
public class Flip {
    /**
     * Array of column names.
     */
    public String[] columnNames;
    /**
     * Array of arrays of the column values.
     */
    public Object[] columns;

    /**
     * Create a Flip (KDB+ table) from the values stored in a Dict.
     *
     * @param dict Values stored in the dict should be an array of Strings for the column names (keys), with an
     *             array of arrays for the column values
     */
    public Flip(Dict dict) {
        columnNames = (String[]) dict.x;
        columns = (Object[]) dict.y;
    }

    /**
     * Create a Flip (KDB+ table) from array of column names and array of arrays of the column values.
     *
     * @param columnNames Array of column names
     * @param columns Array of arrays of the column values
     */
    public Flip(String[] columnNames, Object[] columns) {
        this.columnNames = columnNames;
        this.columns = columns;
    }

    /**
     * Returns the column values given the column name
     *
     * @param s The column name
     * @return The value(s) associated with the column name which can be casted to an array of objects.
     */
    public Object at(String s) {
        return columns[find(columnNames, s)];
    }

    /**
     * Finds index of string in an array
     * @param x String array to search
     * @param y The String to locate in the array
     * @return The index at which the String resides
     */
    private static int find(String[] x,String y){
        int i=0;
        while(i<x.length&&!x[i].equals(y))
            ++i;
        return i;
    }

    @Override
    public String toString() {
        return "Flip{" +
                "columnNames=" + Arrays.toString(columnNames) +
                ", columns=" + Arrays.deepToString(columns) +
                '}';
    }
}
