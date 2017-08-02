/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14;

/**
 *
 * @author tamvadss
 */
public class Constants {
        
    public static final int ZERO = 00;
    public static final double DOUBLE_ZERO = 0.0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int THREE = 3;
    public static final int FOUR = 4;
    public static final int SIXTY = 60;
    public static final int MILLION = 1000000;
    public static final long PLUS_INFINITY = Long.MAX_VALUE;
    public static final long MINUS_INFINITY = Long.MIN_VALUE;
    
    public static final String EMPTY_STRING ="";
    public static final String MINUS_ONE_STRING = "-1";
    public static final String DELIMITER = "______";
    
    public static final String DRIVER_LOG_FILE= "F:\\temporary files here\\SparkPlexDriver1_4.log";           
    public static final String LOG_FOLDER="F:\\temporary files here\\logs\\ccav1_4\\";
    public static final String LOG_FILE_EXTENSION = ".log";
    
    public static final boolean IS_MAXIMIZATION = false;
    
    
    
    
    public static final int NUM_PARTITIONS = 100;
    public static final int RAMP_UP_TO_THIS_MANY_LEAFS = 10000;
    public static double EXPECTED_LEAFS_PER_PARTITION = (RAMP_UP_TO_THIS_MANY_LEAFS +DOUBLE_ZERO)/NUM_PARTITIONS;
    public static  String MPS_FILE_ON_DISK =  "F:\\temporary files here\\atlanta-ip.mps";
    public static   double MIP_WELLKNOWN_SOLUTION = 1200012600 ;
    
    public static   int TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE = 6 ;    
    public static   double MINIMUM_TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE = 0.5 ;//30 seconds
    public static final int SOLUTION_CYCLE_TIME_MINUTES = 12;
    
    
        
 
    //CCA subtree allowed to have slightly less good leafs than asked for in NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE 
    public static   double CCA_TOLERANCE_FRACTION =  0.30;
    public static  double CCA_PACKING_FACTOR_MAXIMUM_ALLOWED =  0.0;
    
    
    
}
