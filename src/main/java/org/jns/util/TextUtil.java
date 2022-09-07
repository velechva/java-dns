package org.jns.util;

import java.net.IDN;

public class TextUtil {
    public static String unicodeToPunyCode(String str) {
        return IDN.toASCII(str, IDN.ALLOW_UNASSIGNED);
    }

    public static String punycodeToUnicode(String str) {
        return IDN.toUnicode(str, IDN.ALLOW_UNASSIGNED);
    }

    public static String aRecordToString(byte[] buf, int begin, int length) {
        String str    = "";
        boolean first = true;

        for (int i = 0; i < length; i++) {
            byte b = buf[begin + i];

            if (!first) {
                str += ".";
            }
            str += String.valueOf(b & 0xFF);

            first = false;
        }

        return str;
    }
}
