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


import java.text.ParseException;
import java.util.*;

import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapProtocolErrorException;
import org.apache.directory.shared.ldap.model.message.*;
import org.apache.directory.shared.ldap.model.filter.BranchNormalizedVisitor;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.filter.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * SearchRequest implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 */
public class SearchRequestImpl extends AbstractAbandonableRequest implements SearchRequest
{
    static final long serialVersionUID = -5655881944020886218L;

    /** Search base distinguished name */
    private Dn baseDn;

    /** Search filter expression tree's root node */
    private ExprNode filterNode;

    /** Search scope enumeration value */
    private SearchScope scope;

    /** Types only return flag */
    private boolean typesOnly;

    /** Max size in entries to return */
    private long sizeLimit;

    /** Max seconds to wait for search to complete */
    private int timeLimit;

    /** Alias dereferencing mode enumeration value (default to DEREF_ALWAYS) */
    private AliasDerefMode aliasDerefMode = AliasDerefMode.DEREF_ALWAYS;

    /** Attributes to return */
    private List<String> attributes = new ArrayList<String>();

    /** The final result containing SearchResponseDone response */
    private SearchResultDone response;


    /**
     * Creates a SearcRequest implementing object used to search the
     * DIT.
     */
    public SearchRequestImpl()
    {
        super( -1, MessageTypeEnum.SEARCH_REQUEST );
    }


    /**
     * Creates a Lockable SearcRequest implementing object used to search the
     * DIT.
     * 
     * @param id the sequential message identifier
     */
    public SearchRequestImpl( final int id )
    {
        super( id, MessageTypeEnum.SEARCH_REQUEST );
    }


    // ------------------------------------------------------------------------
    // SearchRequest Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets a list of the attributes to be returned from each entry which
     * matches the search filter. There are two special values which may be
     * used: an empty list with no attributes, and the attribute description
     * string "*". Both of these signify that all user attributes are to be
     * returned. (The "*" allows the client to request all user attributes in
     * addition to specific operational attributes). Attributes MUST be named at
     * most once in the list, and are returned at most once in an entry. If
     * there are attribute descriptions in the list which are not recognized,
     * they are ignored by the server. If the client does not want any
     * attributes returned, it can specify a list containing only the attribute
     * with OID "1.1". This OID was chosen arbitrarily and does not correspond
     * to any attribute in use. Client implementors should note that even if all
     * user attributes are requested, some attributes of the entry may not be
     * included in search results due to access control or other restrictions.
     * Furthermore, servers will not return operational attributes, such as
     * objectClasses or attributeTypes, unless they are listed by name, since
     * there may be extremely large number of values for certain operational
     * attributes.
     * 
     * @return the collection of attributes to return for each entry
     */
    public List<String> getAttributes()
    {
        return Collections.unmodifiableList( attributes );
    }


    /**
     * Gets the search base as a distinguished name.
     * 
     * @return the search base
     */
    public Dn getBase()
    {
        return baseDn;
    }


    /**
     * Sets the search base as a distinguished name.
     * 
     * @param base
     *            the search base
     */
    public void setBase( Dn base )
    {
        baseDn = base;
    }


    /**
     * Gets the alias handling parameter.
     * 
     * @return the alias handling parameter enumeration.
     */
    public AliasDerefMode getDerefAliases()
    {
        return aliasDerefMode;
    }


    /**
     * Sets the alias handling parameter.
     * 
     * @param aliasDerefAliases
     *            the alias handling parameter enumeration.
     */
    public void setDerefAliases( AliasDerefMode aliasDerefAliases )
    {
        this.aliasDerefMode = aliasDerefAliases;
    }


    /**
     * Gets the search filter associated with this search request.
     * 
     * @return the expression node for the root of the filter expression tree.
     */
    public ExprNode getFilter()
    {
        return filterNode;
    }


    /**
     * {@inheritDoc}
     */
    public void setFilter( ExprNode filter )
    {
        this.filterNode = filter;
    }


    /**
     * {@inheritDoc}
     */
    public void setFilter( String filter ) throws LdapException
    {
        try
        {
            filterNode = FilterParser.parse( filter );
        }
        catch ( ParseException pe )
        {
            String msg = "The filter" + filter + " is invalid.";
            throw new LdapProtocolErrorException( msg );
        }
    }


