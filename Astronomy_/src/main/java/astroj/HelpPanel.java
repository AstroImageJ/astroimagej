/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package astroj;

import ij.IJ;
import util.BrowserOpener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

import static ij.IJ.showMessage;

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class HelpPanel extends JFrame implements ActionListener, DocumentListener {
    private final URL helpURL;
    private JPanel mainPanel;
    private JTextField entry;
    private JLabel jLabel1;
    private JScrollPane jScrollPanel;
    private JLabel status;
    private JEditorPane textArea;
    private int pos = 0;
    private int end = 0;
    private String oldEntry = "";
    private String callingProgramID;

    final static Color  HILIT_COLOR = Color.YELLOW;
    final static Color  ERROR_COLOR = Color.PINK;
    final static String CANCEL_ACTION = "cancel-search";

    final Color entryBg;
    final Highlighter hilit;
    final Highlighter.HighlightPainter painter, painter2;


    public HelpPanel(String filename, String callingProgramName) {

        callingProgramID = callingProgramName;
        initComponents();

        textArea.setEditable(false);
        helpURL = getClass().getClassLoader().getResource(filename);
        if (helpURL != null) {
            try {
                textArea.setPage(helpURL);
            } catch (IOException e) {
                System.err.println("Attempted to read a bad URL: " + helpURL);
            }
        } else {
            System.err.println("Couldn't find file: "+filename);
        }

        hilit = new DefaultHighlighter();
        painter = new DefaultHighlighter.DefaultHighlightPainter(HILIT_COLOR);
        painter2 = new DefaultHighlighter.DefaultHighlightPainter(Color.PINK);
        textArea.setHighlighter(hilit);

        entryBg = entry.getBackground();
        entry.getDocument().addDocumentListener(this);

        InputMap im = entry.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = entry.getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), CANCEL_ACTION);
        am.put(CANCEL_ACTION, new CancelAction());

    }

    /**
     * A straight copy of {@link JEditorPane#scrollToReference(String)} that looks for id,
     * not name to be inline with HTML5.
     */
    private void scrollToReference(String reference) {
        Document d = textArea.getDocument();
        if (d instanceof HTMLDocument) {
            HTMLDocument doc = (HTMLDocument) d;
            HTMLDocument.Iterator iter = doc.getIterator(HTML.Tag.A);
            for (; iter.isValid(); iter.next()) {
                AttributeSet a = iter.getAttributes();
                String nm = (String) a.getAttribute(HTML.Attribute.ID);
                if ((nm != null) && nm.equals(reference)) {
                    // found a matching reference in the document.
                    try {
                        int pos = iter.getStartOffset();
                        Rectangle r = textArea.modelToView(pos);
                        if (r != null) {
                            // the view is visible, scroll it to the
                            // center of the current visible area.
                            Rectangle vis = textArea.getVisibleRect();
                            //r.y -= (vis.height / 2);
                            r.height = vis.height;
                            textArea.scrollRectToVisible(r);
                            textArea.setCaretPosition(pos);
                        }
                    } catch (BadLocationException ble) {
                        ble.printStackTrace();
                    }
                }
            }
        }
    }


    /** This method is called from within the constructor to
     * initialize the form.
     */

    private void initComponents() {

        entry = new JTextField();
        entry.addActionListener(this);

        textArea = new JEditorPane();
        status = new JLabel();
        jLabel1 = new JLabel();
        ImageIcon dialogFrameIcon = createImageIcon("astroj/images/help.png", "Help_Icon");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(callingProgramID+" Help");
        textArea.setEditable(false);
        textArea.setPreferredSize(new Dimension(750, 750));
        textArea.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (e.getURL().sameFile(helpURL)) {
                    scrollToReference(e.getURL().getRef());
                } else {
                    try {
                        BrowserOpener.openURL(e.getURL());
                    } catch (IOException ex) {
                        IJ.error("Failed to open url: " + e.getURL());
                    }
                }
            }
        });

        jScrollPanel = new JScrollPane(textArea);
