/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.utils;
 
import static ca.mcmaster.spccav14.Constants.*; 
import ca.mcmaster.spccav14.cplex.datatypes.BranchingInstruction;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.List;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public   class BranchHandlerUtilities {
         
    private static Logger logger=Logger.getLogger(BranchHandlerUtilities.class);
        
    static {
        logger.setLevel(Level.OFF);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+BranchHandlerUtilities.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
        
     
    
    
    //merge the braching instruction list  into array format used by CPLEX
    public static void mergeBranchingInstructionIntoArray ( List<BranchingInstruction> branchingInstructionList ,   
            IloNumVar[][]  vars,  double[ ] [] bounds , IloCplex.BranchDirection[ ] []  dirs , int childNum , IloNumVar[]  modelVars){
        
        //map of vars and their bounds
        Map<String ,Double> upperBoundMap = new LinkedHashMap<String ,Double>();
        Map<String ,Double> lowerBoundMap = new LinkedHashMap<String ,Double>();
        
        //prepare a map of variables
        Map<String, IloNumVar> modelVariableMap = new LinkedHashMap<String ,IloNumVar>();
        for (int index = ZERO; index < modelVars.length ; index ++ ){
            modelVariableMap.put(modelVars[index].getName(), modelVars[index]);
        }
        
        
        for ( BranchingInstruction bi :  branchingInstructionList){
            for (int index = ZERO; index < bi.size(); index ++ ){
                String varname = bi.varNames.get(index);
                double value = bi.varBounds.get(index);
                boolean isUpperBound = bi.isBranchDirectionDown.get(index);
                
                if (isUpperBound){
                    //check if map already has this var
                    if (upperBoundMap.containsKey(varname)){
                       //get current value 
                       double currentValue = upperBoundMap.get(varname);
                       //insert if more restrictive
                       if (value<currentValue ) upperBoundMap.put(varname, value);
                    } else {
                       //insert 
                       upperBoundMap.put(varname, value);
                    }
                }else {
                    if (lowerBoundMap.containsKey(varname)){
                       //get current value 
                       double currentValue = lowerBoundMap.get(varname);
                       //insert if more restrictive
                       if (value>currentValue ) lowerBoundMap.put(varname, value);
                    } else {
                       //insert 
                       lowerBoundMap.put(varname, value);
                    }
                }
                
            }
        }
        
        //now fill up the arrays
        int arraySize = upperBoundMap.size()+lowerBoundMap.size();
        vars[childNum]= new IloNumVar[arraySize];
        bounds[childNum]= new double[arraySize];
        dirs[childNum] = new IloCplex.BranchDirection[arraySize];
        int index = ZERO;
        for (  Map.Entry <String ,Double>  entry : upperBoundMap.entrySet()){
            vars[childNum][index] = modelVariableMap.get(entry.getKey());
            bounds[childNum][index]= entry.getValue();
            dirs[childNum][index ] = IloCplex.BranchDirection.Down;
            index ++;
        }
         
        for (  Map.Entry <String ,Double>  entry : lowerBoundMap.entrySet()){
            vars[childNum][index] = modelVariableMap.get(entry.getKey());
            bounds[childNum][index]= entry.getValue();
            dirs[childNum][index ] = IloCplex.BranchDirection.Up;
            index ++;
        }
         
    }
    
    public static Map< String, Double >   getUpperBounds   (List <BranchingInstruction> cumulativeBranchingInstructions, String nodeID) {
        Map< String, Double > upperBounds = new HashMap < String, Double > ();
        
        for (BranchingInstruction bi: cumulativeBranchingInstructions){
            
            for (int index = ZERO ; index < bi.size(); index ++){
                if ( bi.isBranchDirectionDown.get(index)){
                     
                    String varName = bi.varNames.get(index);
                    double value = bi.varBounds.get(index);
                    if (upperBounds.containsKey(varName)) {
                        double existingValue = upperBounds.get( varName);
                        if (existingValue>value ) upperBounds.put(varName, value);
                    } else {
                        upperBounds.put(varName, value);
                    }
                }
            }
        }
        
        logger.debug("nodeID "+nodeID);
        for ( Map.Entry< String, Double > entry:upperBounds.entrySet() ){
            logger.debug("Upper bound "+entry.getKey() + " "+ entry.getValue());
        }
        return  upperBounds ;
    }

    public static Map< String, Double >   getLowerBounds   (List <BranchingInstruction> cumulativeBranchingInstructions, String nodeID) {
        Map< String, Double > lowerBounds = new HashMap < String, Double > ();
        
        for (BranchingInstruction bi: cumulativeBranchingInstructions){            
            
            for (int index = ZERO ; index < bi.size(); index ++){
                if ( ! bi.isBranchDirectionDown.get(index)){
                     
                    String varName = bi.varNames.get(index);
                    double value = bi.varBounds.get(index);
                    if (lowerBounds.containsKey(varName)) {
                        double existingValue = lowerBounds.get( varName);
                        if (existingValue<value ) lowerBounds.put(varName, value);
                    } else {
                        lowerBounds.put(varName, value);
                    }
                }
            }            
        }
        
        logger.debug("nodeID "+nodeID);
        for ( Map.Entry< String, Double > entry:lowerBounds.entrySet() ){
            logger.debug("Lower bound "+entry.getKey() + " "+ entry.getValue());
        }
        return  lowerBounds ;
    }
}
