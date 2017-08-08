/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav14.cb;

import java.util.*;

/**
 *
 * @author tamvadss
 * 
 * contains 2 maps to keep track of old node IDs and their corresponding new node IDs
 * 
 */
public class ReincarnationMaps {
    public Map<String, String > newToOld_NodeId_Map = new LinkedHashMap <String, String >();
    public Map<String, String > oldToNew_NodeId_Map = new LinkedHashMap <String, String >();
}
