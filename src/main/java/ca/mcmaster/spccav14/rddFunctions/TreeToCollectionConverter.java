/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import static ca.mcmaster.spccav14.Constants.ONE;
import static ca.mcmaster.spccav14.Constants.ZERO;
import ca.mcmaster.spccav14.cca.CCANode;
import ca.mcmaster.spccav14.cplex.ActiveSubtree;
import ca.mcmaster.spccav14.cplex.ActiveSubtreeCollection;
import ca.mcmaster.spccav14.cplex.datatypes.BranchingInstruction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

/**
 *
 * @author tamvadss
 */
public class TreeToCollectionConverter implements  PairFlatMapFunction< Iterator<Tuple2<Integer,ActiveSubtree >>,Integer,ActiveSubtreeCollection> {

    private double cutoff=ZERO;
    private List<List<CCANode>> leafList =null;
    
    public TreeToCollectionConverter (double cutoff, List<List<CCANode>> leafList ){
        this.cutoff = cutoff;
        this.    leafList=leafList;    
    }
 
    public Iterable<Tuple2<Integer, ActiveSubtreeCollection>> call(Iterator<Tuple2<Integer, ActiveSubtree>> iterator) throws Exception {
        
        List <Tuple2<Integer, ActiveSubtreeCollection>> result= new ArrayList <Tuple2<Integer, ActiveSubtreeCollection>>();
        
        while (iterator.hasNext()) {
            Tuple2<Integer, ActiveSubtree> tuple = iterator.next();
            int partitionNumber = tuple._1;
            ActiveSubtree tree = tuple._2;
            
            if (tree!=null) {
                //convert tree into collection
                ActiveSubtreeCollection astc = new ActiveSubtreeCollection (tree, null, new ArrayList<BranchingInstruction> (), ONE, false, partitionNumber);
             
                Tuple2<Integer, ActiveSubtreeCollection> outputTuple = new Tuple2<Integer, ActiveSubtreeCollection> (partitionNumber,astc ) ;
                result.add(outputTuple);
            }else {
                //create a collection out of leafs
                ActiveSubtreeCollection astc = new ActiveSubtreeCollection (null, leafList.get(partitionNumber-ONE) , new ArrayList<BranchingInstruction> (), cutoff, true, partitionNumber);
                Tuple2<Integer, ActiveSubtreeCollection> outputTuple = new Tuple2<Integer, ActiveSubtreeCollection> (partitionNumber,astc ) ;
                result.add(outputTuple);
            }
        }
        
        return result;
    }
 
     
    
}
