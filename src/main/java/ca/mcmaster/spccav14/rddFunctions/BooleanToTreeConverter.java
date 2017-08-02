/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;
 
import ca.mcmaster.spccav14.cplex.*;
import org.apache.spark.api.java.function.*;

/**
 *
 * @author tamvadss
 */
public class BooleanToTreeConverter implements  Function<Boolean,ActiveSubtree>  {

    public ActiveSubtree call(Boolean v1) throws Exception {
        return v1 ? new ActiveSubtree(): null;
    }
    
}
