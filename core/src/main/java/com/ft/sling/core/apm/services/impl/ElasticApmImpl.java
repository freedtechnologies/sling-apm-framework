package com.ft.sling.core.apm.services.impl;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Span;
import co.elastic.apm.api.Transaction;
import com.ft.sling.core.apm.services.ApmAgent;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service(value = ApmAgent.class)
@Component(immediate = true, policy = ConfigurationPolicy.OPTIONAL, metatype = true, label="Elastic APM Logging Service")
public class ElasticApmImpl implements ApmAgent {

    @Property(label="Enabled", description = "Use this agent to send APM data?", boolValue = false)
    public static final String PROPERTY_ENABLED = "enabled";
    private boolean enabled = false;

    @Property(label="Log Components", description = "Does this agent need to log components in the page (used by elastic APM)?", boolValue = false)
    public static final String PROPERTY_LOG_COMPONENTS = "logcomponents";
    private boolean logComponents = false;

    private static final Logger log = LoggerFactory.getLogger(ElasticApmImpl.class);

    @Override
    public void sendPageMetric(HttpServletRequest request, String transaction) {
        Transaction elasticTransaction = ElasticApm.currentTransaction();
        elasticTransaction.setName(transaction);
    }

    @Override
    public Object startComponentMetric(String transaction) {
        try {
            Span elasticSpan = ElasticApm.currentTransaction().createSpan();
            elasticSpan.setType("sling.component");
            elasticSpan.setName(transaction);
            return elasticSpan;
        } catch (Exception ex) {
            log.error("error setting span", ex);
        }
        return null;
    }

    @Override
    public void endComponentMetric(Object span) {
        if(span==null) {
            return;
        }
        Span elasticSpan = (Span) span;
        elasticSpan.end();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean logComponents() {
        return logComponents;
    }

    @Activate
    protected void activate(final BundleContext bundleContext, final Map<String, Object> properties) {
        configureService(properties);
    }

    @Modified
    private void configureService(Map<String, Object> properties) {
        enabled = PropertiesUtil.toBoolean(properties.get(PROPERTY_ENABLED), false);
        logComponents = PropertiesUtil.toBoolean(properties.get(PROPERTY_LOG_COMPONENTS), false);
        log.info("enabled:" + enabled);
    }
}
