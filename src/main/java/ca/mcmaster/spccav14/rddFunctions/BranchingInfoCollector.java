/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import ca.mcmaster.spccav14.cplex.ActiveSubtree;
import java.util.*;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 */
public class BranchingInfoCollector implements  Function<ActiveSubtree,   List<String>>  {
 
    public List<String> call(ActiveSubtree v1) throws Exception {
        return v1 ==null? new ArrayList<String> () : v1.getNodeCreationInfoList();
    }
    
}
