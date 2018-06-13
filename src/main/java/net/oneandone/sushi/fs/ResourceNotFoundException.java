package net.oneandone.sushi.fs;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource) {
        super("resource not found in classpath: " + resource);
    }
}
