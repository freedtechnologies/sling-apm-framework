package com.freedtechnologies.sling.core.apm.services.impl;

import com.freedtechnologies.sling.core.apm.services.ApmConfig;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service(value = ApmConfig.class)
@Component(immediate = true, policy = ConfigurationPolicy.OPTIONAL, metatype = true, label="APM Logging Configuration")
public class ApmConfigImpl implements ApmConfig {

    @Property(description = "What property is used to log the type of page being rendered?", label = "Transaction Name",
             options = {
        @PropertyOption(name = "TEMPLATE", value = "Template"),
        @PropertyOption(name = "RESOURCE_TYPE", value = "Resource Type"),
    })
    private static final String PROPERTY_TRANSACTION_NAME = "RESOURCE_TYPE";

    private static final Logger log = LoggerFactory.getLogger(ApmConfigImpl.class);


    private ApmConfig.LoggingProperty loggingProperty;


    @Property(unbounded = PropertyUnbounded.ARRAY, label = "Transaction Selectors", description = "If any of these selectors is used to render a page, the transaction name will include all selectors used")
    private static final String PROPERTY_SELECTORS = "SELECTORS";

    private List<String> selectors = new ArrayList<>();

    @Override
    public LoggingProperty getLoggingProperty() {
        return loggingProperty;
    }

    @Override
    public List<String> getLoggingSelectors() {
        return selectors;
    }

    @Activate
    protected void activate(final BundleContext bundleContext, final Map<String, Object> properties) {
        configureService(properties);
    }

    @Modified
    private void configureService(Map<String, Object> properties) {
        loggingProperty = ApmConfig.LoggingProperty.valueOf(PropertiesUtil.toString(properties.get(PROPERTY_TRANSACTION_NAME), "RESOURCE_TYPE"));
        if(properties.get(PROPERTY_SELECTORS)!=null) {
            selectors = Arrays.asList(PropertiesUtil.toStringArray(properties.get(PROPERTY_SELECTORS)));
        }
    }
}
