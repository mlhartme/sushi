/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.sushi.io;

import net.oneandone.sushi.util.Separator;

public enum OS {
    LINUX("Linux", "$", "", ':', "\n"),
    MAC(new String[] { "Mac" /* official Apple Jdks */, "Darwin" /* OpenJdk 7 for Mac OS BSD Port */ }, "$", "", ':', "\n"),
    WINDOWS("Windows", "%", "%", ';', "\r\n");

    private static OS detect() {
        String name;

        name = System.getProperty("os.name");
        for (OS os : values()) {
            for (String substring : os.substrings) {
                if (name.contains(substring)) {
                    return os;
                }
            }
        }
        throw new IllegalArgumentException("unknown os:" + name);
    }

    public static final OS CURRENT = detect();

    private final String[] substrings;
    private final String variablePrefix;
    private final String variableSuffix;

    public final Separator listSeparator;

    public final Separator lineSeparator;

    private OS(String substring, String variablePrefix, String variableSuffix, char listSeparatorChar, String lineSeparator) {
        this(new String[] { substring }, variablePrefix, variableSuffix, listSeparatorChar, lineSeparator);
    }

    private OS(String[] substrings, String variablePrefix, String variableSuffix,
            char listSeparatorChar, String lineSeparator) {
        this.substrings = substrings;
        this.variablePrefix = variablePrefix;
        this.variableSuffix = variableSuffix;
        this.listSeparator = Separator.on(listSeparatorChar);
        this.lineSeparator = Separator.on(lineSeparator);
    }

    public String lines(String ... lines) {
    	StringBuilder result;

    	result = new StringBuilder();
    	for (String line : lines) {
    		result.append(line);
    		result.append(lineSeparator.getSeparator());
    	}
    	return result.toString();
    }

    public String variable(String name) {
        return variablePrefix + name + variableSuffix;
    }
}
