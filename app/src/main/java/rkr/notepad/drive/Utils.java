package rkr.notepad.drive;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class Utils {
    public static String readFromInputStream(InputStream is)  {
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int c;
        try {
            while ((c = bis.read()) != -1) {
                buf.write((byte) c);
            }
            return buf.toString("UTF-8");
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
