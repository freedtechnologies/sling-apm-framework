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
package com.ft.sling.core.apm.filters;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ft.sling.core.apm.services.ApmAgent;
import com.ft.sling.core.apm.services.ApmConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.framework.BundleContext;
import com.day.cq.wcm.api.NameConstants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.JcrConstants;

/**
 * Filter that sets a custom transaction name for the request in APM software based on a sling property
 */
@References({@Reference(name = "apmAgent", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, referenceInterface=ApmAgent.class, bind="bindApmAgent", unbind="unbindApmAgent")})
@SlingFilter(scope= SlingFilterScope.REQUEST, order = Integer.MAX_VALUE,
        description="Sends APM information to provider via filter", name="Apm Page Filter")
@Service(value = Filter.class)
public class ApmPageFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApmPageFilter.class);

    private static final Pattern SERVLET_PATTERN = Pattern.compile("^.+Using servlet (.+)$");

    private static final String EDITOR_PATH = "/libs/cq/gui/content/editor";

    @Reference
    private ApmConfig apmConfig;

    private final List<ApmAgent> apmAgents = Collections.synchronizedList(new ArrayList<>());

    protected synchronized void bindApmAgent(ServiceReference ref) {
        log.trace("in bind");
        ApmAgent config = (ApmAgent) ref.getBundle().getBundleContext().getService(ref);
        log.trace("binding apm agent: " + config.getClass().getSimpleName());
        if(config.isEnabled()) {
            apmAgents.add(config);
        }
    }

    protected synchronized void unbindApmAgent(ServiceReference ref) {
        log.trace("in unbind");
        ApmAgent config = (ApmAgent) ref.getBundle().getBundleContext().getService(ref);
        log.trace("unbind apm agent: " + config.getClass().getSimpleName());
        apmAgents.remove(config);
    }

    @Override
    public void init(FilterConfig paramFilterConfig) throws ServletException {

    }

    /**
     * Access the sling request log to find the type of request,
     * then logs the custom transaction name for the pageview to apm agent
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

            String transaction = null;

            Resource resource = request.getResource();

            // if this is the editor path, get the resource tied to the url suffix, as that is the page we are rendering
            if(resource.getPath().equals(EDITOR_PATH)) {
                String path = request.getRequestPathInfo().getSuffix();
                path = path.substring(0, path.indexOf("."));
                resource = request.getResourceResolver().getResource(path);
            }
            //try to find name of servlet rendering page and jcr primary type since we don't have a resourcetype

            //find servlet namke by looking for regex match in the request progress tracker messages
            Iterator<String> messages = request.getRequestProgressTracker().getMessages();
            while (messages.hasNext()) {
                String message = messages.next();
                Matcher match = SERVLET_PATTERN.matcher(message);
                if (match.find() && match.groupCount() >= 1) {
                    transaction = match.group(1);
                    break;
                }
            }

            // see if transaction was a page being rendered vs java servlet,
            // page renderings have their servlet start with "/", which is the repo path to the page
            if(transaction!=null && transaction.startsWith("/") && resource!=null) {
                //transaction is a page, not a servlet, get resource type of page if available
                //see if we can read the resourcetype from resource itself
                if (!ResourceUtil.isNonExistingResource(resource)) {
                    log.trace("is real resource");
                    // if this is a cq:page resource, fetch the jcr content node to read page type
                    if(resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE).equals(NameConstants.NT_PAGE)) {
                        log.trace("is a page");
                        resource = resource.getChild(JcrConstants.JCR_CONTENT);
                    }
                    if (resource != null) {
                        if(apmConfig.getLoggingProperty().equals(ApmConfig.LoggingProperty.TEMPLATE)) {
                            transaction = resource.getValueMap().get(NameConstants.PN_TEMPLATE, resource.getResourceType());
                        } else {
                            transaction = resource.getResourceType();
                        }
                    }
                }
            }

            // logic the log transaction to apm
            if(transaction!=null) {
                // find any selectors to be added to transaction name
                List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());
                List<String> validSelectors = new ArrayList<>();
                for(String selector : apmConfig.getLoggingSelectors()) {
                    if(selectors.contains(selector)) {
                        validSelectors.add(selector);
                    }
                }
                if(validSelectors.size()>0) {
                    transaction += "-" + StringUtils.join(validSelectors, ",");
                }
                log.trace(transaction);
                // log transaction to each apm agent
                synchronized (apmAgents) {
                    for(ApmAgent agent : apmAgents) {
                        // only log if agent is enabled
                        if(agent.isEnabled()) {
                            agent.sendPageMetric(request, transaction);
                            log.trace("sent metrics");
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("could not generate apm metrics", e);
        } finally {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }


    @Activate
    @Modified
    protected void activate(final BundleContext bundleContext,
                            final Map<String, Object> configuration) {
        log.trace("in activate");
    }

    @Override
    public void destroy() {
    }

}