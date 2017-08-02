/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cca;

import static ca.mcmaster.spccav14.Constants.*;
import   ca.mcmaster.spccav14.utils.*;
import ca.mcmaster.spccav14.cplex.datatypes.*;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * runs the CCA algorithm and finds candidate CCA nodes
 */
public class CCAFinder {
      
    private static Logger logger=Logger.getLogger(CCAFinder.class);
    
    private List<NodeAttachment> allLeafs = new ArrayList<NodeAttachment> () ;    
    private  NodeAttachment root = null;
    
    public List <CCANode> candidateCCANodes = new ArrayList <CCANode> ();
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+CCAFinder.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
               
    //prepares an index using solution tree nodes
    //This index is used to execute the CCA algorithm
    public void initialize (   List<NodeAttachment> allActiveLeafs ) {
        
        allLeafs=allActiveLeafs;
         
        for (NodeAttachment leaf : this.allLeafs){
            
            //climb up from this leaf  
            NodeAttachment thisNode= leaf ;
            NodeAttachment parent = thisNode.parentData;
            while (parent !=null) {
                
                if (parent.ccaInformation==null) {
                    parent.ccaInformation=new CCANode();
                    parent.ccaInformation.nodeID = parent.nodeID;
                }
                
                //set the child refs for parent
                if (parent.leftChildNodeID!=null && parent.leftChildNodeID.equals( thisNode.nodeID)) {                    
                    parent.leftChildRef=thisNode;                   
                }else {   
                    parent.rightChildRef=thisNode;
                }
                                
                //if parent has both left and right references set, we need not climb up 
                if (parent.rightChildRef!=null &&parent.leftChildRef!=null ) break;
                
                //climb up
                thisNode= parent;
                parent = parent.parentData;
                
            }//end while
            
            if (parent ==null) root = thisNode;
        }
        
    }//end init 
    
    public void close () {
        //reset CCA information in every node
        if (this.allLeafs!=null){
            for (NodeAttachment leaf : this.allLeafs){

                //climb up from this leaf  
                NodeAttachment thisNode= leaf ;
                NodeAttachment parent = thisNode.parentData;
                while (parent !=null) {

                    //no need to reset parent and climb up , if already traversed
                    if (parent.ccaInformation==null) break;

                    parent.rightChildRef=null;
                    parent.leftChildRef=null;
                    parent.ccaInformation=null;

                    thisNode= parent;
                    parent = parent.parentData;

                }//end while
            }
        }
        
    }
    
    //these getCandidateCCANodes() methods can be invoked multiple times, each time with a differnt argument
    public List<CCANode> getCandidateCCANodes (List<String> wantedLeafNodeIDs) {
        buildRefCounts(  wantedLeafNodeIDs);
        //printState(root);
        
        //prepare to split tree and find CCA nodes
        candidateCCANodes.clear();
        
        this.splitToCCA(root, wantedLeafNodeIDs.size() );
        return this.candidateCCANodes;
    }
    
    public List<CCANode> getCandidateCCANodes (int count)   {
        buildRefCounts();
        //printState(root);
                
        //prepare to split tree and find CCA nodes
        candidateCCANodes.clear();
        
        this.splitToCCA(root, count);
        return this.candidateCCANodes;
    }
     
    public List<CCANode> getCandidateCCANodesPostRampup (int numPartitions) {
        buildRefCounts();
        //printState(root);
                
        //prepare to split tree and find CCA nodes
        candidateCCANodes.clear();
        
        //create a list of sub-tree roots , each of which should end up on 1 partition
        List<NodeAttachment> subtreeRootList = new ArrayList<NodeAttachment>();               
        //start with the root, and find all the subtree roots for distribution
        subtreeRootList.add(root);
        splitToCCAPostRampup(subtreeRootList, numPartitions);
        
        //if we have subtree roots which have only one child, repeatedly move down  the child nodes till the child has 2 kids
        List<NodeAttachment> removalList = new ArrayList <NodeAttachment>();
        for (NodeAttachment candidateRoot : subtreeRootList){
            if (candidateRoot.ccaInformation.refCountLeft==ZERO || candidateRoot.ccaInformation.refCountRight==ZERO) {
                removalList.add( candidateRoot);                
            }
        }
        for (NodeAttachment node :removalList ) {
            subtreeRootList.remove(node);
            subtreeRootList.add(skipToNodeWithTwoChildren(node)) ;
        }
        
        //convert every subtree root into a CCA node
        for (NodeAttachment node : subtreeRootList){
            CCAUtilities.populateCCAStatistics(node, this.allLeafs) ;
            candidateCCANodes.add(node.ccaInformation);    
        }
         
        return this.candidateCCANodes;
    }
    
