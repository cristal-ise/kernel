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
package org.cristalise.kernel.entity;


import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.Logger;


/**************************************************************************
*  TraceableEntity is the implementation of the CORBA Item, although it 
*  delegates all non-CORBA functions to ItemImplementation.
*  
*  Traditional Pinky/Brain ASCII art:
*  
* <pre>
*                                ,.   '\'\    ,---.
*                            .  | \\  l\\l_ //    |
*        _              _       |  \\/ `/  `.|    |
*      /~\\   \        //~\     | Y |   |   ||  Y |
*      |  \\   \      //  |     |  \|   |   |\ /  |
*      [   ||        ||   ]     \   |  o|o  | >  /
*     ] Y  ||        ||  Y [     \___\_--_ /_/__/
*     |  \_|l,------.l|_/  |     /.-\(____) /--.\
*     |   >'          `<   |     `--(______)----'
*     \  (/~`--____--'~\)  /         u// u / \
*      `-_>-__________-<_-'          / \  / /|
*          /(_#(__)#_)\             ( .) / / ]
*          \___/__\___/              `.`' /   [
*           /__`--'__\                |`-'    |
*        /\(__,>-~~ __)               |       |_
*     /\//\\(  `--~~ )               _l       |-:.
*     '\/  <^\      /^>             |  `   (  <  \\
*          _\ >-__-< /_           ,-\  ,-~~->. \  `:._,/
*        (___\    /___)         (____/    (____)   `-'
*             Kovax            and, paradoxically, Kovax
* </pre>
***************************************************************************/

public class TraceableEntity extends ItemPOA
{

    private final org.omg.PortableServer.POA  mPoa;
    private final ItemImplementation	      mItemImpl;

    public TraceableEntity( ItemPath                   key,
                            org.omg.PortableServer.POA poa )
    {
        Logger.msg(5,"TraceableEntity::constructor() - SystemKey:" + key );
        mPoa	= poa;	
        mItemImpl = new ItemImplementation(key);
    }


    @Override
	public org.omg.PortableServer.POA _default_POA()
    {
        if(mPoa != null)
            return mPoa;
        else
            return super._default_POA();
    }


    @Override
	public SystemKey getSystemKey()
    {
        return mItemImpl.getSystemKey();
    }

    @Override
	public void initialise( SystemKey agentId,
                            String  propString,
                            String  initWfString,
                            String initCollsString
                            )
        throws AccessRightsException,
               InvalidDataException,
               PersistencyException
    {
        synchronized (this) {
        	mItemImpl.initialise(agentId, propString, initWfString, initCollsString);
        }
    }

    @Override
	public String requestAction( SystemKey agentId,
                               String stepPath,
                               int transitionID,
                               String requestData,
                               String attachmentType,
                               byte[] attachment
                              )
        throws AccessRightsException,
               InvalidTransitionException,
               ObjectNotFoundException,
               InvalidDataException,
               PersistencyException,
               ObjectAlreadyExistsException, InvalidCollectionModification
    {
        synchronized (this) {
            return mItemImpl.requestAction(agentId, stepPath, transitionID, requestData, attachmentType, attachment);
        }
    }

    @Override
    public String delegatedAction( SystemKey agentId,
                                SystemKey delegateAgentId,
                                String stepPath,
                                int transitionID,
                                String requestData,
                                String attachmentType,
                                byte[] attachment
                              )
        throws AccessRightsException,
               InvalidTransitionException,
               ObjectNotFoundException,
               InvalidDataException,
               PersistencyException,
               ObjectAlreadyExistsException, InvalidCollectionModification
    {
        synchronized (this) {
            return mItemImpl.delegatedAction(agentId, delegateAgentId, stepPath, transitionID, requestData, attachmentType, attachment);
        }
    }

    @Override
	public String queryLifeCycle( SystemKey agentId,
                                  boolean     filter
                                )
        throws AccessRightsException,
               ObjectNotFoundException,
               PersistencyException
    {
        synchronized (this) {
            return mItemImpl.queryLifeCycle(agentId, filter);
        }
    }

    @Override
	public String queryData(String path)
        throws AccessRightsException,
               ObjectNotFoundException,
               PersistencyException
    {
        synchronized (this) {
            return mItemImpl.queryData(path);
        }
    }
}
