package org.jns.util;

import org.jns.cli.JNSI;
import org.jns.ex.ApplicationException;

import java.io.IOException;
import java.io.InputStream;

public class IOUtil {
    public static String readFileFromResources(String resourceFilePath) {
        try (InputStream is = JNSI.class.getClassLoader().getResourceAsStream(resourceFilePath)) {
            if (is == null) {
                throw new ApplicationException("Could not read " + resourceFilePath);
            }
            return new String(is.readAllBytes());
        }
        catch (IOException ex) {
            throw new ApplicationException("Could not read " + resourceFilePath);
        }
    }
}
