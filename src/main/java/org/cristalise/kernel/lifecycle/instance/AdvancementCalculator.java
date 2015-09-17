/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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
/* Created on 11 mars 2004 */
package org.cristalise.kernel.lifecycle.instance;
import java.util.Hashtable;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.graph.model.Vertex;

//import org.cristalise.kernel.utils.Logger;
/** @author XSeb74 */
@Deprecated
public class AdvancementCalculator
{
	private CompositeActivity activity;
	private Hashtable<Vertex, Object> isMarked;
	private Hashtable<Vertex, Vertex> HasNextMarked;
	public Hashtable<String, Vertex> hasprevActive;
	private long mCurrentNbActExp = 0;
	private long mMaximuNbActexp = 0;
	private long mNbActpassed = 0;
	private long mNbActpassedWithCurrent = 0;
	private long mNbActLeftWithCurrent = 0;
	private long mNbActLeftWithoutCurrent = 0;
	private boolean mIsbranchActive = false;
	private boolean mIsbranchFinished = true;
	private boolean mHasPrevActive = false;
	public AdvancementCalculator()
	{
		isMarked = new Hashtable<Vertex, Object>();
		HasNextMarked = new Hashtable<Vertex, Vertex>();
		hasprevActive = new Hashtable<String, Vertex>();
	}
	public void calculate(CompositeActivity act) throws InvalidDataException
	{
//		Logger.debug(0, act.getName()+" >>>>>>>>>");
		if (act instanceof Workflow)
		{
			calculate((CompositeActivity) act.search("workflow/domain"));
			return;
		}
		activity = act;
		Vertex v = activity.getChildGraphModel().getStartVertex();
		check(v, this);
		isMarked = new Hashtable<Vertex, Object>();
		calc(v, this);
//		Logger.debug(0, act.getName()+" <<<<<<<<<");
	}
	private void check(Vertex v, AdvancementCalculator current)
	{
		current.isMarked.put(v, "");
		Vertex[] nexts = current.activity.getChildGraphModel().getOutVertices(v);
		for (Vertex next : nexts)
			if (current.isMarked.get(next) != null)
				current.HasNextMarked.put(v, next);
			else
				check(next, current);
        int j=0;
		for (Vertex next : nexts)
			if (current.HasNextMarked.get(next) != null)
				j++;
        if (j != 0 && j==nexts.length) current.HasNextMarked.put(v, nexts[0]);
	}
	private void calc(Vertex v, AdvancementCalculator current) throws InvalidDataException
	{
		if (current.isMarked.get(v) != null && !(v instanceof Join))
			return;
		if (v instanceof Activity)
		{
			current.isMarked.put(v, current);
			Activity act = (Activity) v;
			if (v instanceof CompositeActivity)
			{
				CompositeActivity cact = (CompositeActivity) v;
				AdvancementCalculator adv = new AdvancementCalculator();
				adv.isMarked = current.isMarked;
				adv.HasNextMarked = current.HasNextMarked;
				adv.calculate(cact);
				current.mCurrentNbActExp += adv.mCurrentNbActExp;
				current.mMaximuNbActexp += adv.mMaximuNbActexp;
				current.mNbActpassed += adv.mNbActpassed;
				current.mNbActpassedWithCurrent += adv.mNbActpassedWithCurrent;
				current.mIsbranchActive = current.mIsbranchActive || adv.mIsbranchActive || act.getActive();
				current.mNbActLeftWithCurrent += adv.mNbActLeftWithCurrent;
				current.mNbActLeftWithoutCurrent += adv.mNbActLeftWithoutCurrent;
                current.mHasPrevActive |= adv.mHasPrevActive || act.getActive() || adv.hasprevActive.size()!=0;
			}
			else
			{
				current.mCurrentNbActExp += 1;
				current.mMaximuNbActexp += 1;
				if (act.isFinished())
				{
					current.mNbActpassed += 1;
					current.mNbActpassedWithCurrent += 1;
				}
				else if (act.getActive()) 
				{
					current.mIsbranchActive = true;
					current.mIsbranchFinished = false;
					current.mHasPrevActive = true;
					current.mNbActpassedWithCurrent += 1;
					current.mNbActLeftWithCurrent += 1;
				}
				else
				{
					current.mIsbranchFinished = false;
					current.mNbActLeftWithCurrent += 1;
					current.mNbActLeftWithoutCurrent += 1;
				}
			}
		}
		Vertex[] nexts = current.activity.getChildGraphModel().getOutVertices(v);
		if (v instanceof Split)
		{
			current.isMarked.put(v, current);
			AdvancementCalculator[] advs = new AdvancementCalculator[nexts.length];
			for (int i = 0; i < nexts.length; i++)
			{
				advs[i] = new AdvancementCalculator();
				advs[i].mHasPrevActive = current.mHasPrevActive;
				advs[i].isMarked = current.isMarked;
				advs[i].HasNextMarked = current.HasNextMarked;
				advs[i].activity = current.activity;
				if ((v instanceof Loop) && (current.HasNextMarked.get(nexts[i]) != null))
//					Logger.debug(0, v.getID() + " " + nexts[i].getID() + " HasNextMarked")
                    ;
				else
					calc(nexts[i], advs[i]);
			}
			long maximuNbActexp = 0;
			long currentNbActExp = 0;
			long NbActpassed = 0;
			long NbActpassedWithCurrent = 0;
			long NbActLeftWithCurrent = 0;
			long NbActLeftWithoutCurrent = 0;
			boolean hasNobranchFinished = true;
			boolean hasNoBranchActive = true;
			for (AdvancementCalculator adv : advs) {
				if (adv.mIsbranchActive)
					hasNoBranchActive = false;
				if (adv.mIsbranchFinished)
					hasNobranchFinished = false;
			}
			for (AdvancementCalculator adv : advs) {

				if (maximuNbActexp < adv.mMaximuNbActexp)
					maximuNbActexp = adv.mMaximuNbActexp;
				if (adv.mIsbranchActive || adv.mIsbranchFinished || (hasNoBranchActive && hasNobranchFinished))
				{
					if (NbActpassed < adv.mNbActpassed)
						NbActpassed = adv.mNbActpassed;
					if (NbActpassedWithCurrent < adv.mNbActpassedWithCurrent)
						NbActpassedWithCurrent = adv.mNbActpassedWithCurrent;
					if (NbActLeftWithCurrent < adv.mNbActLeftWithCurrent)
						NbActLeftWithCurrent = adv.mNbActLeftWithCurrent;
					if (NbActLeftWithoutCurrent < adv.mNbActLeftWithoutCurrent)
						NbActLeftWithoutCurrent += adv.mNbActLeftWithoutCurrent;
					if (currentNbActExp < adv.mCurrentNbActExp)
						currentNbActExp = adv.mCurrentNbActExp;
				}
			}
			current.mCurrentNbActExp += currentNbActExp;
			current.mNbActpassedWithCurrent += NbActpassedWithCurrent;
			current.mMaximuNbActexp += maximuNbActexp;
			current.mNbActpassed += NbActpassed;
			current.mIsbranchActive = current.mIsbranchActive || !hasNoBranchActive;
			current.mNbActLeftWithCurrent += NbActLeftWithCurrent;
			current.mNbActLeftWithoutCurrent += NbActLeftWithoutCurrent;
			return;
		}
		if (v instanceof Join)
		{
			AdvancementCalculator adv;
			if (current.isMarked.get(v) == null)
			{
				adv = new AdvancementCalculator();
				adv.isMarked = current.isMarked;
				adv.HasNextMarked = current.HasNextMarked;
				adv.activity = current.activity;
				adv.mHasPrevActive = current.mHasPrevActive;
				current.isMarked.put(v, adv);
				if (nexts.length == 1)
					calc(nexts[0], adv);
			}
			else
				adv = (AdvancementCalculator) current.isMarked.get(v);
			current.mCurrentNbActExp += adv.mCurrentNbActExp;
			current.mMaximuNbActexp += adv.mMaximuNbActexp;
			current.mNbActpassed += adv.mNbActpassed;
			current.mNbActpassedWithCurrent += adv.mNbActpassedWithCurrent;
			current.mIsbranchActive = current.mIsbranchActive || (current.mMaximuNbActexp == 0 && adv.mIsbranchActive);
			if (current.mHasPrevActive)
				hasprevActive.put(String.valueOf(v.getID()), v);
			current.mNbActLeftWithCurrent += adv.mNbActLeftWithCurrent;
			current.mNbActLeftWithoutCurrent += adv.mNbActLeftWithoutCurrent;
			return;
		}
		if (nexts.length != 0)
			calc(nexts[0], current);
	}
	public long getLongestWayInAct()
	{
		return mMaximuNbActexp;
	}
	public long getCurrentLongestWayInAct()
	{
		return mCurrentNbActExp;
	}
	public long getNbActLeftWithActive()
	{
		return mNbActLeftWithCurrent;
	}
	public long getNbActLeftWithoutActive()
	{
		return mNbActLeftWithoutCurrent;
	}
	public long getNbActPassedWithoutActive()
	{
		return mNbActpassed;
	}
	public long getNbActPassedWithActive()
	{
		return mNbActpassedWithCurrent;
	}
}
