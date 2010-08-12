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
package org.apache.directory.shared.ldap.codec;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.controls.CodecControl;
import org.apache.directory.shared.ldap.message.AddResponseImpl;
import org.apache.directory.shared.ldap.message.BindResponseImpl;
import org.apache.directory.shared.ldap.message.CompareResponseImpl;
import org.apache.directory.shared.ldap.message.DeleteResponseImpl;
import org.apache.directory.shared.ldap.message.ExtendedResponseImpl;
import org.apache.directory.shared.ldap.message.IntermediateResponseImpl;
import org.apache.directory.shared.ldap.message.ModifyDnResponseImpl;
import org.apache.directory.shared.ldap.message.ModifyResponseImpl;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.message.internal.InternalAddResponse;
import org.apache.directory.shared.ldap.message.internal.InternalBindResponse;
import org.apache.directory.shared.ldap.message.internal.InternalCompareResponse;
import org.apache.directory.shared.ldap.message.internal.InternalDeleteResponse;
import org.apache.directory.shared.ldap.message.internal.InternalExtendedResponse;
import org.apache.directory.shared.ldap.message.internal.InternalIntermediateResponse;
import org.apache.directory.shared.ldap.message.internal.InternalLdapResult;
import org.apache.directory.shared.ldap.message.internal.InternalMessage;
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnResponse;
import org.apache.directory.shared.ldap.message.internal.InternalModifyResponse;
import org.apache.directory.shared.ldap.message.internal.InternalReferral;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;


