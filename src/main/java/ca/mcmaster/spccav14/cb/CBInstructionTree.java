/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cb;
 
import static ca.mcmaster.spccav14.Constants.*;
import ca.mcmaster.spccav14.cca.*;
import java.io.Serializable;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class CBInstructionTree  implements Serializable  {
    
    
    //private static Logger logger=Logger.getLogger(CBInstructionTree.class);
        
    public CCANode ccaRoot;
    public CBInstructionTree leftSubTree=null, rightSubtree=null;
    
    //here is the list of leafs to prune, if you migrate this CB tree
    public List<String> pruneList = new ArrayList<String> ();
    
    //useful to  convert to Map
    public Map<String, CCANode> treeAsMap = new LinkedHashMap<String,CCANode>();
    
    /*static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+CBInstructionTree.class.getSimpleName()+ LOG_FILE_EXTENSION);
             
            rfa.setMaxBackupIndex(TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
             
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }*/
    
    public CBInstructionTree (CCANode root){
        ccaRoot=root;
    }
    
    public Map<String, CCANode> asMap (){
        asMap(this);
        return this.treeAsMap;
    }
    
    public String getRootNodeID (){
        return this.ccaRoot.nodeID;
    }
    
    /*public void print (){
        print (this);
                
        //finally print leafs that should be pruned if the CB is used
        logger.debug( "The prune list for the CB tree is:");
        for (String nodeid: pruneList ){
            logger.debug(nodeid+",");
        }
    }
    
    private void print ( CBInstructionTree tree){
        logger.debug(tree.ccaRoot);
        logger.debug( "\n");
        if (tree.leftSubTree!=null) print(tree.leftSubTree);
        if (tree.rightSubtree!=null)print(tree.rightSubtree);
    }*/
       
    private void asMap (CBInstructionTree tree){
        CCANode ccaRoot = tree.ccaRoot;
        this.treeAsMap.put(ccaRoot.nodeID , ccaRoot );
        if (tree.leftSubTree!=null) asMap (  tree.leftSubTree);
        if (tree.rightSubtree!=null) asMap (  tree.rightSubtree);
    }
       
}
