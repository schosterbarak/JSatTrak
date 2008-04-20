/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.examples;

import gov.nasa.worldwind.WorldWindow;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.beans.*;
import java.net.URISyntaxException;

/**
 * @author tag
 * @version $Id: WMSLayerManagerFrame.java 3611 2007-11-22 16:47:49Z tgaskins $
 */
public class WMSLayerManagerFrame extends JFrame
{
    private static final String[] servers = new String[]
        {
            "http://neowms.sci.gsfc.nasa.gov/wms/wms",
            "http://mapserver.flightgear.org/cgi-bin/landcover",
            "http://wms.jpl.nasa.gov/wms.cgi",
        };

    private final Dimension wmsPanelSize = new Dimension(400, 600);

    private JTabbedPane tabbedPane;
    private int previousTabIndex;
    private WorldWindow wwd;
    private LayerPanel layerPanel;

    public WMSLayerManagerFrame(WorldWindow wwd)
    {
        this.wwd = wwd;
        this.layerPanel = new LayerPanel(wwd, null);

        this.tabbedPane = new JTabbedPane();

        this.tabbedPane.add(new JPanel());
        this.tabbedPane.setTitleAt(0, "+");
        this.tabbedPane.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent changeEvent)
            {
                if (tabbedPane.getSelectedIndex() != 0)
                {
                    previousTabIndex = tabbedPane.getSelectedIndex();
                    return;
                }

                String server = JOptionPane.showInputDialog("Enter wms server URL");
                if (server == null || server.length() < 1)
                {
                    tabbedPane.setSelectedIndex(previousTabIndex);
                    return;
                }

                // Respond by adding a new WMSLayerPanel to the tabbed pane.
                if (addTab(tabbedPane.getTabCount(), server.trim()) != null)
                    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
            }
        });

        // Create a tab for each server and add it to the tabbed panel.
        for (int i = 0; i < servers.length; i++)
        {
            this.addTab(i + 1, servers[i]); // i+1 to place all server tabs to the right of the Add Server tab
        }

        // Display the first server pane by default.
        this.tabbedPane.setSelectedIndex(this.tabbedPane.getTabCount() > 0 ? 1 : 0);
        this.previousTabIndex = this.tabbedPane.getSelectedIndex();

        // Add the tabbed pane to a frame separate from the world window.
        JFrame controlFrame = new JFrame();
        controlFrame.getContentPane().add(tabbedPane);
        controlFrame.pack();
        controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        controlFrame.setVisible(true);
    }

    private WMSLayersPanel addTab(int position, String server)
    {
        // Add a server to the tabbed dialog.
        try
        {
            WMSLayersPanel layersPanel = new WMSLayersPanel(WMSLayerManagerFrame.this.wwd, server, wmsPanelSize);
            this.tabbedPane.add(layersPanel, BorderLayout.CENTER);
            String title = layersPanel.getServerDisplayString();
            this.tabbedPane.setTitleAt(position, title != null && title.length() > 0 ? title : server);

            // Add a listener to notice wms layer selections and tell the layer panel to reflect the new state.
            layersPanel.addPropertyChangeListener("LayersPanelUpdated", new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent propertyChangeEvent)
                {
                    WMSLayerManagerFrame.this.layerPanel.update(WMSLayerManagerFrame.this.wwd);
                }
            });

            return layersPanel;
        }
        catch (URISyntaxException e)
        {
            JOptionPane.showMessageDialog(null, "Server URL is invalid", "Invalid Server URL",
                JOptionPane.ERROR_MESSAGE);
            tabbedPane.setSelectedIndex(previousTabIndex);
            return null;
        }
    }

    private static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            WMSLayerManagerFrame wmsf = new WMSLayerManagerFrame(this.getWwd());
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind WMS Layers", AppFrame.class);
    }
}