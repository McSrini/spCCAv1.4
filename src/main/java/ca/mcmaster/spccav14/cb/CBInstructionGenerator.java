/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cb;
 
import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cca.CCANode;
import ca.mcmaster.spccav14.cplex.datatypes.*;
import ca.mcmaster.spccav14.utils.CCAUtilities;
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
 * updates skip counts into tree under the CCA node, and 
 * uses the counts to generate the CB tree rooted at the CCA
 * 
 */
public class CBInstructionGenerator {
    
    private static Logger logger=Logger.getLogger(CBInstructionGenerator.class);
      
    private  List<NodeAttachment> allActiveLeafs;
    private  List<NodeAttachment> wantedActiveLeafs = new ArrayList<NodeAttachment> ();
    
    //node attachment corresponding to CCA root
    private NodeAttachment ccaRootNodeAttachment;
    
    //return value
    public CBInstructionTree cbInstructionTree ;
    
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+CBInstructionGenerator.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
    
    public CBInstructionGenerator(CCANode ccaNode,   List<NodeAttachment> allLeafs, List<String> wantedLeafs) {
        cbInstructionTree = new CBInstructionTree(ccaNode);         
        this.allActiveLeafs=allLeafs;
        for (NodeAttachment leaf : this.allActiveLeafs){
           if ( wantedLeafs.contains(leaf.nodeID)) wantedActiveLeafs.add(leaf );
            
        }
        this.ccaRootNodeAttachment=getCCARootNodeAttachment();
        
        buildSkipcounts();
        this.printState(ccaRootNodeAttachment);
    }
    
    public CBInstructionTree generateInstructions (  ) {
        return generateInstructions( this.cbInstructionTree, this.ccaRootNodeAttachment, this.cbInstructionTree.pruneList );
    }
    
    //arguments are the reference to the instruction tree subtree starting at this CCA  , and the corresponding node attachment from the solution tree
    private CBInstructionTree generateInstructions (CBInstructionTree tree, NodeAttachment node, List<String> pruneList) {
        
        //if packing factor is not good under this subtree, then create its 2 kids which are also CB trees
        //then make a recursive call on the kids unless the kids happen to be leafs
        //Any CCA node [i.e non-leaf] must have its properties populated
        //We only create CCA nodes
        if (tree.ccaRoot.getPackingFactor()> CCA_PACKING_FACTOR_MAXIMUM_ALLOWED  || node.wasControlledBranchedUpon){
             
            if (tree.ccaRoot.refCountLeft>ZERO){
                
                // create instructions for branching to the left side after skipping past 'skip-count'  nodes
                //AND
                //create CCA node on left side [if its not a leaf]
                                
                NodeAttachment currentNode =node; 
                for (int index = ZERO ; index <= tree.ccaRoot.skipCountLeft; index ++){
                    
                    //the first skip is to the left
                    //any subsequent skips are determined by the side which has a non-zero ref-count
                    if (index == ZERO){
                        tree.ccaRoot.leftChildBranchingInstructions.add( currentNode.getBranchingInstructionForLeftChild());
                        currentNode = currentNode.leftChildRef;
                    } else {
                        if (currentNode.ccaInformation.refCountLeft>ZERO) {
                            tree.ccaRoot.leftChildBranchingInstructions.add( currentNode.getBranchingInstructionForLeftChild());
                            currentNode = currentNode.leftChildRef;
                        }  else {
                            tree.ccaRoot.leftChildBranchingInstructions.add( currentNode.getBranchingInstructionForRightChild());
                            currentNode = currentNode.rightChildRef;
                        } 
                    }
                   
                }
                
                if (!currentNode.isLeaf()){
                    //create CCA node in the tree and make recursive call
                    
                    //populate CCA information
                    CCAUtilities.populateCCAStatistics(currentNode, this.allActiveLeafs) ;
                    //make the link
                    tree.leftSubTree = new CBInstructionTree(currentNode.ccaInformation);
                    //make recursive call
                    generateInstructions (tree.leftSubTree, currentNode, pruneList);  
                } else {
                    //make a note of this leaf in the prune list
                    pruneList.add(currentNode.nodeID);
                    
                }
                
                //record the node ID of the left child to which we will make the controlled branch
                tree.ccaRoot.leftChildNodeID = currentNode.nodeID;
                
            }//left ref count > 0  
            
            //repeat the same for the right side
            
            if (tree.ccaRoot.refCountRight>ZERO){
                
                NodeAttachment currentNode =node; 
                for (int index = ZERO ; index <= tree.ccaRoot.skipCountRight; index ++){
                    
                    if (index == ZERO){
                        tree.ccaRoot.rightChildBranchingInstructions.add( currentNode.getBranchingInstructionForRightChild());
                        currentNode = currentNode.rightChildRef;
                    } else {
                        if (currentNode.ccaInformation.refCountLeft>ZERO) {
                            tree.ccaRoot.rightChildBranchingInstructions.add( currentNode.getBranchingInstructionForLeftChild());
                            currentNode = currentNode.leftChildRef;
                        }  else {
                            tree.ccaRoot.rightChildBranchingInstructions.add( currentNode.getBranchingInstructionForRightChild());
                            currentNode = currentNode.rightChildRef;
                        }                     
                    }
                   
                }
                
                if (!currentNode.isLeaf()){
                    //create CCA node in the tree and make recursive call
                    
                    //populate CCA information
                    CCAUtilities.populateCCAStatistics(currentNode, this.allActiveLeafs) ;
                    //make the link
                    tree.rightSubtree = new CBInstructionTree(currentNode.ccaInformation);
                    //make recursive call
                    generateInstructions (tree.rightSubtree, currentNode, pruneList);  
                }else {
                    //make a note of this leaf in the prune list
                    pruneList.add(currentNode.nodeID);
                }
                                
                //record the node ID of the right child to which we will make the controlled branch
                tree.ccaRoot.rightChildNodeID = currentNode.nodeID;
            } 
            
            
        } else {
            
            //packing factor was tight enough , so no CB instructions
            //Therefore we must add all this node's descendants into the prune list
            pruneList.addAll(tree.ccaRoot.pruneList);
        }
        
        return tree;
    }
    
