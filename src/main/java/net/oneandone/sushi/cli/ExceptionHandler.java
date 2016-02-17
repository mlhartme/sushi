package net.oneandone.sushi.cli;

@FunctionalInterface
public interface ExceptionHandler {
    int handleException(Throwable throwable);
}
