/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cplex.callbacks;
import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cplex.datatypes.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class LeafFetchingNodeHandler extends IloCplex.NodeCallback {
        
    private static Logger logger=Logger.getLogger(LeafFetchingNodeHandler.class);
        
    public List<NodeAttachment> allLeafs = new ArrayList<NodeAttachment>();
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+LeafFetchingNodeHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");     
            exit(1);
        }
          
    }
 
    protected void main() throws IloException {
        if(getNremainingNodes64()> ZERO){
            
            allLeafs.clear();
            long numLeafs = getNremainingNodes64();

            for (int index = ZERO ; index < numLeafs; index ++){
                
                //null check takes care of the corner case when, even after few minutes of solving, the root node of a subtree still has not branched
                if (null!=getNodeData(index)){
                    NodeAttachment attachMent = (NodeAttachment)getNodeData(index);
                    attachMent.sumOfIntegerInfeasibilities = getInfeasibilitySum(index);
                    attachMent.bestEstimateValue = getEstimatedObjValue(index);
                    allLeafs.add( attachMent);
                }
                
                
                 
            }
              
            //printTree();
            abort();
        }
    }
    
    private void printTree() throws IloException{
                
        for (NodeAttachment leaf : allLeafs){
            logger.debug(leaf);
            logger.debug("printing ancestors of "+leaf.nodeID);
            NodeAttachment parent = leaf.parentData;
            while (parent!= null){
                logger.debug(parent);
                parent = parent.parentData;
            }
            logger.debug("---------------------------------------------------");
        }        
    }
}
