/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import static ca.mcmaster.spccav14.Constants.ONE;
import static ca.mcmaster.spccav14.Constants.*;
import static ca.mcmaster.spccav14.Constants.ZERO;
import ca.mcmaster.spccav14.cb.CBInstructionTree;
import ca.mcmaster.spccav14.cca.CCANode;
import ca.mcmaster.spccav14.cplex.ActiveSubtree;
import ca.mcmaster.spccav14.cplex.datatypes.BranchingInstruction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

/**
 *
 * @author tamvadss
 */
public class CBDistributor implements  PairFlatMapFunction< Iterator<Tuple2<Integer,ActiveSubtree>>,Integer,ActiveSubtree> {
    
    private List<CBInstructionTree> cbTreeList = null;
    private   List<CCANode> acceptedCCANodes =null;
    private double cutoff = ZERO;
    
    public CBDistributor (List<CBInstructionTree> cbTreeList , List<CCANode> acceptedCCANodes , double cutoff){
        this.cutoff= cutoff;
        this.cbTreeList=cbTreeList;
        this. acceptedCCANodes =acceptedCCANodes;
    }
 
    public Iterable<Tuple2<Integer, ActiveSubtree>> call(Iterator<Tuple2<Integer, ActiveSubtree>> iterator) throws Exception {
                
        Tuple2<Integer, ActiveSubtree> result = null;
        ActiveSubtree newTree = null;
        
        while (iterator.hasNext()) {
            Tuple2<Integer, ActiveSubtree> tuple = iterator.next();
            int partitionNumber = tuple._1;
            ActiveSubtree tree = tuple._2;
            //if null tree then create and do CB, else just return existing
            if (null==tree){
                newTree = new ActiveSubtree() ;
                newTree.mergeVarBounds(acceptedCCANodes.get(partitionNumber-ONE),  new ArrayList<BranchingInstruction>()  );
                newTree. setCutoffValue(cutoff );
                newTree.reincarnate( cbTreeList.get(partitionNumber-ONE).asMap() ,acceptedCCANodes.get(partitionNumber-ONE).nodeID  , 
                                PLUS_INFINITY , false);
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
