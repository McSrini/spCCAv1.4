/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import static ca.mcmaster.spccav14.Constants.NUM_PARTITIONS;
import ca.mcmaster.spccav14.cca.CCANode;
import ca.mcmaster.spccav14.cplex.ActiveSubtree;
import java.util.*;
import org.apache.spark.api.java.function.Function;

/**
 *
 * @author tamvadss
 */
public class CCACandidatesGenerator implements  Function<ActiveSubtree, List<CCANode> >  {
 
    public List<CCANode> call(ActiveSubtree v1) throws Exception {
        List<CCANode> result = v1!=null? v1.getCandidateCCANodesPostRampup(NUM_PARTITIONS): new ArrayList<CCANode>();   
        return result;
    }
    
}
