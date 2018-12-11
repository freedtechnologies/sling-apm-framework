package com.ft.sling.core.apm.filters;

import com.ft.sling.core.apm.services.ApmAgent;
import com.ft.sling.core.apm.services.ApmConfig;
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
 * Filter that logs to APM agents when a component is done rendering as part of an overall transaction
 */
@References({@Reference(name = "apmAgent", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, referenceInterface=ApmAgent.class)})
@SlingFilter(scope= SlingFilterScope.INCLUDE, order = Integer.MAX_VALUE,
        description="Sends APM segment information to provider via filter", name="Apm Component End Filter")
@Service(value = Filter.class)
public class ApmEndComponentFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApmEndComponentFilter.class);

    private static final Pattern SERVLET_PATTERN = Pattern.compile("^.+Using servlet (.+)$");

    @Reference
    private ApmConfig apmConfig;

    private final List<ApmAgent> apmAgents = Collections.synchronizedList(new ArrayList<>());

    /**
     * When an Apm Agent is binded in felix, it is added to a list of apm agents to log data too
     * @param service
     * @param props
     */
    protected synchronized void bindApmAgent(final ApmAgent service,
                                             final Map<Object, Object> props) {
        log.trace("binding apm agent: " + service.getClass().getSimpleName());
        if(service.isEnabled() && service.logComponents()) {
            apmAgents.add(service);
        }
    }

    /**
     * removes Apm agent when unbinded in felix
     * @param service
     * @param props
     */
    protected synchronized void unbindApmAgent(final ApmAgent service,
                                               final Map<Object, Object> props) {
        log.trace("unbind apm agent: " + service.getClass().getSimpleName());
        apmAgents.remove(service);
    }

    @Reference
    ServletResolver servletResolver;

    @Override
    public void init(FilterConfig paramFilterConfig) throws ServletException {

    }

    /**
     * Logs the name of the component servlet being used for rendering to APM agents configured
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
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

            // get the servlet being used to render this component
            Servlet servlet = servletResolver.resolveServlet(request);

            synchronized (apmAgents) {
                // log for each apm agent
                for(ApmAgent agent : apmAgents) {
                    // only log if agent is enabled
                    if(agent.isEnabled()) {
                        // we get this as a request attribute so we are sure we are ending the correct components lifecycle
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