package org.hisp.dhis.trackedentity;

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

import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.program.Program;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author Abyot Asalefew Gizaw
 */
public interface TrackedEntityAttributeStore
    extends IdentifiableObjectStore<TrackedEntityAttribute>
{
    String ID = TrackedEntityAttributeStore.class.getName();


    /**
     * Get attributes which are displayed in visit schedule
     * 
     * @param displayOnVisitSchedule True/False value
     * 
     * @return List of attributes
     */
    List<TrackedEntityAttribute> getByDisplayOnVisitSchedule( boolean displayOnVisitSchedule );

    /**
     * Get attributes which are displayed in visit schedule
     * 
     * @return List of attributes
     */
    List<TrackedEntityAttribute> getDisplayInListNoProgram();

    /**
     * Check whether there already exists a TrackedEntityInstance with given unique attribute value. If yes, return
     * Optional containing UID of it. Otherwise, return empty Optional.
     *
     * @param params Query params. Contains value of unique attribute that should be checked.
     * @return Optional of TrackedEntityInstance UID or empty Optional.
     */
    Optional<String> getTrackedEntityInstanceUidWithUniqueAttributeValue( TrackedEntityInstanceQueryParams params );

    /**
     * Fetches all {@see TrackedEntityAttribute} linked to all
     * {@see TrackedEntityType} present in the system
     *
     * @return a Set of {@see TrackedEntityAttribute}
     */
    Set<TrackedEntityAttribute> getTrackedEntityAttributesByTrackedEntityTypes();

    /**
     * Fetches all {@see TrackedEntityAttribute} and groups them by {@see Program}
     *
     * @return a Map, where the key is the {@see Program} and the values is a Set of {@see TrackedEntityAttribute} associated
     * to the {@see Program} in the key
     */
    Map<Program, Set<TrackedEntityAttribute>> getTrackedEntityAttributesByProgram();
}