    //return node attachment corresponding to CCA root
    private NodeAttachment getCCARootNodeAttachment () {
        
        //pick any wanted leaf
        NodeAttachment currentNode = this.wantedActiveLeafs.get(ZERO);           
        NodeAttachment parentNode = currentNode.parentData;

        //climb up to the CCA node
        while (! currentNode.nodeID.equals(this.cbInstructionTree.ccaRoot.nodeID)){

            currentNode = parentNode;
            parentNode=currentNode.parentData; 
        }
        
        return currentNode;
    }
    
    private void buildSkipcounts () {
        for (NodeAttachment wantedLeaf : this.wantedActiveLeafs){
            
            NodeAttachment currentNode = wantedLeaf;           
            NodeAttachment parentNode = currentNode.parentData;
            
            //this node, and each of its parents, must do the following 
            //   check if self can be skipped over, i.e. if self's refcounts are like (N>=2, 0) or (0, N>=2)
            //if yes, inform parent of direction and cumulative skip count
            
            //climb up to the CCA node
            while (! currentNode.nodeID.equals(this.cbInstructionTree.ccaRoot.nodeID)){
                
                String currentNodeID = currentNode.nodeID;
                    
                boolean canSelfBeSkippedOver = false;
                
                if (!currentNode.isLeaf()){
                    canSelfBeSkippedOver=currentNode.ccaInformation.refCountLeft  ==ZERO && 
                                               currentNode.ccaInformation.refCountRight  >= ONE;
                    canSelfBeSkippedOver = canSelfBeSkippedOver ||  
                                       (currentNode.ccaInformation.  refCountRight ==ZERO && 
                                       currentNode.ccaInformation. refCountLeft  >= ONE);
                }
                
                Boolean amITheLeftChild = parentNode.leftChildNodeID!=null && parentNode.leftChildNodeID.equals(currentNodeID );
                
                if (canSelfBeSkippedOver) {

                    //Recall that , since I am skippable, at most one kid could have sent me a skip count
                    //So only 1 of my skip counts is non-zero 
                    int mySkipCount = currentNode.ccaInformation.skipCountRight+ currentNode.ccaInformation.skipCountLeft   ;

                    //now send the parent the cumulative skip count 
                    if (amITheLeftChild) {
                        parentNode.ccaInformation.skipCountLeft= ONE + mySkipCount;
                    }  else {
                        parentNode.ccaInformation.skipCountRight  = ONE + mySkipCount;
                    }

                } else {
                    //send 0 skip count to parent
                    if (amITheLeftChild) parentNode.ccaInformation.skipCountLeft=ZERO; else parentNode.ccaInformation.skipCountRight=ZERO;
                } 
                
                currentNode = parentNode;
                parentNode=currentNode.parentData;   
            }
            
        }
    } 
    
    //dump status 
    private void printState(NodeAttachment node) {
        
        if ( node.ccaInformation!=null){
            logger.debug( node.ccaInformation);
        }
        if (node.leftChildRef!=null && !node.leftChildRef.isLeaf()){
            printState( node.leftChildRef);
        }
        if (node.rightChildRef!=null && !node.rightChildRef.isLeaf()){
            printState( node.rightChildRef);
        }
    }
    
}
