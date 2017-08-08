/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cplex.callbacks;
 
import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cb.*;
import ca.mcmaster.spccav14.cplex.datatypes.*;
import ca.mcmaster.spccav14.cca.CCANode;
import ca.mcmaster.spccav14.utils.*;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;


/**
 *
 * @author tamvadss
 * 
 * same as regular solution time branch handler, but
 * branching is controlled and reincarnation Maps are used to map old node IDs to
 * their corresponding new node IDs
 * 
 * 
 */
public class ReincarnationBranchHandler extends IloCplex.BranchCallback {
 
    private static Logger logger=Logger.getLogger(ReincarnationBranchHandler.class);
    private ReincarnationMaps reincarnationMaps;
    private Map<String, CCANode>  instructionTreeAsMap;
    
    private IloNumVar[]  modelVars;
             
    //list of nodes to be pruned
    public List<String> pruneList = new ArrayList<String>();
    
    public double bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ReincarnationBranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
          
    }
    
    public ReincarnationBranchHandler (Map<String, CCANode> instructionTreeAsMap,  ReincarnationMaps reincarnationMaps, IloNumVar[]  modelVars) {
        this .reincarnationMaps=reincarnationMaps;
        this.instructionTreeAsMap = instructionTreeAsMap;
        this.modelVars=modelVars;
        
    }
    
    protected void main() throws IloException {
        
        if ( getNbranches()> 0 ){  
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            if (nodeData==null ) { //it will be null for subtree root
               
                nodeData=new NodeAttachment (      );  
                setNodeData(nodeData);                
                
            } 
            
            if (  pruneList.contains(nodeData.nodeID) ) {
                pruneList.remove( nodeData.nodeID);
                prune();
            }  else {
                
                // branches about to be created
                IloNumVar[][] vars = new IloNumVar[TWO][] ;
                double[ ][] bounds = new double[TWO ][];
                IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
                 
                //we do not take CPLEX default branching, instead we use controlled branching
                //
                //Note that every node controlled branched upon marks itself as controlled branched upon. This is
                //required because in the future, if such nodes are chosen as CCA nodes for migration, then we must 
                //control branch upon them to avoid visiting too many redundant intermidiate nodes
                nodeData.wasControlledBranchedUpon=true;
               
                //now allow  both kids to spawn, note that every CCA node has 2 kids
                for (int childNum = ZERO ;childNum<TWO;  childNum++) {   
                    
                    //apply the bound changes specific to this child
                    
                    //first create the child node attachment
                    NodeAttachment thisChild  =  new NodeAttachment (); 
                    thisChild.parentData = nodeData;
                    thisChild.depthFromSubtreeRoot=nodeData.depthFromSubtreeRoot + ONE;
                    
                    //now get the branching instructions in the CCA node for this child 
                    String oldParentNodeID = reincarnationMaps.newToOld_NodeId_Map.get(nodeData.nodeID);
                    List<BranchingInstruction> branchingInstructionList = childNum == ZERO ? 
                                               instructionTreeAsMap.get(oldParentNodeID ).leftChildBranchingInstructions:
                                               instructionTreeAsMap.get( oldParentNodeID ).rightChildBranchingInstructions;
                    
                    //convert branching instructions into cplex format
                    BranchHandlerUtilities.mergeBranchingInstructionIntoArray (  branchingInstructionList , vars,   bounds ,  
                                                                                 dirs, childNum,  modelVars);
                    
                    //create child and record child node ID
                    IloCplex.NodeId nodeid =makeBranch( vars[childNum],  bounds[childNum],dirs[childNum], getObjValue(), thisChild);
                    thisChild.nodeID =nodeid.toString();
                    thisChild.estimatedLPRelaxationValue = getObjValue();
                    
                    logger.debug(" Node "+nodeData.nodeID + " created child "+  thisChild.nodeID + " using branches as follows ");
                    for (int numBranchingVars = ZERO; numBranchingVars< vars[childNum].length; numBranchingVars++) {
                        logger.debug( " varname " +   vars[childNum][numBranchingVars].getName() + " bound " + bounds[childNum][numBranchingVars] +   
                                   (dirs[childNum][numBranchingVars].equals( IloCplex.BranchDirection.Down) ? " U":" L") ) ;

                    }  
                    
                    //for testing purposes, mark some nodes as bad choices for migration
                    //if ( BAD_MIGRATION_CANDIDATES_DURING_TESTING.contains( thisChild.nodeID))       thisChild.isMigrateable= false;
                         
                                        
                    if (childNum == ZERO) {
                        //update left child info
                        nodeData.leftChildNodeID=thisChild.nodeID;     
                         
                    }else {
                        nodeData.rightChildNodeID  =thisChild.nodeID ;  
                         
                    }
                    nodeData.branchingVars = vars;
                    nodeData.branchingBounds=bounds;
                    nodeData.branchingDirections =dirs;
                    nodeData.estimatedLPRelaxationValue=  getObjValue();
                    
                    //for each kid created , update the reincarnation Maps
                    //cvn jk is boy////58655545185151512128884545  <<- comment from my daughter ! Think of it as a present ;an easter egg !
                    
 
                    
                    String oldChildNodeID = childNum == ZERO? this.instructionTreeAsMap.get(oldParentNodeID ).leftChildNodeID : 
                                                              this.instructionTreeAsMap.get(oldParentNodeID ).rightChildNodeID;
                    if (this.reincarnationMaps.oldToNew_NodeId_Map.containsKey( oldChildNodeID)) {
                        //this is a CCA node and needs to be branched upon
                        this.reincarnationMaps.newToOld_NodeId_Map.put(thisChild.nodeID,oldChildNodeID );
                        this.reincarnationMaps.oldToNew_NodeId_Map.put(oldChildNodeID, thisChild.nodeID);
                        
                    }
                    logger.info(oldChildNodeID + " and corresponding new node id " + thisChild.nodeID) ;

                                        
                }//end for 2 kids
                
                
            }//end if else
            
            this.bestReamining_LPValue = getBestObjValue();
            
        } // end if getNbranches()> 0
        
    }//end main
    
       
    
}
