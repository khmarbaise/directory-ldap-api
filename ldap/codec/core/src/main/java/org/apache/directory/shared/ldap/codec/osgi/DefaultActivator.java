/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.codec.osgi;


import org.apache.directory.shared.ldap.codec.api.LdapApiService;
import org.apache.directory.shared.ldap.codec.api.LdapApiServiceFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;


/**
 * The {@link org.osgi.framework.BundleActivator} for the codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings(
    { "UnusedDeclaration" })
public class DefaultActivator implements BundleActivator
{
    private DefaultLdapCodecService codec;
    private ServiceRegistration registration;


    @SuppressWarnings(
        { "UnusedDeclaration" })
    public DefaultActivator()
    {
        this.codec = new DefaultLdapCodecService();
    }


    /**
     * {@inheritDoc}
     */
    public void start( BundleContext bundleContext ) throws Exception
    {
        registration = bundleContext.registerService( LdapApiService.class.getName(), codec, null );
        LdapApiServiceFactory.initialize( codec );
        System.out.println( "Service registered: " + LdapApiService.class.getName() + " - " + codec );
    }


    /**
     * {@inheritDoc}
     */
    public void stop( BundleContext bundleContext ) throws Exception
    {
        registration.unregister();
    }
}