    /**
     * Gets the different response types generated by a search request.
     * 
     * @return the RESPONSE_TYPES array
     * @see #RESPONSE_TYPES
     */
    public MessageTypeEnum[] getResponseTypes()
    {
        return RESPONSE_TYPES.clone();
    }


    /**
     * Gets the search scope parameter enumeration.
     * 
     * @return the scope enumeration parameter.
     */
    public SearchScope getScope()
    {
        return scope;
    }


    /**
     * Sets the search scope parameter enumeration.
     * 
     * @param scope the scope enumeration parameter.
     */
    public void setScope( SearchScope scope )
    {
        this.scope = scope;
    }


    /**
     * A sizelimit that restricts the maximum number of entries to be returned
     * as a result of the search. A value of 0 in this field indicates that no
     * client-requested sizelimit restrictions are in effect for the search.
     * Servers may enforce a maximum number of entries to return.
     * 
     * @return search size limit.
     */
    public long getSizeLimit()
    {
        return sizeLimit;
    }


    /**
     * Sets sizelimit that restricts the maximum number of entries to be
     * returned as a result of the search. A value of 0 in this field indicates
     * that no client-requested sizelimit restrictions are in effect for the
     * search. Servers may enforce a maximum number of entries to return.
     * 
     * @param entriesMax maximum search result entries to return.
     */
    public void setSizeLimit( long entriesMax )
    {
        sizeLimit = entriesMax;
    }


    /**
     * Gets the timelimit that restricts the maximum time (in seconds) allowed
     * for a search. A value of 0 in this field indicates that no client-
     * requested timelimit restrictions are in effect for the search.
     * 
     * @return the search time limit in seconds.
     */
    public int getTimeLimit()
    {
        return timeLimit;
    }


    /**
     * Sets the timelimit that restricts the maximum time (in seconds) allowed
     * for a search. A value of 0 in this field indicates that no client-
     * requested timelimit restrictions are in effect for the search.
     * 
     * @param secondsMax the search time limit in seconds.
     */
    public void setTimeLimit( int secondsMax )
    {
        timeLimit = secondsMax;
    }


    /**
     * An indicator as to whether search results will contain both attribute
     * types and values, or just attribute types. Setting this field to TRUE
     * causes only attribute types (no values) to be returned. Setting this
     * field to FALSE causes both attribute types and values to be returned.
     * 
     * @return true for only types, false for types and values.
     */
    public boolean getTypesOnly()
    {
        return typesOnly;
    }


    /**
     * An indicator as to whether search results will contain both attribute
     * types and values, or just attribute types. Setting this field to TRUE
     * causes only attribute types (no values) to be returned. Setting this
     * field to FALSE causes both attribute types and values to be returned.
     * 
     * @param typesOnly true for only types, false for types and values.
     */
    public void setTypesOnly( boolean typesOnly )
    {
        this.typesOnly = typesOnly;
    }


    /**
     * {@inheritDoc}
     */
    public void addAttributes( String... attributesToAdd )
    {
        this.attributes.addAll( Arrays.asList( attributesToAdd ) );
    }


    /**
     * Removes an attribute to the set of entry attributes to return.
     * 
     * @param attribute the attribute description or identifier.
     */
    public void removeAttribute( String attribute )
    {
        attributes.remove( attribute );
    }


    /**
     * The result containing response for this request.
     * 
     * @return the result containing response for this request
     */
    public ResultResponse getResultResponse()
    {
        if ( response == null )
        {
            response = new SearchResultDoneImpl( getMessageId() );
        }

        return response;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int hash = 37;

        if ( baseDn != null )
        {
            hash = hash * 17 + baseDn.hashCode();
        }

        hash = hash * 17 + aliasDerefMode.hashCode();
        hash = hash * 17 + scope.hashCode();
        hash = hash * 17 + Long.valueOf( sizeLimit ).hashCode();
        hash = hash * 17 + timeLimit;
        hash = hash * 17 + ( typesOnly ? 0 : 1 );

        if ( attributes != null )
        {
            hash = hash * 17 + attributes.size();

            // Order doesn't matter, thus just add hashCode
            for ( String attr : attributes )
            {
                hash = hash + attr.hashCode();
            }
        }

        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();
        filterNode.accept( visitor );
        hash = hash * 17 + filterNode.toString().hashCode();
        hash = hash * 17 + super.hashCode();

        return hash;
    }


