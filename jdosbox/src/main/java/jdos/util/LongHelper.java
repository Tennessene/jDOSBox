package jdos.util;

public class LongHelper {
    public static boolean isLessThanUnsigned(long n1, long n2) {
      return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
    }
    public static boolean isLessThanOrEqualUnsigned(long n1, long n2) {
          return n1==n2 || ((n1 < n2) ^ ((n1 < 0) != (n2 < 0)));
    }

    public static long unsignedDiv(long l1, long l2) {
        long unsignedRes = 0L;
        if (l1 >= 0L) {
            if (l2 >= 0L)
                unsignedRes = l1 / l2;
        } else if (l2 >= 0L && (l1 -= (unsignedRes = ((l1 >>> 1) / l2) << 1) * l2) < 0L || l1 >= l2)
            unsignedRes++;
        return unsignedRes;
    }
    /*
     *  Licensed to the Apache Software Foundation (ASF) under one or more
     *  contributor license agreements.  See the NOTICE file distributed with
     *  this work for additional information regarding copyright ownership.
     *  The ASF licenses this file to You under the Apache License, Version 2.0
     *  (the "License"); you may not use this file except in compliance with
     *  the License.  You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     *  Unless required by applicable law or agreed to in writing, software
     *  distributed under the License is distributed on an "AS IS" BASIS,
     *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     *  See the License for the specific language governing permissions and
     *  limitations under the License.
     */

    /**
   * Divides an unsigned long a by an unsigned int b. It is supposed that the
   * most significant bit of b is set to 1, i.e. b < 0
   *
   * @param a the dividend
   * @param b the divisor
   * @return the long value containing the unsigned integer remainder in the
   *         left half and the unsigned integer quotient in the right half
   */
    public static long divideLongByInt(long a, int b) {
        long quot;
        long rem;
        long bLong = b & 0xffffffffL;

        if (a >= 0) {
            quot = (a / bLong);
            rem = (a % bLong);
        } else {
            /*
            * Make the dividend positive shifting it right by 1 bit then get the
            * quotient an remainder and correct them properly
            */
            long aPos = a >>> 1;
            long bPos = b >>> 1;
            quot = aPos / bPos;
            rem = aPos % bPos;
            // double the remainder and add 1 if a is odd
            rem = (rem << 1) + (a & 1);
            if ((b & 1) != 0) { // the divisor is odd
                if (quot <= rem) {
                    rem -= quot;
                } else {
                    if (quot - rem <= bLong) {
                        rem += bLong - quot;
                        quot -= 1;
                    } else {
                        rem += (bLong << 1) - quot;
                        quot -= 2;
                    }
                }
            }
        }
        if (quot>0xffffffffl) {
            throw new OverflowException();
        }
        return (rem << 32) | (quot & 0xffffffffL);
    }
}
