package org.cristalise.kernel.collection;


/**
 * Helper enumeration to make built-in Collection names easier to maintain and document
 */
public enum BuiltInCollections {
    /**
     * CollectionDescription of CompositeActivityDef. It is instantiated as ....
     */
    ACTIVITY("Activity"),

    /**
     * CollectionDescription of Module. It is instantiated as ....
     */
    CONTENTS("Contents"),

    /**
     * CollectionDescription of Script. It is instantiated as ...
     * 
     */
    INCLUDE("Include"),

    /**
     * CollectionDescription of elementary ActivityDef. It is instantiated as two Activity properties
     * 
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#SchemaType
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#SchemaVersion
     */
    SCHEMA("Schema"),

    /**
     * CollectionDescription of elementary ActivityDef. It is instantiated as two Activity properties:
     * 
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#StateMachineName
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#StateMachineVersion
     */
    SCRIPT("Script"),

    /**
     * CollectionDescription of elementary ActivityDef. It is instantiated as two Activity properties
     * 
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#StateMachineName
     * @see org.cristalise.kernel.graph.model.BuiltInVertexProperties#StateMachineVersion
     */
    STATE_MACHINE("StateMachine"),
    

    /**
     * 
     */
    WORKFLOW("Workflow"),

    /**
     * 
     */
    WORKFLOW_PRIME("Workflow'");


    private String collectionName;

    private BuiltInCollections(final String n) {
        collectionName = n;
    }

    public String getName() {
        return collectionName;
    }

    public String toString() {
        return getName();
    }
}
