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
public class CutoffUpdaterForCollections implements  Function<ActiveSubtreeCollection, ActiveSubtreeCollection>  {
 
    private double cutoff;
    public CutoffUpdaterForCollections (double cutoff){
        this.cutoff = cutoff;
    }
    public ActiveSubtreeCollection call(ActiveSubtreeCollection v1) throws Exception {
        v1.setCutoff( cutoff);
       
        return v1;
    }
    
}
