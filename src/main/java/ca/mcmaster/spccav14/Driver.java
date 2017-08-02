/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14;

import ca.mcmaster.spccav14.rddFunctions.BooleanToTreeConverter;
import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cca.CCANode;
import ca.mcmaster.spccav14.cplex.*;
import ca.mcmaster.spccav14.cplex.datatypes.NodeAttachment;
import ca.mcmaster.spccav14.cplex.datatypes.SolutionVector;
import ca.mcmaster.spccav14.rddFunctions.*;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.*;
import org.apache.spark.HashPartitioner;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.*;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

/**
 *
 * @author tamvadss
 */
public class Driver {
     
    private static Logger logger=Logger.getLogger(Driver.class);    
    
    public static void main(String[] args) throws Exception   {

        logger.setLevel(Level.DEBUG);
        PatternLayout layout =new PatternLayout("%5p  %d  %F  %L  %m%n");        
        logger.addAppender(new FileAppender(layout, DRIVER_LOG_FILE));
                
        //Driver for distributing the CPLEX  BnB solver on Spark
        SparkConf conf = new SparkConf().setAppName("SparcPlex CCA V1.4");
        JavaSparkContext sc = new JavaSparkContext(conf);
        
        //CCA ramp up is done on partition 0, CB on partition 1 , and so on
        List<Tuple2<Integer, Boolean>> initialListCCA = new ArrayList<Tuple2<Integer, Boolean>> () ;
        for (int index = ZERO; index < NUM_PARTITIONS; index ++) {
            initialListCCA.add( new Tuple2<Integer, Boolean> (index, index == ZERO));
        }
        List<Tuple2<Integer, Boolean>> initialListCB = new ArrayList<Tuple2<Integer, Boolean>> () ;
        for (int index = ZERO; index < NUM_PARTITIONS; index ++) {
            initialListCB.add( new Tuple2<Integer, Boolean> (index, index == ONE));
        }
        List<Tuple2<Integer, Boolean>> initialListLSI = new ArrayList<Tuple2<Integer, Boolean>> () ;
        for (int index = ZERO; index < NUM_PARTITIONS; index ++) {
            initialListLSI.add( new Tuple2<Integer, Boolean> (index, index == TWO));
        }
        List<Tuple2<Integer, Boolean>> initialListSBF = new ArrayList<Tuple2<Integer, Boolean>> () ;
        for (int index = ZERO; index < NUM_PARTITIONS; index ++) {
            initialListSBF.add( new Tuple2<Integer, Boolean> (index, index == THREE));
        }
        List<Tuple2<Integer, Boolean>> initialListBEF = new ArrayList<Tuple2<Integer, Boolean>> () ;
        for (int index = ZERO; index < NUM_PARTITIONS; index ++) {
            initialListBEF.add( new Tuple2<Integer, Boolean> (index, index == FOUR));
        }
        
         //create the active subtrees which will be ramped up
         //Note the per partition operation
        JavaPairRDD < Integer, ActiveSubtree > frontierCCA ; 
        frontierCCA = JavaPairRDD. fromJavaRDD(sc.parallelize(initialListCCA) )                 
                /*realize the movement to desired partition*/
                .partitionBy(new HashPartitioner( NUM_PARTITIONS)) 
                /* finally convert the attachment list into an ActiveSubTreeCollection object*/
                .mapValues(new BooleanToTreeConverter()) 
                //Frontier is used many times, so cache it.
                .cache();
        
        JavaPairRDD < Integer, ActiveSubtree > frontierCB = JavaPairRDD. fromJavaRDD(sc.parallelize(initialListCB) )                 
                /*realize the movement to desired partition*/
                .partitionBy(new HashPartitioner( NUM_PARTITIONS)) 
                /* finally convert the attachment list into an ActiveSubTreeCollection object*/
                .mapValues(new BooleanToTreeConverter()) 
                //Frontier is used many times, so cache it.
                .cache(); 
       
        JavaPairRDD < Integer, ActiveSubtree > frontierLSI = JavaPairRDD. fromJavaRDD(sc.parallelize(initialListLSI) )                 
                /*realize the movement to desired partition*/
                .partitionBy(new HashPartitioner( NUM_PARTITIONS)) 
                /* finally convert the attachment list into an ActiveSubTreeCollection object*/
                .mapValues(new BooleanToTreeConverter()) 
                //Frontier is used many times, so cache it.
                .cache();
        
        JavaPairRDD < Integer, ActiveSubtree > frontierSBF= JavaPairRDD. fromJavaRDD(sc.parallelize(initialListSBF) )                 
                /*realize the movement to desired partition*/
                .partitionBy(new HashPartitioner( NUM_PARTITIONS)) 
                /* finally convert the attachment list into an ActiveSubTreeCollection object*/
                .mapValues(new BooleanToTreeConverter()) 
                //Frontier is used many times, so cache it.
                .cache();
        
        JavaPairRDD < Integer, ActiveSubtree > frontierBEF = JavaPairRDD. fromJavaRDD(sc.parallelize(initialListBEF) )                 
                /*realize the movement to desired partition*/
                .partitionBy(new HashPartitioner( NUM_PARTITIONS)) 
                /* finally convert the attachment list into an ActiveSubTreeCollection object*/
                .mapValues(new BooleanToTreeConverter()) 
                //Frontier is used many times, so cache it.
                .cache();
        
        
        //now ramp up the 5 active subtrees
        frontierCCA = frontierCCA .mapValues(new  RampUp()) 
                //Frontier is used many times, so cache it.
                .cache();
        frontierCB= frontierCB .mapValues(new  RampUp()) 
                //Frontier is used many times, so cache it.
                .cache();
        frontierLSI= frontierLSI .mapValues(new  RampUp()) 
                //Frontier is used many times, so cache it.
                .cache();
        frontierSBF= frontierSBF .mapValues(new  RampUp()) 
                //Frontier is used many times, so cache it.
                .cache();
        frontierBEF= frontierBEF .mapValues(new  RampUp()) 
                //Frontier is used many times, so cache it.
                .cache();
                
        //now verify that all ramp ups are identical
        //1) verify only 1 var was used for every branch
        //2) verify all the branches were identical by comparing the branching info list
        //3) check active leafs in every tree are identical
       
        int maxVarCCA = Collections.max(frontierCCA. mapValues(new   MaxBranchingVarCountFinder()) .values().collect());
        int maxVarCB =  Collections.max(frontierCB. mapValues(new   MaxBranchingVarCountFinder()) .values().collect());
        int maxVarLSI = Collections.max(frontierLSI. mapValues(new   MaxBranchingVarCountFinder()) .values().collect());
        int maxVarSBF = Collections.max(frontierSBF. mapValues(new   MaxBranchingVarCountFinder()) .values().collect());
        int maxVarBEF = Collections.max(frontierBEF. mapValues(new   MaxBranchingVarCountFinder()) .values().collect());
        if (maxVarCCA!=ONE ||maxVarCB!=ONE || maxVarLSI !=ONE|| maxVarSBF!=ONE || maxVarBEF !=ONE ) {
            logger.error("more than one branching var may have been used for some ramp ups, exit");
            exit(ONE);
        }
         
        
        List<String> ccaBranches = frontierCCA. mapValues(new    BranchingInfoCollector()) .values().reduce(new ListSizeComparer());
        List<String> cbBranches  = frontierCB.  mapValues(new    BranchingInfoCollector()) .values().reduce(new ListSizeComparer());
        List<String> sbfBranches  = frontierSBF.  mapValues(new    BranchingInfoCollector()) .values().reduce(new ListSizeComparer());
        List<String> lsiBranches =  frontierLSI.  mapValues(new    BranchingInfoCollector()) .values().reduce(new ListSizeComparer());
        List<String> befBranches  = frontierBEF.  mapValues(new    BranchingInfoCollector()) .values().reduce(new ListSizeComparer());
        
        if (ccaBranches.size()!=cbBranches.size()){
            logger.error ("ramp up not identical - branching conditions vary");
            exit(ONE);
        }
        if (ccaBranches.size()!=lsiBranches.size()){
            logger.error ("ramp up not identical - branching conditions vary");
            exit(ONE);
        }
        if (ccaBranches.size()!=sbfBranches.size()){
            logger.error ("ramp up not identical - branching conditions vary");
            exit(ONE);
        }
        if (ccaBranches.size()!=befBranches.size()){
            logger.error ("ramp up not identical - branching conditions vary");
            exit(ONE);
        }
        for (int index = ZERO; index < ccaBranches.size(); index ++){
            if (! ccaBranches.get(index).equals(cbBranches.get(index) )) {
                logger.error ("ramp up not identical - branching conditions vary");
                exit(ONE);
            }
            if (! ccaBranches.get(index).equals(sbfBranches.get(index) )) {
                logger.error ("ramp up not identical - branching conditions vary");
                exit(ONE);
            }
            if (! ccaBranches.get(index).equals(lsiBranches.get(index) )) {
                logger.error ("ramp up not identical - branching conditions vary");
                exit(ONE);
            }
            if (! ccaBranches.get(index).equals(befBranches.get(index) )) {
                logger.error ("ramp up not identical - branching conditions vary");
                exit(ONE);
            }
        }
        
        
        List<NodeAttachment> ccaLeafs = frontierCCA. mapValues(new    ActiveLeafListFetcher()) .values().reduce(new ListSizeComparer2());
        List<NodeAttachment> cbLeafs  = frontierCB.  mapValues(new    ActiveLeafListFetcher()) .values().reduce(new ListSizeComparer2());
        List<NodeAttachment> sbfLeafs  = frontierSBF.  mapValues(new    ActiveLeafListFetcher()) .values().reduce(new ListSizeComparer2());
        List<NodeAttachment> lsiLeafs =  frontierLSI.  mapValues(new    ActiveLeafListFetcher()) .values().reduce(new ListSizeComparer2());
        List<NodeAttachment> befLeafs  = frontierBEF.  mapValues(new    ActiveLeafListFetcher()) .values().reduce(new ListSizeComparer2());
                
        if (ccaLeafs.size()!=sbfLeafs.size()){
            logger.error ("ramp up not identical - active leaf counts vary");
            exit(ONE);
        }
        if (ccaLeafs.size()!=befLeafs.size()){
            logger.error ("ramp up not identical - active leaf counts vary");
            exit(ONE);
        }
        if (ccaLeafs.size()!=lsiLeafs.size()){
            logger.error ("ramp up not identical - active leaf counts vary");
            exit(ONE);
        }
        if (ccaLeafs.size()!=cbLeafs.size()){
            logger.error ("ramp up not identical - active leaf counts vary");
            exit(ONE);
        }
        for (int index = ZERO; index < ccaLeafs.size(); index ++){
            if (! sbfLeafs.get(index).nodeID.equals(ccaLeafs.get(index).nodeID )){
                logger.error ("ramp up not identical - active leaf ids vary");
                exit(ONE);
            }
            if (! befLeafs.get(index).nodeID.equals(ccaLeafs.get(index).nodeID )){
                logger.error ("ramp up not identical - active leaf ids vary");
                exit(ONE);
            }
            if (! lsiLeafs.get(index).nodeID.equals(ccaLeafs.get(index).nodeID )){
                logger.error ("ramp up not identical - active leaf ids vary");
                exit(ONE);
            }
            if (! cbLeafs.get(index).nodeID.equals(ccaLeafs.get(index).nodeID )){
                logger.error ("ramp up not identical - active leaf ids vary");
                exit(ONE);
            }
        }
        
        
        logger.info("Ramp ups are identical, can proceed");
        
        //
        //get CCA condidates
        List<CCANode> candidateCCANodes = frontierCCA.mapValues( new CCACandidatesGenerator()).values().reduce( new ListSizeComparer3());
        //accept good candidates
        int NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION=ZERO;
        List<CCANode> acceptedCCANodes = new ArrayList<CCANode> ();
        //each accepted CCA node results in some leafs being pruned from the source tree
        List<String> pruneList = new ArrayList<String>();
        for (CCANode ccaNode: candidateCCANodes){

            if (NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION >=NUM_PARTITIONS-ONE )  break; 
            
            if (ccaNode.getPackingFactor() < TWO && ccaNode.pruneList.size() > EXPECTED_LEAFS_PER_PARTITION/TWO ) {
                logger.debug (""+ccaNode.nodeID + " has good packing factor " +ccaNode.getPackingFactor() + 
                        " and prune list size " + ccaNode.pruneList.size() + " depth from root "+ ccaNode.depthOfCCANodeBelowRoot) ; 
                NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION ++;
                //          qxxy               dod       
                acceptedCCANodes.add(ccaNode);
                pruneList .addAll( ccaNode.pruneList);
            }
        }
        if (acceptedCCANodes.size() < NUM_PARTITIONS-ONE) {
            logger.error("this splitToCCAPostRampup partitioning cannot be done  , try ramping up to  a larger number of leafs ");
            exit(ZERO);
        }
        
        logger.info ( "leafs left on home partition is " + (RAMP_UP_TO_THIS_MANY_LEAFS-  pruneList.size()));
        //get the incumbent value after ramp up
        double incumbentValueAfterRampup = IS_MAXIMIZATION? Collections.max(frontierCCA.mapValues( new  IncumbentFetcher()).values().collect()) :
                                                            Collections.min(frontierCCA.mapValues( new  IncumbentFetcher()).values().collect());
        
        //assign one candidate CCA node to each partition other than home 
        frontierCCA = frontierCCA.mapPartitionsToPair(new CCADistributor (  acceptedCCANodes ,incumbentValueAfterRampup),  true);
        frontierCCA.cache() ;
        
        //now solve every partition to completion, exchanging cutoffs every 5 minutes.  
        int iterationCount = ZERO;
        boolean solvedToCompletion = true;
        double incumbent  = incumbentValueAfterRampup;
        //kkkisuuyjkhj pnpk  55595666614226 
        do {
            iterationCount++;
            
            logger.info("Starting solution iteration "+ iterationCount);
            List<SolutionVector> solutionList = frontierCCA.mapPartitionsToPair(new  SubtreeSolver (  pruneList ),  true).values().collect();
            
            solvedToCompletion = true;
            for (SolutionVector soln : solutionList) {
                if (!soln.isAlreadySolvedToCompletion) {
                    solvedToCompletion=false;
                    break;
                }
            }
            
            //find the best solution found, update the incumbent, and use it to update the cutoffs
            boolean cutoffNeedsUpdate = false;
            for (SolutionVector soln : solutionList) {
                if   ( soln.isFeasibleOrOptimal)  {
                    if (  (!IS_MAXIMIZATION  && incumbent> soln.bestKnownSolution)  || (IS_MAXIMIZATION && incumbent <  soln.bestKnownSolution) ) {     
                        //bestKnownSolution =              tree.getSolutionVector();
                        incumbent= soln.bestKnownSolution;    
                        cutoffNeedsUpdate= true;
                    }
                }
            }
            
            if (cutoffNeedsUpdate && !solvedToCompletion){
                frontierCCA.mapValues( new CutoffUpdater(incumbent));
            }
            
        }while (!solvedToCompletion);
        
        logger.info(" CCA test completed in iterations = "+iterationCount);
        
    }//end main
    
}//end class driver
