package com.freedtechnologies.sling.core.apm.services;

import java.util.List;

/**
 * sling properties that can be logged as the transaction name
 */
public interface ApmConfig {
    enum LoggingProperty {
        RESOURCE_TYPE,
        TEMPLATE
    }

    /**
     * gets the property used to log the transaction name
     * @return sling property used for transaction name in apm
     */
    LoggingProperty getLoggingProperty();

    /**
     * if any of these selectors are found in the request, the transaction name will also include them
     * @return list of selectors to log to apm
     */
    List<String> getLoggingSelectors();
}
