package com.github.havlli.EventPilot.logging;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

import java.util.Arrays;

public record ThrowableProxyDTO(
        String className,
        String message,
        String[] stackTrace
) {
    public static ThrowableProxyDTO fromThrowableProxy(IThrowableProxy throwableProxy) {
        return new ThrowableProxyDTO(
                throwableProxy.getClassName(),
                throwableProxy.getMessage(),
                Arrays.stream(throwableProxy.getStackTraceElementProxyArray())
                        .map(StackTraceElementProxy::getSTEAsString)
                        .toArray(String[]::new)
        );
    }
}
