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
package org.apache.directory.shared.ldap.schema;


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * A matchingRule definition. MatchingRules associate a comparator and a
 * normalizer, forming the basic tools necessary to assert actions against
 * attribute values. MatchingRules are associated with a specific Syntax for the
 * purpose of resolving a normalized form and for comparisons.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * 
 * <pre>
 *  4.1.3. Matching Rules
 *  
 *    Matching rules are used by servers to compare attribute values against
 *    assertion values when performing Search and Compare operations.  They
 *    are also used to identify the value to be added or deleted when
 *    modifying entries, and are used when comparing a purported
 *    distinguished name with the name of an entry.
 *  
 *    A matching rule specifies the syntax of the assertion value.
 * 
 *    Each matching rule is identified by an object identifier (OID) and,
 *    optionally, one or more short names (descriptors).
 * 
 *    Matching rule definitions are written according to the ABNF:
 * 
 *      MatchingRuleDescription = LPAREN WSP
 *          numericoid                ; object identifier
 *          [ SP &quot;NAME&quot; SP qdescrs ]  ; short names (descriptors)
 *          [ SP &quot;DESC&quot; SP qdstring ] ; description
 *          [ SP &quot;OBSOLETE&quot; ]         ; not active
 *          SP &quot;SYNTAX&quot; SP numericoid ; assertion syntax
 *          extensions WSP RPAREN     ; extensions
 * 
 *    where:
 *      [numericoid] is object identifier assigned to this matching rule;
 *      NAME [qdescrs] are short names (descriptors) identifying this
 *          matching rule;
 *      DESC [qdstring] is a short descriptive string;
 *      OBSOLETE indicates this matching rule is not active;
 *      SYNTAX identifies the assertion syntax by object identifier; and
 *      [extensions] describe extensions.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC 2252 Section 4.5</a>
 * @see <a
 *      href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis
 *      [MODELS]</a>
 * @see DescriptionUtils#getDescription(MatchingRule)
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MatchingRule extends SchemaObject
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The associated LdapSyntax registry */
    //private final LdapSyntaxRegistry ldapSyntaxRegistry;
    
    /** The associated Comparator registry */
    //private final ComparatorRegistry comparatorRegistry;

    /** The associated Normalizer registry */
    //private final NormalizerRegistry normalizerRegistry;
    
    /** The associated Comparator */
    private LdapComparator<? super Object> ldapComparator;

    /** The associated Normalizer */
    private Normalizer normalizer;

    /** The associated LdapSyntax */
    private LdapSyntax ldapSyntax;
    
    /**
     * Creates a new instance of MatchingRule.
     *
     * @param oid The MatchingRule OID
     * @param registries The Registries reference
     */
    protected MatchingRule( String oid, Registries registries )
    {
        super( SchemaObjectType.MATCHING_RULE, oid );
        
        //ldapSyntaxRegistry = registries.getLdapSyntaxRegistry();
        //normalizerRegistry = registries.getNormalizerRegistry();
        //comparatorRegistry = registries.getComparatorRegistry();

        try
        {
            // Gets the associated C 
            ldapComparator = (LdapComparator<? super Object>)registries.getComparatorRegistry().lookup( oid );

            // Gets the associated N 
            normalizer = registries.getNormalizerRegistry().lookup( oid );

            // Gets the associated SC 
            ldapSyntax = registries.getLdapSyntaxRegistry().lookup( oid );
        }
        catch ( NamingException ne )
        {
            // What can we do here ???
        }
    }


    /**
     * Gets the LdapSyntax used by this MatchingRule.
     * 
     * @return the LdapSyntax of this MatchingRule
     * @throws NamingException if there is a failure resolving the object
     */
    LdapSyntax getLdapSyntax() throws NamingException
    {
        return ldapSyntax;
    }


    /**
     * Gets the LdapComparator enabling the use of this MatchingRule for ORDERING
     * and sorted indexing.
     * 
     * @return the ordering LdapComparator
     * @throws NamingException if there is a failure resolving the object
     */
    public LdapComparator<? super Object> getLdapComparator() throws NamingException
    {
        return ldapComparator;
    }


    /**
     * Gets the Normalizer enabling the use of this MatchingRule for EQUALITY
     * matching and indexing.
     * 
     * @return the associated normalizer
     * @throws NamingException if there is a failure resolving the object
     */
    public Normalizer getNormalizer() throws NamingException
    {
        return normalizer;
    }
}
