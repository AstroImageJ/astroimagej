/*
*   Class   DeepCopy
*
*   USAGE:  Methods for deep copying of an Object and of one, two and three dimensional arrays
*
*   WRITTEN BY: Dr Michael Thomas Flanagan
*
*   DATE:    10-11 April 2012 (Methods taken fron Conv.java)
*   AMENDED: 
*
*   DOCUMENTATION:
*   See Michael Thomas Flanagan's Java library on-line web pages:
*   http://www.ee.ucl.ac.uk/~mflanaga/java/
*   http://www.ee.ucl.ac.uk/~mflanaga/java/DeepCopy.html
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

import java.math.*;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import flanagan.complex.Complex;
import flanagan.complex.ComplexPoly;
import flanagan.complex.ComplexMatrix;
import flanagan.complex.ComplexErrorProp;
import flanagan.analysis.ErrorProp;
import flanagan.circuits.Phasor;


public class DeepCopy{
  
    // CONSTRUCTORS
    public DeepCopy(){
    }
    
    // COPY
    
        // COPY A ONE DIMENSIONAL ARRAY OF double
        public static double[] copy(double[] array){
            if(array==null)return null;
            int n = array.length;
            double[] copy = new double[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF float
        public static float[] copy(float[] array){
            if(array==null)return null;
            int n = array.length;
            float[] copy = new float[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF int
        public static int[] copy(int[] array){
            if(array==null)return null;
            int n = array.length;
            int[] copy = new int[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF long
        public static long[] copy(long[] array){
            if(array==null)return null;
            int n = array.length;
            long[] copy = new long[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }
        


        // COPY A TWO DIMENSIONAL ARRAY OF double
        public static double[][] copy(double[][] array){
            if(array==null)return null;
            int n = array.length;
            double[][] copy = new double[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new double[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF float
        public static float[][] copy(float[][] array){
            if(array==null)return null;
            int n = array.length;
            float[][] copy = new float[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new float[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF int
        public static int[][] copy(int[][] array){
            if(array==null)return null;
            int n = array.length;
            int[][] copy = new int[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new int[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF long
        public static long[][] copy(long[][] array){
            if(array==null)return null;
            int n = array.length;
            long[][] copy = new long[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new long[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF double
        public static double[][][] copy(double[][][] array){
            if(array==null)return null;
            int n = array.length;
            double[][][] copy = new double[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new double[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new double[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }


        // COPY A THREE DIMENSIONAL ARRAY OF float
        public static float[][][] copy(float[][][] array){
            if(array==null)return null;
            int n = array.length;
            float[][][] copy = new float[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new float[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new float[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF int
        public static int[][][] copy(int[][][] array){
            if(array==null)return null;
            int n = array.length;
            int[][][] copy = new int[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new int[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new int[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF long
        public static long[][][] copy(long[][][] array){
            if(array==null)return null;
            int n = array.length;
            long[][][] copy = new long[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new long[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new long[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF double
        public static double[][][][] copy(double[][][][] array){
            if(array==null)return null;
            int n = array.length;
            double[][][][] copy = new double[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new double[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new double[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new double[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF float
        public static float[][][][] copy(float[][][][] array){
            if(array==null)return null;
            int n = array.length;
            float[][][][] copy = new float[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new float[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new float[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new float[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF int
        public static int[][][][] copy(int[][][][] array){
            if(array==null)return null;
            int n = array.length;
            int[][][][] copy = new int[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new int[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new int[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new int[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF long
        public static long[][][][] copy(long[][][][] array){
            if(array==null)return null;
            int n = array.length;
            long[][][][] copy = new long[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new long[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new long[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new long[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF String
        public static String[] copy(String[] array){
            if(array==null)return null;
            int n = array.length;
            String[] copy = new String[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF String
        public static String[][] copy(String[][] array){
            if(array==null)return null;
            int n = array.length;
            String[][] copy = new String[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new String[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF String
        public static String[][][] copy(String[][][] array){
            if(array==null)return null;
            int n = array.length;
            String[][][] copy = new String[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new String[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new String[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF String
        public static String[][][][] copy(String[][][][] array){
            if(array==null)return null;
            int n = array.length;
            String[][][][] copy = new String[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new String[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new String[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new String[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }




        // COPY A ONE DIMENSIONAL ARRAY OF boolean
        public static boolean[] copy(boolean[] array){
            if(array==null)return null;
            int n = array.length;
            boolean[] copy = new boolean[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF boolean
        public static boolean[][] copy(boolean[][] array){
            if(array==null)return null;
            int n = array.length;
            boolean[][] copy = new boolean[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new boolean[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF boolean
        public static boolean[][][] copy(boolean[][][] array){
            if(array==null)return null;
            int n = array.length;
            boolean[][][] copy = new boolean[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new boolean[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new boolean[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF boolean
        public static boolean[][][][] copy(boolean[][][][] array){
            if(array==null)return null;
            int n = array.length;
            boolean[][][][] copy = new boolean[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new boolean[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new boolean[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new boolean[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }



        // COPY A ONE DIMENSIONAL ARRAY OF char
        public static char[] copy(char[] array){
            if(array==null)return null;
            int n = array.length;
            char[] copy = new char[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF char
        public static char[][] copy(char[][] array){
            if(array==null)return null;
            int n = array.length;
            char[][] copy = new char[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new char[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF char
        public static char[][][] copy(char[][][] array){
            if(array==null)return null;
            int n = array.length;
            char[][][] copy = new char[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new char[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new char[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF char
        public static char[][][][] copy(char[][][][] array){
            if(array==null)return null;
            int n = array.length;
            char[][][][] copy = new char[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new char[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new char[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new char[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }
        
        
        
        // COPY A ONE DIMENSIONAL ARRAY OF char
        public static Character[] copy(Character[] array){
            if(array==null)return null;
            int n = array.length;
            Character[] copy = new Character[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF char
        public static Character[][] copy(Character[][] array){
            if(array==null)return null;
            int n = array.length;
            Character[][] copy = new Character[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Character[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }
        
        // COPY A THREE DIMENSIONAL ARRAY OF Character
        public static Character[][][] copy(Character[][][] array){
            if(array==null)return null;
            int n = array.length;
            Character[][][] copy = new Character[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Character[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Character[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }
        
        // COPY A FOUR DIMENSIONAL ARRAY OF Character
        public static Character[][][][] copy(Character[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Character[][][][] copy = new Character[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Character[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Character[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Character[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }


        // COPY A ONE DIMENSIONAL ARRAY OF Complex
        public static Complex[] copy(Complex[] array){
            if(array==null)return null;
            int n = array.length;
            Complex[] copy = new Complex[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Complex
        public static Complex[][] copy(Complex[][] array){
            if(array==null)return null;
            int n = array.length;
            Complex[][] copy = new Complex[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Complex[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Complex
        public static Complex[][][] copy(Complex[][][] array){
            if(array==null)return null;
            int n = array.length;
            Complex[][][] copy = new Complex[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Complex[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Complex[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Complex
        public static Complex[][][][] copy(Complex[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Complex[][][][] copy = new Complex[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Complex[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Complex[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Complex[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }


        // COPY A ONE DIMENSIONAL ARRAY OF ComplexPoly
        public static ComplexPoly[] copy(ComplexPoly[] array){
            if(array==null)return null;
            int n = array.length;
            ComplexPoly[] copy = new ComplexPoly[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF ComplexPoly
        public static ComplexPoly[][] copy(ComplexPoly[][] array){
            if(array==null)return null;
            int n = array.length;
            ComplexPoly[][] copy = new ComplexPoly[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ComplexPoly[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF ComplexPoly
        public static ComplexPoly[][][] copy(ComplexPoly[][][] array){
            if(array==null)return null;
            int n = array.length;
            ComplexPoly[][][] copy = new ComplexPoly[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ComplexPoly[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new ComplexPoly[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF ComplexPoly
        public static ComplexPoly[][][][] copy(ComplexPoly[][][][] array){
            if(array==null)return null;
            int n = array.length;
            ComplexPoly[][][][] copy = new ComplexPoly[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ComplexPoly[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new ComplexPoly[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new ComplexPoly[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF Polynomial
        public static Polynomial[] copy(Polynomial[] array){
            if(array==null)return null;
            int n = array.length;
            Polynomial[] copy = new Polynomial[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Polynomial
        public static Polynomial[][] copy(Polynomial[][] array){
            if(array==null)return null;
            int n = array.length;
            Polynomial[][] copy = new Polynomial[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Polynomial[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Polynomial
        public static Polynomial[][][] copy(Polynomial[][][] array){
            if(array==null)return null;
            int n = array.length;
            Polynomial[][][] copy = new Polynomial[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Polynomial[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Polynomial[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Polynomial
        public static Polynomial[][][][] copy(Polynomial[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Polynomial[][][][] copy = new Polynomial[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Polynomial[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Polynomial[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Polynomial[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF BigDecimal
        public static BigDecimal[] copy(BigDecimal[] array){
            if(array==null)return null;
            int n = array.length;
            BigDecimal[] copy = new BigDecimal[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF BigDecimal
        public static BigDecimal[][] copy(BigDecimal[][] array){
            if(array==null)return null;
            int n = array.length;
            BigDecimal[][] copy = new BigDecimal[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new BigDecimal[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF BigDecimal
        public static BigDecimal[][][] copy(BigDecimal[][][] array){
            if(array==null)return null;
            int n = array.length;
            BigDecimal[][][] copy = new BigDecimal[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new BigDecimal[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new BigDecimal[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF BigDecimal
        public static BigDecimal[][][][] copy(BigDecimal[][][][] array){
            if(array==null)return null;
            int n = array.length;
            BigDecimal[][][][] copy = new BigDecimal[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new BigDecimal[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new BigDecimal[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new BigDecimal[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }




        // COPY A ONE DIMENSIONAL ARRAY OF BigInteger
        public static BigInteger[] copy(BigInteger[] array){
            if(array==null)return null;
            int n = array.length;
            BigInteger[] copy = new BigInteger[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF BigInteger
        public static BigInteger[][] copy(BigInteger[][] array){
            if(array==null)return null;
            int n = array.length;
            BigInteger[][] copy = new BigInteger[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new BigInteger[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF BigInteger
        public static BigInteger[][][] copy(BigInteger[][][] array){
            if(array==null)return null;
            int n = array.length;
            BigInteger[][][] copy = new BigInteger[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new BigInteger[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new BigInteger[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF BigInteger
        public static BigInteger[][][][] copy(BigInteger[][][][] array){
            if(array==null)return null;
            int n = array.length;
            BigInteger[][][][] copy = new BigInteger[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new BigInteger[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new BigInteger[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new BigInteger[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF ErrorProp
        public static ErrorProp[] copy(ErrorProp[] array){
            if(array==null)return null;
            int n = array.length;
            ErrorProp[] copy = new ErrorProp[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF ErrorProp
        public static ErrorProp[][] copy(ErrorProp[][] array){
            if(array==null)return null;
            int n = array.length;
            ErrorProp[][] copy = new ErrorProp[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ErrorProp[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF ErrorProp
        public static ErrorProp[][][] copy(ErrorProp[][][] array){
            if(array==null)return null;
            int n = array.length;
            ErrorProp[][][] copy = new ErrorProp[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ErrorProp[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new ErrorProp[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF ErrorProp
        public static ErrorProp[][][][] copy(ErrorProp[][][][] array){
            if(array==null)return null;
            int n = array.length;
            ErrorProp[][][][] copy = new ErrorProp[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ErrorProp[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new ErrorProp[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new ErrorProp[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }


        // COPY A ONE DIMENSIONAL ARRAY OF ComplexErrorProp
        public static ComplexErrorProp[] copy(ComplexErrorProp[] array){
            if(array==null)return null;
            int n = array.length;
            ComplexErrorProp[] copy = new ComplexErrorProp[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF ComplexErrorProp
        public static ComplexErrorProp[][] copy(ComplexErrorProp[][] array){
            if(array==null)return null;
            int n = array.length;
            ComplexErrorProp[][] copy = new ComplexErrorProp[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ComplexErrorProp[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF ComplexErrorProp
        public static ComplexErrorProp[][][] copy(ComplexErrorProp[][][] array){
            if(array==null)return null;
            int n = array.length;
            ComplexErrorProp[][][] copy = new ComplexErrorProp[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ComplexErrorProp[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new ComplexErrorProp[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF ComplexErrorProp
        public static ComplexErrorProp[][][][] copy(ComplexErrorProp[][][][] array){
            if(array==null)return null;
            int n = array.length;
            ComplexErrorProp[][][][] copy = new ComplexErrorProp[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ComplexErrorProp[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new ComplexErrorProp[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new ComplexErrorProp[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }



        // COPY A ONE DIMENSIONAL ARRAY OF Phasor
        public static Phasor[] copy(Phasor[] array){
            if(array==null)return null;
            int n = array.length;
            Phasor[] copy = new Phasor[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Phasor
        public static Phasor[][] copy(Phasor[][] array){
            if(array==null)return null;
            int n = array.length;
            Phasor[][] copy = new Phasor[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Phasor[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Phasor
        public static Phasor[][][] copy(Phasor[][][] array){
            if(array==null)return null;
            int n = array.length;
            Phasor[][][] copy = new Phasor[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Phasor[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Phasor[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Phasor
        public static Phasor[][][][] copy(Phasor[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Phasor[][][][] copy = new Phasor[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Phasor[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Phasor[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Phasor[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF short
        public static short[] copy(short[] array){
            if(array==null)return null;
            int n = array.length;
            short[] copy = new short[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF short
        public static short[][] copy(short[][] array){
            if(array==null)return null;
            int n = array.length;
            short[][] copy = new short[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new short[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF short
        public static short[][][] copy(short[][][] array){
            if(array==null)return null;
            int n = array.length;
            short[][][] copy = new short[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new short[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new short[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF short
        public static short[][][][] copy(short[][][][] array){
            if(array==null)return null;
            int n = array.length;
            short[][][][] copy = new short[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new short[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new short[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new short[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }




        // COPY A ONE DIMENSIONAL ARRAY OF byte
        public static byte[] copy(byte[] array){
            if(array==null)return null;
            int n = array.length;
            byte[] copy = new byte[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF byte
        public static byte[][] copy(byte[][] array){
            if(array==null)return null;
            int n = array.length;
            byte[][] copy = new byte[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new byte[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF byte
        public static byte[][][] copy(byte[][][] array){
            if(array==null)return null;
            int n = array.length;
            byte[][][] copy = new byte[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new byte[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new byte[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF byte
        public static byte[][][][] copy(byte[][][][] array){
            if(array==null)return null;
            int n = array.length;
            byte[][][][] copy = new byte[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new byte[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new byte[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new byte[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }


        // COPY A ONE DIMENSIONAL ARRAY OF Double
        public static Double[] copy(Double[] array){
            if(array==null)return null;
            int n = array.length;
            Double[] copy = new Double[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Double
        public static Double[][] copy(Double[][] array){
            if(array==null)return null;
            int n = array.length;
            Double[][] copy = new Double[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Double[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Double
        public static Double[][][] copy(Double[][][] array){
            if(array==null)return null;
            int n = array.length;
            Double[][][] copy = new Double[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Double[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Double[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Double
        public static Double[][][][] copy(Double[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Double[][][][] copy = new Double[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Double[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Double[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Double[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF Float
        public static Float[] copy(Float[] array){
            if(array==null)return null;
            int n = array.length;
            Float[] copy = new Float[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Float
        public static Float[][] copy(Float[][] array){
            if(array==null)return null;
            int n = array.length;
            Float[][] copy = new Float[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Float[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Float
        public static Float[][][] copy(Float[][][] array){
            if(array==null)return null;
            int n = array.length;
            Float[][][] copy = new Float[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Float[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Float[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Float
        public static Float[][][][] copy(Float[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Float[][][][] copy = new Float[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Float[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Float[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Float[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF Long
        public static Long[] copy(Long[] array){
            if(array==null)return null;
            int n = array.length;
            Long[] copy = new Long[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Long
        public static Long[][] copy(Long[][] array){
            if(array==null)return null;
            int n = array.length;
            Long[][] copy = new Long[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Long[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Long
        public static Long[][][] copy(Long[][][] array){
            if(array==null)return null;
            int n = array.length;
            Long[][][] copy = new Long[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Long[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Long[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Long
        public static Long[][][][] copy(Long[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Long[][][][] copy = new Long[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Long[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Long[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Long[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF Integer
        public static Integer[] copy(Integer[] array){
            if(array==null)return null;
            int n = array.length;
            Integer[] copy = new Integer[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Integer
        public static Integer[][] copy(Integer[][] array){
            if(array==null)return null;
            int n = array.length;
            Integer[][] copy = new Integer[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Integer[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Integer
        public static Integer[][][] copy(Integer[][][] array){
            if(array==null)return null;
            int n = array.length;
            Integer[][][] copy = new Integer[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Integer[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Integer[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Integer
        public static Integer[][][][] copy(Integer[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Integer[][][][] copy = new Integer[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Integer[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Integer[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Integer[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }


        // COPY A ONE DIMENSIONAL ARRAY OF Short
        public static Short[] copy(Short[] array){
            if(array==null)return null;
            int n = array.length;
            Short[] copy = new Short[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Short
        public static Short[][] copy(Short[][] array){
            if(array==null)return null;
            int n = array.length;
            Short[][] copy = new Short[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Short[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Short
        public static Short[][][] copy(Short[][][] array){
            if(array==null)return null;
            int n = array.length;
            Short[][][] copy = new Short[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Short[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Short[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Short
        public static Short[][][][] copy(Short[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Short[][][][] copy = new Short[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Short[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Short[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Short[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }

        // COPY A ONE DIMENSIONAL ARRAY OF Byte
        public static Byte[] copy(Byte[] array){
            if(array==null)return null;
            int n = array.length;
            Byte[] copy = new Byte[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i];
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Byte
        public static Byte[][] copy(Byte[][] array){
            if(array==null)return null;
            int n = array.length;
            Byte[][] copy = new Byte[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Byte[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j];
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Byte
        public static Byte[][][] copy(Byte[][][] array){
            if(array==null)return null;
            int n = array.length;
            Byte[][][] copy = new Byte[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Byte[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Byte[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k];
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Byte
        public static Byte[][][][] copy(Byte[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Byte[][][][] copy = new Byte[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Byte[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Byte[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Byte[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk];
                        }
                    }
                }
            }
            return copy;
        }


        // COPY A ONE DIMENSIONAL ARRAY OF ArrayMaths
        public static ArrayMaths[] copy(ArrayMaths[] array){
            if(array==null)return null;
            int n = array.length;
            ArrayMaths[] copy = new ArrayMaths[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF ArrayMaths
        public static ArrayMaths[][] copy(ArrayMaths[][] array){
            if(array==null)return null;
            int n = array.length;
            ArrayMaths[][] copy = new ArrayMaths[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ArrayMaths[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF ArrayMaths
        public static ArrayMaths[][][] copy(ArrayMaths[][][] array){
            if(array==null)return null;
            int n = array.length;
            ArrayMaths[][][] copy = new ArrayMaths[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ArrayMaths[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new ArrayMaths[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF ArrayMaths
        public static ArrayMaths[][][][] copy(ArrayMaths[][][][] array){
            if(array==null)return null;
            int n = array.length;
            ArrayMaths[][][][] copy = new ArrayMaths[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ArrayMaths[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new ArrayMaths[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new ArrayMaths[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }


        // COPY A ONE DIMENSIONAL ARRAY OF VectorMaths
        public static VectorMaths[] copy(VectorMaths[] array){
            if(array==null)return null;
            int n = array.length;
            VectorMaths[] copy = new VectorMaths[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF VectorMaths
        public static VectorMaths[][] copy(VectorMaths[][] array){
            if(array==null)return null;
            int n = array.length;
            VectorMaths[][] copy = new VectorMaths[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new VectorMaths[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF VectorMaths
        public static VectorMaths[][][] copy(VectorMaths[][][] array){
            if(array==null)return null;
            int n = array.length;
            VectorMaths[][][] copy = new VectorMaths[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new VectorMaths[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new VectorMaths[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF VectorMaths
        public static VectorMaths[][][][] copy(VectorMaths[][][][] array){
            if(array==null)return null;
            int n = array.length;
            VectorMaths[][][][] copy = new VectorMaths[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new VectorMaths[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new VectorMaths[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new VectorMaths[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }
        
        
        
        // COPY A ONE DIMENSIONAL ARRAY OF Point
        public static Point[] copy(Point[] array){
            if(array==null)return null;
            int n = array.length;
            Point[] copy = new Point[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Point
        public static Point[][] copy(Point[][] array){
            if(array==null)return null;
            int n = array.length;
            Point[][] copy = new Point[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Point[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Point
        public static Point[][][] copy(Point[][][] array){
            if(array==null)return null;
            int n = array.length;
            Point[][][] copy = new Point[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Point[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Point[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Point
        public static Point[][][][] copy(Point[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Point[][][][] copy = new Point[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Point[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Point[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Point[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }


        
        // COPY A ONE DIMENSIONAL ARRAY OF Matrix
        public static Matrix[] copy(Matrix[] array){
            if(array==null)return null;
            int n = array.length;
            Matrix[] copy = new Matrix[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF Matrix
        public static Matrix[][] copy(Matrix[][] array){
            if(array==null)return null;
            int n = array.length;
            Matrix[][] copy = new Matrix[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Matrix[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF Matrix
        public static Matrix[][][] copy(Matrix[][][] array){
            if(array==null)return null;
            int n = array.length;
            Matrix[][][] copy = new Matrix[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Matrix[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Matrix[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF Matrix
        public static Matrix[][][][] copy(Matrix[][][][] array){
            if(array==null)return null;
            int n = array.length;
            Matrix[][][][] copy = new Matrix[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new Matrix[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new Matrix[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new Matrix[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }

        
        
        // COPY A ONE DIMENSIONAL ARRAY OF ComplexMatrix
        public static ComplexMatrix[] copy(ComplexMatrix[] array){
            if(array==null)return null;
            int n = array.length;
            ComplexMatrix[] copy = new ComplexMatrix[n];
            for(int i=0; i<n; i++){
                copy[i] = array[i].copy();
            }
            return copy;
        }

        // COPY A TWO DIMENSIONAL ARRAY OF ComplexMatrix
        public static ComplexMatrix[][] copy(ComplexMatrix[][] array){
            if(array==null)return null;
            int n = array.length;
            ComplexMatrix[][] copy = new ComplexMatrix[n][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ComplexMatrix[m];
                for(int j=0; j<m; j++){
                    copy[i][j] = array[i][j].copy();
                }
            }
            return copy;
        }

        // COPY A THREE DIMENSIONAL ARRAY OF ComplexMatrix
        public static ComplexMatrix[][][] copy(ComplexMatrix[][][] array){
            if(array==null)return null;
            int n = array.length;
            ComplexMatrix[][][] copy = new ComplexMatrix[n][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ComplexMatrix[m][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new ComplexMatrix[l];
                    for(int k=0; k<l;k++)copy[i][j][k] = array[i][j][k].copy();
                }
            }
            return copy;
        }

        // COPY A FOUR DIMENSIONAL ARRAY OF ComplexMatrix
        public static ComplexMatrix[][][][] copy(ComplexMatrix[][][][] array){
            if(array==null)return null;
            int n = array.length;
            ComplexMatrix[][][][] copy = new ComplexMatrix[n][][][];
            for(int i=0; i<n; i++){
                int m = array[i].length;
                copy[i] = new ComplexMatrix[m][][];
                for(int j=0; j<m; j++){
                    int l = array[i][j].length;
                    copy[i][j] = new ComplexMatrix[l][];
                    for(int k=0; k<l;k++){
                        int ll = array[i][j][k].length;
                        copy[i][j][k] = new ComplexMatrix[ll];
                        for(int kk=0; kk<ll;kk++){
                            copy[i][j][k][kk] = array[i][j][k][kk].copy();
                        }
                    }
                }
            }
            return copy;
        }


        // COPY OF AN OBJECT
        // An exception will be thrown if an attempt to copy a non-serialisable object is made.
        // Taken, with minor changes,  from { Java Techniques }
        // http://javatechniques.com/blog/
        public static Object copy(Object obj){
            if(obj==null)return null;
            return DeepCopy.copyObject(obj);
        }

        // COPY OF AN OBJECT
        // An exception will be thrown if an attempt to copy a non-serialisable object is made.
        // Taken, with minor changes,  from { Java Techniques }
        // http://javatechniques.com/blog/
        public static Object copyObject(Object obj){
            if(obj==null)return null;
            Object objDeepCopy = null;
            try {
                // Write the object out to a byte array
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(obj);
                oos.flush();
                oos.close();
                // Make an input stream from the byte array and
                // read a copy of the object back in.
                ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray()));
                objDeepCopy = ois.readObject();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            catch(ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            }
            return objDeepCopy;
        }
} 