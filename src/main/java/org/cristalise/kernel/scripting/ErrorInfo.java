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
package org.cristalise.kernel.scripting;

import java.util.ArrayList;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.cristalise.kernel.entity.agent.Job;

public class ErrorInfo {
    boolean fatal = false;
    Job     failedJob;

    ArrayList<String> errors;

    public ErrorInfo() {
        super();
        errors = new ArrayList<String>();
    }

    public ErrorInfo(String error) {
        this();
        errors.add(error);
    }

    public ErrorInfo(Exception ex) {
        this();
        setFatal();
        for (String frame : ExceptionUtils.getStackFrames(ex)) {
            addError(frame.trim());
        }
    }

    public ErrorInfo(Job job, Exception ex) {
        this(ex);
        failedJob = job;
    }

    public void addError(String error) {
        errors.add(error);
    }

    @Override
    public String toString() {
        StringBuffer err = new StringBuffer();
        for (String element : errors) {
            err.append(element + "\n");
        }
        return err.toString();
    }

    public void setErrors(ArrayList<String> msg) {
        this.errors = msg;
    }

    public ArrayList<String> getErrors() {
        return errors;
    }

    public void setFatal(boolean flag) {
        fatal = flag;
    }

    public void setFatal() {
        fatal = true;
    }

    public boolean getFatal() {
        return fatal;
    }

    public Job getFailedJob() {
        return failedJob;
    }

    public void setFailedJob(Job failedJob) {
        this.failedJob = failedJob;
    }
}
