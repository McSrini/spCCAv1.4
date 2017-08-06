/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import ca.mcmaster.spccav14.cplex.*;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 */
public class TreeCollectionEnder implements  Function<ActiveSubtreeCollection, ActiveSubtreeCollection>  {

     
    public ActiveSubtreeCollection call(ActiveSubtreeCollection v1) throws Exception {
        if ( v1!=null) v1.endAll();
        return v1;
    }
    
}
