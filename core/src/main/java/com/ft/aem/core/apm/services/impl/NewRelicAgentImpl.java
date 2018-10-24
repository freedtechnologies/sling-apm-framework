package com.ft.aem.core.apm.services.impl;

import com.ft.aem.core.apm.services.ApmAgent;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service(value = ApmAgent.class)
@Component(immediate = true, policy = ConfigurationPolicy.OPTIONAL, metatype = true, label="New Relic APM Logging Service")
public class NewRelicAgentImpl implements ApmAgent {

    private static final Logger log = LoggerFactory.getLogger(NewRelicAgentImpl.class);


    @Property(label="Enabled", description = "Use this agent to send APM data?", boolValue = false)
    private static final String PROPERTY_ENABLED = "enabled";
    private boolean enabled = false;

    @Override
    public void sendPageMetric(HttpServletRequest request, String transaction) {
        request.setAttribute("com.newrelic.agent.TRANSACTION_NAME", transaction);
    }

    @Override
    public Object startComponentMetric(String transaction) {
        // not required for new relic, it logs this on it's own
        return null;
    }

    @Override
    public void endComponentMetric(Object span) {
        // not required for new relic, it logs this on it's own
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean logComponents() {
        return false;
    }

    @Activate
    protected void activate(final BundleContext bundleContext, final Map<String, Object> properties) {
        configureService(properties);
    }

    @Modified
    private void configureService(Map<String, Object> properties) {
        enabled = PropertiesUtil.toBoolean(properties.get(PROPERTY_ENABLED), false);
        log.trace("enabled:" + enabled);

    }
}
