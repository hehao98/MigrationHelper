package edu.pku.migrationhelper.util;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by xuyul on 2020/2/21.
 */
public class MathUtils {

    private static final char[] hex = "0123456789abcdef".toCharArray();

    /**
     * translate from python version oscar.py unber(s)
     * Perl BER unpacking
     * Format definition: from http://perldoc.perl.org/functions/pack.html
     *     (see "w" template description)
     * BER is a way to pack several variable-length ints into one
     * binary string. Here we do the reverse
     * :param s: a binary string with packed values
     * :return: a list of unpacked values
     * >>> unber('\x00\x83M')
     * [0, 461]
     * >>> unber('\x83M\x96\x14')
     * [461, 2836]
     * >>> unber('\x99a\x89\x12')
     * [3297, 1170]
     */
    public static List<Long> unber(byte[] content) {
        List<Long> result = new LinkedList<>();
        long acc = 0;
        for (byte b : content) {
            acc = (acc << 7) + (b & 0x7f);
            if ((b & 0x80) == 0) {
                result.add(acc);
                acc = 0;
            }
        }
        return result;
    }

    public static String toHexString(byte[] bytes, int offset, int length) {
        if (null == bytes) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder(length << 1);

            for(int i = 0; i < length; ++i) {
                sb.append(hex[(bytes[offset + i] & 240) >> 4]).append(hex[bytes[offset + i] & 15]);
            }

            return sb.toString();
        }
    }

}