//        jScrollPanel.setPreferredSize(new Dimension(350, 750));
        jLabel1.setText("Search:");

        setIconImage(dialogFrameIcon.getImage());
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
//        mainFrame.addWindowListener(new WindowAdapter(){
//            @Override
//            public void windowClosing(WindowEvent e){
//            saveAndClose();}});
		mainPanel = new JPanel (new SpringLayout());
		mainPanel.setBorder (BorderFactory.createEmptyBorder(10,10,10,10));
        JPanel searchPanel = new JPanel (new SpringLayout());
        searchPanel.add(jLabel1);
        searchPanel.add(entry);
        SpringUtil.makeCompactGrid (searchPanel,1,searchPanel.getComponentCount(), 2,2,2,2);
        mainPanel.add(searchPanel);
        mainPanel.add(jScrollPanel);
        mainPanel.add(status);
        SpringUtil.makeCompactGrid (mainPanel,mainPanel.getComponentCount(),1, 2,2,2,2);

		add(mainPanel);
		pack();
		setResizable(true);
//        mainFrame.setLocation(dialogFrameLocationX, dialogFrameLocationY);
    }

    public void search() {


        String s = entry.getText().toLowerCase();
        if (!s.equalsIgnoreCase(oldEntry))
            {
            hilit.removeAllHighlights();
            }

        if (s.length() <= 0) {
            message("Nothing to search");
            hilit.removeAllHighlights();
            oldEntry = "";
            return;
            }
         oldEntry = s;
         String content = null;
         try {
             Document d = textArea.getDocument();
             content = d.getText(0, d.getLength()).toLowerCase();
             } catch (BadLocationException e) {}

        int index = content.indexOf(s, pos);
        hilit.removeAllHighlights();

        if (index >= 0) {   // match found
            try {
                end = index + s.length();
                hilit.addHighlight(index, end, painter2);
                textArea.setCaretPosition(end);
                entry.setBackground(entryBg);
                message("'" + s + "' found. Press ESC to end search");
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        } else {
            entry.setBackground(ERROR_COLOR);
            IJ.beep();
            message("'" + s + "' not found. Press ESC to start a new search");
            pos = 0;
        }

        int lastIndex = 0;
        int firstOffset = -1;
        int wordSize = s.length();
        while ((lastIndex = content.indexOf(s, lastIndex)) != -1) {
            int endIndex = lastIndex + wordSize;
            try {
                hilit.addHighlight(lastIndex, endIndex, painter);
                } catch (BadLocationException e) {}
            if (firstOffset == -1) {
            firstOffset = lastIndex;
            }
            lastIndex = endIndex;
        }
    }

    void message(String msg) {
        status.setText(msg);
    }

    // DocumentListener methods

    public void insertUpdate(DocumentEvent ev) {
        search();
    }

    public void removeUpdate(DocumentEvent ev) {
        search();
    }

    public void changedUpdate(DocumentEvent ev) {
    }

    class CancelAction extends AbstractAction {
        public void actionPerformed(ActionEvent ev) {
            hilit.removeAllHighlights();
            pos = 0;
            end = 0;
            entry.setBackground(entryBg);
        }
    }

	/**
	 * Response to pressing enter after changing text field.
	 */
    public void actionPerformed (ActionEvent e)
        {
        Object source = e.getSource();
        if (entry.getBackground()==ERROR_COLOR)
            {
            pos = 0;
            }
        else
            pos = end;
        search();
        }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            showMessage("Couldn't find icon file: " + path);
            return null;
        }
    }

//
//    public static void main(String args[]) {
//        final String[] filenames = args;
//        //Schedule a job for the event dispatch thread:
//        //creating and showing this application's GUI.
//	SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                //Turn off metal's use of bold fonts
//                UIManager.put("swing.boldMetal", Boolean.FALSE);
//                new HelpPanel("","");
//            }
//        });
//    }


}
