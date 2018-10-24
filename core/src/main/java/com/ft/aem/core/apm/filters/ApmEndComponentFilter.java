/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ft.aem.core.apm.filters;

import com.ft.aem.core.apm.services.ApmAgent;
import com.ft.aem.core.apm.services.ApmConfig;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.servlets.ServletResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Simple servlet filter component that logs incoming requests.
 */


@References({@Reference(name = "apmAgent", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, referenceInterface=ApmAgent.class, bind="bindApmAgent", unbind="unbindApmAgent")})
@SlingFilter(scope= SlingFilterScope.INCLUDE, order = Integer.MAX_VALUE,
        description="Sends APM segment information to provider via filter", name="Apm Component End Filter")
@Service(value = Filter.class)
public class ApmEndComponentFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApmEndComponentFilter.class);

    private static final Pattern SERVLET_PATTERN = Pattern.compile("^.+Using servlet (.+)$");

    @Reference
    private ApmConfig apmConfig;

    private final List<ApmAgent> apmAgents = Collections.synchronizedList(new ArrayList<>());

    protected synchronized void bindApmAgent(ServiceReference ref) {
        log.trace("in bind");
        ApmAgent config = (ApmAgent) ref.getBundle().getBundleContext().getService(ref);
        log.trace("binding apm agent: " + config.getClass().getSimpleName());
        if(config.isEnabled() && config.logComponents()) {
            apmAgents.add(config);
        }
    }

    protected synchronized void unbindApmAgent(ServiceReference ref) {
        log.trace("in unbind");
        ApmAgent config = (ApmAgent) ref.getBundle().getBundleContext().getService(ref);
        log.trace("unbind apm agent: " + config.getClass().getSimpleName());
        apmAgents.remove(config);
    }

    @Reference
    ServletResolver servletResolver;

    @Override
    public void init(FilterConfig paramFilterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;
        try {
            log.trace("in filter start");
            // we don't need to execute this logic if no apm agents are configured
            if(apmAgents.size()==0) {
                log.trace("no agents found, exiting");
                return;
            }

            Servlet servlet = servletResolver.resolveServlet(request);

            synchronized (apmAgents) {
                for(ApmAgent agent : apmAgents) {
                    // only log if agent is enabled
                    if(agent.isEnabled()) {
                        Object span = request.getAttribute("apm.start.component." + servlet.getServletConfig().getServletName()  + agent.getClass().getSimpleName());
                        agent.endComponentMetric(span);
                        log.trace("sent end span metric");
                    }
                }
            }

        } catch (Exception e) {
            log.warn("could create apm span", e);
        } finally {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }


    @Activate
    @Modified
    protected void activate(final BundleContext bundleContext,
                            final Map<String, Object> configuration) {
    }

    @Override
    public void destroy() {
    }

}