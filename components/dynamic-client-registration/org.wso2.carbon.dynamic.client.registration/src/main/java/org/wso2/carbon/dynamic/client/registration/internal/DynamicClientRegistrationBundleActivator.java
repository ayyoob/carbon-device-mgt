/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.dynamic.client.registration.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.dynamic.client.registration.DynamicClientRegistrationService;
import org.wso2.carbon.dynamic.client.registration.impl.DynamicClientRegistrationImpl;

/**
 * BundleActivator class of DynamicClientRegistration component.
 */
public class DynamicClientRegistrationBundleActivator implements BundleActivator{

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		DynamicClientRegistrationService dynamicClientRegistrationService =
																new DynamicClientRegistrationImpl();
		bundleContext.registerService(DynamicClientRegistrationService.class.getName(),
		                                                     dynamicClientRegistrationService, null);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {

	}

}
