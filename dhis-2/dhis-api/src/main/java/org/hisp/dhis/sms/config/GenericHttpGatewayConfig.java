package org.hisp.dhis.sms.config;

/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.collect.Lists;
import org.hisp.dhis.sms.config.views.SmsConfigurationViews;

@JsonTypeName( "http" )
public class GenericHttpGatewayConfig
    extends SmsGatewayConfig
{
    private static final long serialVersionUID = 6340853488475760213L;

    @JsonView( SmsConfigurationViews.Public.class )
    private String configurationTemplate;

    @JsonView( SmsConfigurationViews.Public.class )
    private boolean useGet;

    @JsonView( SmsConfigurationViews.Public.class )
    private ContentType contentType = ContentType.FORM_URL_ENCODED;

    @JsonView( SmsConfigurationViews.Public.class )
    private List<GenericGatewayParameter> parameters = Lists.newArrayList();

    public List<GenericGatewayParameter> getParameters()
    {
        return parameters;
    }

    public void setParameters( List<GenericGatewayParameter> parameters )
    {
        this.parameters = parameters;
    }

    public boolean isUseGet()
    {
        return useGet;
    }

    public void setUseGet( boolean useGet )
    {
        this.useGet = useGet;
    }

    public String getConfigurationTemplate()
    {
        return configurationTemplate;
    }

    public void setConfigurationTemplate( String configurationTemplate )
    {
        this.configurationTemplate = configurationTemplate;
    }

    public ContentType getContentType()
    {
        return contentType;
    }

    public void setContentType( ContentType contentType )
    {
        this.contentType = contentType;
    }
}
