package com.github.havlli.EventPilot.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LogbackConfig {
    private final PatternLayoutEncoder layoutEncoder;
    private final CustomConsoleAppender consoleAppender;

    public LogbackConfig(
            PatternLayoutEncoder layoutEncoder,
            CustomConsoleAppender consoleAppender
    ) {
        this.layoutEncoder = layoutEncoder;
        this.consoleAppender = consoleAppender;
    }

    @PostConstruct
    public void configure() {
        LoggerContext loggerContext = getLoggerFactory();
        buildLayoutEncoder(loggerContext);
        buildLoggerContext(loggerContext);
        addAppenderOnRoot(loggerContext);
    }

    private void addAppenderOnRoot(LoggerContext loggerContext) {
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(consoleAppender);
    }

    private static LoggerContext getLoggerFactory() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    private void buildLoggerContext(LoggerContext loggerContext) {
        consoleAppender.setName("STDOUT");
        consoleAppender.setEncoder(layoutEncoder);
        consoleAppender.setContext(loggerContext);
        consoleAppender.start();
    }

    private void buildLayoutEncoder(LoggerContext loggerContext) {
        layoutEncoder.setPattern("%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr(18971){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n%wEx");
        layoutEncoder.setContext(loggerContext);
        layoutEncoder.start();
    }
}