    //if a node has only 1 child, repeatedly move down  the child nodes till the child has 2 kids
    private NodeAttachment skipToNodeWithTwoChildren (NodeAttachment node) {
        NodeAttachment result = node ;
        while (result.ccaInformation.refCountLeft==ZERO || result.ccaInformation.refCountRight==ZERO ){
            //move down to the non null side
            if (result.ccaInformation.refCountLeft!=ZERO) {
                result = result.leftChildRef;
            }else {
                result = result.rightChildRef;
            }
        }
        
        return result;
    }
     
    private void buildRefCounts( ){
       buildRefCounts(null);
    }
    
    //pass in null to build counts using isMigratable flag inside the leaf
    private void buildRefCounts(List<String> wantedLeafNodeIDs){
        clearRefCountsAndSkipCounts();
        for (NodeAttachment leaf : this.allLeafs){
            if (wantedLeafNodeIDs!=null && !wantedLeafNodeIDs.contains(leaf.nodeID)) continue;
            
            //climb up from this leaf  
            NodeAttachment thisNode= leaf ;
            NodeAttachment parent = thisNode.parentData;
            while (parent !=null) {
                 
                if (parent.leftChildNodeID!=null && parent.leftChildNodeID.equals( thisNode.nodeID)) {      
                    if (thisNode.isLeaf() ){
                        parent.ccaInformation.refCountLeft =  wantedLeafNodeIDs!=null ? ONE: thisNode.isMigrateable ? ONE : ZERO;
                    } else {
                        parent.ccaInformation.refCountLeft =thisNode.ccaInformation.refCountLeft +  thisNode.ccaInformation.refCountRight;
                    }
                } else {
                    if (thisNode.isLeaf() ){
                        parent.ccaInformation.refCountRight =wantedLeafNodeIDs!=null ? ONE: thisNode.isMigrateable ? ONE : ZERO;
                    } else {
                        parent.ccaInformation.refCountRight =thisNode.ccaInformation.refCountLeft +  thisNode.ccaInformation.refCountRight;
                    }
                }
                
                thisNode= parent;
                parent = parent.parentData;
                
            }//end while
        }
    }
    
    //split biggest remaining subtree root into 2 pieces until number of pieces exceeds numPartitions
    private void splitToCCAPostRampup(    List<NodeAttachment> subtreeRootList, int numPartitions){
        if (subtreeRootList.size()< numPartitions){
            
            //pick the subtree root with the largest ref-count and split it into two
            //note that eligible sutree roots are those , for which at least 1 child is a non-leaf node ( i.e.   valid CCA node)
            long maxRefCount = MINUS_INFINITY;
            int indexOfMax = -ONE;
             
            for (int index = ZERO; index < subtreeRootList.size(); index ++){
                NodeAttachment thisSubtreeRoot = subtreeRootList.get(index);
                boolean isLeftEligible = thisSubtreeRoot.leftChildRef!=null && !thisSubtreeRoot.leftChildRef.isLeaf() && 
                        (thisSubtreeRoot.leftChildRef.ccaInformation.refCountLeft+ thisSubtreeRoot.leftChildRef.ccaInformation.refCountRight >ONE);
                boolean isRightEligible = thisSubtreeRoot.rightChildRef!=null && !thisSubtreeRoot.rightChildRef.isLeaf() &&
                        (thisSubtreeRoot.rightChildRef.ccaInformation.refCountLeft+ thisSubtreeRoot.rightChildRef.ccaInformation.refCountRight >ONE);
                if  (!isRightEligible && !isLeftEligible) continue;
                if (thisSubtreeRoot.ccaInformation.refCountLeft + thisSubtreeRoot.ccaInformation.refCountRight > maxRefCount) {
                    maxRefCount=thisSubtreeRoot.ccaInformation.refCountLeft + thisSubtreeRoot.ccaInformation.refCountRight;
                    indexOfMax= index;
                    
                }
            }
            
            logger.debug("maxRefCount " + maxRefCount + " at Index "+ indexOfMax + " " + subtreeRootList.get(indexOfMax).ccaInformation.refCountLeft + " + "+subtreeRootList.get(indexOfMax).ccaInformation.refCountRight );
            if (indexOfMax< ZERO){
                //this partitioning cannot be done
                logger.error("this splitToCCAPostRampup partitioning cannot be done  , try ramping up to  a larger number of leafs ");
            } else{
                //get the candidate with biggest ref count
                NodeAttachment thisSubtreeRoot = subtreeRootList.get(indexOfMax);
                //split it into 2
                subtreeRootList.remove(thisSubtreeRoot );
                if (thisSubtreeRoot.leftChildRef!=null && !thisSubtreeRoot.leftChildRef.isLeaf()) {
                    if (thisSubtreeRoot.leftChildRef.ccaInformation.refCountLeft +thisSubtreeRoot.leftChildRef.ccaInformation.refCountRight > ONE) 
                        subtreeRootList.add(thisSubtreeRoot.leftChildRef);
                        logger.debug("addded left child having refcount" + thisSubtreeRoot.leftChildRef.ccaInformation.refCountLeft + " + "+thisSubtreeRoot.leftChildRef.ccaInformation.refCountRight) ;
                }
                if (thisSubtreeRoot.rightChildRef!=null&& !thisSubtreeRoot.rightChildRef.isLeaf()) {
                    if (thisSubtreeRoot.rightChildRef.ccaInformation.refCountLeft + thisSubtreeRoot.rightChildRef.ccaInformation.refCountRight>ONE ) 
                        subtreeRootList.add(thisSubtreeRoot.rightChildRef);
                        logger.debug("addded right child having refcount" + thisSubtreeRoot.rightChildRef.ccaInformation.refCountLeft + " + " +thisSubtreeRoot.rightChildRef.ccaInformation.refCountRight) ;
                }

                //make a recursive call
                splitToCCAPostRampup(     subtreeRootList,   numPartitions);
            }
            
        }       
        
    }
    
