/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cplex.ActiveSubtree;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 */
public class IncumbentFetcher implements  Function<ActiveSubtree, Double>  {
 
    public Double call(ActiveSubtree v1) throws Exception {
        return v1==null? DOUBLE_ZERO+(IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY): v1.getObjectiveValue();
    }
    
}
