package org.jedkos.ceptic.common;

public class RegexHelper {

    public static String escape(String string) {
        return string.replaceAll("[\\<\\(\\[\\{\\\\\\^\\-\\=\\$\\!\\|\\]\\}\\)\\?\\*\\+\\.\\>]", "\\\\$0");
        //return string.replaceAll("[\\W]", "\\\\$0");
    }

    public static String escape(String string, int times) {
        for (int i = 0; i < times; i++) {
            string = escape(string);
        }
        return string;
    }

}
