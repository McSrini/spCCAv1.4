/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import static ca.mcmaster.spccav14.Constants.*;
import static ca.mcmaster.spccav14.Constants.SOLUTION_CYCLE_TIME_MINUTES;
import ca.mcmaster.spccav14.cplex.*;
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
public class SubtreeCollectionSolver implements  PairFlatMapFunction< Iterator<Tuple2<Integer,ActiveSubtreeCollection>>,Integer,SolutionVector> {
    
    private  List<String> pruneList = null;
    private NodeSelectionStartegyEnum nodeSelectionStrategy ;
    
    public SubtreeCollectionSolver ( List<String> pruneList, NodeSelectionStartegyEnum nodeSelectionStrategy ) {
        this .    pruneList=   pruneList;
        this. nodeSelectionStrategy=  nodeSelectionStrategy;
         
    }
    
    public Iterable<Tuple2<Integer, SolutionVector>> call(Iterator<Tuple2<Integer, ActiveSubtreeCollection>> iterator) throws Exception {
        Tuple2<Integer, SolutionVector> resultTuple = null;
        List <Tuple2<Integer, SolutionVector>> resultList = new ArrayList <Tuple2<Integer, SolutionVector>>();
        
        while (iterator.hasNext()) {
            Tuple2<Integer, ActiveSubtreeCollection> tuple = iterator.next();
            int partitionNumber = tuple._1;
            ActiveSubtreeCollection astc = tuple._2;
             
            astc.solve( true, SOLUTION_CYCLE_TIME_MINUTES  ,     
                                true,    TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE,  nodeSelectionStrategy , partitionNumber ==ZERO?  pruneList : null);
            
           
                    
            //ishnais
            //check if solved to completion, if feasible, and the solution value
            //we are not caring right now about the variable vector right now
            SolutionVector solutionVector = new SolutionVector();
            solutionVector.isAlreadySolvedToCompletion =  astc.isAlreadySolvedToCompletion();
            solutionVector.isFeasibleOrOptimal =  astc.getIncumbentValue() < PLUS_INFINITY &&  astc.getIncumbentValue() > MINUS_INFINITY ;
            if ( solutionVector.isFeasibleOrOptimal) solutionVector.bestKnownSolution = astc.getIncumbentValue();
            
            resultTuple = new Tuple2<Integer, SolutionVector> (partitionNumber, solutionVector ) ;
        }
        
        resultList.add(resultTuple);
        return resultList;
    }
 
}
