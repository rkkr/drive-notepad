package rkr.notepad.drive;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class Utils {
    public static String readFromInputStream(InputStream is)  {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder builder = new StringBuilder();
        int c;
        try {
            while ((c = reader.read()) != -1) {
                builder.append((char)c);
            }
            reader.close();
            return builder.toString();
        }
        catch (java.io.IOException e) {
            return null;
        }
    }

    public static boolean writeToOutputStream(OutputStream out, String contents)  {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        try {
            writer.write(contents);
            writer.close();
            return true;
        }
        catch (java.io.IOException e) {
            return false;
        }
    }
}
