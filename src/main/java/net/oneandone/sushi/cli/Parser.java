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
package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.SimpleTypeException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A command line parser defined by option, value and child annotations taken from the defining class
 * used to create the parser. The resulting syntax is
 * 
 * line = option* child
 *      | option* value1 ... valueN value0*
 *      
 * Running the parser configures an instance of the defining class. It takes a command line and an 
 * instance of the defining class, options and values perform side effects, children create 
 * sub-instances.
 */
public class Parser {
    public static Parser create(Schema metadata, Class<?> cl) {
        Parser parser;
        Option option;
        Value value;
        Remaining remaining;
        Child child;
        
        parser = new Parser(metadata);
        for (Method m : cl.getMethods()) {
            option = m.getAnnotation(Option.class);
            if (option != null) {
                parser.addOption(option.value(), ArgumentMethod.create(option.value(), metadata, m));
            }
            value = m.getAnnotation(Value.class);
            if (value != null) {
                parser.addValue(value.position(), ArgumentMethod.create(value.name(), metadata, m));
            }
            remaining = m.getAnnotation(Remaining.class);
            if (remaining != null) {
                parser.addValue(0, ArgumentMethod.create(remaining.name(), metadata, m));
            }
            child = m.getAnnotation(Child.class);
            if (child != null) {
                parser.addChild(new ChildMethod(child.value(), m));
            }
        }
        while (!Object.class.equals(cl)) {
            for (Field f: cl.getDeclaredFields()) {
                option = f.getAnnotation(Option.class);
                if (option != null) {
                    parser.addOption(option.value(), ArgumentField.create(option.value(), metadata, f));
                }
                value = f.getAnnotation(Value.class);
                if (value != null) {
                    parser.addValue(value.position(), ArgumentField.create(value.name(), metadata, f));
                }
                remaining = f.getAnnotation(Remaining.class);
                if (remaining != null) {
                    parser.addValue(0, ArgumentField.create(remaining.name(), metadata, f));
                }
            }
            cl = cl.getSuperclass();
        }
        return parser;
    }

    //--
    
    private final Schema metadata;
    private final Map<String, Argument> options;
    private final List<Argument> values; // and remaining at index 0
    private final Map<String, ChildMethod> children;
    
    public Parser(Schema metadata) {
        this.metadata = metadata;
        this.options = new HashMap<String, Argument>();
        this.values = new ArrayList<Argument>();
        values.add(null);
        this.children = new HashMap<String, ChildMethod>();
    }

    public void addOption(String name, Argument arg) {
        if (options.put(name, arg) != null) {
            throw new IllegalArgumentException("duplicate option: " + name);
        }
    }
    
    public void addValue(int position, Argument arg) {
        while (position >= values.size()) {
            values.add(null);
        }
        if (values.get(position) != null) {
            throw new IllegalArgumentException("duplicate argument for position " + position);
        }
        values.set(position, arg);
    }

    public void addChild(ChildMethod factory) {
        if (children.put(factory.getName(), factory) != null) {
            throw new IllegalArgumentException("duplicate child command " + factory.getName());
        }
    }
    
    private static boolean isBoolean(Argument arg) {
        return arg.getType().getType().equals(Boolean.class);
    }

    public void checkValues() {
        int i;
        int max;
        
        max = values.size();
        for (i = 0; i < max; i++) {
            if (values.get(i) == null) {
                throw new IllegalStateException("missing value " + i);
            }
        }
    }

    //--
    
    /** convenience methode */
    public Object run(Object target, String... args) {
        return run(target, 0, Arrays.asList(args));
    }

    /** @return target object or child object */
    public Object run(Object target, int start, List<String> args) {
        int i, max;
        String arg;
        Argument argument;
        String value;
        
        max = args.size();
        for (i = start; i < max; i++) {
            arg = args.get(i);
            if (arg.length() > 1 && arg.startsWith("-")) {
                argument = options.get(arg.substring(1));
                if (argument == null) {
                    throw new ArgumentException("unknown option " + arg);
                }
                if (isBoolean(argument)) {
                    value = "true";
                } else {
                    if (i + 1 >= max) {
                        throw new ArgumentException("missing value for option " + arg);
                    }
                    i++;
                    value = args.get(i);
                }
                set(argument, target, value);
            } else {
                break;
            }
        }

        if (children.size() > 0 && i < max) {
            ChildMethod child = lookupChild(args.get(i));
            if (child != null) {
                // dispatch to child command
                target = child.invoke(target);
                return Parser.create(metadata, target.getClass()).run(target, i + 1, args);
            }
        }
        // don't dispatch, target remains unchanged
        for (int position = 1; position < values.size(); position++, i++) {
            if (i >= max) {
                throw new ArgumentException("missing argument '" + values.get(position).getName() + "'");
            }
            set(values.get(position), target, args.get(i));
        }
        if (values.get(0) != null) {
            for ( ; i < max; i++) {
                set(values.get(0), target, args.get(i));
            }
        }
        if (i != max) {
            if (children.size() > 0 && values.size() == 1 && values.get(0) == null) {
                throw new ArgumentException("unknown command, expected on of " + children.keySet());
            } else {
                StringBuilder builder;
                
                builder = new StringBuilder("unknown value(s):");
                for ( ; i < max; i++) {
                    builder.append(' ');
                    builder.append(args.get(i));
                }
                throw new ArgumentException(builder.toString());
            }
        }
        return target;
    }

    public ChildMethod lookupChild(String name) {
        return children.get(name);
    }
    
    //--
    
    public void set(Argument arg, Object obj, String value) {
        Object converted;
        
        try {
            converted = run(arg.getType(), value);
        } catch (ArgumentException e) {
            throw new ArgumentException("invalid argument " + arg.getName() + ": " + e.getMessage());
        }
        arg.set(obj, converted);
    }
    
    public Object run(SimpleType simple, String arg) {
        try {
            return simple.stringToValue(arg);
        } catch (SimpleTypeException e) {
            throw new ArgumentException(e.getMessage(), e);
        }
    }
}
