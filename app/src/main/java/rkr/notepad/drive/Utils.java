/**
 * Copyright 2015 Raimondas Rimkus
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
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