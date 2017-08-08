/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cplex;

import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cb.CBInstructionGenerator;
import ca.mcmaster.spccav14.cb.CBInstructionTree;
import ca.mcmaster.spccav14.cb.ReincarnationMaps;
import ca.mcmaster.spccav14.cca.*;
import ca.mcmaster.spccav14.cplex.callbacks.*;
import ca.mcmaster.spccav14.cplex.datatypes.*;
import ca.mcmaster.spccav14.utils.*;
import static ca.mcmaster.spccav14.utils.BranchHandlerUtilities.*;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static ilog.cplex.IloCplex.Status.Feasible;
import static ilog.cplex.IloCplex.Status.Infeasible;
import static ilog.cplex.IloCplex.Status.Optimal;
import static ilog.cplex.IloCplex.Status.Unknown;
import static java.lang.System.exit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class ActiveSubtree {    
    
    private static Logger logger=Logger.getLogger(ActiveSubtree.class);
    
    private IloCplex cplex   ;
    private boolean isEnded = false;    
    private double lpRelaxValueAfterCCAMerge ;
     
    public final String guid =  UUID.randomUUID().toString();
    public String seedCCANodeID = MINUS_ONE_STRING; // this will change if this subtree ws created by importing a CCA , it is used for logging   

    //vars in the model
    private IloNumVar[]  modelVars;
        
    //    handlers for the CPLEX object
    private BranchHandler branchHandler;
    private RampUpNodeHandler rampUpNodeHandler;
    private LeafFetchingNodeHandler leafFetchNodeHandler;
    
    //our list of active leafs after each solve cycle
    private List<NodeAttachment> allActiveLeafs  ;     
    
    //use this object to run CCA algorithms
    private CCAFinder ccaFinder =new CCAFinder();
    
    private CBInstructionGenerator cbInstructionGenerator ;
    
    //this IloCplex object, if constructed by merging variable bounds, is differnt from the original MIP by these bounds
    //When extracting a CCA node from this Active Subtree , keep in mind that the CCA node branching instructions should be combined with these instructions
    public List<BranchingInstruction> instructionsFromOriginalMip = new ArrayList<BranchingInstruction>();
        
    //temporarily, I am introducing these two variables which are used for statistics
    public long numActiveLeafsAfterSimpleSolve=ZERO ;
    public long numActiveLeafsWithGoodLPAfterSimpleSolve=ZERO ;
    public double bestOFTheBestEstimates = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    public double lowestSumOFIntegerInfeasibilities = PLUS_INFINITY;
    
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtree.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging"+ ex);       
            exit(1);
        }
          
    }
    
    public ActiveSubtree (  ) throws Exception{
        
        this.cplex= new IloCplex();   
        cplex.importModel(MPS_FILE_ON_DISK);
        
        this.modelVars=CplexUtilities.getVariablesInModel(cplex);
        
        //create all the call back handlers
        //these are used depending on which method is invoked
        
        branchHandler = new BranchHandler(    );
        rampUpNodeHandler = new RampUpNodeHandler( RAMP_UP_TO_THIS_MANY_LEAFS   );                
        leafFetchNodeHandler = new LeafFetchingNodeHandler(); 
    
    }
        
    public void end(){
        if (!this.isEnded)     {
            this.cplex.end();
            isEnded=true;
        }
    }
    
    public void solve(long leafCountLimit, double cutoff, int timeLimitMinutes, boolean isRampUp, boolean setCutoff) throws IloException{
        
        logger.debug(" Solve begins at "+LocalDateTime.now()) ;
        
        //before solving , reset the CCA finder object which 
        //has an index built upon the current solution tree nodes
        this.ccaFinder.close();
        
        //set callbacks for regular solution
        cplex.clearCallbacks();
        this.cplex.use(branchHandler);
        
        if (isRampUp) this.cplex.use(rampUpNodeHandler) ;
       
        if (setCutoff) setCutoffValue(  cutoff);
        setParams (  timeLimitMinutes,isRampUp );
                
        cplex.solve();
        
        //solve complete - now get the active leafs
        this.cplex.use(branchHandler);
        this.cplex.use(leafFetchNodeHandler);  
        cplex.solve();
        allActiveLeafs = leafFetchNodeHandler.allLeafs;
        
        //initialize the CCA finder
        ccaFinder .initialize(allActiveLeafs);
        
        logger.debug(" Solve concludes at "+LocalDateTime.now()) ;
        
    }
    
    public void simpleSolve(double timeLimitMinutes, boolean useEmptyCallback, boolean useInMemory, List<String> pruneList) throws IloException{
        logger.debug("simpleSolve Started at "+LocalDateTime.now()) ;
        cplex.clearCallbacks();
        if (useEmptyCallback) {
            this.cplex.use(new PruneBranchHandler( pruneList));
            //this.cplex.use(new PruneNodeHandler( pruneList));
        }  
        setParams (  timeLimitMinutes, useInMemory);
        cplex.solve();
        
        //get leafs  
        LeafCountingNodeHandler lcnh = new LeafCountingNodeHandler(MIP_WELLKNOWN_SOLUTION);
        this.cplex.use(lcnh);  
        cplex.solve();
        //this.allActiveLeafs= lcnh.allLeafs;
        
        numActiveLeafsAfterSimpleSolve =lcnh.numLeafs;
        numActiveLeafsWithGoodLPAfterSimpleSolve =lcnh.numLeafsWithGoodLP;
        this.bestOFTheBestEstimates = lcnh.bestOFTheBestEstimates;
        this.lowestSumOFIntegerInfeasibilities = lcnh.lowestSumOFIntegerInfeasibilities;
                        
        logger.debug("simpleSolve completed at "+LocalDateTime.now()) ;
    }  
    
    //this method is used when reincarnating a tree in a controlled fashion
    //similar to solve(), but we use controlled branching instead of CPLEX default branching
    public    void reincarnate ( Map<String, CCANode> instructionTreeAsMap, String ccaRootNodeID, double cutoff, boolean setCutoff) throws IloException{
        
        logger.debug("Reincarnating tree with cca root node id "+ ccaRootNodeID);
        
        //reset CCA finder
        this.ccaFinder.close();
        
        //set callbacks 
        ReincarnationMaps reincarnationMaps=createReincarnationMaps(instructionTreeAsMap,ccaRootNodeID);
        this.cplex.use( new ReincarnationBranchHandler(instructionTreeAsMap,  reincarnationMaps, this.modelVars));
        this.cplex.use( new ReincarnationNodeHandler(reincarnationMaps));      
        
        if (setCutoff) setCutoffValue(  cutoff);
        setParams (  -ONE, true);//no time limit
         
        cplex.solve();
        
        //solve complete - now get the active leafs
        //restore regular branch handler
        this.cplex.use(branchHandler);
        this.cplex.use(leafFetchNodeHandler);  
        cplex.solve();
        allActiveLeafs = leafFetchNodeHandler.allLeafs;
        
        //initialize the CCA finder
        ccaFinder .initialize(allActiveLeafs);
    }
       
    public void setCutoffValue(double cutoff) throws IloException {
        if (!IS_MAXIMIZATION) {
            cplex.setParam(    IloCplex.Param.MIP.Tolerances.UpperCutoff, cutoff);
        }else {
            cplex.setParam(    IloCplex.Param.MIP.Tolerances.LowerCutoff, cutoff);
        }
    }
    
    public void setParams (double timeLimitMinutes, boolean inMemory) throws IloException {
        if (inMemory) cplex.setParam(IloCplex.Param.MIP.Strategy.File, ZERO); 
        if (timeLimitMinutes>ZERO) cplex.setParam(IloCplex.Param.TimeLimit, timeLimitMinutes*SIXTY); 
        
    }
    
    //a temporary measure for checking ramp ups identical
    public List<String> getNodeCreationInfoList (){
        return this.branchHandler.nodeCreationInfoList;
    }
    public int getMaxBranchingVars () {
        return this.branchHandler.maxBranchingVars;
    }
    
    public List<NodeAttachment> getActiveLeafList() throws IloException {
        return allActiveLeafs ==null? null: Collections.unmodifiableList(allActiveLeafs) ;
    }
    
    //use this method to split the ramped-up tree into roughly equal partitions
    public List<CCANode> getCandidateCCANodesPostRampup (int numPartitions)   {
        return ccaFinder.  getCandidateCCANodesPostRampup ( numPartitions);        
    } 
    
    public double getObjectiveValue() throws IloException {
        return this.cplex.getObjValue();
    }
    
    public boolean isFeasible () throws IloException {
        return this.cplex.getStatus().equals(Feasible);
    }
    
    public boolean isUnFeasible () throws IloException {
        return this.cplex.getStatus().equals(Infeasible);
    }
        
    public boolean isUnknown () throws IloException {
        return this.cplex.getStatus().equals(Unknown);
    }
        
    public boolean isOptimal () throws IloException {
        return this.cplex.getStatus().equals(Optimal);
    }
    
    public String getStatus () throws IloException {
        return this.cplex.getStatus().toString();
    }
    
    public List<CCANode> getActiveLeafsAsCCANodes (List<String> wantedLeafNodeIDs) {
        List <CCANode> ccaNodeList = new ArrayList <CCANode> ();
        
        for (NodeAttachment leaf : this.allActiveLeafs) {
            if (wantedLeafNodeIDs!=null && !wantedLeafNodeIDs.contains( leaf.nodeID))continue ;
            CCANode ccaNode = new CCANode();   
            leaf.ccaInformation=ccaNode;
            leaf.ccaInformation.nodeID= leaf.nodeID;
            getBranchingInstructionForCCANode( leaf);
                        
            getCCANodeLPRelaxValue(leaf);
            
            //populate CCA node with best-estimate and sum-of-infeasibilities
            //this is not done non-leaf regular CCA nodes
            ccaNode.sumOfIntegerInfeasibilities = leaf.sumOfIntegerInfeasibilities;
            ccaNode.bestEstimateValue= leaf.bestEstimateValue;
            logger.debug(" cca node properties for round robin LP BE SI "+ ccaNode.lpRelaxationValue + ", "+ 
                         ccaNode.bestEstimateValue + ", "+ccaNode.sumOfIntegerInfeasibilities );
            
            ccaNodeList.add( ccaNode);
        }
        
        return ccaNodeList;
    }
    
    public static void getCCANodeLPRelaxValue (NodeAttachment node ) {
        node.ccaInformation.lpRelaxationValue=  node.estimatedLPRelaxationValue;
    }
    
    //climb up all the way to root
    public static void getBranchingInstructionForCCANode (NodeAttachment node ){
        
        NodeAttachment thisNode = node;
        NodeAttachment parent = node.parentData;
        while (parent !=null){
            
            if (parent.rightChildRef!=null && parent.rightChildNodeID.equals( thisNode.nodeID)) {
                //must be right child
                 
                node.ccaInformation.branchingInstructionList.add( parent.getBranchingInstructionForRightChild()) ;
            } else {
                //must be the left child
                 
                node.ccaInformation.branchingInstructionList.add(parent.getBranchingInstructionForLeftChild()) ;
            }
            
            thisNode = parent;
            parent = parent.parentData;
        }
    }
        
    //create sub problem by changing var bounds
    public void mergeVarBounds (CCANode ccaNode, List<BranchingInstruction> instructionsFromOriginalMip ) throws IloException {
        List<BranchingInstruction> cumulativeInstructions = new ArrayList<BranchingInstruction>();
        cumulativeInstructions.addAll(ccaNode.branchingInstructionList);
        cumulativeInstructions.addAll(instructionsFromOriginalMip);
        
        this.instructionsFromOriginalMip =cumulativeInstructions;
        this.lpRelaxValueAfterCCAMerge= ccaNode.lpRelaxationValue;
        this.seedCCANodeID = ccaNode.nodeID;
         
        //merge var bounds
        Map< String, Double >   lowerBounds= getLowerBounds(cumulativeInstructions, ccaNode.nodeID);
        Map< String, Double >   upperBounds= getUpperBounds(cumulativeInstructions, ccaNode.nodeID);
        CplexUtilities.merge(cplex, lowerBounds, upperBounds);
    }
    
        
    public double getBestRemaining_LPValue() throws IloException{
        return this.cplex.getBestObjValue();
        //return this.allActiveLeafs==null? this.lpRelaxValueAfterCCAMerge: this.branchHandler.bestReamining_LPValue;
    }
    
    //if wanted leafs are not specified, every migratable leaf under this CCA is assumed to be wanted
    public CBInstructionTree getCBInstructionTree (CCANode ccaNode ) {
        List<String> wantedLeafs = new ArrayList<String> ();
        for (NodeAttachment node :  this.allActiveLeafs){
            if (ccaNode.pruneList.contains(node.nodeID) && node.isMigrateable) wantedLeafs.add(node.nodeID);
        }
        cbInstructionGenerator = new CBInstructionGenerator( ccaNode,     allActiveLeafs,   wantedLeafs) ;
        return cbInstructionGenerator.generateInstructions( );
    }
        
    public CBInstructionTree getCBInstructionTree (CCANode ccaNode, List<String> wantedLeafs) {
        
        cbInstructionGenerator = new CBInstructionGenerator( ccaNode,     allActiveLeafs,   wantedLeafs) ;
        return cbInstructionGenerator.generateInstructions( );
    }
        
    private  ReincarnationMaps createReincarnationMaps (Map<String, CCANode> instructionTreeAsMap, String ccaRootNodeID){
        ReincarnationMaps   maps = new ReincarnationMaps ();
                
        for (String key : instructionTreeAsMap.keySet()){
            if (instructionTreeAsMap.get(key).leftChildNodeID!=null){
                //  this  needs to be branched upon , using the branching instructions in the CCA node
                maps.oldToNew_NodeId_Map.put( key,null  );
            }
        }
        
        //both maps can start with original MIP which is always node ID -1
        //but right now we are starting from the root CCA
        maps.oldToNew_NodeId_Map.put( ccaRootNodeID ,MINUS_ONE_STRING  );
        maps.newToOld_NodeId_Map.put( MINUS_ONE_STRING,ccaRootNodeID  );
        
        return maps;
    }
    
}
