/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cplex.datatypes;
 
import static ca.mcmaster.spccav14.Constants.*;
import java.io.Serializable;
import java.util.*;

/**
 *
 * @author tamvadss
 */
public class SolutionVector implements Serializable {
    
    public List<String> variableNames = new ArrayList<String>();
    public double[] values  ;
    
    public boolean isAlreadySolvedToCompletion = false ; //optimal or unfeasible
    public boolean isFeasibleOrOptimal   = false ;  
    public double bestKnownSolution = DOUBLE_ZERO +  (IS_MAXIMIZATION ?  MINUS_INFINITY : PLUS_INFINITY);
    
    public void addVar (String name ) {
        variableNames.add(name);
        
    }
    public void setVariableValues ( double[] values ) {
        this .   values =   values ;
    }
    
}
