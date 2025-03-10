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

package org.hisp.dhis.visualization;

import static java.util.Arrays.asList;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PRETTY_NAMES;
import static org.hisp.dhis.common.DimensionalObjectUtils.NAME_SEP;
import static org.hisp.dhis.common.DimensionalObjectUtils.getSortedKeysMap;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.NumberType;
import org.hisp.dhis.color.ColorSet;
import org.hisp.dhis.common.*;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.legend.LegendDisplayStrategy;
import org.hisp.dhis.legend.LegendDisplayStyle;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.User;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "visualization", namespace = DXF_2_0 )
public class Visualization
    extends BaseAnalyticalObject
        implements MetadataObject
{
    public static final String REPORTING_MONTH_COLUMN_NAME = "reporting_month_name";
    public static final String PARAM_ORGANISATIONUNIT_COLUMN_NAME = "param_organisationunit_name";
    public static final String ORGANISATION_UNIT_IS_PARENT_COLUMN_NAME = "organisation_unit_is_parent";
    public static final String SPACE = " ";
    public static final String TOTAL_COLUMN_NAME = "total";
    public static final String TOTAL_COLUMN_PRETTY_NAME = "Total";
    public static final String EMPTY = "";

    private static final String ILLEGAL_FILENAME_CHARS_REGEX = "[/\\?%*:|\"'<>.]";

    public static final Map<String, String> COLUMN_NAMES = DimensionalObjectUtils.asMap( DATA_X_DIM_ID, "data",
        CATEGORYOPTIONCOMBO_DIM_ID, "categoryoptioncombo", PERIOD_DIM_ID, "period", ORGUNIT_DIM_ID,
        "organisationunit" );

    // -------------------------------------------------------------------------
    // Common attributes
    // -------------------------------------------------------------------------

    /**
     * The type of this visualization object.
     */
    private VisualizationType type;

    /**
     * The object responsible to hold parameters related to reporting.
     */
    private ReportingParams reportingParams;

    /**
     * Indicates the criteria to apply to data measures.
     */
    private String measureCriteria;

    /**
     * Dimensions to cross tabulate / use as columns.
     */
    private List<String> columnDimensions = new ArrayList<>();

    /**
     * Dimensions to use as rows.
     */
    private List<String> rowDimensions = new ArrayList<>();

    /**
     * Dimensions to use as filter.
     */
    private List<String> filterDimensions = new ArrayList<>();

    /**
     * Indicates rendering of row totals for the table.
     */
    private boolean rowTotals;

    /**
     * Indicates rendering of column totals for the table.
     */
    private boolean colTotals;

    /**
     * Indicates rendering of row sub-totals for the table.
     */
    private boolean rowSubTotals;

    /**
     * Indicates rendering of column sub-totals for the table.
     */
    private boolean colSubTotals;

    /**
     * The number type.
     */
    private NumberType numberType;

    /**
     * The regression type.
     */
    private RegressionType regressionType = RegressionType.NONE;

    // -------------------------------------------------------------------------
    // Display definitions
    // -------------------------------------------------------------------------

    /*
     * # The display strategy for empty row items.
     */
    private HideEmptyItemStrategy hideEmptyRowItems = HideEmptyItemStrategy.NONE;

    /**
     * The display density of the text.
     */
    private DisplayDensity displayDensity;

    /**
     * The font size of the text.
     */
    private FontSize fontSize;

    /**
     * The list of optional axes for this visualization.
     */
    private List<Axis> optionalAxes = new ArrayList<>();

    /*
     * # Legend related attributes.
     */
    private LegendDisplayStyle legendDisplayStyle;

    private LegendSet legendSet;

    private LegendDisplayStrategy legendDisplayStrategy;

    private ColorSet colorSet;

    // -------------------------------------------------------------------------
    // Display items for graphics/charts
    // -------------------------------------------------------------------------

    /*
     * # Settings more likely to be applied to graphics or charts.
     */
    private Double targetLineValue;

    private Double baseLineValue;

    private String baseLineLabel;

    private String targetLineLabel;

    private Double rangeAxisMaxValue;

    private Double rangeAxisMinValue;

    private Integer rangeAxisSteps; // Minimum 1

    private Integer rangeAxisDecimals;

    private String domainAxisLabel;

    private String rangeAxisLabel;

    /**
     * The period of years of this visualization. See RelativePeriodEnum for a valid
     * list of enum based strings.
     */
    private List<String> yearlySeries = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Flags
    // -------------------------------------------------------------------------

    /**
     * Used by charts to hide or not data/values within the rendered model.
     */
    private boolean showData;

    /**
     * Apply or not rounding.
     */
    private boolean skipRounding;

    /**
     * Indicates whether the visualization contains regression columns. More likely
     * to be applicable to pivot and reports.
     */
    private boolean regression;

    /**
     * Indicates whether the visualization contains cumulative values or columns.
     */
    private boolean cumulative;

    /**
     * User stacked values or not. Very likely to be applied for graphics/charts.
     */
    private boolean percentStackedValues;

    /**
     * Indicates showing organisation unit hierarchy names.
     */
    private boolean showHierarchy;

    /**
     * Indicates showing organisation unit hierarchy names.
     */
    private boolean showDimensionLabels;

    /**
     * Indicates whether to hide rows with no data values.
     */
    private boolean hideEmptyRows;

    /**
     * Indicates whether to hide columns with no data values.
     */
    private boolean hideEmptyColumns;

    /**
     * Show/hide the legend. Very likely to be used by graphics/charts.
     */
    private boolean hideLegend;

    /**
     * Show/hide space between columns.
     */
    private boolean noSpaceBetweenColumns;

    // -------------------------------------------------------------------------
    // Non-persisted attributes, used for internal operation/rendering phase
    // -------------------------------------------------------------------------

    private transient I18nFormat format;

    private transient List<Period> relativePeriodsList = new ArrayList<>();

    private transient User relativeUser;

    private transient List<OrganisationUnit> organisationUnitsAtLevel = new ArrayList<>();

    private transient List<OrganisationUnit> organisationUnitsInGroups = new ArrayList<>();

    /**
     * The name of the visualization (monthly based).
     */
    private transient String visualizationPeriodName;

    private transient Map<String, String> parentGraphMap = new HashMap<>();

    /*
     * Common transient collections used to return the respective values to the
     * client. They are the main attributes for any kind of visualization.
     */
    private transient List<DimensionalObject> columns = new ArrayList<>();

    private transient List<DimensionalObject> rows = new ArrayList<>();

    private transient List<DimensionalObject> filters = new ArrayList<>();

    /**
     * Used to return tabular data, mainly related to analytics queries.
     */
    private transient Grid dataItemGrid = null;

    /**
     * The title for a possible tabulated data.
     */
    private transient String gridTitle;

    /*
     * Collections mostly used for analytics tabulated data, like pivots or reports.
     */
    private transient List<List<DimensionalItemObject>> gridColumns = new ArrayList<>();

    private transient List<List<DimensionalItemObject>> gridRows = new ArrayList<>();

    public Visualization()
    {
    }

    public Visualization( final String name )
    {
        this();
        this.name = name;
    }

    @Override
    @JsonProperty
    @JsonDeserialize( contentAs = BaseDimensionalObject.class )
    @JacksonXmlElementWrapper( localName = "columns", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "column", namespace = DXF_2_0 )
    public List<DimensionalObject> getColumns()
    {
        return columns;
    }

    @Override
    public void setColumns( List<DimensionalObject> columns )
    {
        this.columns = columns;
    }

    @Override
    @JsonProperty
    @JsonDeserialize( contentAs = BaseDimensionalObject.class )
    @JacksonXmlElementWrapper( localName = "rows", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "row", namespace = DXF_2_0 )
    public List<DimensionalObject> getRows()
    {
        return rows;
    }

    @Override
    public void setRows( List<DimensionalObject> rows )
    {
        this.rows = rows;
    }

    @Override
    @JsonProperty
    @JsonDeserialize( contentAs = BaseDimensionalObject.class )
    @JacksonXmlElementWrapper( localName = "filters", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "filter", namespace = DXF_2_0 )
    public List<DimensionalObject> getFilters()
    {
        return filters;
    }

    @Override
    public void setFilters( List<DimensionalObject> filters )
    {
        this.filters = filters;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Map<String, String> getParentGraphMap()
    {
        return parentGraphMap;
    }

    @Override
    public void setParentGraphMap( Map<String, String> parentGraphMap )
    {
        this.parentGraphMap = parentGraphMap;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "columnDimensions", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "columnDimension", namespace = DXF_2_0 )
    public List<String> getColumnDimensions()
    {
        return columnDimensions;
    }

    public void setColumnDimensions( List<String> columnDimensions )
    {
        this.columnDimensions = columnDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "rowDimensions", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "rowDimension", namespace = DXF_2_0 )
    public List<String> getRowDimensions()
    {
        return rowDimensions;
    }

    public void setRowDimensions( List<String> rowDimensions )
    {
        this.rowDimensions = rowDimensions;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "filterDimensions", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "filterDimension", namespace = DXF_2_0 )
    public List<String> getFilterDimensions()
    {
        return filterDimensions;
    }

    public void setFilterDimensions( List<String> filterDimensions )
    {
        this.filterDimensions = filterDimensions;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public ReportingParams getReportingParams()
    {
        return reportingParams;
    }

    public void setReportingParams( ReportingParams reportingParams )
    {
        this.reportingParams = reportingParams;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public LegendSet getLegendSet()
    {
        return legendSet;
    }

    public void setLegendSet( LegendSet legendSet )
    {
        this.legendSet = legendSet;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public LegendDisplayStrategy getLegendDisplayStrategy()
    {
        return legendDisplayStrategy;
    }

    public void setLegendDisplayStrategy( LegendDisplayStrategy legendDisplayStrategy )
    {
        this.legendDisplayStrategy = legendDisplayStrategy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getMeasureCriteria()
    {
        return measureCriteria;
    }

    public void setMeasureCriteria( String measureCriteria )
    {
        this.measureCriteria = measureCriteria;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isRegression()
    {
        return regression;
    }

    public void setRegression( boolean regression )
    {
        this.regression = regression;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isCumulative()
    {
        return cumulative;
    }

    public void setCumulative( boolean cumulative )
    {
        this.cumulative = cumulative;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isRowTotals()
    {
        return rowTotals;
    }

    public void setRowTotals( boolean rowTotals )
    {
        this.rowTotals = rowTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isColTotals()
    {
        return colTotals;
    }

    public void setColTotals( boolean colTotals )
    {
        this.colTotals = colTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isRowSubTotals()
    {
        return rowSubTotals;
    }

    public void setRowSubTotals( boolean rowSubTotals )
    {
        this.rowSubTotals = rowSubTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isColSubTotals()
    {
        return colSubTotals;
    }

    public void setColSubTotals( boolean colSubTotals )
    {
        this.colSubTotals = colSubTotals;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isHideEmptyRows()
    {
        return hideEmptyRows;
    }

    public void setHideEmptyRows( boolean hideEmptyRows )
    {
        this.hideEmptyRows = hideEmptyRows;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isHideEmptyColumns()
    {
        return hideEmptyColumns;
    }

    public void setHideEmptyColumns( boolean hideEmptyColumns )
    {
        this.hideEmptyColumns = hideEmptyColumns;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public DisplayDensity getDisplayDensity()
    {
        return displayDensity;
    }

    public void setDisplayDensity( DisplayDensity displayDensity )
    {
        this.displayDensity = displayDensity;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public FontSize getFontSize()
    {
        return fontSize;
    }

    public void setFontSize( FontSize fontSize )
    {
        this.fontSize = fontSize;
    }

    @JsonProperty( "optionalAxes" )
    @JacksonXmlElementWrapper( localName = "optionalAxes", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "axis", namespace = DXF_2_0 )
    public List<Axis> getOptionalAxes()
    {
        return optionalAxes;
    }

    public void setOptionalAxes(List<Axis> optionalAxes)
    {
        this.optionalAxes = optionalAxes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public LegendDisplayStyle getLegendDisplayStyle()
    {
        return legendDisplayStyle;
    }

    public void setLegendDisplayStyle( LegendDisplayStyle legendDisplayStyle )
    {
        this.legendDisplayStyle = legendDisplayStyle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public NumberType getNumberType()
    {
        return numberType;
    }

    public void setNumberType( NumberType numberType )
    {
        this.numberType = numberType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isShowHierarchy()
    {
        return showHierarchy;
    }

    public void setShowHierarchy( boolean showHierarchy )
    {
        this.showHierarchy = showHierarchy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isShowDimensionLabels()
    {
        return showDimensionLabels;
    }

    public void setShowDimensionLabels( boolean showDimensionLabels )
    {
        this.showDimensionLabels = showDimensionLabels;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isSkipRounding()
    {
        return skipRounding;
    }

    public void setSkipRounding( boolean skipRounding )
    {
        this.skipRounding = skipRounding;
    }

    @JsonIgnore
    public I18nFormat getFormat()
    {
        return format;
    }

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    @JsonIgnore
    public List<Period> getRelativePeriodsList()
    {
        return relativePeriodsList;
    }

    public void setRelativePeriodsList( List<Period> relativePeriodsList )
    {
        this.relativePeriodsList = relativePeriodsList;
    }

    @JsonIgnore
    public User getRelativeUser()
    {
        return relativeUser;
    }

    public void setRelativeUser( User relativeUser )
    {
        this.relativeUser = relativeUser;
    }

    @JsonIgnore
    public List<OrganisationUnit> getOrganisationUnitsAtLevel()
    {
        return organisationUnitsAtLevel;
    }

    public void setOrganisationUnitsAtLevel( List<OrganisationUnit> organisationUnitsAtLevel )
    {
        this.organisationUnitsAtLevel = organisationUnitsAtLevel;
    }

    @JsonIgnore
    public List<OrganisationUnit> getOrganisationUnitsInGroups()
    {
        return organisationUnitsInGroups;
    }

    public void setOrganisationUnitsInGroups( List<OrganisationUnit> organisationUnitsInGroups )
    {
        this.organisationUnitsInGroups = organisationUnitsInGroups;
    }

    @JsonIgnore
    public Grid getDataItemGrid()
    {
        return dataItemGrid;
    }

    public void setDataItemGrid( Grid dataItemGrid )
    {
        this.dataItemGrid = dataItemGrid;
    }

    @JsonIgnore
    public List<List<DimensionalItemObject>> getGridColumns()
    {
        return gridColumns;
    }

    public Visualization setGridColumns( List<List<DimensionalItemObject>> gridColumns )
    {
        this.gridColumns = gridColumns;
        return this;
    }

    @JsonIgnore
    public List<List<DimensionalItemObject>> getGridRows()
    {
        return gridRows;
    }

    public Visualization setGridRows( List<List<DimensionalItemObject>> gridRows )
    {
        this.gridRows = gridRows;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getVisualizationPeriodName()
    {
        return visualizationPeriodName;
    }

    public void setVisualizationPeriodName( String visualizationPeriodName )
    {
        this.visualizationPeriodName = visualizationPeriodName;
    }

    @JsonIgnore
    public String getGridTitle()
    {
        return gridTitle;
    }

    public Visualization setGridTitle( String gridTitle )
    {
        this.gridTitle = gridTitle;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public RegressionType getRegressionType()
    {
        return regressionType;
    }

    public void setRegressionType( RegressionType regressionType )
    {
        this.regressionType = regressionType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getDomainAxisLabel()
    {
        return domainAxisLabel;
    }

    public void setDomainAxisLabel( String domainAxisLabel )
    {
        this.domainAxisLabel = domainAxisLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getRangeAxisLabel()
    {
        return rangeAxisLabel;
    }

    public void setRangeAxisLabel( String rangeAxisLabel )
    {
        this.rangeAxisLabel = rangeAxisLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public VisualizationType getType()
    {
        return type;
    }

    public void setType( VisualizationType type )
    {
        this.type = type;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isHideLegend()
    {
        return hideLegend;
    }

    public void setHideLegend( boolean hideLegend )
    {
        this.hideLegend = hideLegend;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isNoSpaceBetweenColumns()
    {
        return noSpaceBetweenColumns;
    }

    public void setNoSpaceBetweenColumns( boolean noSpaceBetweenColumns )
    {
        this.noSpaceBetweenColumns = noSpaceBetweenColumns;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getBaseLineLabel()
    {
        return baseLineLabel;
    }

    public void setBaseLineLabel( String baseLineLabel )
    {
        this.baseLineLabel = baseLineLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getTargetLineLabel()
    {
        return targetLineLabel;
    }

    public void setTargetLineLabel( String targetLineLabel )
    {
        this.targetLineLabel = targetLineLabel;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public HideEmptyItemStrategy getHideEmptyRowItems()
    {
        return hideEmptyRowItems;
    }

    public void setHideEmptyRowItems( HideEmptyItemStrategy hideEmptyRowItems )
    {
        this.hideEmptyRowItems = hideEmptyRowItems;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public ColorSet getColorSet()
    {
        return colorSet;
    }

    public void setColorSet( ColorSet colorSet )
    {
        this.colorSet = colorSet;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isShowData()
    {
        return showData;
    }

    public void setShowData( boolean showData )
    {
        this.showData = showData;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Double getTargetLineValue()
    {
        return targetLineValue;
    }

    public void setTargetLineValue( Double targetLineValue )
    {
        this.targetLineValue = targetLineValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Double getBaseLineValue()
    {
        return baseLineValue;
    }

    public void setBaseLineValue( Double baseLineValue )
    {
        this.baseLineValue = baseLineValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public boolean isPercentStackedValues()
    {
        return percentStackedValues;
    }

    public void setPercentStackedValues( boolean percentStackedValues )
    {
        this.percentStackedValues = percentStackedValues;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Double getRangeAxisMaxValue()
    {
        return rangeAxisMaxValue;
    }

    public void setRangeAxisMaxValue( Double rangeAxisMaxValue )
    {
        this.rangeAxisMaxValue = rangeAxisMaxValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Double getRangeAxisMinValue()
    {
        return rangeAxisMinValue;
    }

    public void setRangeAxisMinValue( Double rangeAxisMinValue )
    {
        this.rangeAxisMinValue = rangeAxisMinValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Integer getRangeAxisSteps()
    {
        return rangeAxisSteps;
    }

    public void setRangeAxisSteps( Integer rangeAxisSteps )
    {
        this.rangeAxisSteps = rangeAxisSteps;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Integer getRangeAxisDecimals()
    {
        return rangeAxisDecimals;
    }

    public void setRangeAxisDecimals( Integer rangeAxisDecimals )
    {
        this.rangeAxisDecimals = rangeAxisDecimals;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "yearlySeries", namespace = DXF_2_0 )
    @JacksonXmlProperty( localName = "yearlySerie", namespace = DXF_2_0 )
    public List<String> getYearlySeries()
    {
        return yearlySeries;
    }

    public void setYearlySeries( List<String> yearlySeries )
    {
        this.yearlySeries = yearlySeries;
    }

    @Override
    public void init( final User user, final Date date, final OrganisationUnit organisationUnit,
        final List<OrganisationUnit> organisationUnitsAtLevel, final List<OrganisationUnit> organisationUnitsInGroups,
        final I18nFormat format )
    {
        this.relativeUser = user;
        this.relativePeriodDate = date;
        this.relativeOrganisationUnit = organisationUnit;
        this.organisationUnitsAtLevel = organisationUnitsAtLevel;
        this.organisationUnitsInGroups = organisationUnitsInGroups;
        this.format = format;
    }

    @Override
    public void populateAnalyticalProperties()
    {
        for ( String column : columnDimensions )
        {
            columns.add( getDimensionalObject( column ) );
        }

        for ( String row : rowDimensions )
        {
            rows.add( getDimensionalObject( row ) );
        }

        for ( String filter : filterDimensions )
        {
            filters.add( getDimensionalObject( filter ) );
        }
    }

    @Override
    protected void clearTransientStateProperties()
    {
        format = null;
        relativePeriodsList = new ArrayList<>();
        relativeUser = null;
        organisationUnitsAtLevel = new ArrayList<>();
        organisationUnitsInGroups = new ArrayList<>();
        itemOrganisationUnitGroups = new ArrayList<>();
        dataItemGrid = null;
        visualizationPeriodName = null;
        gridTitle = null;
        columns = new ArrayList<>();
        rows = new ArrayList<>();
        filters = new ArrayList<>();
        parentGraphMap = new HashMap<>();
        gridColumns = new ArrayList<>();
        gridRows = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Business logic
    // -------------------------------------------------------------------------

    public List<OrganisationUnit> getAllOrganisationUnits()
    {
        if ( transientOrganisationUnits != null && !transientOrganisationUnits.isEmpty() )
        {
            return transientOrganisationUnits;
        }
        else
        {
            return organisationUnits;
        }
    }

    public List<Period> getAllPeriods()
    {
        final List<Period> list = new ArrayList<>();

        list.addAll( relativePeriodsList );

        for ( Period period : periods )
        {
            if ( !list.contains( period ) )
            {
                list.add( period );
            }
        }

        return list;
    }

    // -------------------------------------------------------------------------
    // Display supportive methods
    // -------------------------------------------------------------------------

    /**
     * Tests whether this visualization has reporting parameters.
     */
    public boolean hasReportingParams()
    {
        return reportingParams != null;
    }

    /**
     * Adds an empty list of DimensionalItemObjects to the given list if empty.
     */
    public static void addListIfEmpty( final List<List<DimensionalItemObject>> list )
    {
        if ( list != null && list.size() == 0 )
        {
            list.add( asList( new DimensionalItemObject[0] ) );
        }
    }

    /**
     * Generates a grid for this visualization based on the given aggregate value
     * map.
     *
     * @param grid the grid, should be empty and not null.
     * @param valueMap the mapping of identifiers to aggregate values.
     * @param displayProperty the display property to use for meta data.
     * @param reportParamColumns whether to include report parameter columns.
     * @return a grid.
     */
    public Grid getGrid( final Grid grid, Map<String, Object> valueMap, final DisplayProperty displayProperty,
        final boolean reportParamColumns )
    {
        valueMap = getSortedKeysMap( valueMap );

        // ---------------------------------------------------------------------
        // Title
        // ---------------------------------------------------------------------

        if ( name != null )
        {
            grid.setTitle( name );
            grid.setSubtitle( gridTitle );
        }
        else
        {
            grid.setTitle( gridTitle );
        }

        // ---------------------------------------------------------------------
        // Headers
        // ---------------------------------------------------------------------

        final Map<String, String> metaData = getMetaData();
        metaData.putAll( PRETTY_NAMES );

        for ( String row : rowDimensions )
        {
            final String name = StringUtils.defaultIfEmpty( metaData.get( row ), row );
            final String col = StringUtils.defaultIfEmpty( COLUMN_NAMES.get( row ), row );

            grid.addHeader( new GridHeader( name + " ID", col + "id", TEXT, String.class.getName(), true, true ) );
            grid.addHeader( new GridHeader( name, col + "name", TEXT, String.class.getName(), false, true ) );
            grid.addHeader( new GridHeader( name + " code", col + "code", TEXT, String.class.getName(), true, true ) );
            grid.addHeader( new GridHeader( name + " description", col + "description", TEXT, String.class.getName(),
                true, true ) );
        }

        if ( reportParamColumns )
        {
            grid.addHeader( new GridHeader( "Reporting month", REPORTING_MONTH_COLUMN_NAME, TEXT,
                String.class.getName(), true, true ) );
            grid.addHeader( new GridHeader( "Organisation unit parameter", PARAM_ORGANISATIONUNIT_COLUMN_NAME, TEXT,
                String.class.getName(), true, true ) );
            grid.addHeader( new GridHeader( "Organisation unit is parent", ORGANISATION_UNIT_IS_PARENT_COLUMN_NAME,
                TEXT, String.class.getName(), true, true ) );
        }

        final int startColumnIndex = grid.getHeaders().size();
        final int numberOfColumns = getGridColumns().size();

        for ( List<DimensionalItemObject> column : gridColumns )
        {
            grid.addHeader( new GridHeader( getColumnName( column ), getPrettyColumnName( column, displayProperty ),
                NUMBER, Double.class.getName(), false, false ) );
        }

        // ---------------------------------------------------------------------
        // Values
        // ---------------------------------------------------------------------

        for ( List<DimensionalItemObject> row : gridRows )
        {
            grid.addRow();

            // -----------------------------------------------------------------
            // Row meta data
            // -----------------------------------------------------------------

            for ( DimensionalItemObject object : row )
            {
                grid.addValue( object.getDimensionItem() );
                grid.addValue( object.getDisplayProperty( displayProperty ) );
                grid.addValue( object.getCode() );
                grid.addValue( object.getDisplayDescription() );
            }

            if ( reportParamColumns )
            {
                grid.addValue( visualizationPeriodName );
                grid.addValue( getParentOrganisationUnitName() );
                grid.addValue( isCurrentParent( row ) ? "Yes" : "No" );
            }

            // -----------------------------------------------------------------
            // Row data values
            // -----------------------------------------------------------------

            boolean hasValue = false;

            for ( List<DimensionalItemObject> column : gridColumns )
            {
                final String key = DimensionalObjectUtils.getKey( column, row );

                final Object value = valueMap.get( key );

                grid.addValue( value );

                hasValue = hasValue || value != null;
            }

            if ( hideEmptyRows && !hasValue )
            {
                grid.removeCurrentWriteRow();
            }

            // TODO hide empty columns
        }

        if ( hideEmptyColumns )
        {
            grid.removeEmptyColumns();
        }

        if ( regression )
        {
            grid.addRegressionToGrid( startColumnIndex, numberOfColumns );
        }

        if ( cumulative )
        {
            grid.addCumulativesToGrid( startColumnIndex, numberOfColumns );
        }

        // ---------------------------------------------------------------------
        // Sort and limit
        // ---------------------------------------------------------------------

        if ( sortOrder != NONE )
        {
            grid.sortGrid( grid.getWidth(), sortOrder );
        }

        if ( topLimit > 0 )
        {
            grid.limitGrid( topLimit );
        }

        // ---------------------------------------------------------------------
        // Show hierarchy option
        // ---------------------------------------------------------------------

        if ( showHierarchy && rowDimensions.contains( ORGUNIT_DIM_ID )
            && grid.hasInternalMetaDataKey( ORG_UNIT_ANCESTORS.getKey() ) )
        {
            final int ouIdColumnIndex = rowDimensions.indexOf( ORGUNIT_DIM_ID ) * 4;

            addHierarchyColumns( grid, ouIdColumnIndex );
        }
        return grid;
    }

    /**
     * Indicates whether this Visualization is multi-dimensional.
     */
    public boolean isDimensional()
    {
        return !getDataElements().isEmpty() && (columnDimensions.contains( CATEGORYOPTIONCOMBO_DIM_ID )
            || rowDimensions.contains( CATEGORYOPTIONCOMBO_DIM_ID ));
    }

    /**
     * Generates a pretty column name based on the given display property of the
     * argument objects. Null arguments are ignored in the name.
     */
    public static String getPrettyColumnName( final List<DimensionalItemObject> objects,
        final DisplayProperty displayProperty )
    {
        final StringBuilder builder = new StringBuilder();

        for ( DimensionalItemObject object : objects )
        {
            builder.append( object != null ? (object.getDisplayProperty( displayProperty ) + SPACE) : EMPTY );
        }
        return builder.length() > 0 ? builder.substring( 0, builder.lastIndexOf( SPACE ) ) : TOTAL_COLUMN_PRETTY_NAME;
    }

    /**
     * Generates a column name based on short-names of the argument objects. Null
     * arguments are ignored in the name.
     * <p/>
     * The period column name must be static when on columns so it can be re-used in
     * reports, hence the name property is used which will be formatted only when
     * the period dimension is on rows.
     */
    public static String getColumnName( final List<DimensionalItemObject> objects )
    {
        final StringBuffer buffer = new StringBuffer();

        for ( DimensionalItemObject object : objects )
        {
            if ( object != null && object instanceof Period )
            {
                buffer.append( object.getName() ).append( NAME_SEP );
            }
            else
            {
                buffer.append( object != null ? (object.getShortName() + NAME_SEP) : EMPTY );
            }
        }

        final String column = columnEncode( buffer.toString() );

        return column != null && column.length() > 0 ? column.substring( 0, column.lastIndexOf( NAME_SEP ) )
            : TOTAL_COLUMN_NAME;
    }

    /**
     * Generates a string which is acceptable as a filename.
     */
    public static String columnEncode( String string )
    {
        if ( string != null )
        {
            string = string.replaceAll( "<", "_lt" );
            string = string.replaceAll( ">", "_gt" );
            string = string.replaceAll( ILLEGAL_FILENAME_CHARS_REGEX, EMPTY );
            string = string.length() > 255 ? string.substring( 0, 255 ) : string;
            string = string.toLowerCase();
        }
        return string;
    }

    /**
     * Checks whether the given List of IdentifiableObjects contains an object which
     * is an OrganisationUnit and has the currentParent property set to true.
     *
     * @param objects the List of IdentifiableObjects.
     */
    public static boolean isCurrentParent( final List<? extends IdentifiableObject> objects )
    {
        for ( IdentifiableObject object : objects )
        {
            if ( object != null && object instanceof OrganisationUnit && ((OrganisationUnit) object).isCurrentParent() )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the name of the parent organisation unit, or an empty string if null.
     */
    public String getParentOrganisationUnitName()
    {
        return relativeOrganisationUnit != null ? relativeOrganisationUnit.getName() : EMPTY;
    }

    /**
     * Adds grid columns for each organisation unit level.
     */
    @SuppressWarnings( "unchecked" )
    private void addHierarchyColumns( final Grid grid, final int ouIdColumnIndex )
    {
        Map<Object, List<?>> ancestorMap = (Map<Object, List<?>>) grid.getInternalMetaData()
            .get( ORG_UNIT_ANCESTORS.getKey() );

        Assert.notEmpty( ancestorMap, "Ancestor map cannot be null or empty when show hierarchy is enabled" );

        int newColumns = ancestorMap.values().stream().mapToInt( List::size ).max().orElseGet( () -> 0 );

        List<GridHeader> headers = new ArrayList<>();

        for ( int i = 0; i < newColumns; i++ )
        {
            int level = i + 1;

            String name = String.format( "Org unit level %d", level );
            String column = String.format( "orgunitlevel%d", level );

            headers.add( new GridHeader( name, column, TEXT, String.class.getName(), false, true ) );
        }

        grid.addHeaders( ouIdColumnIndex, headers );
        grid.addAndPopulateColumnsBefore( ouIdColumnIndex, ancestorMap, newColumns );
    }
}
