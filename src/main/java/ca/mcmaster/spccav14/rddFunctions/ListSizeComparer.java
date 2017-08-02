/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.rddFunctions;

import java.util.List;
import org.apache.spark.api.java.function.Function2;

/**
 *
 * @author tamvadss
 */
public class ListSizeComparer implements Function2<List<String>, List<String>, List<String> > {
 
    public List<String> call(List<String> v1, List<String> v2) throws Exception {
         
        return v1.size()> v2.size ()? v1 : v2;
    }
    
}
