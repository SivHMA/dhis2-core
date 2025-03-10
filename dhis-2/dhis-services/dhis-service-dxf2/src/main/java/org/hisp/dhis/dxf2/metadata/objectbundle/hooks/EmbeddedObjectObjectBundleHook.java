package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

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

import org.hibernate.Session;
import org.hisp.dhis.common.BaseAnalyticalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.DefaultAnalyticalObjectImportHandler;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
public class EmbeddedObjectObjectBundleHook
    extends AbstractObjectBundleHook
{
    @Autowired
    private DefaultAnalyticalObjectImportHandler analyticalObjectImportHandler;

    @Autowired
    private SchemaValidator schemaValidator;

    @Override
    public List<ErrorReport> validate( IdentifiableObject object, ObjectBundle bundle )
    {
        List<ErrorReport> errors = new ArrayList<>();

        Class<? extends IdentifiableObject> klass = object.getClass();
        Schema schema = schemaService.getDynamicSchema( klass );

        schema.getEmbeddedObjectProperties().keySet()
            .stream()
            .forEach( propertyName -> {
                Property property = schema.getEmbeddedObjectProperties().get( propertyName );
                Object propertyObject = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

                if ( property.getPropertyType().equals( PropertyType.COMPLEX ) )
                {
                    List<ErrorReport> unformattedErrors = schemaValidator
                        .validateEmbeddedObject( propertyObject, klass );
                    errors.addAll( formatEmbeddedErrorReport( unformattedErrors, propertyName ) );
                }
                else if ( property.getPropertyType().equals( PropertyType.COLLECTION ) )
                {
                    Collection<?> collection = (Collection<?>) propertyObject;
                    for ( Object item : collection )
                    {
                        List<ErrorReport> unformattedErrors = schemaValidator
                            .validateEmbeddedObject( property.getItemKlass().cast( item ), klass );
                        errors.addAll( formatEmbeddedErrorReport( unformattedErrors, propertyName ) );
                    }
                }
            } );

        return errors;
    }

    private List<ErrorReport> formatEmbeddedErrorReport( List<ErrorReport> errors, String embeddedPropertyName )
    {
        for ( ErrorReport errorReport : errors )
        {
            errorReport.setErrorProperty( embeddedPropertyName + "." + errorReport.getErrorProperty() );
        }

        return errors;
    }

    @Override
    public <T extends IdentifiableObject> void preCreate( T object, ObjectBundle bundle )
    {
        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        if ( schema == null || schema.getEmbeddedObjectProperties().isEmpty() )
        {
            return;
        }

        Collection<Property> properties = schema.getEmbeddedObjectProperties().values();

        handleEmbeddedObjects( object, bundle, properties );
    }

    @Override
    public <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle )
    {
        Schema schema = schemaService.getDynamicSchema( object.getClass() );

        if ( schema == null || schema.getEmbeddedObjectProperties().isEmpty() )
        {
            return;
        }

        Collection<Property> properties = schema.getEmbeddedObjectProperties().values();

        clearEmbeddedObjects( persistedObject, bundle, properties );
        handleEmbeddedObjects( object, bundle, properties );
    }

    private <T extends IdentifiableObject> void clearEmbeddedObjects( T object, ObjectBundle bundle, Collection<Property> properties )
    {
        for ( Property property : properties )
        {
            if ( property.isCollection() )
            {
                if ( ReflectionUtils.isSharingProperty( property ) && bundle.isSkipSharing() )
                {
                    continue;
                }

                ( ( Collection<?> ) ReflectionUtils.invokeMethod( object, property.getGetterMethod() ) ).clear();
            }
            else
            {
                ReflectionUtils.invokeMethod( object, property.getSetterMethod(), (Object) null );
            }
        }
    }

    private <T extends IdentifiableObject> void handleEmbeddedObjects( T object, ObjectBundle bundle, Collection<Property> properties )
    {
        for ( Property property : properties )
        {
            Object propertyObject =  ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

            if ( property.isCollection() )
            {
                Collection<?> objects = (Collection<?>) propertyObject;
                objects.forEach( itemPropertyObject ->
                {
                    handleProperty( itemPropertyObject, bundle, property );
                    handleEmbeddedAnalyticalProperty( itemPropertyObject, bundle, property );
                } );
            }
            else
            {
                handleProperty( propertyObject, bundle, property );
                handleEmbeddedAnalyticalProperty( propertyObject, bundle, property );
            }
        }
    }
    
    private void handleProperty( Object o, ObjectBundle bundle, Property property )
    {
        if ( o != null && bundle != null && property != null )
        {
            if ( property.isIdentifiableObject() )
            {
                ((BaseIdentifiableObject) o).setAutoFields();
            }

            Schema embeddedSchema = schemaService.getDynamicSchema( o.getClass() );
            for ( Property embeddedProperty : embeddedSchema.getPropertyMap().values() )
            {
                if ( PeriodType.class.isAssignableFrom( embeddedProperty.getKlass() ) )
                {
                    PeriodType periodType = ReflectionUtils.invokeMethod( o, embeddedProperty.getGetterMethod() );

                    if ( periodType != null )
                    {
                        periodType = bundle.getPreheat().getPeriodTypeMap().get( periodType.getName() );
                        ReflectionUtils.invokeMethod( o, embeddedProperty.getSetterMethod(), periodType );
                    }
                }
            }

            preheatService.connectReferences( o, bundle.getPreheat(), bundle.getPreheatIdentifier() );
        }
    }

    private void handleEmbeddedAnalyticalProperty(Object identifiableObject, ObjectBundle bundle, Property property )
    {
        if ( identifiableObject == null || !property.isAnalyticalObject() ) return;

        Session session = sessionFactory.getCurrentSession();

        Schema propertySchema = schemaService.getDynamicSchema( property.getItemKlass() );

        analyticalObjectImportHandler.handleAnalyticalObject( session, propertySchema, ( BaseAnalyticalObject ) identifiableObject, bundle );
    }
}
