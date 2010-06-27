/*
Copyright 2008-2010 Gephi
Authors : Eduardo Ramos <eduramiba@gmail.com>
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gephi.datalaboratory.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.JPanel;
import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.datalaboratory.api.DataLaboratoryHelper;
import org.gephi.datalaboratory.impl.manipulators.edges.DeleteEdges;
import org.gephi.datalaboratory.impl.manipulators.nodes.DeleteNodes;
import org.gephi.datalaboratory.spi.Manipulator;
import org.gephi.datalaboratory.spi.ManipulatorUI;
import org.gephi.datalaboratory.spi.attributecolumns.AttributeColumnsManipulator;
import org.gephi.datalaboratory.spi.attributecolumns.AttributeColumnsManipulatorUI;
import org.gephi.datalaboratory.spi.edges.EdgesManipulator;
import org.gephi.datalaboratory.spi.edges.EdgesManipulatorBuilder;
import org.gephi.datalaboratory.spi.generalactions.GeneralActionsManipulator;
import org.gephi.datalaboratory.spi.nodes.NodesManipulator;
import org.gephi.datalaboratory.spi.nodes.NodesManipulatorBuilder;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Implementation of the DataLaboratoryHelper interface
 * declared in the Data Laboratory API.
 * @author Eduardo Ramos <eduramiba@gmail.com>
 * @see DataLaboratoryHelper
 */
@ServiceProvider(service = DataLaboratoryHelper.class)
public class DataLaboratoryHelperImpl implements DataLaboratoryHelper {

    public NodesManipulator[] getNodesManipulators() {
        ArrayList<NodesManipulator> nodesManipulators = new ArrayList<NodesManipulator>();
        for (NodesManipulatorBuilder nm : Lookup.getDefault().lookupAll(NodesManipulatorBuilder.class)) {
            nodesManipulators.add(nm.getNodesManipulator());
        }
        sortManipulators(nodesManipulators);
        return nodesManipulators.toArray(new NodesManipulator[0]);
    }

    public EdgesManipulator[] getEdgesManipulators() {
        ArrayList<EdgesManipulator> edgesManipulators = new ArrayList<EdgesManipulator>();
        for (EdgesManipulatorBuilder em : Lookup.getDefault().lookupAll(EdgesManipulatorBuilder.class)) {
            edgesManipulators.add(em.getEdgesManipulator());
        }
        sortManipulators(edgesManipulators);
        return edgesManipulators.toArray(new EdgesManipulator[0]);
    }

    public GeneralActionsManipulator[] getGeneralActionsManipulators() {
        ArrayList<GeneralActionsManipulator> generalActionsManipulators = new ArrayList<GeneralActionsManipulator>();
        generalActionsManipulators.addAll(Lookup.getDefault().lookupAll(GeneralActionsManipulator.class));
        sortManipulators(generalActionsManipulators);
        return generalActionsManipulators.toArray(new GeneralActionsManipulator[0]);
    }

    public AttributeColumnsManipulator[] getAttributeColumnsManipulators(){
        ArrayList<AttributeColumnsManipulator> attributeColumnsManipulators=new ArrayList<AttributeColumnsManipulator>();
        attributeColumnsManipulators.addAll(Lookup.getDefault().lookupAll(AttributeColumnsManipulator.class));
        sortAttributeColumnsManipulators(attributeColumnsManipulators);
        return attributeColumnsManipulators.toArray(new AttributeColumnsManipulator[0]);
    }

    private void sortManipulators(ArrayList<? extends Manipulator> m) {
        Collections.sort(m, new Comparator<Manipulator>() {

            public int compare(Manipulator o1, Manipulator o2) {
                //Order by type, position.
                if (o1.getType() == o2.getType()) {
                    return o1.getPosition() - o2.getPosition();
                } else {
                    return o1.getType() - o2.getType();
                }
            }
        });
    }

    private void sortAttributeColumnsManipulators(ArrayList<? extends AttributeColumnsManipulator> m) {
        Collections.sort(m, new Comparator<AttributeColumnsManipulator>() {

            public int compare(AttributeColumnsManipulator o1, AttributeColumnsManipulator o2) {
                //Order by type, position.
                if (o1.getType() == o2.getType()) {
                    return o1.getPosition() - o2.getPosition();
                } else {
                    return o1.getType() - o2.getType();
                }
            }
        });
    }

    public void executeManipulator(final Manipulator m) {
        new Thread(new Runnable() {

            public void run() {
                ManipulatorUI ui = m.getUI();
                //Show a dialog for the manipulator UI if it provides one. If not, execute the manipulator directly:
                if (ui != null) {
                    ui.setup(m);
                    JPanel settingsPanel = ui.getSettingsPanel();
                    DialogDescriptor dd = new DialogDescriptor(settingsPanel, NbBundle.getMessage(DataLaboratoryHelperImpl.class, "SettingsPanel.title", ui.getDisplayName()));
                    if (DialogDisplayer.getDefault().notify(dd).equals(NotifyDescriptor.OK_OPTION)) {
                        ui.unSetup();
                        m.execute();
                    }else{
                        ui.unSetup();
                    }
                } else {
                    m.execute();
                }
            }
        }).start();
    }

    public void executeAttributeColumnsManipulator(final AttributeColumnsManipulator m, final AttributeTable table, final AttributeColumn column) {
        new Thread(new Runnable() {

            public void run() {
                AttributeColumnsManipulatorUI ui = m.getUI();
                //Show a dialog for the manipulator UI if it provides one. If not, execute the manipulator directly:
                if (ui != null) {
                    ui.setup(m);
                    JPanel settingsPanel = ui.getSettingsPanel();
                    DialogDescriptor dd = new DialogDescriptor(settingsPanel, NbBundle.getMessage(DataLaboratoryHelperImpl.class, "SettingsPanel.title", ui.getDisplayName()));
                    if (DialogDisplayer.getDefault().notify(dd).equals(NotifyDescriptor.OK_OPTION)) {
                        ui.unSetup();
                        m.execute(table,column);
                    }else{
                        ui.unSetup();
                    }
                } else {
                    m.execute(table,column);
                }
            }
        }).start();
    }

    public NodesManipulator getDeleteNodesManipulator() {
        return new DeleteNodes();
    }

    public EdgesManipulator getDeleEdgesManipulator() {
        return new DeleteEdges();
    }
}
