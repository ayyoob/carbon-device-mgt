/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.wso2.carbon.webapp.authenticator.framework;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.tomcat.ext.valves.CarbonTomcatValve;
import org.wso2.carbon.tomcat.ext.valves.CompositeValve;
import org.wso2.carbon.webapp.authenticator.framework.authenticator.WebappAuthenticator;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class WebappAuthenticationHandler extends CarbonTomcatValve {

    private static final Log log = LogFactory.getLog(WebappAuthenticationHandler.class);
    private static final String BYPASS_URIS = "bypass-uris";

    @Override
    public void invoke(Request request, Response response, CompositeValve compositeValve) {

        if (this.isContextSkipped(request) || (!this.isAdminService(request) && this.skipAuthentication(request))) {
            this.getNext().invoke(request, response, compositeValve);
            return;
        }

        String byPassURIs = request.getContext().findParameter(WebappAuthenticationHandler.BYPASS_URIS);

        if(byPassURIs != null && !byPassURIs.isEmpty()) {

            List<String> requestURI = Arrays.asList(byPassURIs.split(","));

            if(requestURI != null && requestURI.size() > 0) {
                for (String pathURI : requestURI) {
                    pathURI = pathURI.replace("\n", "").replace("\r", "").trim();

                    if (request.getRequestURI().equals(pathURI)) {
                        this.getNext().invoke(request, response, compositeValve);
                        return;
                    }
                }
            }
        }

        WebappAuthenticator authenticator = WebappAuthenticatorFactory.getAuthenticator(request);
        if (authenticator == null) {
            String msg = "Failed to load an appropriate authenticator to authenticate the request";
            AuthenticationFrameworkUtil.handleResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, msg);
            return;
        }
        WebappAuthenticator.Status status = authenticator.authenticate(request, response);
        this.processResponse(request, response, compositeValve, status);
    }

    private boolean isAdminService(Request request) {
        String param = request.getContext().findParameter("isAdminService");
        return (param != null && Boolean.parseBoolean(param));
    }

    private boolean skipAuthentication(Request request) {
        String param = request.getContext().findParameter("doAuthentication");
        return (param == null || !Boolean.parseBoolean(param));
    }

    private boolean isContextSkipped(Request request) {
        String ctx = request.getContext().getPath();
        if (ctx == null || "".equals(ctx)) {
            ctx = request.getContextPath();
            if (ctx == null || "".equals(ctx)) {
                String requestUri = request.getRequestURI();
                if ("/".equals(requestUri)) {
                    return true;
                }
                StringTokenizer tokenizer = new StringTokenizer(request.getRequestURI(), "/");
                if (!tokenizer.hasMoreTokens()) {
                   return false;
                }
                ctx = tokenizer.nextToken();
            }
        }
        return (ctx.equalsIgnoreCase("carbon") || ctx.equalsIgnoreCase("services"));
    }

    private void processResponse(Request request, Response response, CompositeValve compositeValve,
                                 WebappAuthenticator.Status status) {
        switch (status) {
            case SUCCESS:
            case CONTINUE:
                this.getNext().invoke(request, response, compositeValve);
                break;
            case FAILURE:
                String msg = "Failed to authorize incoming request";
                log.error(msg);
                AuthenticationFrameworkUtil.handleResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED, msg);
                break;
        }
    }

}
