/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import ca.mcmaster.spccav14.cplex.ActiveSubtree;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 */
public class TreeEnder implements  Function<ActiveSubtree, ActiveSubtree>  {

     
    public ActiveSubtree call(ActiveSubtree v1) throws Exception {
        if ( v1!=null) v1.end();
        return v1;
    }
    
}
