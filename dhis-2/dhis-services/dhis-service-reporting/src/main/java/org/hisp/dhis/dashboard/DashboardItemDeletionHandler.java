package org.hisp.dhis.dashboard;

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

import static com.google.common.base.Preconditions.checkNotNull;

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component( "org.hisp.dhis.dashboard.DashboardItemDeletionHandler" )
public class DashboardItemDeletionHandler extends DeletionHandler
{
    private final DashboardService dashboardService;

    public DashboardItemDeletionHandler( DashboardService dashboardService )
    {
        checkNotNull( dashboardService );
        this.dashboardService = dashboardService;
    }

    @Override
    protected String getClassName()
    {
        return DashboardItem.class.getSimpleName();
    }

    @Override
    public String allowDeleteMap( Map map )
    {
        return dashboardService.countMapDashboardItems( map ) == 0 ? null : ERROR;
    }

    @Override
    public String allowDeleteChart( Chart chart )
    {
        return dashboardService.countChartDashboardItems( chart ) == 0 ? null : ERROR;
    }

    @Override
    public String allowDeleteEventChart( EventChart eventChart )
    {
        return dashboardService.countEventChartDashboardItems( eventChart ) == 0 ? null : ERROR;
    }

    @Override
    public String allowDeleteReportTable( ReportTable reportTable )
    {
        return dashboardService.countReportTableDashboardItems( reportTable ) == 0 ? null : ERROR;
    }

    @Override
    public String allowDeleteVisualization( Visualization visualization )
    {
        return dashboardService.countVisualizationDashboardItems( visualization ) == 0 ? null : ERROR;
    }

    @Override
    public String allowDeleteReport( Report report )
    {
        return dashboardService.countReportDashboardItems( report ) == 0 ? null : ERROR;
    }
    
    @Override
    public String allowDeleteDocument( Document document )
    {
        return dashboardService.countDocumentDashboardItems( document ) == 0 ? null : ERROR;
    }
    
    @Override
    public String allowDeleteUser( User user )
    {
        return dashboardService.countUserDashboardItems( user ) == 0 ? null : ERROR;
    }
}