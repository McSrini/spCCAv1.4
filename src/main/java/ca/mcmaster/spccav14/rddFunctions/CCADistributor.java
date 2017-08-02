/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cca.CCANode;
import ca.mcmaster.spccav14.cplex.ActiveSubtree;
import ca.mcmaster.spccav14.cplex.datatypes.*; 
import java.util.*;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

/**
 *
 * @author tamvadss
 */
public class CCADistributor implements  PairFlatMapFunction< Iterator<Tuple2<Integer,ActiveSubtree>>,Integer,ActiveSubtree> {
    
    private List<CCANode> acceptedCCANodes = null;
    private double cutoff = ZERO;
    
    public CCADistributor (List<CCANode> acceptedCCANodes ,double cutoff){
        this.cutoff= cutoff;
        this.acceptedCCANodes=acceptedCCANodes;
    }
 
    public Iterable<Tuple2<Integer, ActiveSubtree>> call(Iterator<Tuple2<Integer, ActiveSubtree>> iterator) throws Exception {
        
        Tuple2<Integer, ActiveSubtree> result = null;
        ActiveSubtree newTree = null;
        
        while (iterator.hasNext()) {
            Tuple2<Integer, ActiveSubtree> tuple = iterator.next();
            int partitionNumber = tuple._1;
            ActiveSubtree tree = tuple._2;
            //if null tree then create , else just return existing
            if (null==tree){
                newTree = new ActiveSubtree() ;
                newTree.mergeVarBounds(acceptedCCANodes.get(partitionNumber-ONE),  new ArrayList<BranchingInstruction>()  );
                newTree. setCutoffValue(cutoff );
            }else {
                newTree = tree;
            }
            
            result = new Tuple2<Integer, ActiveSubtree> (partitionNumber, newTree) ;
        }
        
        List <Tuple2<Integer, ActiveSubtree>> tupleList = new ArrayList <Tuple2<Integer, ActiveSubtree>>();
        tupleList.add(result);
        return tupleList;
    }
    
}
