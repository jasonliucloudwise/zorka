/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.viewer;


import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.SymbolicException;
import com.jitlogic.zorka.common.tracedata.SymbolicStackElement;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class ErrorDetailView extends JPanel {

    private JSplitPane splitPane;
    private JTextArea exceptionDisplay;
    private JTextArea attributeDisplay;

    public ErrorDetailView() {
        createUI();
    }

    private void createUI() {
        setBorder(new EmptyBorder(3, 3, 3, 3));
        setLayout(new BorderLayout(0, 0));

        splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        add(splitPane, BorderLayout.CENTER);

        JPanel attributePanel = new JPanel();
        attributePanel.setLayout(new BorderLayout(0, 0));
        attributeDisplay = new JTextArea();
        attributeDisplay.setEditable(false);
        attributeDisplay.setLineWrap(true);
        attributeDisplay.setWrapStyleWord(true);
        JScrollPane scrAttributeDisplay = new JScrollPane();
        scrAttributeDisplay.setViewportView(attributeDisplay);
        attributePanel.add(new Label("Method info"), BorderLayout.NORTH);
        attributePanel.add(scrAttributeDisplay, BorderLayout.CENTER);

        splitPane.setTopComponent(attributePanel);

        JPanel exceptionPanel = new JPanel();
        exceptionPanel.setLayout(new BorderLayout(0, 0));
        exceptionDisplay = new JTextArea();
        exceptionDisplay.setEditable(false);
        JScrollPane scrExceptionDisplay = new JScrollPane();
        scrExceptionDisplay.setViewportView(exceptionDisplay);
        exceptionPanel.add(new Label("Thrown exception:"), BorderLayout.NORTH);
        exceptionPanel.add(scrExceptionDisplay, BorderLayout.CENTER);

        splitPane.setBottomComponent(exceptionPanel);

        splitPane.setResizeWeight(0.5);
    }


    public void update(SymbolRegistry symbols, ViewerTraceRecord record) {
        StringBuilder sb = new StringBuilder();
        SymbolicException cause = findCause(record);
        SymbolicException exception = findException(record);

        if (exception != null) {
            exceptionDisplay.setText(printException(symbols, exception, cause, sb));
        } else {
            exceptionDisplay.setText("<no exception>");
        }

        StringBuilder mdesc = new StringBuilder();

        mdesc.append(record.prettyPrint() + "\n\n");

        if (record.numAttrs() > 0) {
            for (Map.Entry<?, Object> e : record.getAttrs().entrySet()) {
                mdesc.append(record.sym((Integer) e.getKey()) + "=" + e.getValue() + "\n");
            }
        }

        attributeDisplay.setText(mdesc.toString());
    }


    private SymbolicException findException(ViewerTraceRecord record) {
        if (record.getException() != null) {
            return (SymbolicException) record.getException();
        } else if (0 != (record.getFlags() & ViewerTraceRecord.EXCEPTION_PASS) && record.numChildren() > 0) {
            return findException((ViewerTraceRecord) record.getChild(record.numChildren() - 1));
        } else {
            return null;
        }
    }

    private SymbolicException findCause(ViewerTraceRecord record) {

        SymbolicException cause = null;

        if (0 != (record.getFlags() & ViewerTraceRecord.EXCEPTION_WRAP) && record.numChildren() > 0) {
            // Identify cause of wrapped exception)
            ViewerTraceRecord child = (ViewerTraceRecord) record.getChild(record.numChildren() - 1);
            if (child.getException() != null) {
                cause = ((SymbolicException) child.getException()).getCause();
            } else {
                return findCause(child);
            }
        }

        return cause;
    }


    private String printException(SymbolRegistry symbols, SymbolicException ex, SymbolicException cause, StringBuilder sb) {
        sb.append(symbols.symbolName(ex.getClassId()));
        sb.append(": ");
        sb.append(ex.getMessage());
        sb.append("\n");
        for (SymbolicStackElement sse : ex.getStackTrace()) {
            sb.append("    at ");
            sb.append(symbols.symbolName(sse.getClassId()));
            sb.append(".");
            sb.append(symbols.symbolName(sse.getMethodId()));
            sb.append("(): ");
            sb.append(symbols.symbolName(sse.getFileId()));
            sb.append(":");
            sb.append(sse.getLineNum());
            sb.append("\n");
        }

        if (ex.getCause() != null || cause != null) {
            sb.append("Caused by: ");
            printException(symbols, cause != null ? cause : ex.getCause(), null, sb);
        }

        return sb.toString();
    }

}
