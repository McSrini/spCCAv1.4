/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cplex.datatypes;
 
import java.io.Serializable;
import java.util.*;
import java.util.List;
import static ca.mcmaster.spccav14.Constants.*;

/**
 *
 * @author tamvadss
 */
public class BranchingInstruction  implements Serializable {
        
    //variables used to create this child by branching the parent
    public List<String>  varNames = new ArrayList< String> (); 
    //is this an upper bound ?
    public List< Boolean > isBranchDirectionDown = new ArrayList< Boolean >(); 
    public List< Double>  varBounds = new ArrayList< Double >();
    
    public     BranchingInstruction( ) {
        
    }
        
    public       BranchingInstruction (String[] names, Boolean[] dirs, double[] bounds) {
        for (String name : names) {
            varNames.add(name);
        }
        for (Boolean dir  : dirs){
            isBranchDirectionDown.add(dir);
        }
        for (Double bound : bounds)   {
            varBounds.add(bound);
        }
    }
 
    public     BranchingInstruction(BranchingInstruction anotherInstruction ) {
        this.varNames.addAll(anotherInstruction.varNames);
        this.varBounds.addAll( anotherInstruction.varBounds);
        this.isBranchDirectionDown.addAll(anotherInstruction.isBranchDirectionDown);
    }
        
    public     void merge (BranchingInstruction anotherInstruction ) {
        this.varNames.addAll(anotherInstruction.varNames);
        this.varBounds.addAll( anotherInstruction.varBounds);
        this.isBranchDirectionDown.addAll(anotherInstruction.isBranchDirectionDown);
    }
        
    public int size () {
        return varNames.size();
    }
             
    public BranchingInstruction subtract (  BranchingInstruction minusInstruction ) {
        BranchingInstruction bi = new BranchingInstruction ();
               
        for (int index = ZERO; index < this.varBounds.size(); index ++){
            if (! minusInstruction.varNames.contains(this.varNames.get(index)))  {
                bi.varNames.add( this.varNames.get(index));
                bi.varBounds.add(this.varBounds.get(index));
                bi.isBranchDirectionDown.add(this.isBranchDirectionDown.get(index));
            }
        }
        
        return bi;
    }
    
    public String toString (){
        String result =EMPTY_STRING;
        
        for (int index = ZERO; index < varNames.size(); index ++) {
            String varname = varNames.get(index);
            Double varbound = this.varBounds.get(index);
            int isDown = this.isBranchDirectionDown.get(index) ? ONE: ZERO;
            result += "("+varname + "," +varbound+ ","+isDown +") ";
        }
        
        
        return result ;
    }
    
}
