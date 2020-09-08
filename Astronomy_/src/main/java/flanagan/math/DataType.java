/*
*   Class   DataType
*
*   USAGE:  Methods for determining object type
*    
*
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:    8-12 April 2012
*   AMENDED: 
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web pages:
*   http://www.ee.ucl.ac.uk/~mflanaga/java/
*   http://www.ee.ucl.ac.uk/~mflanaga/java/DataType.html
*
*   Copyright (c) 2012
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

package flanagan.math;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ArrayDeque;
import java.util.Vector;
import java.math.BigDecimal;
import java.math.BigInteger;

public class DataType{
    
    private final String[] typeList = {"String", "BigDecimal", "BigInteger", "Double", "Float", "Long", "Integer", "Character", "Short", "Byte", "Boolean", "Object", "ArrayList", "LinkedList", "ArrayDeque", "Vector", "Complex", "Phasor", "ErrorProp", "ComplexErrorProp", "Matrix", "ComplexMatrix", "PhasorMatrix", "Polynomial", "ComplexPoly", "ArrayMaths", "VectorMaths", "Point", "BlackBox", "OpenLoop", "ClosedLoop", "Prop", "PropDeriv", "PropInt", "PropIntDeriv", "FirstOrder", "SecondOrder", "Compensator", "LowPassPassive", "HighPassPassive", "DelayLine", "ZeroOrderHold", "Transducer","AtoD", "DtoA"};
    private final int nTypes = this.typeList.length;
    private final int nNumerical = 9;               // number of numerical types for inclusion in highest precision replacement
    private Object obj = null;                      // entered Object
    private String objTypeName = null;              // Object type name
    private int objTypeDim = 0;                     // number of dimensions of an Objects arrays
    private int objTypeCode = -1;                   // object type code
    private boolean typeFound = false;              // = true when Object type name, code and dimensions found
    private Object[] arrayObjects = null;           // array Objects
    private String[] arrayNames = null;             // array type names
    private int[] arrayDims = null;                 // array numbers of dimensions 
    private int[] arrayCodes = null;                // array codes
    private int nArrayElements = 0;                 // number of array elements
    private boolean arrayDone = false;              // = true when an array analysed
    private int arrayFlag = -1;                     // array flag
                                                    // = 0: object does not contain an array, i.e. is not an ArrayList, ArrayDeque, LinkedList or Vector
                                                    // = 1: Object contains an array, e.g is ArrayList, and the number of dimensions = 0 and all elements are numerically interconvertable
                                                    // = 2: Object contains an array, e.g is ArrayList, and the number of dimensions > 0
                                                    // = 3: Object contains an array but not all elements are numerically interconvertable
    private boolean highestDone = false;            // = true whwn highest precision name found
    private String highestName  = null;             // highest precision name
    private int highestCode  = -1;                  // highest precision code
    private Object highestArray = null;             // highest precision converted array
    
    // CONSTRUCTORS
    public DataType(){
    }
    
    public DataType(Object obj){
        this.obj = obj;
        this.inner();      
    }

     
    // TYPE NAME
    // Type name returned as name or name[]...
    // Instance method
    public final String getTypeName(){
        if(!this.typeFound)this.inner();
        return this.objTypeName;
    }

    // Static method
    public static String getTypeName(Object obj){
        DataType objType = new DataType(obj);
        return objType.getTypeName();
    }
    
    // private method -> type name code and dimension
    private void inner(){
        
        this.arrayFlag = 0;
        String fullname = obj.getClass().getName();
        
        // Dimension
        boolean test = true;
        this.objTypeDim = 0;
        int pos0 = -1;
        int pos1 = 0;
        while(test){
            pos0 = fullname.indexOf("[", pos1);
            if(pos0==-1){
                test = false;
            }
            else{
                pos1 = pos0+1;
                this.objTypeDim++;
            }
        }
        
        
        // Type Name
        this.objTypeName = null;
        test = true;
        while(test){
            pos0 = fullname.lastIndexOf(".");
            this.objTypeName = fullname.substring(pos0+1);
            if(fullname.charAt(fullname.length()-1)==';')this.objTypeName = this.objTypeName.substring(0,this.objTypeName.length()-1);
            test = false;
        }
        for(int j=0;j<objTypeDim;j++)this.objTypeName += "[]";
         
        // Code
        test = true;
        int counter0 = 0;
        while(test){
            if(fullname.indexOf(this.typeList[counter0])!=-1){
                this.objTypeCode = counter0;
                test = false;
            }
            else{
                counter0++;
                if(counter0>=this.nTypes){
                    String rearrName = null;
                    if(this.objTypeDim==0){
                        rearrName = fullname;
                    }
                    else{
                        rearrName = fullname.substring(this.objTypeDim+1, fullname.length()-1);
                    }
                    System.out.println("Method Object.getTypeCode: the object, " + rearrName + ", is not included in the Conv class list of objects, -1 returned");
                    test = false;
                }
            }
        } 
               
        this.arrayInner();
        
        this.typeFound = true;
        
    }
    
    // private method -> array names, codes and dimensions
    private void arrayInner(){
        this.arrayNames = null;
        this.arrayCodes = null;
        this.arrayDims = null;
        
        if(this.objTypeName.equals("ArrayList")){
            this.arrayFlag = 1;
            ArrayList al = (ArrayList)this.obj;
            this.nArrayElements = al.size();
            this.arrayObjects = al.toArray();
            this.fillArray();
        }
        else{
            if(this.objTypeName.equals("ArrayDeque")){
                this.arrayFlag = 1;
                ArrayDeque ad = (ArrayDeque)this.obj;
                this.nArrayElements = ad.size();
                this.arrayObjects = ad.toArray();
                this.fillArray();
            }
            else{
                if(this.objTypeName.equals("LinkedList")){
                    this.arrayFlag = 1;
                    LinkedList ll = (LinkedList)this.obj;
                    this.nArrayElements = ll.size();
                    this.arrayObjects = ll.toArray();
                    this.fillArray();
                }
                else{
                    if(this.objTypeName.equals("Vector")){
                        this.arrayFlag = 1;
                        Vector vv = (Vector)this.obj;
                        this.nArrayElements = vv.size();
                        this.arrayObjects = vv.toArray();
                        this.fillArray();
                    }
                    else{
                        if(this.objTypeName.equals("Object[]")){
                            this.arrayFlag = 1;
                            this.arrayObjects = (Object[])this.obj;
                            this.nArrayElements = this.arrayObjects.length;
                            this.fillArray();
                        }
                    }
                }
            }
        }
 
        this.arrayDone = true;
    }
    
    private void fillArray(){
        this.arrayNames = new String[this.nArrayElements];
        this.arrayCodes = new int[this.nArrayElements];
        this.arrayDims = new int[this.nArrayElements];
        Object[] retd = null;
        for(int i=0; i<this.nArrayElements; i++){
                retd = DataType.getTypeData(this.arrayObjects[i]);
                this.arrayNames[i] = (String)retd[0];
                this.arrayDims[i] = (Integer)retd[1];
                this.arrayCodes[i] = (Integer)retd[2];
       }
        
        for(int i=0; i<this.nArrayElements; i++){  
            if(this.arrayCodes[i]<10){
                if(this.arrayDims[i]>0){
                    this.arrayFlag = 2;
                }
            }
            else{
                this.arrayFlag = 3;
            }
            if(this.arrayFlag==2 || this.arrayFlag==3)break;
        }
    }
                
    
    // GET TYPE CODE
    // Code returned as index of typeList name 
    // instance method
    public final int getTypeCode(){
        if(!this.typeFound)this.inner();
        return this.objTypeCode;
    }
        
    // Static method
    public static int getTypeCode(Object obj){
        DataType objType = new DataType(obj);
        return objType.getTypeCode();
    }
  
    
    // GET NUMBER OF DIMENSIONS
    // Code returned as index  intof typeList name 
    public final int getTypeDimension(){
        if(!this.typeFound)this.inner();
        return this.objTypeDim;
    }
        
    // Static method
    public static int getTypeDimension(Object obj){
        DataType objType = new DataType(obj);
        return objType.getTypeDimension();
    }
    
    
    // GET ALL TYPE DATA
    // -> type name, dimension, code 
    public final Object[] getTypeData(){
        Object[] ret = new Object[3];
        if(!this.typeFound)this.inner();
        ret[0] = (Object)this.objTypeName;
        ret[1] = (Object)(new Integer(this.objTypeDim));
        ret[2] = (Object)(new Integer(this.objTypeCode));
        return ret;
    }
        
    // Static method
    public static Object[] getTypeData(Object obj){
        DataType objType = new DataType(obj);
        return objType.getTypeData();
    }
  

    
      
    
    // ARRAY ELEMENT NAMES
    // instant method
    public String[] getArrayNames(){
        String[] ret = null;
        
        if(!this.arrayDone)this.arrayInner();
        switch(this.arrayFlag){
            case 0: System.out.println("Method DataType.getArayNames: The entered Object is not a collection  - null returned");
                    break;
            case 1: ret = this.arrayNames;
                    break;
            case 2: System.out.println("Method DataType.getArayNames: The entered Object is an array of collections  - null returned");
                    break;
        }
        return ret;
    }
        
    // static method
    public static String[] getArrayNames(Object al){
        DataType objType = new DataType(al);
        return objType.getArrayNames();
    }
    
    
    
    // ARRAY ELEMENT DIMENSIONS
    // instant method
    public int[] getArrayDimensions(){
        int[] ret = null;
        
        if(!this.arrayDone)this.arrayInner();
        switch(this.arrayFlag){
            case 0: System.out.println("Method DataType.getArayDimensions: The entered Object is not a collection  - null returned");
                    break;
            case 1: 
            case 2: ret = this.arrayDims;
                    break;
        }
        return ret;
    }
        
    // static method
    public static int[] getArrayDimensions(Object al){
        DataType objType = new DataType(al);
        return objType.getArrayDimensions();
    }
    

    // ARRAY ELEMENT CODES
    // instant method
    public int[] getArrayCodes(){
        int[] ret = null;
        
        if(!this.arrayDone)this.arrayInner();
        switch(this.arrayFlag){
            case 0: System.out.println("Method DataType.getArrayCodes: The entered Object is not a collection  - null returned");
                    break;
            case 1: 
            case 2: ret = this.arrayCodes;
        }
        return ret;
    }
        
    // static method
    public static int[] getArrayCodes(Object al){
        DataType objType = new DataType(al);
        return objType.getArrayCodes();
    }
    

    // ALL ARRAY ELEMENT DATA
    // -> type name, dimension, code for all elements
    // instant method
    public final ArrayList<Object> getArrayData(){
        if(!this.typeFound)this.inner();
        ArrayList<Object> ret = new ArrayList<Object>();
        ret.add(this.arrayNames);
        ret.add(this.arrayDims);
        ret.add(this.arrayCodes);   
        return ret;
    }
    
    // static method
    public static ArrayList<Object> getArrayData(Object al){
        DataType objType = new DataType(al);
        return objType.getArrayData();
    }
    
    
    
    
    
    // HIGHEST PRECISION PRESENT
    // Return highest precision name
    // instance method
    public String highestPrecisionName(){
        String name = null;
        if(this.arrayFlag==0){
            System.out.println("Method DataType.highestPrecisionName: The entered Object does not contain a collection - null returned");
        } 
        else{
            if(!highestDone)this.toHighestPrecision();
            name = this.highestName;
       }      
       return name;
    }
    
    // static method
    public static String highestPrecisionName(Object obj){
        DataType objt = new DataType(obj);
        return objt.highestPrecisionName();
    }
    
    // Return highest precision code
    // instance method
    public int highestPrecisionCode(){
        int code = -1;
        if(this.arrayFlag==0){
            System.out.println("Method DataType.highestPrecisionCode: The entered Object does not contain a collection - null returned");
        } 
        else{
            if(!highestDone)this.toHighestPrecision();
            code = this.highestCode;
       }      
       return code;
    }
    
    // static method
    public static int highestPrecisionCode(Object obj){
        DataType objt = new DataType(obj);
        return objt.highestPrecisionCode();
    }
    
    // Return an array of the elements of the entered Object converted to the precision of the element of highest precision
    //instance method
    //instance method
    public Object convert_to_highestPrecision(){
        return this.convertToHighestPrecision();
    }
    
    public Object convertToHighestPrecision(){
        Object ret = null;
        switch(this.arrayFlag){
            case 0: System.out.println("Method DataType.convert_to_highestPrecision: The entered Object is not contain a collection - null returned");
                    break;
            case 2: System.out.println("Method DataType.convert_to_highestPrecision: The entered Object collection contains dimensioned elements - null returned"); 
                    break;
            case 3: System.out.println("Method DataType.convert_to_highestPrecision The entered Object array contains an element that is not numerically interconvertable - null returned");
                    break;
            case 1: if(!highestDone)this.toHighestPrecision();
                    ret = this.highestArray;
                    break;
        }
        return ret;
    }
    
    // static method
    public static Object convert_to_highestPrecision(Object obj){
        DataType objt = new DataType(obj);
        return objt.convertToHighestPrecision();
    }
        
    public static Object convertToHighestPrecision(Object obj){
        DataType objt = new DataType(obj);
        return objt.convertToHighestPrecision();
    }
    
    // private method to find highestprecision name and to convert array of objects to highest precision if possible
    private void toHighestPrecision(){
            if(!this.typeFound)this.inner();
            int minCode = this.minCode(this.arrayCodes, this.arrayNames);
            this.highestName = this.typeList[minCode];
            this.highestCode = minCode;       
            if(this.arrayFlag==1){
                    switch(minCode){
                        case 1: BigDecimal[] bd = new BigDecimal[this.nArrayElements]; 
                                for(int i=0; i<this.nArrayElements;i++){
                                    switch(this.arrayCodes[i]){
                                        case 0: bd[i] = Conv.convert_String_to_BigDecimal((String)this.arrayObjects[i]);
                                                break;
                                        case 1: bd[i] = (BigDecimal)(this.arrayObjects[i]);
                                                break;
                                        case 2: bd[i] = Conv.convert_BigInteger_to_BigDecimal((BigInteger)this.arrayObjects[i]);
                                                break;    
                                        case 3: bd[i] = Conv.convert_Double_to_BigDecimal((Double)this.arrayObjects[i]);
                                                break;     
                                        case 4: bd[i] = Conv.convert_Float_to_BigDecimal((Float)this.arrayObjects[i]);
                                                break;
                                        case 5: bd[i] = Conv.convert_Long_to_BigDecimal((Long)this.arrayObjects[i]);
                                                break;
                                        case 6: bd[i] = Conv.convert_Integer_to_BigDecimal((Integer)this.arrayObjects[i]);
                                                break; 
                                        case 7: bd[i] = Conv.convert_Character_to_BigDecimal((Character)this.arrayObjects[i]);
                                                break;
                                        case 8: bd[i] = Conv.convert_Short_to_BigDecimal((Short)this.arrayObjects[i]);
                                                break;
                                        case 9: bd[i] = Conv.convert_Byte_to_BigDecimal((Byte)this.arrayObjects[i]);
                                                break;
                                    }
                                }
                                this.highestArray = (Object)bd;
                                break;
                        case 2: BigInteger[] bi = new BigInteger[this.nArrayElements]; 
                                for(int i=0; i<this.nArrayElements;i++){
                                    switch(this.arrayCodes[i]){
                                        case 0: bi[i] = Conv.convert_String_to_BigInteger((String)this.arrayObjects[i]);
                                                break;
                                        case 2: bi[i] = (BigInteger)this.arrayObjects[i];
                                                break;    
                                        case 3: bi[i] = Conv.convert_Double_to_BigInteger((Double)this.arrayObjects[i]);
                                                break;     
                                        case 4: bi[i] = Conv.convert_Float_to_BigInteger((Float)this.arrayObjects[i]);
                                                break;
                                        case 5: bi[i] = Conv.convert_Long_to_BigInteger((Long)this.arrayObjects[i]);
                                                break;
                                        case 6: bi[i] = Conv.convert_Integer_to_BigInteger((Integer)this.arrayObjects[i]);
                                                break;   
                                        case 7: bi[i] = Conv.convert_Character_to_BigInteger((Character)this.arrayObjects[i]);
                                                break;
                                        case 8: bi[i] = Conv.convert_Short_to_BigInteger((Short)this.arrayObjects[i]);
                                                break;
                                        case 9: bi[i] = Conv.convert_Byte_to_BigInteger((Byte)this.arrayObjects[i]);
                                                break;
                                    }
                                }
                                this.highestArray = (Object)bi;
                                break;
                        case 3: Double[] dd = new Double[this.nArrayElements]; 
                                for(int i=0; i<this.nArrayElements;i++){
                                    switch(this.arrayCodes[i]){    
                                        case 0: dd[i] = Conv.convert_String_to_Double((String)this.arrayObjects[i]);
                                                break;                                     
                                        case 3: dd[i] = (Double)this.arrayObjects[i];
                                                break;     
                                        case 4: dd[i] = Conv.convert_Float_to_Double((Float)this.arrayObjects[i]);
                                                break;
                                        case 5: dd[i] = Conv.convert_Long_to_Double((Long)this.arrayObjects[i]);
                                                break;
                                        case 6: dd[i] = Conv.convert_Integer_to_Double((Integer)this.arrayObjects[i]);
                                                break;   
                                        case 7: dd[i] = Conv.convert_Character_to_Double((Character)this.arrayObjects[i]);
                                                break;
                                        case 8: dd[i] = Conv.convert_Short_to_Double((Short)this.arrayObjects[i]);
                                                break;
                                        case 9: dd[i] = Conv.convert_Byte_to_Double((Byte)this.arrayObjects[i]);
                                                break;
                                    }
                                }
                                this.highestArray = (Object)dd;
                                break;
                        case 4: Float[] ff = new Float[this.nArrayElements]; 
                                for(int i=0; i<this.nArrayElements;i++){
                                    switch(this.arrayCodes[i]){    
                                        case 0: ff[i] = Conv.convert_String_to_Float((String)this.arrayObjects[i]);
                                                break;    
                                        case 4: ff[i] = (Float)this.arrayObjects[i];
                                                break;
                                        case 5: ff[i] = Conv.convert_Long_to_Float((Long)this.arrayObjects[i]);
                                                break;
                                        case 6: ff[i] = Conv.convert_Integer_to_Float((Integer)this.arrayObjects[i]);
                                                break;   
                                        case 7: ff[i] = Conv.convert_Character_to_Float((Character)this.arrayObjects[i]);
                                                break;
                                        case 8: ff[i] = Conv.convert_Short_to_Float((Short)this.arrayObjects[i]);
                                                break;
                                        case 9: ff[i] = Conv.convert_Byte_to_Float((Byte)this.arrayObjects[i]);
                                                break;
                                    }
                                }
                                this.highestArray = (Object)ff;
                                break;
                        case 5: Long[] ll = new Long[this.nArrayElements]; 
                                for(int i=0; i<this.nArrayElements;i++){
                                    switch(this.arrayCodes[i]){    
                                        case 0: ll[i] = Conv.convert_String_to_Long((String)this.arrayObjects[i]);
                                                break;            
                                        case 5: ll[i] = (Long)this.arrayObjects[i];
                                                break;
                                        case 6: ll[i] = Conv.convert_Integer_to_Long((Integer)this.arrayObjects[i]);
                                                break;   
                                        case 7: ll[i] = Conv.convert_Character_to_Long((Character)this.arrayObjects[i]);
                                                break;
                                        case 8: ll[i] = Conv.convert_Short_to_Long((Short)this.arrayObjects[i]);
                                                break;
                                        case 9: ll[i] = Conv.convert_Byte_to_Long((Byte)this.arrayObjects[i]);
                                                break;
                                    }
                                }
                                this.highestArray = (Object)ll;
                                break;
                        case 6: Integer[] ii = new Integer[this.nArrayElements]; 
                                for(int i=0; i<this.nArrayElements;i++){
                                    switch(this.arrayCodes[i]){    
                                        case 0: ii[i] = Conv.convert_String_to_Integer((String)this.arrayObjects[i]);
                                                break;  
                                        case 6: ii[i] = (Integer)this.arrayObjects[i];
                                                break; 
                                        case 7: ii[i] = Conv.convert_Character_to_Integer((Character)this.arrayObjects[i]);
                                                break;
                                        case 8: ii[i] = Conv.convert_Short_to_Integer((Short)this.arrayObjects[i]);
                                                break;
                                        case 9: ii[i] = Conv.convert_Byte_to_Integer((Byte)this.arrayObjects[i]);
                                                break;
                                    }
                                }
                                this.highestArray = (Object)ii;
                                break;
                        case 7: Integer[] ci = new Integer[this.nArrayElements]; 
                                for(int i=0; i<this.nArrayElements;i++){
                                    switch(this.arrayCodes[i]){    
                                        case 0: ci[i] = Conv.convert_String_to_Integer((String)this.arrayObjects[i]);
                                                break;   
                                        case 7: ci[i] =Conv.convert_Character_to_Integer((Character)this.arrayObjects[i]);
                                                break;
                                        case 8: ci[i] = Conv.convert_Short_to_Integer((Short)this.arrayObjects[i]);
                                                break;
                                        case 9: ci[i] = Conv.convert_Byte_to_Integer((Byte)this.arrayObjects[i]);
                                                break;
                                    }
                                }
                                this.highestArray = (Object)ci;
                                break;
                        case 8: Short[] ss = new Short[this.nArrayElements]; 
                                for(int i=0; i<this.nArrayElements;i++){
                                    switch(this.arrayCodes[i]){    
                                        case 0: ss[i] = Conv.convert_String_to_Short((String)this.arrayObjects[i]);
                                                break;   
                                        case 8: ss[i] = (Short)this.arrayObjects[i];
                                                break;
                                        case 9: ss[i] = Conv.convert_Byte_to_Short((Byte)this.arrayObjects[i]);
                                                break;
                                    }
                                }
                                this.highestArray = (Object)ss;
                                break;
                        case 9: Byte[] bb = new Byte[this.nArrayElements]; 
                                for(int i=0; i<this.nArrayElements;i++){
                                    switch(this.arrayCodes[i]){    
                                        case 0: bb[i] = Conv.convert_String_to_Byte((String)this.arrayObjects[i]);
                                                break; 
                                        case 9: bb[i] = (Byte)this.arrayObjects[i];
                                                break;
                                    }
                                }
                                this.highestArray = (Object)bb;
                                break;
                    }
                    this.highestDone = true;
            }
    }

    
    private int minCode(int[] codeT, Object[] objList){
        int nMin = -1;
        int retI = -1;
        int n = codeT.length;
        
        int counter = 0;
        boolean test0 = true;
        boolean test1 = true;
   
        while(test0){
            if(codeT[counter]>this.nNumerical){
                System.out.println("The entered Object collection contains Objects that the convert to highest precision methods cannot process - null returned");
                test0 = false;
                test1 = false;
            }
            else{
                counter++;
                if(counter>=n)test0 = false;
            }
        }
        for(int i=0; i<n; i++)if(codeT[i]==8)codeT[i]=6;
        if(test1){
            boolean test2 = true;
            nMin = Fmath.minimum(codeT);
            if(nMin==0){
                ArrayList<Integer> al = new ArrayList<Integer>();
                int nn = 0;
                for(int i=0; i<n; i++){
                    if(codeT[i]==0){
                        al.add(new Integer(i));
                        nn++;
                    }
                }
                for(int i=0; i<nn; i++){
                    retI = -1;
                    int ii = al.get(i);
                    try{
                        BigDecimal xbd = new BigDecimal(((String)objList[ii]).trim());
                        retI = 1;
                        try{
                            BigInteger xbi = new BigInteger(((String)objList[ii]).trim());
                            retI = 2;
                            try{
                                double xdl = Double.valueOf((String)objList[ii]);
                                retI = 3;
                                try{
                                    float xf = Float.valueOf((String)objList[ii]);
                                    retI = 4;
                                    try{
                                        long xl = Long.valueOf((String)objList[ii]);
                                        retI = 5;
                                        try{
                                            int xi = Integer.valueOf((String)objList[ii]);
                                            retI = 6;
                                            try{
                                                short xs = Short.valueOf((String)objList[ii]);
                                                retI = 7;
                                                try{
                                                    byte xb = Byte.valueOf((String)objList[ii]);
                                                    retI = 9;
                                                }catch(NumberFormatException err){
                                                    test2 = false;
                                                }
                                            }catch(NumberFormatException err){
                                                test2 = false;
                                            }
                                        }catch(NumberFormatException err){
                                            test2 = false;
                                        }
                                    }catch(NumberFormatException err){
                                        test2 = false;
                                    }
                                }catch(NumberFormatException err){
                                    test2 = false;
                                }
                            }catch(NumberFormatException err){
                                test2 = false;
                            }
                        }catch(NumberFormatException err){
                            test2 = false;
                        }
                    }catch(NumberFormatException err){
                        test2 = false;
                    }
                    codeT[ii] = retI;
                }   
            }
        }
        nMin = Fmath.minimum(codeT);
        
        if(nMin==2){
            boolean test = true;
            counter =  0;
            while(test){
                if(codeT[counter]>2 && codeT[counter]<5){
                    nMin = 1;
                    test = false;
                } 
                else{
                    counter++;
                    if(counter>=n)test = false;
                }
            }
        }
        return nMin;
    }
    
    // CONVERT TO double
    // Return an array of the elements of the entered Object converted to double
    //instance method
    public double[]  convert_to_double(){
        double[] ret = null;
        switch(this.arrayFlag){
            case 0: System.out.println("Method DataType.convert_to_double: The entered Object is not contain a collection - null returned");
                    break;
            case 2: System.out.println("Method DataType.convert_to_double: The entered Object collection contains dimensioned elements - null returned"); 
                    break;
            case 3: System.out.println("Method DataType.convert_to_double: The entered Object array contains an element that is not numerically interconvertable - null returned");
                    break;
            case 1: if(!highestDone)this.toHighestPrecision();
                    ret = new double[this.nArrayElements];
                    switch(this.highestCode){
                        case 1: BigDecimal[] bd = (BigDecimal[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_BigDecimal_to_double(bd[i]);
                                break;
                        case 2: BigInteger[] bi = (BigInteger[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_BigInteger_to_double(bi[i]);
                                break; 
                        case 3: Double[] dd = (Double[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = dd[i].doubleValue();
                                break; 
                        case 4: Float[] ff = (Float[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Float_to_double(ff[i]);
                                break;
                        case 5: Long[] ll = (Long[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Long_to_double(ll[i]);
                                break;    
                        case 6: Integer[] ii = (Integer[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Integer_to_double(ii[i]);
                                break; 
                        case 8: Short[] ss = (Short[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Short_to_double(ss[i]);
                                break;     
                        case 9: Byte[] bb = (Byte[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Byte_to_double(bb[i]);
                                break;   
                    }
        }  
        return ret;
    }
    
    // static method
    public static double[] convert_to_double(Object obj){
        DataType dt = new DataType(obj);
        return dt.convert_to_double();
    }
    
       
    // CONVERT TO BigDecimal
    // Return an array of the elements of the entered Object converted to BigDecimal
    //instance method
    public BigDecimal[] convert_to_BigDecimal(){
        BigDecimal[] ret = null;
        switch(this.arrayFlag){
            case 0: System.out.println("Method DataType.convert_to_BigDecimal: The entered Object is not contain a collection - null returned");
                    break;
            case 2: System.out.println("Method DataType.convert_to_BigDecimal: The entered Object collection contains dimensioned elements - null returned"); 
                    break;
            case 3: System.out.println("Method DataType.convert_to_BigDecimal: The entered Object array contains an element that is not numerically interconvertable - null returned");
                    break;
            case 1: if(!highestDone)this.toHighestPrecision();
                    ret = new BigDecimal[this.nArrayElements];
                    switch(this.highestCode){
                        case 1: ret = (BigDecimal[])this.convert_to_highestPrecision();
                                break;
                        case 2: BigInteger[] bi = (BigInteger[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_BigInteger_to_BigDecimal(bi[i]);
                                break; 
                        case 3: Double[] dd = (Double[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Double_to_BigDecimal(dd[i]);
                                break; 
                        case 4: Float[] ff = (Float[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Float_to_BigDecimal(ff[i]);
                                break;
                        case 5: Long[] ll = (Long[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Long_to_BigDecimal(ll[i]);
                                break;    
                        case 6: Integer[] ii = (Integer[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Integer_to_BigDecimal(ii[i]);
                                break; 
                        case 8: Short[] ss = (Short[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Short_to_BigDecimal(ss[i]);
                                break;     
                        case 9: Byte[] bb = (Byte[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Byte_to_BigDecimal(bb[i]);
                                break;   
                    }
        }  
        return ret;
    }
    
    // static method
    public static BigDecimal[] convert_to_BigDecimal(Object obj){
        DataType dt = new DataType(obj);
        return dt.convert_to_BigDecimal();
    }
    
  
    // CONVERT TO String
    // Return an array of the elements of the entered Object converted to String
    //instance method
    public String[] convert_to_String(){
        String[] ret = null;
        switch(this.arrayFlag){
            case 0: System.out.println("Method DataType.convert_to_String: The entered Object is not contain a collection - null returned");
                    break;
            case 2: System.out.println("Method DataType.convert_to_String: The entered Object collection contains dimensioned elements - null returned"); 
                    break;
            case 3: System.out.println("Method DataType.convert_to_String: The entered Object array contains an element that is not numerically interconvertable - null returned");
                    break;
            case 1: if(!highestDone)this.toHighestPrecision();
                    ret = new String[this.nArrayElements];
                    switch(this.highestCode){
                        case 1: BigDecimal[] bd = (BigDecimal[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_BigDecimal_to_String(bd[i]);
                                break;
                        case 2: BigInteger[] bi = (BigInteger[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_BigInteger_to_String(bi[i]);
                                break; 
                        case 3: Double[] dd = (Double[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Double_to_String(dd[i]);
                                break; 
                        case 4: Float[] ff = (Float[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Float_to_String(ff[i]);
                                break;
                        case 5: Long[] ll = (Long[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Long_to_String(ll[i]);
                                break;    
                        case 6: Integer[] ii = (Integer[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Integer_to_String(ii[i]);
                                break; 
                        case 8: Short[] ss = (Short[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Short_to_String(ss[i]);
                                break;     
                        case 9: Byte[] bb = (Byte[])this.convert_to_highestPrecision();
                                for(int i=0; i<this.nArrayElements; i++)ret[i] = Conv.convert_Byte_to_String(bb[i]);
                                break;   
                    }
        }  
        return ret;
    }
    
    // static method
    public static String[] convert_to_String(Object obj){
        DataType dt = new DataType(obj);
        return dt.convert_to_String();
    }
    
}