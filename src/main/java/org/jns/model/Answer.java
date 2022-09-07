package org.jns.model;

public class Answer {
    public byte[] type;  // 0x0001 -> A record, 0x0005 -> CNAME
    public byte[] clazz; // 0x0001 -> Internet address
    public String questionName;
    public String text;

    public String toString() {
        String typeStr;

        if (type[0] == 0x00 && type[1] == 0x01) { typeStr = "A"; }
        if (type[0] == 0x00 && type[1] == 0x05) { typeStr = "CNAME"; }
        else                                    { typeStr = "U"; }

        String classStr;

        if (clazz[0] == 0x00 && clazz[1] == 0x00) { classStr = "IP"; }
        else                                      { classStr = "U"; }

        return String.format("%s: %s (%s) -> %s", questionName, typeStr, classStr, text);
    }
}
