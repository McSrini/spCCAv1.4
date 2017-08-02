/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cplex.*;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 */
public class MaxBranchingVarCountFinder implements  Function<ActiveSubtree, Integer>  {
 
    public Integer call(ActiveSubtree v1) throws Exception {
        return v1!=null? v1.getMaxBranchingVars(): ZERO;
    }
    
}
