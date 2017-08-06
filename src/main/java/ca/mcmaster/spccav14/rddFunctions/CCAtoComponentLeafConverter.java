/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import ca.mcmaster.spccav14.cca.*;
import ca.mcmaster.spccav14.cplex.*;
import java.util.*;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 * 
 * find the CCA node list corresponding to each accepted CCA node
 * note that for a given CCA node, its leafs are returned as CCA nodes
 * 
 */
public class CCAtoComponentLeafConverter implements  Function<ActiveSubtree, List<List<CCANode>>>  {
    private List<CCANode> acceptedNodes;
    
    List<List<CCANode>> result = new ArrayList<List<CCANode>>();
    
    public CCAtoComponentLeafConverter (List<CCANode> acceptedNodes){
        this. acceptedNodes =   acceptedNodes;
    }
 
    public List<List<CCANode>> call(ActiveSubtree v1) throws Exception {
        if (v1 != null) {
            for (CCANode acceptedNode: acceptedNodes) {
                List<CCANode> leafList = v1.getActiveLeafsAsCCANodes( acceptedNode.pruneList);
                result.add(leafList);
            }
           
        }
        
        return result;
    }
    
    
}
