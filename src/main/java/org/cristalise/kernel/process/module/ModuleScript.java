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
package org.cristalise.kernel.process.module;

import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;

public class ModuleScript {


	public String target;
	public String event;
	public String lang;
	public String script;
	public ModuleScript() {
	}
	
	public ModuleScript(String target, String event, String lang, String script) {
		super();
		this.target = target;
		this.event = event;
		this.lang = lang;
		this.script = script;
	}
	
	public Script getScript(String ns, AgentProxy user) throws ScriptingEngineException {
        return new Script(lang, ns+" "+target+" "+event, script, user);
	}

	public boolean shouldRun(String event, boolean isServer) {
		return ((this.target == null || this.target.length() == 0 || isServer == target.equals("server")) && 
				(this.event == null || this.event.length() == 0 || event.equals(this.event)));
	}
}
