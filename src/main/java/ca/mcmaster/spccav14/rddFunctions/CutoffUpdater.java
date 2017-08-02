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
public class CutoffUpdater implements  Function<ActiveSubtree, ActiveSubtree>  {
 
    private double cutoff;
    public CutoffUpdater (double cutoff){
        this.cutoff = cutoff;
    }
    public ActiveSubtree call(ActiveSubtree v1) throws Exception {
        if (v1.isFeasible() && v1.getObjectiveValue()<cutoff && !IS_MAXIMIZATION) {
            //do nothing
        } else         if (v1.isFeasible() && v1.getObjectiveValue()>cutoff &&  IS_MAXIMIZATION) {
            //do nothing 
        } else if (v1.isUnFeasible() || v1.isOptimal())  {
            //do nothing
        } else{
            v1.setCutoffValue( cutoff);
        }
        return v1;
    }
    
}
