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
package net.oneandone.sushi.util;

import java.util.Map;

public class Substitution {
    public static Substitution ant() {
        return new Substitution("${", "}", '\\');        
    }
    public static Substitution path() {
        return new Substitution("__", "__", '\\');     
    }
    
	private final String prefix;
	private final String suffix;
	/** do not match if this character prefixes the prefix */ 
    private final char escape;
	
	public Substitution(String prefix, String suffix, char escape) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.escape = escape;
	}
	
	public String apply(String content, Map<String, String> variables) throws SubstitutionException {
		StringBuilder builder;
		int start;
		int end;
		int last;
		String var;
		String replaced;
		
		builder = new StringBuilder();
		last = 0;
		while (true) {
			start = content.indexOf(prefix, last);
			if (start == -1) {
				if (last == 0) {
					return content;
				} else {
					builder.append(content.substring(last));
					return builder.toString();
				}
			}
			end = content.indexOf(suffix, start + prefix.length());
			if (start > 0 && content.charAt(start - 1) == escape) {
			    builder.append(content.substring(last, start - 1));
			    builder.append(prefix);
			    last = start + prefix.length();
			} else {
			    if (end == -1) {
			        throw new SubstitutionException("missing end marker");
			    } 
			    var = content.substring(start + prefix.length(), end);
			    replaced = variables.get(var);
			    if (replaced == null) {
			        throw new SubstitutionException("undefined variable: " + var);
			    }
			    builder.append(content.substring(last, start));
			    builder.append(replaced);
			    last = end + suffix.length();
			}
		}
	}
}
