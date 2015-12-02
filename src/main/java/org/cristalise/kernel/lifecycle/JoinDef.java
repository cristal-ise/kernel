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
package org.cristalise.kernel.lifecycle;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.instance.Join;
import org.cristalise.kernel.lifecycle.instance.WfVertex;

/**
 * @version $Revision: 1.18 $ $Date: 2005/09/29 10:18:31 $
 * @author $Author: abranson $
 */
public class JoinDef extends WfVertexDef
{
	/**
	 * @see java.lang.Object#Object()
	 */
	public JoinDef()
	{
	}
	/**
	 * @see org.cristalise.kernel.lifecycle.WfVertexDef#verify()
	 */
	@Override
	public boolean verify()
	{
		mErrors.removeAllElements();
		int nbOutEdges = getOutEdges().length;
		int nbInEdges = getInEdges().length;
		String type = (String) getProperties().get("Type");
        if (nbInEdges < 1)
        {
            mErrors.add("not enough previous");
            return false;
        }
        if (type != null && type.equals("Route"))
		{
			if (nbInEdges > 1)
			{
				mErrors.add("Bad nb of previous");
				return false;
			}
		}
		if (nbOutEdges > 1)
		{
			mErrors.add("too many next");
			return false;
		}
		if (nbOutEdges == 0)
		{
			if (!((CompositeActivityDef) getParent()).hasGoodNumberOfActivity())
			{
				mErrors.add("too many endpoints");
				return false;
			}
		}
		return true;
	}
	@Override
	public boolean isJoin()
	{
		return true;
	}

	@Override
	public WfVertex instantiate() throws InvalidDataException, ObjectNotFoundException {
		Join newJoin = new Join();
		configureInstance(newJoin);
		return newJoin;
	}
}
