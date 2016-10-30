/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.kernel.persistency.outcome;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.lookup.InvalidItemPathException;

/**
 * OutcomeInit.Query = org.cristalise.kernel.persistency.outcome.QueryOutcomeInitiator
 */
public class QueryOutcomeInitiator implements OutcomeInitiator {

    @Override
    public String initOutcome(Job job) throws InvalidDataException {
        return initOutcomeInstance(job).getData();
    }

    @Override
    public Outcome initOutcomeInstance(Job job) throws InvalidDataException {
        if (job.hasQuery()) {
            try {
                Outcome o = new Outcome(-1, job.getItemProxy().executeQuery(job.getQuery()), job.getSchema());
                o.validateAndCheck();
                return o;
            }
            catch (PersistencyException | ObjectNotFoundException | InvalidItemPathException e) {
                throw new InvalidDataException("Error executing Query:"+e.getMessage());
            }
        }
        else
            throw new InvalidDataException("No Query was defined for job:"+job);
    }
}
