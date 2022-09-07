package org.jns.model;

public class Question {
    public String name;
    public byte[] type;
    public byte[] clazz;

    public String toString() {
        String typeStr;

        if (type[0] == 0x00 && type[1] == 0x01) { typeStr = "A"; }
        if (type[0] == 0x00 && type[1] == 0x05) { typeStr = "CNAME"; }
        else                                    { typeStr = "U"; }

        String classStr;

        if (clazz[0] == 0x00 && clazz[1] == 0x00) { classStr = "IP"; }
        else                                      { classStr = "U"; }

        return String.format("%s (%s) -> %s", typeStr, classStr, name);
    }
}
