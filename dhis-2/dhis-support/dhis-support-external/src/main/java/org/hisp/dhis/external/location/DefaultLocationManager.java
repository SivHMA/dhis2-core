package org.hisp.dhis.external.location;

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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.external.util.LogOnceLogger;
import org.slf4j.event.Level;

import javax.annotation.PostConstruct;

import static java.io.File.separator;

/**
 * @author Lars Helge Overland
 */
public class DefaultLocationManager extends LogOnceLogger
    implements LocationManager
{
    private static final Log log = LogFactory.getLog( DefaultLocationManager.class );

    private static final String DEFAULT_DHIS2_HOME = "/opt/dhis2";

    private static final String DEFAULT_ENV_VAR = "DHIS2_HOME";
    private static final String DEFAULT_SYS_PROP = "dhis2.home";

    private String externalDir;

    private String environmentVariable;

    private String systemProperty;

    public DefaultLocationManager( String environmentVariable, String systemProperty )
    {
        this.environmentVariable = environmentVariable;
        this.systemProperty = systemProperty;
    }

    public static DefaultLocationManager getDefault()
    {
        return new DefaultLocationManager( DEFAULT_ENV_VAR, DEFAULT_SYS_PROP );
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    @PostConstruct
    public void init()
    {
        String path = System.getProperty( systemProperty );

        if ( path != null )
        {
            log( log, Level.INFO, "System property " + systemProperty + " points to " + path );

            if ( directoryIsValid( new File( path ) ) )
            {
                externalDir = path;
            }
        }
        else
        {
            log( log, Level.INFO, "System property " + systemProperty + " not set" );

            path = System.getenv( environmentVariable );

            if ( path != null )
            {
                log( log, Level.INFO, "Environment variable " + environmentVariable + " points to " + path );

                if ( directoryIsValid( new File( path ) ) )
                {
                    externalDir = path;
                }
            }
            else
            {
                log( log, Level.INFO, "Environment variable " + environmentVariable + " not set" );

                path = DEFAULT_DHIS2_HOME;

                if ( directoryIsValid( new File( path ) ) )
                {
                    externalDir = path;
                    log( log, Level.INFO, "Home directory set to " + DEFAULT_DHIS2_HOME );

                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // LocationManager implementation
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // InputStream
    // -------------------------------------------------------------------------

    @Override
    public InputStream getInputStream( String fileName )
        throws LocationManagerException
    {
        return getInputStream( fileName, new String[0] );
    }

    @Override
    public InputStream getInputStream( String fileName, String... directories )
        throws LocationManagerException
    {
        File file = getFileForReading( fileName, directories );

        try
        {
            return new BufferedInputStream( new FileInputStream( file ) );
        }
        catch ( FileNotFoundException ex )
        {
            throw new LocationManagerException( "Could not find file", ex );
        }
    }

    // -------------------------------------------------------------------------
    // File for reading
    // -------------------------------------------------------------------------

    @Override
    public File getFileForReading( String fileName )
        throws LocationManagerException
    {
        return getFileForReading( fileName, new String[0] );
    }

    @Override
    public File getFileForReading( String fileName, String... directories )
        throws LocationManagerException
    {
        File directory = buildDirectory( directories );

        File file = new File( directory, fileName );

        if ( !canReadFile( file ) )
        {
            throw new LocationManagerException( "File " + file.getAbsolutePath() + " cannot be read" );
        }

        return file;
    }

    // -------------------------------------------------------------------------
    // OutputStream
    // -------------------------------------------------------------------------

    @Override
    public OutputStream getOutputStream( String fileName )
        throws LocationManagerException
    {
        return getOutputStream( fileName, new String[0] );
    }

    @Override
    public OutputStream getOutputStream( String fileName, String... directories )
        throws LocationManagerException
    {
        File file = getFileForWriting( fileName, directories );

        try
        {
            return new BufferedOutputStream( new FileOutputStream( file ) );
        }
        catch ( FileNotFoundException ex )
        {
            throw new LocationManagerException( "Could not find file", ex );
        }
    }

    // -------------------------------------------------------------------------
    // File for writing
    // -------------------------------------------------------------------------

    @Override
    public File getFileForWriting( String fileName )
        throws LocationManagerException
    {
        return getFileForWriting( fileName, new String[0] );
    }

    @Override
    public File getFileForWriting( String fileName, String... directories )
        throws LocationManagerException
    {
        File directory = buildDirectory( directories );

        if ( !directoryIsValid( directory ) )
        {
            throw new LocationManagerException( "Directory " + directory.getAbsolutePath() + " cannot be created" );
        }

        return new File( directory, fileName );
    }

    @Override
    public File buildDirectory( String... directories )
        throws LocationManagerException
    {
        if ( externalDir == null )
        {
            throw new LocationManagerException( "External directory not set" );
        }

        StringBuilder directoryPath = new StringBuilder( externalDir + separator );

        for ( String dir : directories )
        {
            directoryPath.append( dir ).append( separator );
        }

        return new File( directoryPath.toString() );
    }

    // -------------------------------------------------------------------------
    // External directory and environment variable
    // -------------------------------------------------------------------------

    @Override
    public File getExternalDirectory()
        throws LocationManagerException
    {
        if ( externalDir == null )
        {
            throw new LocationManagerException( "External directory not set" );
        }

        return new File( externalDir );
    }

    @Override
    public String getExternalDirectoryPath()
        throws LocationManagerException
    {
        if ( externalDir == null )
        {
            throw new LocationManagerException( "External directory not set" );
        }

        return externalDir;
    }

    @Override
    public boolean externalDirectorySet()
    {
        return externalDir != null;
    }

    @Override
    public String getEnvironmentVariable()
    {
        return environmentVariable;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Tests whether the file exists and can be read by the application.
     */
    private boolean canReadFile( File file )
    {
        if ( !file.exists() )
        {
            log( log, Level.INFO, "File " + file.getAbsolutePath() + " does not exist" );
            return false;
        }

        if ( !file.canRead() )
        {
            log( log, Level.INFO, "File " + file.getAbsolutePath() + " cannot be read" );

            return false;
        }

        return true;
    }

    /**
     * Tests whether the directory is writable by the application if the directory
     * exists. Tries to create the directory including necessary parent directories
     * if the directory does not exists, and tests whether the directory construction
     * was successful and not prevented by a SecurityManager in any way.
     */
    private boolean directoryIsValid( File directory )
    {
        if ( directory.exists() )
        {
            if( !directory.canWrite() )
            {
                log( log, Level.INFO, "Directory " + directory.getAbsolutePath() + " is not writeable" );
                return false;
            }
        }
        else
        {
            try
            {
                if ( !directory.mkdirs() )
                {
                    log( log, Level.INFO, "Directory " + directory.getAbsolutePath() + " cannot be created" );
                    return false;
                }
            }
            catch ( SecurityException ex )
            {
                log( log, Level.INFO, "Directory " + directory.getAbsolutePath() + " cannot be accessed" );
                return false;
            }
        }

        return true;
    }
}
