/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.data.Privileges;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 *
 * @author Buddhika
 */
public class PrivilageNode extends DefaultTreeNode{
    private Privileges p;
    
     public PrivilageNode(Object data, TreeNode parent) {
        super(data, parent);
     
    }

    public PrivilageNode(Object data, TreeNode parent,Privileges p) {
        super(data, parent);
        this.p = p;
    }

    public Privileges getP() {
        return p;
    }

    public void setP(Privileges p) {
        this.p = p;
    }
    
    
}
