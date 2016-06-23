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

package org.cristalise.kernel.graph.model;


/**
 * Enumeration to define all Vertex properties which are used by collection and lifecycle packages
 *
 */
public enum BuiltInVertexProperties {
    /**
     * String property. The name of the Agent associated with Activities. Can be null or undefined.
     */
    AgentName("Agent Name"),

    /**
     * String property. The role of the Agent associated with Activities. Can be null or undefined.
     */
    AgentRole("Agent Role"),

    /**
     * String property. It is used in ActivitySlotDef to override the name of the ActivityDef
     */
    Name,
    
    /**
     * Integer property. It is used in CollectionMember to store the version of DescriptionDependency
     */
    Version,
    
    /**
     * String property ...
     */
    Description,

    /**
     * Boolean property ...
     */
    Breakpoint,
    
    /**
     * String property
     */
    Viewpoint,

    /**
     * String property to hold the name of the OutcomeInititator to be used by the Job associated with Activities. 
     * The name is used to find the class name defined in the Config section of the module.xml. 
     * For example, the OutcomeInitiator named <b>Empty</b> is defined like this:
     * <pre>
     * {@code<Config name="OutcomeInit.Empty">org.cristalise.kernel.persistency.outcome.EmptyOutcomeInitiator</Config>}
     * </pre>
     * 
     * Can be null or undefined.
     */
    OutcomeInit,

    /**
     * String property to hold either the name of the Script or the UUID of the Schema Item associated with Splits.
     * Can be null or undefined.
     */
    RoutingScriptName,

    /**
     * Integer property to hold the version of the Schema associated with Splits. Can be null or undefined. 
     */
    RoutingScriptVersion,

    /**
     * String property. Routing expression associated with Splits. It is interpreted with the Script class. 
     */
    RoutingExpr,

    /**
     * String property. Either the name of the Schema or the UUID of the Schema Item associated with Activities.
     * Can be null or undefined.
     */
    SchemaType,

    /**
     * Integer property to hold the version of the Schema associated with Activities.
     * Can be null or undefined.
     */
    SchemaVersion,

    /**
     * String property. Either the name of the Schema or the UUID of the Schema Item associated with Activities.
     * Can be null or undefined.
     */
    ScriptName,

    /**
     * Integer property to hold the version of the Script associated with Activities. Can be null or undefined.
     */
    ScriptVersion,
    
    /**
     * String property to hold either the name of the StateMachine or the UUID of the StateMachine Item associated with Activities.
     * Can be null or undefined. The default StateMachine is called Default
     */
    StateMachineName,
    
    /**
     * Integer property to hold the version of the StateMachine associated with Activities. Can be null or undefined.
     */
    StateMachineVersion,

    /**
     * String property. The type of object the Activity is going to create. Values are Schema, Script, StateMachine. 
     * Used in script DescriptionCollectionSetter. Can be null or undefined.
     */
    ObjectType,

    /**
     * Boolean property. Makes CompositeActitivy abortable, i.e. it can be finished even if it has active children.
     */
    Abortable,

    /**
     * Boolean property. Enables the Loop Transition of the CompositeActivity StateMachine
     */
    RepeatWhen;

    /**
     * This is only needed for backward compatibility because 'Agent Name' and 'Agent Role' string are used
     */
    private String alternativeName;

    private BuiltInVertexProperties() {
        alternativeName = name();
    }

    private BuiltInVertexProperties(String n) {
        alternativeName = n;
    }

    public String getAlternativeName() {
        return alternativeName;
    }
}
