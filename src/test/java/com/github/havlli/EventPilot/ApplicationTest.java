package com.github.havlli.EventPilot;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class ApplicationTest {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationTest.class);
    
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        LOG.info("Application Context loaded with {} beans!", applicationContext.getBeanDefinitionCount());
    }
}