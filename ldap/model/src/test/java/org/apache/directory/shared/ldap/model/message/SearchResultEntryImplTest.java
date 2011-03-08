/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.model.message;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.SearchResultEntryImpl;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test cases for the methods of the SearchResponseEntryImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev: 946251 $
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class SearchResultEntryImplTest
{
    /**
     * Creates and populates an EntryAttribute with a specific id.
     * 
     * @param id the id for the attribute
     * @return the EntryAttribute assembled for testing
     */
    private EntryAttribute getEntry( String id )
    {
        EntryAttribute attr = new DefaultEntryAttribute( id );
        attr.add( "value0" );
        attr.add( "value1" );
        attr.add( "value2" );
        return attr;
    }


    /**
     * Creates and populates an Entry object
     * 
     * @return The populated Entry object
     */
    private Entry getEntry() throws LdapException
    {
        Entry attrs = new DefaultEntry();
        attrs.put( getEntry( "attr0" ) );
        attrs.put( getEntry( "attr1" ) );
        attrs.put( getEntry( "attr2" ) );
        return attrs;
    }


    /**
     * Tests for equality when the same object reference is used.
     */
    @Test
    public void testEqualsSameObject()
    {
        SearchResultEntryImpl resp = new SearchResultEntryImpl( 5 );
        assertTrue( "the same object should be equal", resp.equals( resp ) );
    }


    /**
     * Tests for equality when an exact copy is compared.
     */
    @Test
    public void testEqualsExactCopy() throws LdapException
    {
        SearchResultEntryImpl resp0 = new SearchResultEntryImpl( 5 );
        resp0.setEntry( getEntry() );
        resp0.setObjectName( new Dn( "dc=example,dc=com" ) );

        SearchResultEntryImpl resp1 = new SearchResultEntryImpl( 5 );
        resp1.setEntry( getEntry() );
        resp1.setObjectName( new Dn( "dc=example,dc=com" ) );

        assertTrue( "exact copies should be equal", resp0.equals( resp1 ) );
        assertTrue( "exact copies should be equal", resp1.equals( resp0 ) );
    }


    /**
     * Tests for equal hashCode when the same object reference is used.
     */
    @Test
    public void testHashCodeSameObject()
    {
        SearchResultEntryImpl resp = new SearchResultEntryImpl( 5 );
        assertTrue( resp.hashCode() == resp.hashCode() );
    }


    /**
     * Tests for equal hashCode when an exact copy is compared.
     */
    @Test
    public void testHashCodeExactCopy() throws LdapException
    {
        SearchResultEntryImpl resp0 = new SearchResultEntryImpl( 5 );
        resp0.setEntry( getEntry() );
        resp0.setObjectName( new Dn( "dc=example,dc=com" ) );

        SearchResultEntryImpl resp1 = new SearchResultEntryImpl( 5 );
        resp1.setEntry( getEntry() );
        resp1.setObjectName( new Dn( "dc=example,dc=com" ) );

        assertTrue( resp0.hashCode() == resp1.hashCode() );
    }


    /**
     * Tests for inequality when the objectName dn is not the same.
     */
    @Test
    public void testNotEqualDiffObjectName() throws LdapException
    {
        SearchResultEntryImpl resp0 = new SearchResultEntryImpl( 5 );
        resp0.setEntry( getEntry() );
        resp0.setObjectName( new Dn( "dc=apache,dc=org" ) );

        SearchResultEntryImpl resp1 = new SearchResultEntryImpl( 5 );
        resp1.setEntry( getEntry() );
        resp1.setObjectName( new Dn( "dc=example,dc=com" ) );

        assertFalse( "different object names should not be equal", resp1.equals( resp0 ) );
        assertFalse( "different object names should not be equal", resp0.equals( resp1 ) );
    }
}