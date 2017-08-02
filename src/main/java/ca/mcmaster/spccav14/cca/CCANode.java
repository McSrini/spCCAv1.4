/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cca;
 
import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cplex.datatypes.*;
import java.io.Serializable;
import static java.lang.System.exit;
import java.util.*; 

/**
 *
 * @author tamvadss
 * 
 */
public class CCANode  implements Serializable {
    
    //private static Logger logger=Logger.getLogger(CCANode.class);
    
    public String nodeID;
    public double     lpRelaxationValue ;
    //for leaf nodes that are converted into CCA nodes for round-robin, we have 2 more properties which help with round-robin
    public double     bestEstimateValue ;
    public double     sumOfIntegerInfeasibilities ;
             
    public int refCountLeft  =ZERO ;
    public int refCountRight  =ZERO;
    
    //these two are used by CB 
    public int skipCountLeft=ZERO, skipCountRight= ZERO;
     
    //if this CCA node is used , how many node LPs need to be solved ?
    public int numNodeLPSolvesNeeded=ZERO;
    //record # of nodes with one missing branch, and their depth away from root
    public Map<Integer, Integer > mapOfNodesWithOneMissingBranch= new LinkedHashMap <Integer, Integer > ();
    
    public int depthOfCCANodeBelowRoot;
    //max depth of current solution tree
    public int maxDepthOFTree;
    
    //how to get to this node from its parent, which could be the original MIP
    public List<BranchingInstruction> branchingInstructionList= new ArrayList<BranchingInstruction>();
    
    //if this CCA node has kids, how to create them. This is used only during controlled branching.
    public List<BranchingInstruction> leftChildBranchingInstructions =new ArrayList<BranchingInstruction>();
    public String leftChildNodeID = null;
    public List<BranchingInstruction> rightChildBranchingInstructions=new ArrayList<BranchingInstruction>();
    public String rightChildNodeID = null;
    //if the following flag is set, this CCA node should not be used as it is, must controlled branch under it.
    public boolean isControlledBranchingRequired = false;
 
    //nodes that need to be pruned from original MIP, in case this CCA node is chosen for migration
    public List<String> pruneList=new ArrayList<String>();
    
    /*static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ CCANode.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }*/
    
    public boolean areCCAStatisticsPopulated (){
        return numNodeLPSolvesNeeded>ZERO;
    }
    
    public int getNumGoodLeafsRepresented() {
        return  refCountLeft+refCountRight;
    }
        
    //how good is this CCA node?
    //packing factor < 2.0 is desireable
    public double getPackingFactor (){
        //currently ignoring count of nodes with missing branches, and other factors such as depth
        return ( DOUBLE_ZERO+  numNodeLPSolvesNeeded )/ ( DOUBLE_ZERO+ pruneList.size())  ;
    }
    
    /*public String toString () {
        String result =          "Printing CCA Node "+ this.nodeID + "\n";
        result +=          "Refcounts  "+ this.refCountLeft + " , " + this.refCountRight+ "\n" ;
        result +=          "Skip counts  "+ this.skipCountLeft + " , " + this.skipCountRight+ "\n" ;
        result += "LP solves needed "+ this.numNodeLPSolvesNeeded+ "\n";
        result += " Here is the single branch map size "+ this.mapOfNodesWithOneMissingBranch.entrySet().size()
                +"\n";
        for (Map.Entry<Integer, Integer> entry : this.mapOfNodesWithOneMissingBranch.entrySet()) {
            // result +=(entry.getKey() + ", " + entry.getValue() + "\n");
        }
        result += " Depth below root " + this.depthOfCCANodeBelowRoot + " and max depth" + this.maxDepthOFTree+ "\n";
        result += " Here is the prune list"+ "\n";
        for (String nodeid: this.pruneList){
            result += (nodeid + ",");
        }
        result += " \n Here are the branching instructions"+ "\n";
        for (BranchingInstruction bi: branchingInstructionList){
            result += bi +",";
        }
        
        result += " \n Here are the LEFT SIDE branching instructions for left child "+ this.leftChildNodeID+"\n";
        for (BranchingInstruction bi: this.leftChildBranchingInstructions){
            result += bi +",";
        }
       
        result += " \n Here are the RIGHT SIDE branching instructions for right child "+ this.rightChildNodeID+"\n";
        for (BranchingInstruction bi: this.rightChildBranchingInstructions){
            result += bi +",";
        }

        return result + "\n";
    }*/
    
}
