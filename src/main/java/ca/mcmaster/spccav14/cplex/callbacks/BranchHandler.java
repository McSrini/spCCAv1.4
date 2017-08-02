/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cplex.callbacks;
 
import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cplex.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import java.io.File;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class BranchHandler extends IloCplex.BranchCallback {
    
    private static Logger logger=Logger.getLogger(BranchHandler.class);
         
    //list of nodes to be pruned
    public List<String> pruneList = new ArrayList<String>();
    
    public double bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    
    //a temporary measure used for  ensuring identical ramp ups
    //every time a node is created, record its node id, its parent node id, branching var , dir and value
    public List<String> nodeCreationInfoList = new ArrayList<String>();
    public int maxBranchingVars = ZERO;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+BranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging"+ex);        
            exit(1);
        }
          
    }
 
    protected void main() throws IloException {
        
        if ( getNbranches()> 0 ){  
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            if (nodeData==null ) { //it will be null for subtree root
               
                nodeData=new NodeAttachment (      );  
                setNodeData(nodeData);                
                
            } 
            
            if (isHaltFilePresent()) {
                System.err.println("Halt file found. Stopping...") ;
                logger.error("Halt file found. Stopping...") ;
                exit(ONE);
            }
            
            if (  pruneList.contains(nodeData.nodeID) ) {
                pruneList.remove( nodeData.nodeID);
                prune();
            }  else {
                
                //get the branches about to be created
                IloNumVar[][] vars = new IloNumVar[TWO][] ;
                double[ ][] bounds = new double[TWO ][];
                IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
                getBranches(  vars, bounds, dirs);
                
                //record left and right child branching conditions
                nodeData.branchingVars = vars;
                nodeData.branchingBounds=bounds;
                nodeData.branchingDirections =dirs;
                nodeData.estimatedLPRelaxationValue=  getObjValue();

                //now allow  both kids to spawn
                for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {   
                    
                    //apply the bound changes specific to this child
                    
                    //first create the child node attachment
                    NodeAttachment thisChild  =  new NodeAttachment (); 
                    thisChild.parentData = nodeData;
                    thisChild.depthFromSubtreeRoot=nodeData.depthFromSubtreeRoot + ONE;
                                                         
                    //record child node ID
                    IloCplex.NodeId nodeid = makeBranch(childNum,thisChild );
                    thisChild.nodeID =nodeid.toString();
                    thisChild.estimatedLPRelaxationValue = getObjValue();
                    
                    //logger.debug(" Node "+nodeData.nodeID + " created child "+  thisChild.nodeID + " varname " +   vars[childNum][ZERO].getName() + " bound " + bounds[childNum][ZERO] +   (dirs[childNum][ZERO].equals( IloCplex.BranchDirection.Down) ? " U":" L") ) ;
                    
                    //for testing purposes, mark some nodes as bad choices for migration
                    //if ( BAD_MIGRATION_CANDIDATES_DURING_TESTING.contains( thisChild.nodeID))       thisChild.isMigrateable= false;
                         
                    //for ramp-up testing, temporary measure
                    maxBranchingVars= Math.max(vars[childNum].length ,maxBranchingVars) ; //should always be 1
                    this.nodeCreationInfoList.add( nodeid.toString() +DELIMITER+nodeData.nodeID +DELIMITER+
                            vars[childNum][ZERO].getName()+DELIMITER+
                            bounds[childNum][ZERO] + DELIMITER+
                            (dirs[childNum][ZERO].equals( IloCplex.BranchDirection.Down) ? " U":" L") );
                                        
                    if (childNum == ZERO) {
                        //update left child info
                        nodeData.leftChildNodeID=thisChild.nodeID;   
                    }else {
                        nodeData.rightChildNodeID  =thisChild.nodeID ;                          
                    }

                    
                }//end for 2 kids
                
                
            }//end if else
            
            this.bestReamining_LPValue = getBestObjValue();
            
        } // end if getNbranches()> 0
        
    }//end main
        
    private static boolean isHaltFilePresent (){
        File file = new File("F:\\temporary files here\\haltfile.txt");
         
        return file.exists();
    }

}
