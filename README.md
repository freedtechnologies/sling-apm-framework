# Sling APM Framework

**The Sling APM Framework version 1.0.0 supports New Relic and Elastic APM providers.**  

The Sling APM Framework is a collection of OSGi filters and services that allow you to easily group and analyze AEM pageviews within application performance metric software.

When APM software is used to monitor sling out of the box, all of your web transactions are logged as the same name, which provides no value.  The Sling APM framework allows you to instead rename your web transactions to use the sling:resourceType or cq:template of a page.

This allows you to easily look for performance bottlenecks for specific resource types or templates to more easily pinpoint ill performant code. 

In addition to viewing the response time of each type of page being rendered, the Sling APM framework will also log to your APM provider anytime a component is included in the page.

This allows you to not only view the response time of a given type of page, but also allows you to drill down into the individual components that make up the page, so you can easily identify individual components causing bottlenecks.

If you are using another APM provider, it can easily be implemented using the `ApmAgent` interface and will automatically start logging transaction data.  Please submit a pull request if you add your own provider so others can use it!

## How To Configure

more info to come

## How To Integrate With Your Own APM Provider

more info to come