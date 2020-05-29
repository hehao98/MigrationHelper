package anonymous.migrationhelper.util;

import java.util.Arrays;

/**
 * Created by xxx on 2020/2/22.
 */
public class LZFUtils {

    public static class LZFException extends Exception {
        public LZFException() {
        }

        public LZFException(String message) {
            super(message);
        }

        public LZFException(String message, Throwable cause) {
            super(message, cause);
        }

        public LZFException(Throwable cause) {
            super(cause);
        }

        public LZFException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    public static byte[] lzfDecompressFromPerl(byte[] content) throws LZFException {
        int len = content.length;
        if (len == 0) return content;
        byte firstByte = content[0];

        if (firstByte == 0x00) {
            return Arrays.copyOfRange(content, 1, len);
        }

        int start;
        int resSize;
        if ((firstByte & 0x80) == 0 && len >= 1) {
            start = 1;
            resSize = content[0] & 0xff;
        } else if ((firstByte & 0x20) == 0 && len >= 2) {
            start = 2;
            resSize = content[0] & 0x1f;
            resSize = (resSize << 6) | content[1] & 0x3f;
        } else if ((firstByte & 0x10) == 0 && len >= 3) {
            start = 3;
            resSize = content[0] & 0x0f;
            resSize = (resSize << 6) | content[1] & 0x3f;
            resSize = (resSize << 6) | content[2] & 0x3f;
        } else if ((firstByte & 0x08) == 0 && len >= 4) {
            start = 4;
            resSize = content[0] & 0x07;
            resSize = (resSize << 6) | content[1] & 0x3f;
            resSize = (resSize << 6) | content[2] & 0x3f;
            resSize = (resSize << 6) | content[3] & 0x3f;
        } else if ((firstByte & 0x04) == 0 && len >= 5) {
            start = 5;
            resSize = content[0] & 0x03;
            resSize = (resSize << 6) | content[1] & 0x3f;
            resSize = (resSize << 6) | content[2] & 0x3f;
            resSize = (resSize << 6) | content[3] & 0x3f;
            resSize = (resSize << 6) | content[4] & 0x3f;
        } else if ((firstByte & 0x02) == 0 && len >= 6) {
            start = 6;
            resSize = content[0] & 0x01;
            resSize = (resSize << 6) | content[1] & 0x3f;
            resSize = (resSize << 6) | content[2] & 0x3f;
            resSize = (resSize << 6) | content[3] & 0x3f;
            resSize = (resSize << 6) | content[4] & 0x3f;
            resSize = (resSize << 6) | content[5] & 0x3f;
        } else {
            throw new LZFException ("compressed data corrupted (invalid length)");
        }

        if(resSize == 0) {
            throw new LZFException("compressed data corrupted (invalid length)");
        }

        byte[] result = new byte[resSize];

        int res = lzfDecompress(content, start, len - start, result);
        if(res != resSize) {
            throw new LZFException("compressed data corrupted (size mismatch)");
        }
        return result;
    }

    public static int lzfDecompress(byte[] in_data, int in_start, int in_len, byte[] out_data) throws LZFException {
        int out_len = out_data.length;
        int ip = in_start;
        int op = 0;
        int in_end = ip + in_len;
        int out_end = op + out_len;
        do {
            int ctrl = 0xff & in_data[ip++];
            if(ctrl < (1 << 5)) {
                ctrl++;
                if (op + ctrl > out_end) {
                    throw new LZFException("E2BIG");
                }
                if (ip + ctrl > in_end) {
                    throw new LZFException("EINVAL");
                }
                lzfMovsb(out_data, op, in_data, ip, ctrl);
                ip += ctrl;
                op += ctrl;
            } else {
                int len = ctrl >> 5;
                int ref = op - ((ctrl & 0x1f) << 8) - 1;
                if (ip >= in_end) {
                    throw new LZFException("EINVAL");
                }
                if (len == 7)
                {
                    len += 0xff & in_data[ip++];
                    if (ip >= in_end)
                    {
                        throw new LZFException("EINVAL");
                    }
                }

                ref -= 0xff & in_data[ip++];

                if (op + len + 2 > out_end)
                {
                    throw new LZFException("E2BIG");
                }

                if (ref < 0)
                {
                    throw new LZFException("EINVAL");
                }

                len += 2;
                lzfMovsb (out_data, op, out_data, ref, len);
                op += len;
            }
        } while (ip < in_end);

        return op;
    }

    public static void lzfMovsb(byte[] out_data, int op, byte[] in_data, int ip, int len) throws LZFException {
        if(len < 0) throw new LZFException("lzfMovsb len < 0");
        while(len-- > 0) {
            out_data[op++] = in_data[ip++];
        }
    }
}
