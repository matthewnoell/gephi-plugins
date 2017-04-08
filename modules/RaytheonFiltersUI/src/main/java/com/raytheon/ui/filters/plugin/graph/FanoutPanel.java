/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.raytheon.ui.filters.plugin.graph;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import com.raytheon.filters.plugin.graph.FanoutBuilder.FanoutFilter;

/**
 *
 * @author mattn
 */
public class FanoutPanel extends javax.swing.JPanel {

    private FanoutFilter fanoutFilter;
    
    /**
     * Creates new form FanoutPanel
     */
    public FanoutPanel() {
        initComponents();
        
        okButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                fanoutFilter.getProperties()[0].setValue(nodeIdTextField.getText());
            }
        });
        
        depthComboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                int depth = -1;
                int index = depthComboBox.getSelectedIndex();
                if (index == depthComboBox.getModel().getSize() - 1) {
                    depth = Integer.MAX_VALUE;
                } else {
                    depth = index + 1;
                }
                if (!fanoutFilter.getDepth().equals(depth)) {
                    fanoutFilter.getProperties()[1].setValue(depth);
                }
            }
        });
        
        withSelfCheckbox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (!fanoutFilter.isSelf() == withSelfCheckbox.isSelected()) {
                    fanoutFilter.getProperties()[2].setValue(withSelfCheckbox.isSelected());
                }
            }
        });
    }
    
    public void setup(FanoutFilter fanoutFilter) {
        this.fanoutFilter = fanoutFilter;
        nodeIdTextField.setText(fanoutFilter.getPattern());

        int depth = fanoutFilter.getDepth();
        if (depth == Integer.MAX_VALUE) {
            depthComboBox.setSelectedIndex(depthComboBox.getModel().getSize() - 1);
        } else {
            depthComboBox.setSelectedIndex(depth - 1);
        }

        withSelfCheckbox.setSelected(fanoutFilter.isSelf());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        labelNodeId = new javax.swing.JLabel();
        nodeIdTextField = new javax.swing.JTextField();
        labelDepth = new javax.swing.JLabel();
        depthComboBox = new javax.swing.JComboBox();
        okButton = new javax.swing.JButton();
        withSelfCheckbox = new javax.swing.JCheckBox();

        labelNodeId.setText(org.openide.util.NbBundle.getMessage(FanoutPanel.class, "FanoutPanel.labelNodeId.text")); // NOI18N

        nodeIdTextField.setText(org.openide.util.NbBundle.getMessage(FanoutPanel.class, "FanoutPanel.nodeIdTextField.text")); // NOI18N
        nodeIdTextField.setToolTipText(org.openide.util.NbBundle.getMessage(FanoutPanel.class, "FanoutPanel.nodeIdTextField.toolTipText")); // NOI18N
        nodeIdTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nodeIdTextFieldActionPerformed(evt);
            }
        });

        labelDepth.setText(org.openide.util.NbBundle.getMessage(FanoutPanel.class, "FanoutPanel.labelDepth.text")); // NOI18N

        depthComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "Max" }));

        okButton.setText(org.openide.util.NbBundle.getMessage(FanoutPanel.class, "FanoutPanel.okButton.text")); // NOI18N
        okButton.setMargin(new java.awt.Insets(2, 7, 2, 7));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        withSelfCheckbox.setText(org.openide.util.NbBundle.getMessage(FanoutPanel.class, "FanoutPanel.withSelfCheckbox.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(labelNodeId)
                    .addComponent(labelDepth))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nodeIdTextField)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(withSelfCheckbox)
                            .addComponent(depthComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 46, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelNodeId)
                    .addComponent(okButton)
                    .addComponent(nodeIdTextField))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelDepth)
                    .addComponent(depthComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(withSelfCheckbox)
                .addContainerGap())
        );

        nodeIdTextField.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(FanoutPanel.class, "FanoutPanel.nodeIdTextField.AccessibleContext.accessibleDescription")); // NOI18N
        withSelfCheckbox.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(FanoutPanel.class, "FanoutPanel.withSelfCheckbox.AccessibleContext.accessibleName")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_okButtonActionPerformed

    private void nodeIdTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nodeIdTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_nodeIdTextFieldActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox depthComboBox;
    private javax.swing.JLabel labelDepth;
    private javax.swing.JLabel labelNodeId;
    private javax.swing.JTextField nodeIdTextField;
    private javax.swing.JButton okButton;
    private javax.swing.JCheckBox withSelfCheckbox;
    // End of variables declaration//GEN-END:variables
}
