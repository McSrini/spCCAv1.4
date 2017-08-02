/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import static ca.mcmaster.spccav14.Constants.*;
import static ca.mcmaster.spccav14.Constants.SOLUTION_CYCLE_TIME_MINUTES;
import ca.mcmaster.spccav14.cplex.ActiveSubtree;
import ca.mcmaster.spccav14.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spccav14.cplex.datatypes.SolutionVector;
import java.util.*;
import java.util.List;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

/**
 *
 * @author tamvadss
 */
public class SubtreeSolver implements  PairFlatMapFunction< Iterator<Tuple2<Integer,ActiveSubtree>>,Integer,SolutionVector> {
    
    private  List<String> pruneList = null;
    public SubtreeSolver ( List<String> pruneList) {
        this .    pruneList=   pruneList;
    }
    
    public Iterable<Tuple2<Integer, SolutionVector>> call(Iterator<Tuple2<Integer, ActiveSubtree>> iterator) throws Exception {
        Tuple2<Integer, SolutionVector> resultTuple = null;
        List <Tuple2<Integer, SolutionVector>> resultList = new ArrayList <Tuple2<Integer, SolutionVector>>();
        
        while (iterator.hasNext()) {
            Tuple2<Integer, ActiveSubtree> tuple = iterator.next();
            int partitionNumber = tuple._1;
            ActiveSubtree tree = tuple._2;
             
            tree.simpleSolve( SOLUTION_CYCLE_TIME_MINUTES ,  true,  false, partitionNumber == ZERO ? pruneList: null); 
            
            //ishnais
            //check if solved to completion, if feasible, and the solution value
            //we are not caring right now about the variable vector right now
            SolutionVector solutionVector = new SolutionVector();
            solutionVector.isAlreadySolvedToCompletion =  tree.isUnFeasible() || tree.isOptimal();
            solutionVector.isFeasibleOrOptimal = tree.isFeasible()|| tree.isOptimal();
            if ( solutionVector.isFeasibleOrOptimal) solutionVector.bestKnownSolution = tree.getObjectiveValue();
            
            resultTuple = new Tuple2<Integer, SolutionVector> (partitionNumber, solutionVector ) ;
        }
        
        resultList.add(resultTuple);
        return resultList;
    }
 
}
