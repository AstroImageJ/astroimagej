/*
*   Class   FileManipulate
*
*   This class contains easy to use methods for reading in a file
*   character by character, including non-print characters, e.g. line return character
*   as a character (char), a wrapper class character (Character) or as the ISO code int equivalent (int)
*   and, consequently, of editing the file, e.g returning a file of a designated line length
*   or a file of a single line
*
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:       20 May 2012 (Incorporates FileInputAsChar [13 September 2005])
*   REVISED:
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web page:
*   http://www.ee.ucl.ac.uk/~mflanaga/java/FileManipulate.html
*   http://www.ee.ucl.ac.uk/~mflanaga/java/
*
*   Copyright (c) 2005 - 2012
*
*   PERMISSION TO COPY:
*   Permission to use, copy and modify this software and its documentation for
*   NON-COMMERCIAL purposes is granted, without fee, provided that an acknowledgement
*   to the author, Michael Thomas Flanagan at www.ee.ucl.ac.uk/~mflanaga, appears in all copies.
*
*   Dr Michael Thomas Flanagan makes no representations about the suitability
*   or fitness of the software for any or for a particular purpose.
*   Michael Thomas Flanagan shall not be liable for any damages suffered
*   as a result of using, modifying or distributing this software or its derivatives.
*
***************************************************************************************/

package flanagan.io;

import java.io.*;

public class FileManipulate{

        // Instance variables
        protected String fileName = "";           //input file name
        protected String stemName = "";           //input file name without extension
        protected String pathName = "";           //input file path name
        protected String dirPath = "";            //path to directory containing input file
        protected BufferedReader input = null;    //instance of BufferedReader
        protected boolean testFullLine = false;   //false if fullLine is empty
        protected boolean testFullLineT = false;  //false if fullLineT is empty
        protected boolean eof = false;            //true if reading beyond end of file attempted
        protected boolean fileFound = true;       //true if file named is found

        // Constructor
        public FileManipulate(String pathName){
                this.pathName = pathName;
                this.fileName = pathName;
                int posSlash = pathName.indexOf("//");
                int posBackSlash = pathName.indexOf("\\");
                if(posSlash!=-1 || posBackSlash!=-1){
                    File file = new File(this.pathName);
                    this.fileName = file.getName();
                    this.dirPath = (file.getParentFile()).toString();
                }
                int posDot = this.fileName.indexOf('.');
                if(posDot==-1){
                    this.stemName = this.fileName;
                }
                else{
                    this.stemName = this.fileName.substring(0, posDot);
                }

                try{
                    this.input = new BufferedReader(new FileReader(this.pathName));
                }catch(java.io.FileNotFoundException e){
                    System.out.println(e);
                    fileFound=false;
                }
        }

        // Methods

        // Get file path
        public String getPathName(){
            return this.pathName;
        }

        // Get file name
        public String getFileName(){
            return this.fileName;
        }

        // Get file name without the extension
        public String getStemName(){
            return this.stemName;
        }

        // Get path to directory containing the file
        public String getDirPath(){
            return this.dirPath;
        }

        // Reads the next character from the file returning it as a char
        public final synchronized char readchar(){
                int ich = -1;
                char ch = '\u0000';
                try{
                        ich = input.read();
                }catch(java.io.IOException e){
                        System.out.println(e);
                }
                if(ich==-1){
                    System.out.println("FileManip.readchar:  attempt to read beyond end of file");
                    eof = true;
                    ch = '\u0000';
                }
                else{
                    ch = (char) ich;
                }
                return ch;
        }

        // Reads the next character from the file returning it as a Char
        public final synchronized Character readCharacter(){
                int ich = -1;
                char ch = '\u0000';
                Character wch = null;

                try{
                        ich = input.read();
                }catch(java.io.IOException e){
                        System.out.println(e);
                }
                if(ich==-1){
                    System.out.println("FileManip.readChar:  attempt to read beyond end of file");
                    eof = true;
                    ch = '\u0000';
                    wch = null;
                }
                else{
                    ch = (char) ich;
                    wch = new Character(ch);
                }
                return wch;
        }

        // Reads the next character from the file returning it as the ISO int code
        public final synchronized int readint(){
                int ch = -1;
                try{
                        ch = input.read();
                }catch(java.io.IOException e){
                        System.out.println(e);
                }
                if(ch==-1){
                    System.out.println("FileManip.readint:  attempt to read beyond end of file");
                    eof = true;
                }
                return ch;
        }

        // Close file
        public final synchronized void close(){
            if(fileFound){
                try{
                        input.close();
                }catch(java.io.IOException e){
                        System.out.println(e);
                }
            }
        }

        // Get the end of file status, eof.
        public boolean eof(){
            return eof;
        }

        // Get the file existence status, fileFound.
        public boolean fileFound(){
            return fileFound;
        }
        
        // Converts all the lines in a file to a single line without control characters
        public String toSingleLine(){
            String single = "";
            int ch = '\u0000';
            boolean test = true;
            while(test){
                
                try{
                    ch = input.read();
                }catch(java.io.IOException e){
                    test = false;
                }
                if(ch==-1){
                    //System.out.println("FileManip.readint:  attempt to read beyond end of file");
                    eof = true;
                    test = false;
                }
                if(test){
                    if(ch>31)single += (char)ch;
                }
            }   
            return single;
        }
        
        // Converts all the lines in a file to a single line without control characters
        // and outputs the single line as a txt file 
        // file name not provided as an argument
        public void toSingleLineFile(){
            String filename = this.stemName + "SingleLine" + ".txt";
            filename = Db.readLine("Input the single line filename", filename);
            this.toSingleLineFile(filename);            
        }
        
        // Converts all the lines in a file to a single line without control characters
        // and outputs the single line as a txt file of given file name
        public void toSingleLineFile(String filename){
            FileOutput fout = new FileOutput(filename);
            String line = this.toString();
            fout.println(line);
            fout.close();
        }

        
        // Converts all the lines to a length of lineLength characters or nearest without breaking a word 
        // file name not provided as an argument
        public void toLineLength(int requiredLineLength){
            String filename = this.stemName + "LineLength" + requiredLineLength + ".txt";
            filename = Db.readLine("Input the output filename", filename);
            this.toLineLength(requiredLineLength, filename);            
        }
        
        // Converts all the lines to a length of lineLength characters or nearest without breaking a word
        // output file name supplied
        public void toLineLength(int requiredLineLength, String filename){
            FileOutput fout = new FileOutput(filename);
            String single = this.toSingleLine();
            String line = single;
            String hold = null;
            int currentLineLength = -1;
            boolean test = true;
            int ii = 0;
            while(test){
                
                currentLineLength = line.length();
                if(currentLineLength<=requiredLineLength){
                    fout.println(line);
                    test = false;
                }
                else{
                    if(line.charAt(requiredLineLength)==' '){                  
                        fout.println(line.substring(0, requiredLineLength)); 
                        if(requiredLineLength+1<currentLineLength){
                            line = line.substring(requiredLineLength+1);
                            currentLineLength=line.length();
                                    
                        }
                        else{
                            test = false;
                        }
                    }
                    else{
                        hold = line.substring(0, requiredLineLength); 
                        int pos0 = hold.lastIndexOf(' ');
                        fout.println(hold.substring(0, pos0));
                        if(pos0+1<currentLineLength){
                            line = line.substring(pos0+1);
                            currentLineLength=line.length();
                        }
                        else{
                            test = false;
                        }
                        
                    }
                }
            }
            fout.close();
        }
        
}
