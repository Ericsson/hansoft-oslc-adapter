/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *     Michael Fiedler     - initial API and implementation for Bugzilla adapter
 *     
 *******************************************************************************/
package com.ericsson.eif.hansoft.services;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.lyo.oslc4j.application.OslcWinkApplication;
import org.eclipse.lyo.oslc4j.core.exception.OslcCoreApplicationException;
import org.eclipse.lyo.oslc4j.core.model.AllowedValues;
import org.eclipse.lyo.oslc4j.core.model.Compact;
import org.eclipse.lyo.oslc4j.core.model.CreationFactory;
import org.eclipse.lyo.oslc4j.core.model.Dialog;
import org.eclipse.lyo.oslc4j.core.model.Error;
import org.eclipse.lyo.oslc4j.core.model.ExtendedError;
import org.eclipse.lyo.oslc4j.core.model.OAuthConfiguration;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.PrefixDefinition;
import org.eclipse.lyo.oslc4j.core.model.Preview;
import org.eclipse.lyo.oslc4j.core.model.Property;
import org.eclipse.lyo.oslc4j.core.model.Publisher;
import org.eclipse.lyo.oslc4j.core.model.QueryCapability;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.eclipse.lyo.oslc4j.core.model.Service;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.eclipse.lyo.oslc4j.core.model.ServiceProviderCatalog;
import org.eclipse.lyo.oslc4j.provider.jena.JenaProvidersRegistry;
import org.eclipse.lyo.oslc4j.provider.json4j.Json4JProvidersRegistry;

import com.ericsson.eif.hansoft.Constants;
import com.ericsson.eif.hansoft.resources.HansoftChangeRequest;
import com.ericsson.eif.hansoft.resources.Person;

public class HansoftApplication extends OslcWinkApplication {

    private static final Set<Class<?>> RESOURCE_CLASSES = new HashSet<Class<?>>();
    private static final Map<String, Class<?>> RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP = new HashMap<String, Class<?>>();

    private static final Logger logger = Logger
            .getLogger(HansoftApplication.class.getName());

    static {
        try {
            RESOURCE_CLASSES.addAll(JenaProvidersRegistry.getProviders());
            RESOURCE_CLASSES.addAll(Json4JProvidersRegistry.getProviders());
            RESOURCE_CLASSES.add(HansoftChangeRequestService.class);
            RESOURCE_CLASSES.add(Person.class);

            RESOURCE_CLASSES
                    .add(Class
                            .forName("org.eclipse.lyo.server.oauth.webapp.services.ConsumersService"));
            RESOURCE_CLASSES
                    .add(Class
                            .forName("org.eclipse.lyo.server.oauth.webapp.services.OAuthService"));

            // Catalog resources.

            RESOURCE_CLASSES.add(ServiceProviderCatalogService.class);
            RESOURCE_CLASSES.add(ServiceProviderService.class);

            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_ALLOWED_VALUES, AllowedValues.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_COMPACT, Compact.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_CREATION_FACTORY, CreationFactory.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_DIALOG, Dialog.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_ERROR, Error.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_EXTENDED_ERROR, ExtendedError.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_OAUTH_CONFIGURATION,
                    OAuthConfiguration.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_PREFIX_DEFINITION,
                    PrefixDefinition.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_PREVIEW, Preview.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_PROPERTY, Property.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_PUBLISHER, Publisher.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_QUERY_CAPABILITY, QueryCapability.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_RESOURCE_SHAPE, ResourceShape.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_SERVICE, Service.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_SERVICE_PROVIDER, ServiceProvider.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    OslcConstants.PATH_SERVICE_PROVIDER_CATALOG,
                    ServiceProviderCatalog.class);
            RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP.put(
                    Constants.PATH_CHANGE_REQUEST, HansoftChangeRequest.class);
        } catch (ClassNotFoundException e) {
            logger.error("HansoftApplication class failed to initialize.", e);
        }
    }

    public HansoftApplication() throws OslcCoreApplicationException,
            URISyntaxException {
        super(RESOURCE_CLASSES, OslcConstants.PATH_RESOURCE_SHAPES,
                RESOURCE_SHAPE_PATH_TO_RESOURCE_CLASS_MAP);
    }
}
