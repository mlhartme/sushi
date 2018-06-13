package net.oneandone.sushi.fs;

import java.net.URL;

public class ResourceFromModuleException extends RuntimeException {
    public ResourceFromModuleException(URL resource) {
        super("resource from module, not in classpath: " + resource);
    }
}
