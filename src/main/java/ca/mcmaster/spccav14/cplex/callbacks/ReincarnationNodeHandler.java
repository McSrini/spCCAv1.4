/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cplex.callbacks;
 
import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cb.*;
import ca.mcmaster.spccav14.cplex.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * 
 * as long as there is a new node that corresponds to an old node that requires controlled branching, redirect to it
 * Abort once no such node exists
 * 
 */
public class ReincarnationNodeHandler extends IloCplex.NodeCallback {
    
    private static Logger logger=Logger.getLogger(ReincarnationNodeHandler.class);
    private ReincarnationMaps reincarnationMaps;
            
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ReincarnationNodeHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
          
    }
 
    public ReincarnationNodeHandler (ReincarnationMaps reincarnationMaps) {
        this .reincarnationMaps=reincarnationMaps;
    }
        
    protected void main() throws IloException {
        if(getNremainingNodes64()> ZERO){
                           
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData(ZERO);
            if (nodeData==null ) { //it will be null for subtree root
               
                nodeData=new NodeAttachment (      );  
                setNodeData(ZERO,nodeData);                
                
            } 
            
            //WARNING NOTE - BE CAREFUL WITH getNodeId(ZERO) CALL
            //WE SET SUBTREE ROOT NODE ID TO -1 , WHICH DOES NOT CORRESPOND TO THE NODE ID AS REPORTED BY THE API CALL
            //THIS HOWEVER DOES NOT SEEM TO MATTER , BECAUSE THE NODE CALLBACK NEVER SELECTS THE SUBTREE ROOT ( SEEMS LIKE
            //IT GOES DIRECTLY INTO THE BRANCH CALLBACK WHEN STARTING TO SOLVE A SUBPROBLEM
           
            //first check the default node selection
            String selectedNodeID = getNodeId(ZERO).toString(); 
            String oldNodeId = this.reincarnationMaps.newToOld_NodeId_Map.get(selectedNodeID);
                    
            if (oldNodeId==null ) {
                
                //try to find any active leaf that corresponds to an old node
                //if no such active leaf exists, migration is complete because:
                //  1) all old nodes have been branched upon , OR
                //  2) some old nodes got resolved, so their descendents will never be created or branched upon
                //
                long selectedIndex =  checkMergeCompletion();
                if ( -ONE == selectedIndex) { 
                    //stop  , migration is complete                    
                    abort();
                } else {
                    //select this node
                    selectNode( selectedIndex);
                }
                
            }else {
                //do nothing, take CPLEX's default node selection
            }
        }
    }
    
    private long checkMergeCompletion() throws IloException {
        long selectedIndex = -ONE;
                        
        //pick up any active leaf, which  corresponds to an old node
       
        for (long index = ZERO; index<getNremainingNodes64(); index ++ ) {
            String newNodeId=getNodeId(index).toString();
            
            if ( this.reincarnationMaps.newToOld_NodeId_Map.get(newNodeId) !=null) {
                //we have found a candidate node
                selectedIndex=index;
                break;               
            }
        }
                  
        return selectedIndex;
    }
}