    //start from root and split tree into left and right, looking for candidate CCA nodes
    //count can be desired count, or the size of the list of desired leafs
    private void splitToCCA( NodeAttachment thisNode, int count){
        
        if (thisNode.isLeaf()) {
            //not a valid CCA candidate, discard
        } else  if (isSplitNeeded(thisNode,   count)) {
            
            if (thisNode.ccaInformation.refCountLeft>ZERO) {
                splitToCCA(   thisNode.leftChildRef,   count);
            }
            if (thisNode.ccaInformation.refCountRight>ZERO) {
                splitToCCA(   thisNode.rightChildRef,   count);
            }
            
        } else {
            //check if valid CCA candidate, else discard 
            if (thisNode.ccaInformation.refCountLeft+ thisNode.ccaInformation.refCountRight>= count*(ONE-CCA_TOLERANCE_FRACTION)) {
                //found a valid candidate
                //add branching instructions, # of redundant LP solves needed and so on
                CCAUtilities.populateCCAStatistics(thisNode, this.allLeafs) ;
                candidateCCANodes.add(thisNode.ccaInformation);                
            }
        }
        
    }
    
    private boolean isSplitNeeded ( NodeAttachment thisNode, int count) {
        boolean result = false;
               
        if (thisNode.ccaInformation.refCountLeft  >= count ) result = true;
        if ( thisNode.ccaInformation.refCountRight >=count ) result = true;
        
        if (    (thisNode.ccaInformation.refCountLeft  >= count*(ONE-CCA_TOLERANCE_FRACTION) ) && 
                (thisNode.ccaInformation.refCountRight  < count*(CCA_TOLERANCE_FRACTION) )  ) {
            result = true;
        }
        
        if (    (thisNode.ccaInformation.refCountRight  >= count*(ONE-CCA_TOLERANCE_FRACTION) ) && 
                (thisNode.ccaInformation.refCountLeft  < count*(CCA_TOLERANCE_FRACTION) )  ) {
            result = true;
        }
        
        return result;
        
    }
    
    private void clearRefCountsAndSkipCounts() {
        for (NodeAttachment leaf : this.allLeafs){
            
            //climb up from this leaf  
            NodeAttachment thisNode= leaf ;
            NodeAttachment parent = thisNode.parentData;
            while (parent !=null) {
                
                parent.ccaInformation.refCountLeft=ZERO;
                parent.ccaInformation.refCountRight=ZERO; 
                parent.ccaInformation.skipCountLeft=ZERO;
                parent.ccaInformation.skipCountRight=ZERO;
                 
                thisNode= parent;
                parent = parent.parentData;
                
            }//end while
        }
    }
    

}
