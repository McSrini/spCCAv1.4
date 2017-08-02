/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cplex;

import static ca.mcmaster.spccav14.Constants.*;
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
    
}