    /**
     * Checks to see if two search requests are equal. The Lockable properties
     * and the get/set context specific parameters are not consulted to
     * determine equality. The filter expression tree comparison will normalize
     * the child order of filter branch nodes then generate a string
     * representation which is comparable. For the time being this is a very
     * costly operation.
     * 
     * @param obj the object to check for equality to this SearchRequest
     * @return true if the obj is a SearchRequest and equals this SearchRequest,
     *         false otherwise
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( ! super.equals( obj ) )
        {
            return false;
        }

        SearchRequest req = ( SearchRequest ) obj;

        if ( !req.getBase().equals( baseDn ) )
        {
            return false;
        }

        if ( req.getDerefAliases() != aliasDerefMode )
        {
            return false;
        }

        if ( req.getScope() != scope )
        {
            return false;
        }

        if ( req.getSizeLimit() != sizeLimit )
        {
            return false;
        }

        if ( req.getTimeLimit() != timeLimit )
        {
            return false;
        }

        if ( req.getTypesOnly() != typesOnly )
        {
            return false;
        }

        if ( req.getAttributes() == null && attributes != null && attributes.size() > 0 )
        {
            return false;
        }

        if ( req.getAttributes() != null && attributes == null && req.getAttributes().size() > 0 )
        {
            return false;
        }

        if ( req.getAttributes() != null && attributes != null )
        {
            if ( req.getAttributes().size() != attributes.size() )
            {
                return false;
            }

            for ( String attribute : attributes )
            {
                if ( ! req.getAttributes().contains( attribute ) )
                {
                    return false;
                }
            }
        }

        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();
        req.getFilter().accept( visitor );
        filterNode.accept( visitor );

        String myFilterString = filterNode.toString();
        String reqFilterString = req.getFilter().toString();

        return myFilterString.equals( reqFilterString );
    }


    /**
     * Return a string the represent a SearchRequest
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "    SearchRequest\n" );
        sb.append( "        baseDn : '" ).append( baseDn ).append( "'\n" );

        if ( filterNode != null )
        {
            sb.append( "        filter : '" );
            sb.append( filterNode.toString() );
            sb.append( "'\n" );
        }

        sb.append( "        scope : " );

        switch ( scope )
        {
            case OBJECT:
                sb.append( "base object" );
                break;

            case ONELEVEL:
                sb.append( "single level" );
                break;

            case SUBTREE:
                sb.append( "whole subtree" );
                break;
        }

        sb.append( '\n' );

        sb.append( "        typesOnly : " ).append( typesOnly ).append( '\n' );

        sb.append( "        Size Limit : " );

        if ( sizeLimit == 0L )
        {
            sb.append( "no limit" );
        }
        else
        {
            sb.append( sizeLimit );
        }

        sb.append( '\n' );

        sb.append( "        Time Limit : " );

        if ( timeLimit == 0 )
        {
            sb.append( "no limit" );
        }
        else
        {
            sb.append( timeLimit );
        }

        sb.append( '\n' );

        sb.append( "        Deref Aliases : " );

        switch ( aliasDerefMode )
        {
            case NEVER_DEREF_ALIASES:
                sb.append( "never Deref Aliases" );
                break;

            case DEREF_IN_SEARCHING:
                sb.append( "deref In Searching" );
                break;

            case DEREF_FINDING_BASE_OBJ:
                sb.append( "deref Finding Base Obj" );
                break;

            case DEREF_ALWAYS:
                sb.append( "deref Always" );
                break;
        }

        sb.append( '\n' );
        sb.append( "        attributes : " );

        boolean isFirst = true;

        if ( attributes != null )
        {
            for ( String attribute : attributes )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append(", ");
                }

                sb.append('\'').append( attribute ).append('\'');
            }
        }

        sb.append( '\n' );

        // The controls
        sb.append( super.toString() );

        return sb.toString();
    }
}