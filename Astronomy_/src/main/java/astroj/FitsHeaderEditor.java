// FITS_Header_Editor.java
package astroj;

import ij.*;
import ij.io.SaveDialog;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class FitsHeaderEditor implements ListSelectionListener, ActionListener, ItemListener
	{
	ImagePlus imp;
    int slice = 1;
	String[][] header = null;
	String[][] rows = null;

	JTable table = null;
	JFrame frame = null;
    JPanel panel;
    JScrollPane scrollPane;
	JCheckBox keywordLockCB;
    JTextField searchTF;

	public static String SAVE      = "SAVE";
    public static String SAVEFILE  = "SAVE FILE";
    public static String SAVEFILEAS  = "SAVE FILE AS...";
    public static String SAVEASTEXT  = "SAVE AS TEXT...";
	public static String CANCEL    = "CANCEL";
	public static String DELETE    = "DELETE";
	public static String INSERT    = "INSERT";
	public static String VERSION   = "2012-05-01";
    public static final String NL = System.getProperty("line.separator");  


	boolean keywordLock = true;
	boolean changed = false;

    int frameX = -999999;
    int frameY = -999999;
    int frameWidth = 600;
    int frameHeight = 600;
    int lastGoodSearchCol = -1;
    int lastGoodSearchRow = -1;

	public FitsHeaderEditor(ImagePlus imagePlus)
		{
		// PLACE FITS HEADER IN A STRING ARRAY
        imp = imagePlus;
        slice = imp.getCurrentSlice();
        getPrefs();
       
		String[] hdr = FitsJ.getHeader(imp);
        if (hdr == null)
            {
            IJ.beep();
            IJ.showMessage("No valid FITS header");
            return;
            }
		int l = hdr.length;
		header = new String[l][5];
		for (int i=0; i < l; i++)
			{
			String card = hdr[i];
			String ctype = FitsJ.getCardType(card);

			header[i][0] = ""+(i+1);
			header[i][4] = ctype;

			if (ctype == "C" || ctype == "H")	// COMMENT OR HISTORY
				{
				header[i][1] = card.substring(0,7);
				header[i][2] = card.substring(8,card.length()).trim();
				header[i][3] = null;
				}
			else if (ctype == "S")
				{
				header[i][1] = FitsJ.getCardKey (card);
				header[i][2] = FitsJ.getCardValue (card);
				header[i][3] = FitsJ.getCardComment (card);
				}
			else if (ctype == "E")
				{
				header[i][1] = "END";
				header[i][2] = null;
				header[i][3] = null;
				}
			else
                {
				header[i][1] = FitsJ.getCardKey (card);
				header[i][2] = FitsJ.getCardValue (card);
				header[i][3] = FitsJ.getCardComment (card);
				}
			}

		// CREATE GUI

		frame = new JFrame ("FITS Header Editor ("+IJU.getSliceFilename(imp, slice)+")");
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e){
            savePrefs();}});
		
