package org.hisp.dhis.program;

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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.parser.expression.*;
import org.hisp.dhis.parser.expression.literal.DefaultLiteral;
import org.hisp.dhis.parser.expression.literal.SqlLiteral;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.parser.expression.ParserUtils.*;
import static org.hisp.dhis.program.DefaultProgramIndicatorService.PROGRAM_INDICATOR_FUNCTIONS;
import static org.hisp.dhis.program.DefaultProgramIndicatorService.PROGRAM_INDICATOR_ITEMS;
import static org.mockito.Mockito.when;

/**
 * @author Jim Grace
 */
public class ProgramSqlGeneratorItemsTest
    extends DhisConvenienceTest
{
    private ProgramIndicator programIndicator;

    private ProgramStage programStageA;

    private Program programA;

    private DataElement dataElementA;

    private TrackedEntityAttribute attributeA;

    private Constant constantA;

    private Map<String, Constant> constantMap;

    private Date startDate = getDate( 2020, 1, 1 );

    private Date endDate = getDate( 2020, 12, 31 );

    @org.junit.Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Mock
    private ConstantService constantService;

    @Mock
    private ProgramStageService programStageService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private TrackedEntityAttributeService attributeService;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    private StatementBuilder statementBuilder;

    @Before
    public void setUp()
    {
        dataElementA = createDataElement( 'A' );
        dataElementA.setDomainType( DataElementDomain.TRACKER );
        dataElementA.setUid( "DataElmentA" );

        attributeA = createTrackedEntityAttribute( 'A', ValueType.NUMBER );
        attributeA.setUid( "Attribute0A" );

        constantA = new Constant( "Constant A", 123.456 );
        constantA.setUid( "constant00A" );

        constantMap = new ImmutableMap.Builder<String, Constant>()
            .put( "constant00A", new Constant( "constant", 123.456 ) )
            .build();

        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );

        programStageA = new ProgramStage( "StageA", programA );
        programStageA.setSortOrder( 1 );
        programStageA.setUid( "ProgrmStagA" );

        programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programA.setUid( "Program000A" );

        statementBuilder = new PostgreSQLStatementBuilder();

        programIndicator = new ProgramIndicator();
        programIndicator.setProgram( programA );
        programIndicator.setAnalyticsType( AnalyticsType.EVENT );
    }

    @Test
    public void testDataElement()
    {
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );

        String sql = test( "#{ProgrmStagA.DataElmentA}" );
        assertThat( sql, is( "coalesce(\"DataElmentA\"::numeric,0)" ) );
    }

    @Test
    public void testDataElementAllowingNulls()
    {
        when( dataElementService.getDataElement( dataElementA.getUid() ) ).thenReturn( dataElementA );
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );

        String sql = test( "d2:oizp(#{ProgrmStagA.DataElmentA})" );
        assertThat( sql, is( "coalesce(case when \"DataElmentA\" >= 0 then 1 else 0 end, 0)" ) );
    }

    @Test
    public void testDataElementNotFound()
    {
        when( attributeService.getTrackedEntityAttribute( attributeA.getUid() ) ).thenReturn( attributeA );
        when( constantService.getConstant( constantA.getUid() ) ).thenReturn( constantA );
        when( programStageService.getProgramStage( programStageA.getUid() ) ).thenReturn( programStageA );

        thrown.expect( ParserException.class );
        test( "#{ProgrmStagA.NotElementA}" );
    }

    @Test
    public void testAttribute()
    {
        when( attributeService.getTrackedEntityAttribute( attributeA.getUid() ) ).thenReturn( attributeA );

        String sql = test( "A{Attribute0A}" );
        assertThat( sql, is( "coalesce(\"Attribute0A\"::numeric,0)" ) );
    }

    @Test
    public void testAttributeAllowingNulls()
    {
        when( attributeService.getTrackedEntityAttribute( attributeA.getUid() ) ).thenReturn( attributeA );

        String sql = test( "d2:oizp(A{Attribute0A})" );
        assertThat( sql, is( "coalesce(case when \"Attribute0A\" >= 0 then 1 else 0 end, 0)" ) );
    }

    @Test
    public void testAttributeNotFound()
    {
        thrown.expect( ParserException.class );
        test( "A{NoAttribute}" );
    }

    @Test
    public void testConstant()
    {
        String sql = test( "C{constant00A}" );
        assertThat( sql, is( "123.456" ) );
    }

    @Test
    public void testConstantNotFound()
    {
        thrown.expect( ParserException.class );
        test( "C{notConstant}" );
    }

    @Test
    public void testInvalidItemType()
    {
        thrown.expect( ParserException.class );
        test( "I{notValidItm}" );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String test( String expression )
    {
        test( expression, new DefaultLiteral(), FUNCTION_EVALUATE, ITEM_GET_DESCRIPTIONS );

        return castString( test( expression, new SqlLiteral(), FUNCTION_GET_SQL, ITEM_GET_SQL ) );
    }

    private Object test( String expression, ExprLiteral exprLiteral,
        ExprFunctionMethod functionMethod, ExprItemMethod itemMethod )
    {
        Set<String> dataElementsAndAttributesIdentifiers = new LinkedHashSet<>();
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "a" );
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "b" );
        dataElementsAndAttributesIdentifiers.add( BASE_UID + "c" );

        CommonExpressionVisitor visitor = CommonExpressionVisitor.newBuilder()
            .withFunctionMap( PROGRAM_INDICATOR_FUNCTIONS )
            .withItemMap( PROGRAM_INDICATOR_ITEMS )
            .withFunctionMethod( functionMethod )
            .withItemMethod( itemMethod )
            .withConstantMap( constantMap )
            .withProgramIndicatorService( programIndicatorService )
            .withProgramStageService( programStageService )
            .withDataElementService( dataElementService )
            .withAttributeService( attributeService )
            .withRelationshipTypeService( relationshipTypeService )
            .withStatementBuilder( statementBuilder )
            .withI18n( new I18n( null, null ) )
            .withSamplePeriods( DEFAULT_SAMPLE_PERIODS )
            .buildForProgramIndicatorExpressions();

        visitor.setExpressionLiteral( exprLiteral );
        visitor.setProgramIndicator( programIndicator );
        visitor.setReportingStartDate( startDate );
        visitor.setReportingEndDate( endDate );
        visitor.setDataElementAndAttributeIdentifiers( dataElementsAndAttributesIdentifiers );

        return Parser.visit( expression, visitor );
    }
}