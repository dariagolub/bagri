package com.bagri.visualvm.manager.ui;

import com.bagri.visualvm.manager.event.ApplicationEvent;
import com.bagri.visualvm.manager.event.EventBus;
import com.bagri.visualvm.manager.model.Schema;
import com.bagri.visualvm.manager.model.SchemaManagement;
import com.bagri.visualvm.manager.service.SchemaManagementService;
import com.bagri.visualvm.manager.service.ServiceException;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

public class SchemaPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(SchemaManagementPanel.class.getName());
    private final SchemaManagementService schemaService;
    private final EventBus<ApplicationEvent> eventBus;
    private final Schema schema;
    private JTabbedPane tabbedPane;
    private JToolBar toolBar;
    private XTable grid;
    private JTextArea query;
    private JTextArea queryResult;

    public SchemaPanel(SchemaManagementService schemaService, EventBus<ApplicationEvent> eventBus, Schema schema) {
        super(new GridLayout(1, 1));
        this.schema = schema;
        this.schemaService = schemaService;
        this.eventBus = eventBus;
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab(schema.getSchemaName() + SchemaManagement.SCHEMA_DETAILS, createSchemaInfoPanel());
        tabbedPane.addTab(SchemaManagement.DOCUMENT_MANAGEMENT, createSchemaDocumentsPanel());
        tabbedPane.addTab(SchemaManagement.QUERY_MANAGEMENT, createSchemaQueryPanel());
        add(tabbedPane);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());
        setBorder(BorderFactory.createEmptyBorder());
    }

    private JPanel createSchemaInfoPanel() {
        JPanel panel = new JPanel();
        return panel;
    }

    private JPanel createSchemaDocumentsPanel() {
        JPanel panel = new JPanel();
        return panel;
    }

    private JPanel createSchemaQueryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JToolBar queryToolbar = new JToolBar();
        // "Run Query" button
        JButton runQuery = new JButton("Run Query");
        runQuery.setToolTipText("Executes query");
        runQuery.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Object result = schemaService.runQuery(schema, query.getText());
                    queryResult.setText((null != result) ? result.toString() : "Null");
                } catch (ServiceException e1) {
                    StringWriter sw = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(sw);
                    e1.printStackTrace(printWriter);
                    queryResult.setText(sw.toString());
                }
            }
        });
        queryToolbar.add(runQuery);
        queryToolbar.addSeparator();
        queryToolbar.setFloatable(false);
        panel.add(queryToolbar, BorderLayout.PAGE_START);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setTopComponent(createQueryEditorPanel());
        splitPane.setBottomComponent(createQueryResultsPanel());
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createQueryEditorPanel() {
        //Create a text area.
        query = new JTextArea();
        query.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        query.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if ( SwingUtilities.isRightMouseButton ( e ) ) {
                    // TODO: add Icons
                    // TODO: display "Cut" and "Copy" as grayed text, if text is not selected
                    JPopupMenu menu = new JPopupMenu ();
                    JMenuItem menuItem = new JMenuItem(new DefaultEditorKit.CutAction());
                    menuItem.setText("Cut");
                    menuItem.setMnemonic(KeyEvent.VK_T);
                    menu.add(menuItem);

                    menuItem = new JMenuItem(new DefaultEditorKit.CopyAction());
                    menuItem.setText("Copy");
                    menuItem.setMnemonic(KeyEvent.VK_C);
                    menu.add(menuItem);

                    // TODO: display "paste" as grayed text if clipboard is empty.
                    menuItem = new JMenuItem(new DefaultEditorKit.PasteAction());
                    menuItem.setText("Paste");
                    menuItem.setMnemonic(KeyEvent.VK_P);
                    menu.add(menuItem);
                    menu.show(query, e.getX(), e.getY());
                }
             }
        });
        JScrollPane areaScrollPane = new JScrollPane(query);
        areaScrollPane.setPreferredSize(new Dimension(500, 150));
        areaScrollPane.setMinimumSize(new Dimension(500, 150));
        query.setCaretPosition(0);
        return areaScrollPane;
    }

    private JScrollPane createQueryResultsPanel() {
        //Create a text area.
        queryResult = new JTextArea();
        queryResult.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane areaScrollPane = new JScrollPane(queryResult);
        areaScrollPane.setPreferredSize(new Dimension(500, 150));
        areaScrollPane.setMinimumSize(new Dimension(500, 150));
        queryResult.setEditable(false);
        queryResult.setCaretPosition(0);
        return areaScrollPane;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
//        if (!enabled) {
//            grid.clearSelection();
//            grid.getTableHeader().setEnabled(false);
//            grid.setEnabled(false);
//            for (Component c : toolBar.getComponents()) {
//                c.setEnabled(false);
//            }
//            toolBar.setEnabled(false);
//            for (Component c : tabbedPane.getComponents()) {
//                c.setEnabled(false);
//            }
//            tabbedPane.setEnabled(false);
//        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
//        if (!grid.isLoaded()) {
//            grid.reload();
//        }
    }

}