//		frame.add (new JLabel("Image: "+imp.getShortTitle()));

        buildTable();

		}


    public void buildTable()
        {
        boolean newTable = true;
        int col1size = 100;
        int col2size = 100;
        int col3size = 100;
        int col4size = 100;
        int col5size = 100;
        if (table != null)
            {
            newTable = false;
            }
        if (!newTable)
            {
            col1size = table.getColumnModel().getColumn(0).getWidth();
            col2size = table.getColumnModel().getColumn(1).getWidth();
            col3size = table.getColumnModel().getColumn(2).getWidth();
            col4size = table.getColumnModel().getColumn(3).getWidth();
            col5size = table.getColumnModel().getColumn(4).getWidth();
            frameWidth = frame.getWidth();
            frameHeight = frame.getHeight();
            frameX = frame.getX();
            frameY = frame.getY();
            frame.remove(panel);
            }
        panel = new JPanel(new BorderLayout());
		table = new JTable (new FITSTableModel());
		// table.setFillsViewportHeight(true);		Java 1.6
		table.setShowGrid(true);

        table.setColumnSelectionAllowed(false);

		ListSelectionModel model = table.getSelectionModel();
		model.addListSelectionListener(this);
		FontMetrics metrics = table.getFontMetrics(table.getFont());

        int w = metrics.stringWidth("M");
        if (newTable)
            {
            col1size = 5*w;
            col2size = 9*w;
            col3size = 50*w;
            col4size = 50*w;
            col5size = 5*w;
            }

		TableColumn col = table.getColumnModel().getColumn(0);
		col.setPreferredWidth(col1size);
		col = table.getColumnModel().getColumn(1);
		col.setPreferredWidth (col2size);
        col = table.getColumnModel().getColumn(2);
		col.setPreferredWidth (col3size);
        col = table.getColumnModel().getColumn(3);
		col.setPreferredWidth (col4size);
		col = table.getColumnModel().getColumn(4);
		col.setPreferredWidth (col5size);
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setToolTipText("<html>B = boolean<br>C = comment<br>E = end<br>H = history<br>I = integer<br>R = real<br>S = string<br>? = unknown</html>");
		col.setCellRenderer(renderer);
        table.setPreferredScrollableViewportSize(new Dimension(col1size+col2size+col3size+col4size+col5size,table.getRowHeight()*header.length>600 ? 600 : table.getRowHeight()*header.length));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
		scrollPane = new JScrollPane (table);
		panel.add (scrollPane, BorderLayout.CENTER);

		// ADD BUTTONS

		JPanel gui = new JPanel();


		keywordLockCB = new JCheckBox("Lock keywords", keywordLock);
        keywordLockCB.setToolTipText("Lock or unlock editing of the keyword field");
        keywordLockCB.addItemListener(this);
        gui.add (keywordLockCB);
        
        JLabel searchLabel = new JLabel("Search:");
        gui.add (searchLabel);
        
        searchTF = new JTextField(10); 
        searchTF.setToolTipText("Enter search text and press enter");
        searchTF.addActionListener(new java.awt.event.ActionListener() {
           public void actionPerformed(ActionEvent e) {
           int rows = table.getRowCount();
           int cols = table.getColumnCount();
           if (rows < 1 || cols < 1) return;
           int startRow = table.getSelectedRow();
           if (startRow < 0) startRow = 0;
           int startCol = table.getSelectedColumn();
           if (startCol < 0) startCol = 0;
           if (startRow == lastGoodSearchRow && startCol == lastGoodSearchCol) 
               {
               if (startCol == 4)
                   {
                   startCol = 0;
                   if (startRow < rows - 1)
                       {
                       startRow++;
                       }
                   else
                       {
                       startRow = 0;
                       startCol = 0;
                       }
                   }
               else
                   {
                   startCol++;
                   }
               }
           Object cellObject;
           String cellText = "";
           String searchText = searchTF.getText().trim().toLowerCase();
           for (int j=startRow; j<rows; j++)
               {
               for (int i=0; i<cols; i++)
                   {
                   if (j == startRow && i == 0) i = startCol;
                   cellObject = table.getValueAt(j, i);
                   if (cellObject==null) 
                       {
                       cellText = " ";
                       }
                   else
                       {
                       cellText = cellObject.toString().toLowerCase();
                       }
                   if (cellText.contains(searchText))
                       {
                       //IJ.log("found "+searchText+" at ("+j+","+i+")");
                       table.changeSelection(j, i, false, false);
                       lastGoodSearchCol = i;
                       lastGoodSearchRow = j;
                       break;
                       }
                   }
               if (cellText.contains(searchText))
                   {
                   break;
                   }
               if (j == rows - 1)
                   {
                   IJ.error("Search string not found");
                   lastGoodSearchRow = 0;
                   lastGoodSearchCol = 0;
                   table.changeSelection(0, 0, false, false);
                   }
               }
           }
        });
        gui.add (searchTF);

		JButton deleterow = new JButton (DELETE);
        deleterow.setToolTipText("Delete selected row(s)");
		deleterow.addActionListener (this);
		gui.add (deleterow);

		JButton insertrow = new JButton (INSERT);
        insertrow.setToolTipText("Insert a row below the (first) selected row");
		insertrow.addActionListener (this);
		gui.add (insertrow);
        
		JButton saveAsText = new JButton (SAVEASTEXT);
        saveAsText.setToolTipText("Save header as text file");
		saveAsText.addActionListener (this);
		gui.add (saveAsText);         

		JButton save = new JButton (SAVE);
        save.setToolTipText("Save header changes to AstroImageJ memory and exit the editor");
		save.addActionListener (this);
		gui.add (save);

		JButton saveFile = new JButton (SAVEFILE);
        saveFile.setToolTipText("Save header changes and image to original file name on hard drive and exit the editor");
		saveFile.addActionListener (this);
		gui.add (saveFile);
        
		JButton saveFileAs = new JButton (SAVEFILEAS);
        saveFileAs.setToolTipText("Save header changes and image to new file name on hard drive and exit the editor");
		saveFileAs.addActionListener (this);
		gui.add (saveFileAs);        

		JButton cancel = new JButton (CANCEL);
        cancel.setToolTipText("Cancel changes and exit the editor");
		cancel.addActionListener (this);
		gui.add (cancel);

		// JButton cutcol = new JButton (CUT);
		// cutcol.addActionListener (this);
		// gui.add (cutcol);

		// JButton pastecol = new JButton (PASTE);
		// pastecol.addActionListener (this);
		// gui.add (pastecol);



        gui.setMinimumSize(new Dimension(10,400));
		panel.add (gui, BorderLayout.SOUTH);
		table.doLayout();
		frame.add (panel);

		frame.pack ();
        if (!newTable)
            {
            frame.setSize(frameWidth, frameHeight);
            }
        Dimension mainScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (!Prefs.isLocationOnScreen(new Point(frameX, frameY)))
            {
            frameX = mainScreenSize.width/2 - frame.getWidth()/2;
            frameY = mainScreenSize.height/2 - frame.getHeight()/2;
            }
        frame.setLocation(frameX, frameY);

		frame.setVisible (true);
    }


    public void itemStateChanged (ItemEvent e)
		{
		Object source = e.getItemSelectable();
		if (e.getStateChange() == ItemEvent.SELECTED)
			{
			if (source == keywordLockCB)
				keywordLock = true;
            }
        else
            {
			if (source == keywordLockCB)
				keywordLock = false;
            }
        }

	// ActionListener METHODS

	public void actionPerformed (ActionEvent e)
		{
		String cmd = e.getActionCommand();
		if (cmd.equals(CANCEL))
			{
			frame.setVisible (false);
            savePrefs();
            frame.dispose();
			frame = null;
			return;
			}
		else if (cmd.equals(SAVE))
			{
            if (table.getCellEditor() != null) //finalize the last edit if enter has not been pressed
                {
                table.getCellEditor().stopCellEditing();
                }

            String[] hdr = extractHeader();
            if (hdr == null)
                {
                IJ.beep();
                IJ.showMessage ("Header edit error, changes not saved!");
                return;
                }
            else if (imp.getStack().isVirtual())
                {
                IJ.beep();
                IJ.showMessage ("Can not save changes to virtual stack.\nUse 'Save File' to save file with new header to hard drive.");
                return;
                }

            imp.setSlice(slice);
            FitsJ.putHeader(imp,hdr);

			frame.setVisible (false);
            savePrefs();
            frame.dispose();
			frame = null;
            if (imp.getCanvas() instanceof AstroCanvas)
                if (((AstroStackWindow)imp.getWindow()).hasWCS())
                    ((AstroStackWindow)imp.getWindow()).setAstroProcessor(true);            
			return;
			}
		else if (cmd.equals(SAVEASTEXT))
			{
            if (table.getCellEditor() != null) //finalize the last edit if enter has not been pressed
                {
                table.getCellEditor().stopCellEditing();
                }

            String[] hdr = extractHeader();
            if (hdr == null)
                {
                IJ.showMessage ("Header edit error, text file not saved!");
                return;
                }
            String hdrString = "";
            for (int i = 0; i < hdr.length; i++)
                {
                hdrString += hdr[i];
                hdrString += NL;
                }

			SaveDialog sd = new SaveDialog("Save header as text file...", IJU.getSliceFilename(imp, slice), ".txt");
			String name = sd.getFileName();
			if (name==null || name.trim().equals("")) return;
			String path = sd.getDirectory() + name;
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(path, false));
                out.write(hdrString);
                out.close();
                }
            catch (IOException ioe) 
                {
                IJ.showMessage("Error writing header as text file");
                return;
                }
            if (imp.getCanvas() instanceof AstroCanvas)
                if (((AstroStackWindow)imp.getWindow()).hasWCS())
                    ((AstroStackWindow)imp.getWindow()).setAstroProcessor(true);            
			return;
			}        
		else if (cmd.equals(SAVEFILE))
			{
            if (table.getCellEditor() != null) //finalize the last edit if enter has not been pressed
                {
                table.getCellEditor().stopCellEditing();
                }

            String[] hdr = extractHeader();
            if (hdr == null)
                {
                IJ.showMessage ("Header edit error, changes not saved!");
                return;
                }
            
            imp.setSlice(slice);
            
            String imageDirname = imp.getOriginalFileInfo().directory;
            String imageFilename = IJU.getSliceFilename(imp, slice);
            
            
            if (imp.getStack().isVirtual())
                {
                ImagePlus imp2 = new ImagePlus(imp.getStack().getSliceLabel(slice), imp.getStack().getProcessor(slice) ); 
                imp2.setCalibration(imp.getCalibration());  
                imp2.setFileInfo(imp.getFileInfo());
                imp2.setProcessor ("WCS_"+IJU.getSliceFilename(imp, slice), imp.getStack().getProcessor(slice)); 
                FitsJ.putHeader(imp2,hdr);
                IJU.saveFile(imp2, imageDirname+imageFilename); 
                if (slice < imp.getStackSize())
                    {
                    imp.setSlice(slice+1);
                    imp.setSlice(slice);
                    }
                else if (slice > 1)
                    {
                    imp.setSlice(slice-1);
                    imp.setSlice(slice);
                    }
                    
                }
            else
                {
                FitsJ.putHeader(imp,hdr);
                IJU.saveFile(imp, imageDirname+imageFilename); 
                }
			frame.setVisible (false);
            savePrefs();
            frame.dispose();
			frame = null;
            if (imp.getCanvas() instanceof AstroCanvas)
                if (((AstroStackWindow)imp.getWindow()).hasWCS())
                    ((AstroStackWindow)imp.getWindow()).setAstroProcessor(true);            
			return;
			}
		else if (cmd.equals(SAVEFILEAS))
			{
            if (table.getCellEditor() != null) //finalize the last edit if enter has not been pressed
                {
                table.getCellEditor().stopCellEditing();
                }

            String[] hdr = extractHeader();
            if (hdr == null)
                {
                IJ.showMessage ("Header edit error, changes not saved!");
                return;
                }
            
            imp.setSlice(slice);
            
			SaveDialog sd = new SaveDialog("Save image and header as...", IJU.getSliceFilename(imp, slice), "");
			String name = sd.getFileName();
			if (name==null || name.trim().equals("")) return;
			String path = sd.getDirectory() + name;            
            
            if (imp.getStack().isVirtual())
                {
                ImagePlus imp2 = null;
                imp2 = imp.duplicate();
                FitsJ.putHeader(imp2,hdr);
                IJU.saveFile(imp2, path);
//                IJ.run(imp2, "FITS...", path);
                }
            else
                {
                FitsJ.putHeader(imp,hdr);
                IJU.saveFile(imp, path);
//                IJ.run(imp, "FITS...", path);
                }
			frame.setVisible (false);
            savePrefs();
            frame.dispose();
			frame = null;
            if (imp.getCanvas() instanceof AstroCanvas)
                if (((AstroStackWindow)imp.getWindow()).hasWCS())
                    ((AstroStackWindow)imp.getWindow()).setAstroProcessor(true);            
			return;
			}        
		else if (cmd.equals(DELETE))
			{
			int[] selectedrow = table.getSelectedRows();
            int numrows = selectedrow.length;
            if (numrows < 1)
                return;
            if (selectedrow[numrows-1]==header.length - 1)
                numrows--;
            if (numrows < 1)
                return;
            String[][] newHeader = new String[header.length-numrows][5];
            int numfound = 0;
            for (int i=0; i<header.length; i++)
                {
                if (numfound<numrows && i==selectedrow[numfound])
                    numfound++;
                else
                    {
                    newHeader[i-numfound][0] = ""+(i-numfound+1);
                    newHeader[i-numfound][1] = header[i][1];
                    newHeader[i-numfound][2] = header[i][2];
                    newHeader[i-numfound][3] = header[i][3];
                    newHeader[i-numfound][4] = header[i][4];
                    }
                }

            header = newHeader;
            buildTable();
            if (numrows==1) table.changeSelection(selectedrow[0],1,false,false);
			}
		else if (cmd.equals(INSERT))
			{
			int row = table.getSelectedRow();
			if (row < 0 || row == table.getRowCount()-1) row = table.getRowCount()-2;
            String[][] newHeader = new String[header.length+1][5];
            for (int j=0; j<5; j++)
                {
                for (int i=0; i <= row; i++) {newHeader[i][j] = header[i][j];}
                if (j==0) newHeader[row+1][j]=""+(row+2);
                else if (j==1 || j==2 || j==3) newHeader[row+2][j]="test";
                else newHeader[row+1][j]=" ";
                for (int i=row+2; i < newHeader.length; i++)
                    {
                    if (j==0)
                        newHeader[i][j] = ""+(i+1);
                    else
                        newHeader[i][j] = header[i-1][j];
                    }
                }
            header = newHeader;
            buildTable();
            table.changeSelection(row+1,1,false,false);
			}
		}

	protected String[] extractHeader()
		{
		int n=0;
		int l = header.length;
		String[] hdr = new String[l];
		String key,val,comment;

		// COPY BASIC HEADER, CHECKING FOR 80 CHARACTER LIMIT AND FOR EMPTY FIELDS

		for (int i=0; i < l; i++)
			{
			if (header[i][1] != null && (header[i][2] != null || header[i][4].trim().equals("H") ||
                header[i][4].trim().equals("C") || header[i][4].trim().equals("E")))
				{
				key = header[i][1].trim();
                if (key.length() > 8)
                    {
                    IJ.showMessage("Row "+(i+1)+" 'Keyword' entry is too long: length must be <= 8 characters!");
                    table.changeSelection(i,0,false,false);
                    return null;
                    }
				while (key.length() < 8)
                    {
                    key += " ";
                    }
				val = header[i][2] != null ? header[i][2].trim() : "";

                // AKEYWORD=_VALUE_/_COMMENT
                // 1234567890     123		= 10 CHARS FOR KEY, > 8 FOR COMMENT
                if (val.length() > 70)
                    {
                    IJ.showMessage("Row "+(i+1)+" 'Value' entry is too long: length must be <= 70 characters!");
                    table.changeSelection(i,1,false,false);
                    return null;
                    }
                comment = "";
                if (val.length() < 67 && header[i][3] != null)
                    {
                    comment = " / "+header[i][3].trim();
                    if (comment.length() > (67-val.length()))
                        comment = comment.substring(0,67-val.length());
                    while ((val.length()+comment.length()) < 70) {val += " ";}
                    }
                else	{
                    while ((val.length()) < 70) {val += " ";}
                    }
                hdr[n++] = key+(header[i][4].equals("H")||header[i][4].equals("C")||header[i][4].equals("E") ? "" : "= ")+val+comment;
				}
			}
        
		// ADD A HISTORY ENTRY
//        hdr = FitsJ.addHistory("Header modified by AstroIJ FITS editor, Version "+VERSION,hdr);
		return hdr;
                
		}

	// ListListener METHODS

	public void valueChanged (ListSelectionEvent e)
		{
		int i1 = e.getFirstIndex();
		int i2 = e.getLastIndex();
		}

	double parseDouble(String s)
        {
        double defaultValue = Double.NaN;
		if (s==null) return defaultValue;
		try {
			defaultValue = Double.parseDouble(s);
		    }
        catch (NumberFormatException e) {defaultValue = Double.NaN;}
		return defaultValue;
        }

	Integer parseInteger(String s)
        {
        Integer defaultValue = Integer.MIN_VALUE;
		if (s==null) return defaultValue;
		try {
			defaultValue = Integer.parseInt(s);
		    }
        catch (NumberFormatException e) {defaultValue = Integer.MIN_VALUE;}
		return defaultValue;
        }

    void getPrefs()
        {
        frameX = (int) Prefs.get ("fitsedit.frameX",frameX);
        frameY = (int) Prefs.get ("fitsedit.frameY",frameY);
        keywordLock = Prefs.get ("fitsedit.editKeys",keywordLock);
        }

    void savePrefs() {
        Prefs.set ("fitsedit.frameX",frame.getLocation().x);
        Prefs.set ("fitsedit.frameY",frame.getLocation().y);
        Prefs.set ("fitsedit.editKeys",keywordLock);
        }

	/**
	 * A private TableModel for FITS data
	 */
	class FITSTableModel extends AbstractTableModel
		{
		private String[] columnNames = { "#","Keyword","Value","Comment","Type" };
        String keyword;
        String value;
        String comment;
        String type;
		public int getColumnCount() { return 5; }
		public int getRowCount() { return header.length; }
		public String getColumnName(int col) { return columnNames[col]; }
		public Object getValueAt(int row, int col) { return header[row][col]; }

		/*
		 * The 1st and last columns aren't editable, and the 2nd also if ....
		 */
		public boolean isCellEditable(int row, int col)
			{
			if (col == 0) 
                {
                IJ.showMessage("Lines number is set automatically and can not be edited by the user.");
				return false;
                }
			else if (col == 4)
                {
                IJ.showMessage("Value type is set automatically and can not be edited by the user.");
				return false;
                }            
            else if (header != null && header[row][1] != null && (header[row][1].trim().equals("SIMPLE") || header[row][1].trim().equals("BITPIX") || header[row][1].trim().equals("NAXIS") ||
                     header[row][1].trim().equals("NAXIS1") || header[row][1].trim().equals("NAXIS2") || header[row][1].trim().equals("BZERO") ||
                     header[row][1].trim().equals("BSCALE")))
                {
                IJ.showMessage("Changes to Row "+(row+1)+" not allowed. The value for keyword "+header[row][1]+"\n" +
                               "is automatically set by AIJ when the file is saved to disk.");
                return false;
                }
			else if (col == 3 && header[row][4].equals("C")) 
                {
                IJ.showMessage("'Comment' field is not used when keyword is COMMENT. Add comment text to 'Value' field.");
				return false;
                } 
			else if (col == 3 && header[row][4].equals("H")) 
                {
                IJ.showMessage("'Comment' field is not used when keyword is HISTORY. Add history text to 'Value' field.");
				return false;
                }             
			else if (col == 1 && keywordLock && !header[row][4].equals(" "))
                {
                IJ.showMessage("Keyword editing locked. Disable at bottom of editor panel to change keyword.");
				return false;
                }
            else if (row == header.length - 1 && header[row][4].equals("E"))  //don't allow user to edit last row if it contains END card
                {
                IJ.showMessage("When the last row contains keyword END, the row cannot be edited.");
				return false;
                }
			return true;
			}

		public void setValueAt(Object newvalue, int row, int col)
			{
			if (newvalue instanceof String)
				{
                keyword = col == 1 ? ((String)newvalue).toUpperCase().trim() : (header[row][1]==null ? "" : header[row][1].trim());
				value = col == 2 ? ((String)newvalue).trim() : (header[row][2]==null ? "" : header[row][2].trim());
                comment = col == 3 ? ((String)newvalue).trim() : header[row][3];
                type = header[row][4].trim();
                if (col == 1)
                    {
                    if (keyword.equalsIgnoreCase("END"))
                        {
                        IJ.showMessage("Row "+(row+1)+" keyword value 'END' is not allowed!");
                        keyword =  header[row][1] != null ? header[row][1].trim() : "";
                        }
                    else if (keyword.equalsIgnoreCase("SIMPLE") || keyword.equalsIgnoreCase("BITPIX") || keyword.equalsIgnoreCase("NAXIS") ||
                             keyword.equalsIgnoreCase("NAXIS1") || keyword.equalsIgnoreCase("NAXIS2") || keyword.equalsIgnoreCase("BZERO") ||
                             keyword.equalsIgnoreCase("BSCALE"))
                        {
                        IJ.showMessage("New keyword value "+ keyword.toUpperCase().trim() +" is not allowed!\n" +
                                       "This keyword is automatically generated when the file is written to disk.");
                        keyword =  header[row][1] != null ? header[row][1].trim() : "";
                        }                    
                    else if (!keyword.matches("[-A-Z0-9_]{"+keyword.length()+"}"))
                        {
                        IJ.showMessage("Row "+(row+1)+" keyword '"+keyword+"' is not allowed! Valid characters are A-Z_0-9-");
                        keyword =  header[row][1] != null ? header[row][1].trim() : "";
                        }
                    else
                        keyword = keyword.substring(0, keyword.length() < 8 ? keyword.length() : 8).toUpperCase();
                    }
                getType();
				header[row][1] = keyword;
                header[row][2] = value;
                header[row][3] = comment;
                header[row][4] = type;
				changed = true;
				}
			fireTableRowsUpdated(row, row);
			}



        void getType()
            {
            if (keyword.equals(""))
                {
                type = " ";
                }
            else if (keyword.equals("COMMENT"))
                {
                type = "C";
                }
            else if (keyword.equals("HISTORY"))
                {
                type = "H";
                }
            else if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\"")))
                {
                value = "'"+value.substring(1, value.length()-1)+"'";
                type = "S";
                }
            else if (value.equalsIgnoreCase("T") || value.equalsIgnoreCase("F"))
                {
                value = value.toUpperCase();
                type = "B";
                }
            else if (parseInteger(value)!=Integer.MIN_VALUE)
                {
                type = "I";
                }
            else if (!Double.isNaN(parseDouble(value.toUpperCase().replace("D", "E"))))
                {
                value = value.toUpperCase();
                type = "R";
                }
            else
                {
                if (value.startsWith("'") || value.startsWith("\"")) value = value.substring(1, value.length());
                if (value.endsWith("'") || value.endsWith("\"")) value = value.substring(0, value.length()-1);
                value = "'"+value+"'";
                type = "S";
                }
            }
//        public void insertRow()
//            {
//			int row = table.getSelectedRow();
//			if (row < 0) row = table.getRowCount()-1;
//
//
//            }


		}

	}
