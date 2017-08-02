/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import ca.mcmaster.spccav14.cplex.datatypes.NodeAttachment;
import java.util.List;
import org.apache.spark.api.java.function.Function2;

/**
 *
 * @author tamvadss
 */
public class ListSizeComparer2 implements Function2<List<NodeAttachment>, List<NodeAttachment>, List<NodeAttachment> > {
 
    public List<NodeAttachment> call(List<NodeAttachment> v1, List<NodeAttachment> v2) throws Exception {
         
        return v1.size()> v2.size ()? v1 : v2;
    }
    
}
