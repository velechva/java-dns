package org.jns.util;

public class BytesUtil {
    public static int bitAtPosition(byte b, int pos) {
        return b >> pos & 0x1;
    }

    public static int parseUInt16(byte[] b, int begin) {
        return b[begin + 1] & 0xFF | (b[begin] & 0xFF) << 8;
    }
}
