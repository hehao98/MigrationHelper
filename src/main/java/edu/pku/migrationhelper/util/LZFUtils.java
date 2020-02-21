package edu.pku.migrationhelper.util;

import com.ning.compress.lzf.LZFDecoder;
import com.ning.compress.lzf.LZFException;

import java.util.Arrays;

/**
 * Created by xuyul on 2020/2/22.
 */
public class LZFUtils {

    // TODO fix bug
    public static byte[] decompressFromPerl(byte[] content) throws LZFException {
        if(content.length == 0) return content;
        if(content[0] == 0x00) {
            return Arrays.copyOfRange(content, 1, content.length);
        }
        int lower = content[0];
        int csize = content.length;
        int start = 1;
        int mask = 0x80;
        while (mask != 0 && csize > start && (lower & mask) != 0){
            mask >>= 1 + ((mask == 0x80) ? 1 : 0);
            start += 1;
        }
        if (mask == 0 || csize < start) {
            throw new LZFException ("LZF compressed data header is corrupted");
        }
        return LZFDecoder.decode(Arrays.copyOfRange(content, start, content.length));
    }

}
