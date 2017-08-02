/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import ca.mcmaster.spccav14.cplex.ActiveSubtree;
import ca.mcmaster.spccav14.cplex.datatypes.NodeAttachment;
import java.util.ArrayList;
import java.util.List;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 */
public class ActiveLeafListFetcher implements  Function<ActiveSubtree,  List<NodeAttachment>>  {
 
    public List<NodeAttachment> call(ActiveSubtree v1) throws Exception {
        return v1==null? new ArrayList<NodeAttachment>(): v1.getActiveLeafList();
    }
    
}
