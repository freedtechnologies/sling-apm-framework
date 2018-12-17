package com.freedtechnologies.sling.core.apm.services;

import javax.servlet.http.HttpServletRequest;

/**
 * Apm Agents are used to log APM data to a specific provider
 * this interface can be implemented by multiple apm agent providers,
 * the sling filters will iterate over all configured providers to log apm metrics
 */
public interface ApmAgent {
    /**
     * sets the transaction name for the page being rendered
     * @param request the request object
     * @param transaction the name of the transaction to use
     */
    void sendPageMetric(HttpServletRequest request, String transaction);

    /**
     * starts a span within a transaction for an individual component
     * note, some APM providers already do this for you and this doesn't need to be implemented
     * @param transaction transaction name
     * @return the object representing the component rendering in the apm api
     */
    Object startComponentMetric(String transaction);

    /**
     * ends a components rendering time span
     * @param span the object that was originally used to create the timespan, filter stores this in the request context
     */
    void endComponentMetric(Object span);

    /**
     * only enabled apm agents will log data, this is configured via felix
     * @return
     */
    boolean isEnabled();

    /**
     * Should individual component renderings also be logged or is this functionality disabled
     * @return
     */
    boolean logComponents();
}
