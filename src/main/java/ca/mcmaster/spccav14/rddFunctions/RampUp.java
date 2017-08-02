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
public class RampUp  implements  Function<ActiveSubtree,ActiveSubtree>  { 
 
    public ActiveSubtree call(ActiveSubtree tree) throws Exception {
        
        ActiveSubtree rampedUpTree = null;
        if  (tree!=null) {
            //ramp up
            tree.solve( RAMP_UP_TO_THIS_MANY_LEAFS, PLUS_INFINITY, MILLION, true, false)  ;
            rampedUpTree = tree;
        }
        
        return rampedUpTree;
    }
    
}
