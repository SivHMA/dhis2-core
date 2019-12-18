package org.hisp.dhis.scheduling;

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

import static org.hisp.dhis.scheduling.JobStatus.DISABLED;
import static org.hisp.dhis.scheduling.JobStatus.SCHEDULED;
import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

import java.util.Date;

import javax.annotation.Nonnull;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.SecondaryMetadataObject;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.EventProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.MetadataSyncJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.scheduling.parameters.SmsJobParameters;
import org.hisp.dhis.scheduling.parameters.TrackerProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.jackson.JobConfigurationSanitizer;
import org.hisp.dhis.schema.annotation.Property;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This class defines configuration for a job in the system. The job is defined with general identifiers, as well as job
 * specific, such as jobType {@link JobType}.
 * <p>
 * All system jobs should be included in JobType enum and can be scheduled/executed with {@link SchedulingManager}.
 * <p>
 * The class uses a custom deserializer to handle several potential {@link JobParameters}.
 *
 * The configurable property in the configurable property in the method based on the job type we are adding.
 *
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "jobConfiguration", namespace = DxfNamespaces.DXF_2_0 )
@JsonDeserialize( converter = JobConfigurationSanitizer.class )
public class JobConfiguration
    extends BaseIdentifiableObject implements SecondaryMetadataObject
{
    private String cronExpression;

    private JobType jobType;

    private JobStatus jobStatus;

    private Date nextExecutionTime;

    private JobStatus lastExecutedStatus = JobStatus.NOT_STARTED;

    private Date lastExecuted;

    private String lastRuntimeExecution;

    private JobParameters jobParameters;

    private boolean continuousExecution = false;

    private boolean enabled = true;

    private boolean inMemoryJob = false;

    private String userUid;

    private boolean leaderOnlyJob = false;

    public JobConfiguration()
    {
    }

    public JobConfiguration( String name, JobType jobType, String userUid, boolean inMemoryJob )
    {
        this.name = name;
        this.jobType = jobType;
        this.userUid = userUid;
        this.inMemoryJob = inMemoryJob;
        init();
    }

    public JobConfiguration( String name, JobType jobType, String cronExpression, JobParameters jobParameters,
        boolean continuousExecution, boolean enabled )
    {
        this( name, jobType, cronExpression, jobParameters, continuousExecution, enabled, false );
    }

    public JobConfiguration( String name, JobType jobType, String cronExpression, JobParameters jobParameters,
        boolean continuousExecution, boolean enabled, boolean inMemoryJob )
    {
        this.name = name;
        this.cronExpression = cronExpression;
        this.jobType = jobType;
        this.jobParameters = jobParameters;
        this.continuousExecution = continuousExecution;
        this.enabled = enabled;
        this.inMemoryJob = inMemoryJob;
        setJobStatus( enabled ? SCHEDULED : DISABLED );
        init();
    }

    private void init()
    {
        if ( inMemoryJob )
        {
            setAutoFields();
        }
    }

    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    }

    public void setJobType( JobType jobType )
    {
        this.jobType = jobType;
    }

    public void setJobStatus( JobStatus jobStatus )
    {
        this.jobStatus = jobStatus;
    }

    public void setLastExecuted( Date lastExecuted )
    {
        this.lastExecuted = lastExecuted;
    }

    public void setLastExecutedStatus( JobStatus lastExecutedStatus )
    {
        this.lastExecutedStatus = lastExecutedStatus;
    }

    public void setLastRuntimeExecution( String lastRuntimeExecution )
    {
        this.lastRuntimeExecution = lastRuntimeExecution;
    }

    public void setJobParameters( JobParameters jobParameters )
    {
        this.jobParameters = jobParameters;
    }

    /**
     * Only set next execution time if the job is not continuous.
     */
    public void setNextExecutionTime( Date nextExecutionTime )
    {
        if ( cronExpression == null || cronExpression.equals( "" ) || cronExpression.equals( "* * * * * ?" ) )
        {
            return;
        }

        if ( nextExecutionTime != null )
        {
            this.nextExecutionTime = nextExecutionTime;
        }
        else
        {
            this.nextExecutionTime = new CronTrigger( cronExpression ).nextExecutionTime( new SimpleTriggerContext() );
        }
    }

    public void setContinuousExecution( boolean continuousExecution )
    {
        this.continuousExecution = continuousExecution;
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public void setLeaderOnlyJob( boolean leaderOnlyJob )
    {
        this.leaderOnlyJob = leaderOnlyJob;
    }

    public void setInMemoryJob( boolean inMemoryJob )
    {
        this.inMemoryJob = inMemoryJob;
    }

    public void setUserUid( String userUid )
    {
        this.userUid = userUid;
    }

    @JacksonXmlProperty
    @JsonProperty
    public String getCronExpression()
    {
        return cronExpression;
    }

    @JacksonXmlProperty
    @JsonProperty
    @JsonTypeId
    public JobType getJobType()
    {
        return jobType;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public JobStatus getJobStatus()
    {
        return jobStatus;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public Date getLastExecuted()
    {
        return lastExecuted;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public JobStatus getLastExecutedStatus()
    {
        return lastExecutedStatus;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public String getLastRuntimeExecution()
    {
        return lastRuntimeExecution;
    }

    @JacksonXmlProperty
    @JsonProperty
    @Property( required = FALSE )
    @JsonTypeInfo( use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "jobType" )
    @JsonSubTypes( value = {
        @JsonSubTypes.Type( value = AnalyticsJobParameters.class, name = "ANALYTICS_TABLE" ),
        @JsonSubTypes.Type( value = MonitoringJobParameters.class, name = "MONITORING" ),
        @JsonSubTypes.Type( value = PredictorJobParameters.class, name = "PREDICTOR" ),
        @JsonSubTypes.Type( value = PushAnalysisJobParameters.class, name = "PUSH_ANALYSIS" ),
        @JsonSubTypes.Type( value = SmsJobParameters.class, name = "SMS_SEND" ),
        @JsonSubTypes.Type( value = MetadataSyncJobParameters.class, name = "META_DATA_SYNC" ),
        @JsonSubTypes.Type( value = EventProgramsDataSynchronizationJobParameters.class, name = "EVENT_PROGRAMS_DATA_SYNC" ),
        @JsonSubTypes.Type( value = TrackerProgramsDataSynchronizationJobParameters.class, name = "TRACKER_PROGRAMS_DATA_SYNC" ),
    } )
    public JobParameters getJobParameters()
    {
        return jobParameters;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public Date getNextExecutionTime()
    {
        return nextExecutionTime;
    }

    @JacksonXmlProperty
    @JsonProperty
    public boolean isContinuousExecution()
    {
        return continuousExecution;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public boolean isConfigurable()
    {
        return jobType.isConfigurable();
    }

    @JacksonXmlProperty
    @JsonProperty
    public boolean isEnabled()
    {
        return enabled;
    }

    @JacksonXmlProperty
    @JsonProperty
    public boolean isLeaderOnlyJob()
    {
        return leaderOnlyJob;
    }

    public boolean isInMemoryJob()
    {
        return inMemoryJob;
    }

    @JacksonXmlProperty
    @JsonProperty( access = JsonProperty.Access.READ_ONLY )
    public String getUserUid()
    {
        return userUid;
    }

    /**
     * Checks if this job has changes compared to the specified job configuration that are only
     * allowed for configurable jobs.
     *
     * @param jobConfiguration the job configuration that should be checked.
     * @return <code>true</code> if this job configuration has changes in fields that are only
     * allowed for configurable jobs, <code>false</code> otherwise.
     */
    public boolean hasNonConfigurableJobChanges( @Nonnull JobConfiguration jobConfiguration )
    {
        if ( jobType != jobConfiguration.getJobType() )
        {
            return true;
        }
        if ( jobStatus != jobConfiguration.getJobStatus() )
        {
            return true;
        }
        if ( jobParameters != jobConfiguration.getJobParameters() )
        {
            return true;
        }
        if ( continuousExecution != jobConfiguration.isContinuousExecution() )
        {
            return true;
        }
        return enabled != jobConfiguration.isEnabled();
    }

    @Override
    public String toString()
    {
        return "JobConfiguration{" +
            "uid='" + uid + '\'' +
            ", displayName='" + displayName + '\'' +
            ", cronExpression='" + cronExpression + '\'' +
            ", jobType=" + jobType +
            ", jobParameters=" + jobParameters +
            ", enabled=" + enabled +
            ", continuousExecution=" + continuousExecution +
            ", inMemoryJob=" + inMemoryJob +
            ", lastRuntimeExecution='" + lastRuntimeExecution + '\'' +
            ", userUid='" + userUid + '\'' +
            ", leaderOnlyJob=" + leaderOnlyJob +
            ", jobStatus=" + jobStatus +
            ", nextExecutionTime=" + nextExecutionTime +
            ", lastExecutedStatus=" + lastExecutedStatus +
            ", lastExecuted=" + lastExecuted + '}';
    }
}
