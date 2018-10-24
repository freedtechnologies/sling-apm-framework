package com.ft.aem.core.apm.services;

import javax.servlet.http.HttpServletRequest;

public interface ApmAgent {
    void sendPageMetric(HttpServletRequest request, String transaction);
    Object startComponentMetric(String transaction);
    void endComponentMetric(Object span);
    boolean isEnabled();
    boolean logComponents();
}
