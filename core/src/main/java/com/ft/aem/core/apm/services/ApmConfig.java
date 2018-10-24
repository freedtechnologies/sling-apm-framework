package com.ft.aem.core.apm.services;

import java.util.List;

public interface ApmConfig {
    enum LoggingProperty {
        RESOURCE_TYPE,
        TEMPLATE
    }

    LoggingProperty getLoggingProperty();

    List<String> getLoggingSelectors();
}