/**
 * LDAP BER encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapProtocolEncoder extends ProtocolEncoderAdapter
{
    /**
     * Encode a Ldap request and write it to the remote server.
     * 
     * @param session The session containing the LdapMessageContainer
     * @param message The LDAP message we have to encode to a Byte stream
     * @param out The callback we have to invoke when the message has been encoded 
     */
    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws Exception
    {
        ByteBuffer buffer = encodeMessage( ( InternalMessage ) message );

        IoBuffer ioBuffer = IoBuffer.wrap( buffer );

        out.write( ioBuffer );
    }


    /**
     * Generate the PDU which contains the encoded object. 
     * 
     * The generation is done in two phases : 
     * - first, we compute the length of each part and the
     * global PDU length 
     * - second, we produce the PDU. 
     * 
     * <pre>
     * 0x30 L1 
     *   | 
     *   +--> 0x02 L2 MessageId  
     *   +--> ProtocolOp 
     *   +--> Controls 
     *   
     * L2 = Length(MessageId)
     * L1 = Length(0x02) + Length(L2) + L2 + Length(ProtocolOp) + Length(Controls)
     * LdapMessageLength = Length(0x30) + Length(L1) + L1
     * </pre>
     * 
     * @param message The message to encode
     * @return A ByteBuffer that contains the PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encodeMessage( InternalMessage message ) throws EncoderException
    {
        int length = computeMessageLength( message );
        ByteBuffer buffer = ByteBuffer.allocate( length );

        if ( ( message instanceof InternalBindResponse ) || ( message instanceof InternalDeleteResponse )
            || ( message instanceof InternalAddResponse ) || ( message instanceof InternalCompareResponse )
            || ( message instanceof InternalExtendedResponse ) || ( message instanceof InternalModifyResponse )
            || ( message instanceof InternalModifyDnResponse ) || ( message instanceof InternalIntermediateResponse ) )
        {
            try
            {
                try
                {
                    // The LdapMessage Sequence
                    buffer.put( UniversalTag.SEQUENCE_TAG );

                    // The length has been calculated by the computeLength method
                    buffer.put( TLV.getBytes( message.getMessageLength() ) );
                }
                catch ( BufferOverflowException boe )
                {
                    throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
                }

                // The message Id
                Value.encode( buffer, message.getMessageId() );

                // Add the protocolOp part
                encodeProtocolOp( buffer, message );

                // Do the same thing for Controls, if any.
                Map<String, Control> controls = message.getControls();

                if ( ( controls != null ) && ( controls.size() > 0 ) )
                {
                    // Encode the controls
                    buffer.put( ( byte ) LdapConstants.CONTROLS_TAG );
                    buffer.put( TLV.getBytes( message.getControlsLength() ) );

                    // Encode each control
                    for ( Control control : controls.values() )
                    {
                        ( ( CodecControl ) control ).encode( buffer );
                    }
                }
            }
            catch ( EncoderException ee )
            {
                MessageEncoderException exception = new MessageEncoderException( message.getMessageId(), ee
                    .getMessage() );

                throw exception;
            }
        }
        else
        {
            LdapMessageCodec ldapRequest = ( LdapMessageCodec ) LdapTransformer.transform( message );
            buffer = ldapRequest.encode();
        }

        buffer.flip();

        return buffer;
    }


    /**
     * Compute the LdapMessage length LdapMessage : 
     * 0x30 L1 
     *   | 
     *   +--> 0x02 0x0(1-4) [0..2^31-1] (MessageId) 
     *   +--> protocolOp 
     *   [+--> Controls] 
     *   
     * MessageId length = Length(0x02) + length(MessageId) + MessageId.length 
     * L1 = length(ProtocolOp) 
     * LdapMessage length = Length(0x30) + Length(L1) + MessageId length + L1
     */
    private int computeMessageLength( InternalMessage message )
    {
        // The length of the MessageId. It's the sum of
        // - the tag (0x02), 1 byte
        // - the length of the Id length, 1 byte
        // - the Id length, 1 to 4 bytes
        int ldapMessageLength = 1 + 1 + Value.getNbBytes( message.getMessageId() );

        // Get the protocolOp length
        ldapMessageLength += computeProtocolOpLength( message );

        Map<String, Control> controls = message.getControls();

        // Do the same thing for Controls, if any.
        if ( controls.size() > 0 )
        {
            // Controls :
            // 0xA0 L3
            //   |
            //   +--> 0x30 L4
            //   +--> 0x30 L5
            //   +--> ...
            //   +--> 0x30 Li
            //   +--> ...
            //   +--> 0x30 Ln
            //
            // L3 = Length(0x30) + Length(L5) + L5
            // + Length(0x30) + Length(L6) + L6
            // + ...
            // + Length(0x30) + Length(Li) + Li
            // + ...
            // + Length(0x30) + Length(Ln) + Ln
            //
            // LdapMessageLength = LdapMessageLength + Length(0x90)
            // + Length(L3) + L3
            int controlsSequenceLength = 0;

            // We may have more than one control. ControlsLength is L4.
            for ( Control control : controls.values() )
            {
                controlsSequenceLength += ( ( CodecControl ) control ).computeLength();
            }

            // Computes the controls length
            // 1 + Length.getNbBytes( controlsSequenceLength ) + controlsSequenceLength;
            message.setControlsLength( controlsSequenceLength );

            // Now, add the tag and the length of the controls length
            ldapMessageLength += 1 + TLV.getNbBytes( controlsSequenceLength ) + controlsSequenceLength;
        }

        // Store the messageLength
        message.setMessageLength( ldapMessageLength );

        // finally, calculate the global message size :
        // length(Tag) + Length(length) + length

        return 1 + ldapMessageLength + TLV.getNbBytes( ldapMessageLength );
    }


    /**
     * Compute the LdapResult length 
     * 
     * LdapResult : 
     * 0x0A 01 resultCode (0..80)
     *   0x04 L1 matchedDN (L1 = Length(matchedDN)) 
     *   0x04 L2 errorMessage (L2 = Length(errorMessage)) 
     *   [0x83 L3] referrals 
     *     | 
     *     +--> 0x04 L4 referral 
     *     +--> 0x04 L5 referral 
     *     +--> ... 
     *     +--> 0x04 Li referral 
     *     +--> ... 
     *     +--> 0x04 Ln referral 
     *     
     * L1 = Length(matchedDN) 
     * L2 = Length(errorMessage) 
     * L3 = n*Length(0x04) + sum(Length(L4) .. Length(Ln)) + sum(L4..Ln) 
     * L4..n = Length(0x04) + Length(Li) + Li 
     * Length(LdapResult) = Length(0x0x0A) +
     *      Length(0x01) + 1 + Length(0x04) + Length(L1) + L1 + Length(0x04) +
     *      Length(L2) + L2 + Length(0x83) + Length(L3) + L3
     */
    private int computeLdapResultLength( InternalLdapResult ldapResult )
    {
        int ldapResultLength = 0;

        // The result code : always 3 bytes
        ldapResultLength = 1 + 1 + 1;

        // The matchedDN length
        if ( ldapResult.getMatchedDn() == null )
        {
            ldapResultLength += 1 + 1;
        }
        else
        {
            byte[] matchedDNBytes = StringTools.getBytesUtf8( StringTools
                .trimLeft( ldapResult.getMatchedDn().getName() ) );
            ldapResultLength += 1 + TLV.getNbBytes( matchedDNBytes.length ) + matchedDNBytes.length;
            ldapResult.setMatchedDnBytes( matchedDNBytes );
        }

        // The errorMessage length
        byte[] errorMessageBytes = StringTools.getBytesUtf8( ldapResult.getErrorMessage() );
        ldapResultLength += 1 + TLV.getNbBytes( errorMessageBytes.length ) + errorMessageBytes.length;
        ldapResult.setErrorMessageBytes( errorMessageBytes );

        InternalReferral referral = ldapResult.getReferral();

        if ( referral != null )
        {
            Collection<String> ldapUrls = referral.getLdapUrls();

            if ( ( ldapUrls != null ) && ( ldapUrls.size() != 0 ) )
            {
                int referralsLength = 0;

                // Each referral
                for ( String ldapUrl : ldapUrls )
                {
                    byte[] ldapUrlBytes = StringTools.getBytesUtf8( ldapUrl );
                    referralsLength += 1 + TLV.getNbBytes( ldapUrlBytes.length ) + ldapUrlBytes.length;
                    referral.addLdapUrlBytes( ldapUrlBytes );
                }

                ldapResult.setReferralsLength( referralsLength );

                // The referrals
                ldapResultLength += 1 + TLV.getNbBytes( referralsLength ) + referralsLength;
            }
        }

        return ldapResultLength;
    }


    /**
     * Encode the LdapResult message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    private ByteBuffer encodeLdapResult( ByteBuffer buffer, InternalLdapResult ldapResult ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04023 ) );
        }

        try
        {
            // The result code
            buffer.put( UniversalTag.ENUMERATED_TAG );
            buffer.put( ( byte ) 1 );
            buffer.put( ( byte ) ldapResult.getResultCode().getValue() );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        // The matchedDN
        Value.encode( buffer, ldapResult.getMatchedDnBytes() );

        // The error message
        Value.encode( buffer, ldapResult.getErrorMessageBytes() );

        // The referrals, if any
        InternalReferral referral = ldapResult.getReferral();

        if ( referral != null )
        {
            Collection<byte[]> ldapUrlsBytes = referral.getLdapUrlsBytes();

            if ( ( ldapUrlsBytes != null ) && ( ldapUrlsBytes.size() != 0 ) )
            {
                // Encode the referrals sequence
                // The referrals length MUST have been computed before !
                buffer.put( ( byte ) LdapConstants.LDAP_RESULT_REFERRAL_SEQUENCE_TAG );
                buffer.put( TLV.getBytes( ldapResult.getReferralsLength() ) );

                // Each referral
                for ( byte[] ldapUrlBytes : ldapUrlsBytes )
                {
                    // Encode the current referral
                    Value.encode( buffer, ldapUrlBytes );
                }
            }
        }

        return buffer;
    }


    /**
     * Compute the AddResponse length 
     * 
     * AddResponse : 
     * 
     * 0x69 L1
     *  |
     *  +--> LdapResult
     * 
     * L1 = Length(LdapResult)
     * 
     * Length(AddResponse) = Length(0x69) + Length(L1) + L1
     */
    private int computeAddResponseLength( AddResponseImpl addResponse )
    {
        int addResponseLength = computeLdapResultLength( addResponse.getLdapResult() );

        addResponse.setAddResponseLength( addResponseLength );

        return 1 + TLV.getNbBytes( addResponseLength ) + addResponseLength;
    }


    /**
     * Compute the BindResponse length 
     * 
     * BindResponse : 
     * <pre>
     * 0x61 L1 
     *   | 
     *   +--> LdapResult
     *   +--> [serverSaslCreds] 
     *   
     * L1 = Length(LdapResult) [ + Length(serverSaslCreds) ] 
     * Length(BindResponse) = Length(0x61) + Length(L1) + L1
     * </pre>
     */
    private int computeBindResponseLength( BindResponseImpl bindResponse )
    {
        int ldapResultLength = computeLdapResultLength( bindResponse.getLdapResult() );

        int bindResponseLength = ldapResultLength;

        byte[] serverSaslCreds = bindResponse.getServerSaslCreds();

        if ( serverSaslCreds != null )
        {
            bindResponseLength += 1 + TLV.getNbBytes( serverSaslCreds.length ) + serverSaslCreds.length;
        }

        bindResponse.setBindResponseLength( bindResponseLength );

        return 1 + TLV.getNbBytes( bindResponseLength ) + bindResponseLength;
    }


    /**
     * Compute the CompareResponse length 
     * 
     * CompareResponse :
     * 
     * 0x6F L1
     *  |
     *  +--> LdapResult
     * 
     * L1 = Length(LdapResult)
     * 
     * Length(CompareResponse) = Length(0x6F) + Length(L1) + L1
     */
    private int computeCompareResponseLength( CompareResponseImpl compareResponse )
    {
        int compareResponseLength = computeLdapResultLength( compareResponse.getLdapResult() );

        compareResponse.setCompareResponseLength( compareResponseLength );

        return 1 + TLV.getNbBytes( compareResponseLength ) + compareResponseLength;
    }


    /**
     * Compute the DelResponse length 
     * 
     * DelResponse :
     * 
     * 0x6B L1
     *  |
     *  +--> LdapResult
     * 
     * L1 = Length(LdapResult)
     * 
     * Length(DelResponse) = Length(0x6B) + Length(L1) + L1
     */
    private int computeDeleteResponseLength( DeleteResponseImpl deleteResponse )
    {
        int deleteResponseLength = computeLdapResultLength( deleteResponse.getLdapResult() );

        deleteResponse.setDeleteResponseLength( deleteResponseLength );

        return 1 + TLV.getNbBytes( deleteResponseLength ) + deleteResponseLength;
    }


    /**
     * Compute the ExtendedResponse length
     * 
     * ExtendedResponse :
     * 
     * 0x78 L1
     *  |
     *  +--> LdapResult
     * [+--> 0x8A L2 name
     * [+--> 0x8B L3 response]]
     * 
     * L1 = Length(LdapResult)
     *      [ + Length(0x8A) + Length(L2) + L2
     *       [ + Length(0x8B) + Length(L3) + L3]]
     * 
     * Length(ExtendedResponse) = Length(0x78) + Length(L1) + L1
     * 
     * @return The ExtendedResponse length
     */
    private int computeExtendedResponseLength( InternalExtendedResponse extendedResponse )
    {
        int ldapResultLength = computeLdapResultLength( extendedResponse.getLdapResult() );

        int extendedResponseLength = ldapResultLength;

        String id = extendedResponse.getID();

        if ( id != null )
        {
            byte[] idBytes = StringTools.getBytesUtf8( id );
            extendedResponse.setIDBytes( idBytes );
            int idLength = idBytes.length;
            extendedResponseLength += 1 + TLV.getNbBytes( idLength ) + idLength;
        }

        byte[] encodedValue = extendedResponse.getEncodedValue();

        if ( encodedValue != null )
        {
            extendedResponseLength += 1 + TLV.getNbBytes( encodedValue.length ) + encodedValue.length;
        }

        extendedResponse.setExtendedResponseLength( extendedResponseLength );

        return 1 + TLV.getNbBytes( extendedResponseLength ) + extendedResponseLength;
    }


    /**
     * Compute the ModifyResponse length 
     * 
     * ModifyResponse : 
     * <pre>
     * 0x67 L1 
     *   | 
     *   +--> LdapResult 
     *   
     * L1 = Length(LdapResult) 
     * Length(ModifyResponse) = Length(0x67) + Length(L1) + L1
     * </pre>
     */
    private int computeModifyResponseLength( ModifyResponseImpl modifyResponse )
    {
        int modifyResponseLength = computeLdapResultLength( modifyResponse.getLdapResult() );

        modifyResponse.setModifyResponseLength( modifyResponseLength );

        return 1 + TLV.getNbBytes( modifyResponseLength ) + modifyResponseLength;
    }


    /**
     * Compute the ModifyDNResponse length 
     * 
     * ModifyDNResponse : 
     * <pre>
     * 0x6D L1 
     *   | 
     *   +--> LdapResult 
     *   
     * L1 = Length(LdapResult) 
     * Length(ModifyDNResponse) = Length(0x6D) + Length(L1) + L1
     * </pre>
     */
    private int computeModifyDnResponseLength( ModifyDnResponseImpl modifyDnResponse )
    {
        int modifyDnResponseLength = computeLdapResultLength( modifyDnResponse.getLdapResult() );

        modifyDnResponse.setModifyDnResponseLength( modifyDnResponseLength );

        return 1 + TLV.getNbBytes( modifyDnResponseLength ) + modifyDnResponseLength;
    }


    /**
     * Compute the intermediateResponse length
     * 
     * intermediateResponse :
     * 
     * 0x79 L1
     *  |
     * [+--> 0x80 L2 name
     * [+--> 0x81 L3 response]]
     * 
     * L1 = [ + Length(0x80) + Length(L2) + L2
     *      [ + Length(0x81) + Length(L3) + L3]]
     * 
     * Length(IntermediateResponse) = Length(0x79) + Length(L1) + L1
     * 
     * @return The IntermediateResponse length
     */
    private int computeIntermediateResponseLength( InternalIntermediateResponse intermediateResponse )
    {
        int intermediateResponseLength = 0;

        if ( !StringTools.isEmpty( intermediateResponse.getResponseName() ) )
        {
            byte[] responseNameBytes = StringTools.getBytesUtf8( intermediateResponse.getResponseName() );

            int responseNameLength = responseNameBytes.length;
            intermediateResponseLength += 1 + TLV.getNbBytes( responseNameLength ) + responseNameLength;
            intermediateResponse.setOidBytes( responseNameBytes );
        }

        byte[] encodedValue = intermediateResponse.getResponseValue();

        if ( encodedValue != null )
        {
            intermediateResponseLength += 1 + TLV.getNbBytes( encodedValue.length ) + encodedValue.length;
        }

        intermediateResponse.setIntermediateResponseLength( intermediateResponseLength );

        return 1 + TLV.getNbBytes( intermediateResponseLength ) + intermediateResponseLength;
    }


    /**
     * Encode the AddResponse message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     */
    private void encodeAddResponse( ByteBuffer buffer, AddResponseImpl addResponse ) throws EncoderException
    {
        try
        {
            // The AddResponse Tag
            buffer.put( LdapConstants.ADD_RESPONSE_TAG );
            buffer.put( TLV.getBytes( addResponse.getAddResponseLength() ) );

            // The LdapResult
            encodeLdapResult( buffer, addResponse.getLdapResult() );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Encode the BindResponse message to a PDU.
     * 
     * BindResponse :
     * <pre>
     * LdapResult.encode 
     * [0x87 LL serverSaslCreds]
     * </pre>
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    private void encodeBindResponse( ByteBuffer bb, BindResponseImpl bindResponse ) throws EncoderException
    {
        try
        {
            // The BindResponse Tag
            bb.put( LdapConstants.BIND_RESPONSE_TAG );
            bb.put( TLV.getBytes( bindResponse.getBindResponseLength() ) );

            // The LdapResult
            encodeLdapResult( bb, bindResponse.getLdapResult() );

            // The serverSaslCredential, if any
            byte[] serverSaslCreds = bindResponse.getServerSaslCreds();

            if ( serverSaslCreds != null )
            {
                bb.put( ( byte ) LdapConstants.SERVER_SASL_CREDENTIAL_TAG );

                bb.put( TLV.getBytes( serverSaslCreds.length ) );

                if ( serverSaslCreds.length != 0 )
                {
                    bb.put( serverSaslCreds );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Encode the CompareResponse message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     */
    private void encodeCompareResponse( ByteBuffer buffer, CompareResponseImpl compareResponse )
        throws EncoderException
    {
        try
        {
            // The CompareResponse Tag
            buffer.put( LdapConstants.COMPARE_RESPONSE_TAG );
            buffer.put( TLV.getBytes( compareResponse.getCompareResponseLength() ) );

            // The LdapResult
            encodeLdapResult( buffer, compareResponse.getLdapResult() );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Encode the DelResponse message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     */
    private void encodeDeleteResponse( ByteBuffer buffer, DeleteResponseImpl deleteResponse ) throws EncoderException
    {
        try
        {
            // The DelResponse Tag
            buffer.put( LdapConstants.DEL_RESPONSE_TAG );
            buffer.put( TLV.getBytes( deleteResponse.getDeleteResponseLength() ) );

            // The LdapResult
            encodeLdapResult( buffer, deleteResponse.getLdapResult() );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Encode the ExtendedResponse message to a PDU. 
     * ExtendedResponse :
     * LdapResult.encode()
     * [0x8A LL response name]
     * [0x8B LL response]
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    private void encodeExtendedResponse( ByteBuffer buffer, ExtendedResponseImpl extendedResponse )
        throws EncoderException
    {
        try
        {
            // The ExtendedResponse Tag
            buffer.put( LdapConstants.EXTENDED_RESPONSE_TAG );
            buffer.put( TLV.getBytes( extendedResponse.getExtendedResponseLength() ) );

            // The LdapResult
            encodeLdapResult( buffer, extendedResponse.getLdapResult() );

            // The ID, if any
            byte[] idBytes = extendedResponse.getIDBytes();

            if ( idBytes != null )
            {
                buffer.put( ( byte ) LdapConstants.EXTENDED_RESPONSE_RESPONSE_NAME_TAG );
                buffer.put( TLV.getBytes( idBytes.length ) );

                if ( idBytes.length != 0 )
                {
                    buffer.put( idBytes );
                }
            }

            // The encodedValue, if any
            byte[] encodedValue = extendedResponse.getEncodedValue();

            if ( encodedValue != null )
            {
                buffer.put( ( byte ) LdapConstants.EXTENDED_RESPONSE_RESPONSE_TAG );

                buffer.put( TLV.getBytes( encodedValue.length ) );

                if ( encodedValue.length != 0 )
                {
                    buffer.put( encodedValue );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Encode the IntermediateResponse message to a PDU. 
     * IntermediateResponse :
     *   0x79 LL
     *     [0x80 LL response name]
     *     [0x81 LL responseValue]
     * 
     * @param buffer The buffer where to put the PDU
     */
    private void encodeIntermediateResponse( ByteBuffer buffer, IntermediateResponseImpl intermediateResponse )
        throws EncoderException
    {
        try
        {
            // The ExtendedResponse Tag
            buffer.put( LdapConstants.INTERMEDIATE_RESPONSE_TAG );
            buffer.put( TLV.getBytes( intermediateResponse.getIntermediateResponseLength() ) );

            // The responseName, if any
            byte[] responseNameBytes = intermediateResponse.getOidBytes();

            if ( ( responseNameBytes != null ) && ( responseNameBytes.length != 0 ) )
            {
                buffer.put( ( byte ) LdapConstants.INTERMEDIATE_RESPONSE_NAME_TAG );
                buffer.put( TLV.getBytes( responseNameBytes.length ) );
                buffer.put( responseNameBytes );
            }

            // The encodedValue, if any
            byte[] encodedValue = intermediateResponse.getResponseValue();

            if ( encodedValue != null )
            {
                buffer.put( ( byte ) LdapConstants.INTERMEDIATE_RESPONSE_VALUE_TAG );

                buffer.put( TLV.getBytes( encodedValue.length ) );

                if ( encodedValue.length != 0 )
                {
                    buffer.put( encodedValue );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Encode the ModifyResponse message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     */
    private void encodeModifyResponse( ByteBuffer buffer, ModifyResponseImpl modifyResponse ) throws EncoderException
    {
        try
        {
            // The ModifyResponse Tag
            buffer.put( LdapConstants.MODIFY_RESPONSE_TAG );
            buffer.put( TLV.getBytes( modifyResponse.getModifyResponseLength() ) );

            // The LdapResult
            encodeLdapResult( buffer, modifyResponse.getLdapResult() );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Encode the ModifyDnResponse message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     */
    private void encodeModifyDnResponse( ByteBuffer buffer, ModifyDnResponseImpl modifyDnResponse )
        throws EncoderException
    {
        try
        {
            // The ModifyResponse Tag
            buffer.put( LdapConstants.MODIFY_DN_RESPONSE_TAG );
            buffer.put( TLV.getBytes( modifyDnResponse.getModifyDnResponseLength() ) );

            // The LdapResult
            encodeLdapResult( buffer, modifyDnResponse.getLdapResult() );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Compute the protocolOp length 
     */
    private int computeProtocolOpLength( InternalMessage message )
    {
        switch ( message.getType() )
        {
            case ADD_RESPONSE:
                return computeAddResponseLength( ( AddResponseImpl ) message );

            case BIND_RESPONSE:
                return computeBindResponseLength( ( BindResponseImpl ) message );

            case COMPARE_RESPONSE:
                return computeCompareResponseLength( ( CompareResponseImpl ) message );

            case DEL_RESPONSE:
                return computeDeleteResponseLength( ( DeleteResponseImpl ) message );

            case EXTENDED_RESPONSE:
                return computeExtendedResponseLength( ( ExtendedResponseImpl ) message );

            case INTERMEDIATE_RESPONSE:
                return computeIntermediateResponseLength( ( IntermediateResponseImpl ) message );

            case MODIFY_RESPONSE:
                return computeModifyResponseLength( ( ModifyResponseImpl ) message );

            case MODIFYDN_RESPONSE:
                return computeModifyDnResponseLength( ( ModifyDnResponseImpl ) message );

            default:
                return 0;
        }
    }


    private void encodeProtocolOp( ByteBuffer bb, InternalMessage message ) throws EncoderException
    {
        switch ( message.getType() )
        {
            case ADD_RESPONSE:
                encodeAddResponse( bb, ( AddResponseImpl ) message );
                break;

            case BIND_RESPONSE:
                encodeBindResponse( bb, ( BindResponseImpl ) message );
                break;

            case COMPARE_RESPONSE:
                encodeCompareResponse( bb, ( CompareResponseImpl ) message );
                break;

            case DEL_RESPONSE:
                encodeDeleteResponse( bb, ( DeleteResponseImpl ) message );
                break;

            case EXTENDED_RESPONSE:
                encodeExtendedResponse( bb, ( ExtendedResponseImpl ) message );
                break;

            case INTERMEDIATE_RESPONSE:
                encodeIntermediateResponse( bb, ( IntermediateResponseImpl ) message );
                break;

            case MODIFY_RESPONSE:
                encodeModifyResponse( bb, ( ModifyResponseImpl ) message );
                break;

            case MODIFYDN_RESPONSE:
                encodeModifyDnResponse( bb, ( ModifyDnResponseImpl ) message );
                break;
        }
    }
}
