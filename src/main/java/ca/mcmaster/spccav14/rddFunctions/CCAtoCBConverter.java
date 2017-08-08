/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import ca.mcmaster.spccav14.cb.CBInstructionTree;
import ca.mcmaster.spccav14.cca.CCANode;
import ca.mcmaster.spccav14.cplex.ActiveSubtree;
import java.util.*;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 */
public class CCAtoCBConverter implements  Function<ActiveSubtree, List<CBInstructionTree>>  {
    
    private List<CCANode> acceptedCCANodes;
    public CCAtoCBConverter (List<CCANode> acceptedCCANodes ){
       this.acceptedCCANodes=acceptedCCANodes;
    }
 
    public List<CBInstructionTree> call(ActiveSubtree v1) throws Exception {
        List<CBInstructionTree> result = new ArrayList<CBInstructionTree>();
        if (v1!=null){
            //for each accepted CCA node, convert it into a CB instruction tree
            for (CCANode acceptedCCANode: acceptedCCANodes){
                result.add(v1.getCBInstructionTree(acceptedCCANode)) ;
            }
            
        }
        
        return result;
    }
    
    
}
