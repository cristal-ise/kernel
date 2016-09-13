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
package org.cristalise.kernel.entity.proxy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.utils.Logger;


public class MemberSubscription<C extends C2KLocalObject> implements Runnable {
    public static final String ERROR = "Error";
    public static final String END = "theEND";

    ItemProxy subject;
    String interest;
    // keep the subscriber by weak reference, so it is not kept from the garbage collector if no longer used
    WeakReference<ProxyObserver<C>> observerReference;
    ArrayList<String> contents = new ArrayList<String>();
    boolean preLoad;

    public MemberSubscription(ProxyObserver<C> observer, String interest, boolean preLoad) {
        setObserver(observer);
        this.interest = interest;
        this.preLoad = preLoad;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Member Subscription: "+subject.getPath()+":"+interest);
        if (preLoad) loadChildren();
    }

    private void loadChildren() {
        C newMember;
        ProxyObserver<C> observer = getObserver();
        if (observer == null) return; //reaped

        try {
            // fetch contents of path
            String children = subject.queryData(interest+"/all");
            StringTokenizer tok = new StringTokenizer(children, ",");
            ArrayList<String> newContents = new ArrayList<String>();
            
            while (tok.hasMoreTokens()) newContents.add(tok.nextToken());

            // look to see what's new
            for (String newChild: newContents) {

                // load child object
                try {
                    newMember = (C)subject.getObject(interest+"/"+newChild);
                    contents.remove(newChild);
                    observer.add(newMember);
                }
                catch (ObjectNotFoundException ex) {
                    observer.control(ERROR, "Listed member "+newChild+" was not found.");
                }
                catch (ClassCastException ex) {
                    Logger.error(ex);
                    observer.control(ERROR, "Listed member "+newChild+" was the wrong type.");
                }
            }
            // report what's left in old contents as deleted
            for (String oldChild: contents) {
                observer.remove(interest+"/"+oldChild);
            }
            //replace contents arraylist
            contents = newContents;
            //report that we're done
            observer.control(END, null);
        }
        catch (Exception ex) {
            observer.control(ERROR, "Query on "+interest+" failed with "+ex.getMessage());
        }
    }

    public boolean isRelevant(String path) {
        Logger.msg(7, "MemberSubscription.isRelevant() - path "+path+" to "+interest);
        return (path.startsWith(interest));
    }

    public void update(String path, boolean deleted) {
        ProxyObserver<C> observer = getObserver();
        if (observer == null) return; //reaped
        Logger.msg(7, "MemberSubscription.update() - path "+path +" for "+observer+". Interest: "+interest+" Was Deleted:"+deleted);

        if (!path.startsWith(interest)) // doesn't concern us
            return;

        if (path.equals(interest)) // refresh contents
            loadChildren();
        else {
            String name = path.substring(interest.length());
            if (deleted) {
                Logger.msg(4, "MemberSubscription.update() - Removing "+path);
                contents.remove(name);
                observer.remove(name);
            }
            else {
                try {
                    C newMember = (C)subject.getObject(path);
                    Logger.msg(4, "MemberSubscription.update() - Adding "+path);
                    contents.add(name);
                    observer.add(newMember);
                }
                catch (ObjectNotFoundException e) {
                    Logger.error("MemberSubscription: could not load "+path);
                    Logger.error(e);
                }
            }
        }
    }

    public void setObserver(ProxyObserver<C> observer) {
        observerReference = new WeakReference<ProxyObserver<C>>(observer);
    }

    public void setSubject(ItemProxy subject) {
        this.subject = subject;
    }

    public ProxyObserver<C> getObserver() {
        return observerReference.get();
    }
}

