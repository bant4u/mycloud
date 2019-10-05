/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mycloud;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;



public final class MatrixManipulations {
public static double lambdaMax = 0;   
// to represent fraction write 1/9.0
// saaty's scale vector
private static double[] SSV = new double[] { 1 / 9.0, 1 / 8.0, 1 / 7.0, 1 / 6.0, 1 / 5.0, 1 / 4.0,
1 / 3.0, 1 / 2.0,
    1, 2, 3, 4, 5, 6, 7, 8, 9 };
private static DecimalFormat df = new DecimalFormat(".###");

public static RealMatrix createRandomInconsistentMatrix(int order, int missingElementsCount) {
    double[][] matrixData = new double[order][order];
    for (int i = 0; i < order; i++) {
    for (int j = 0; j < order; j++) {
     if (i == j) {
      matrixData[i][j] = 1;
     } else if (i < j) {
      Random random = new Random();
      // generates random int between 0 (inclusive) and
      // 17(exclusive) ie 0‐16 ie 17 elements.
      int s = random.nextInt(17);
      matrixData[i][j] = SSV[s];
      matrixData[j][i] = 1 / matrixData[i][j];
     }
    } // end for j
    } // end for i
    // filling missing entries by ‐2  
    int indexr[] = new int[missingElementsCount];
    int indexc[] = new int[missingElementsCount];
    for (int k = 0; k < missingElementsCount; k++) {
    Random random = new Random();
    int r, c;
    boolean flag;
    do {
     flag = false;
     r = random.nextInt(order);
     c = random.nextInt(order);
     //r should not be greater or equal to c index
     if(r>c || r==c) flag =true;
     else if (r < c) {
      // r and c should not be in previous indexr and indexc list        
      for(int count=0;count<missingElementsCount;count++){
       if(indexr[count]==r && indexc[count]==c) flag = true;
      }          
     }
    } while (flag == true);
    indexr[k] = r;
    indexc[k] = c;
    matrixData[r][c] = -2;
    matrixData[c][r] = -2;
    }
    return MatrixUtils.createRealMatrix(matrixData);
}
public static double[] extractUpperTriangularMatrixData(RealMatrix m) {
    double[][] matrixData = m.getData();
    int n = m.getColumnDimension();
    double[] data = new double[n * (n - 1) / 2];
    int count = 0;
    for (int i = 0; i < n; i++) {
    for (int j = 0; j < n; j++) {
     if (i < j) {
      data[count] = matrixData[i][j];
      count++;
     }
    }
    }
    return data;
}
public static RealMatrix completeMatrixWithLowerTraingularData(double[] inputData, int dimension)
{
    // creating square matrix with given dimension
    double[][] matrixData = new double[dimension][dimension];
    // count for traversing data
    int count = 0;
    for (int i = 0; i < dimension; i++) {
    for (int j = 0; j < dimension; j++) {
     if (i == j)
      matrixData[i][j] = 1;
     if (i < j) {
      matrixData[i][j] = inputData[count];
      count++;
     }
     if (i > j) {
      //checking missing elements in the inputData vector
      if(matrixData[j][i]!= -2){
       matrixData[i][j] = 1 / matrixData[j][i];
      }
      else{
       matrixData[i][j] = -2;
      }
     }
    }
    }
    return MatrixUtils.createRealMatrix(matrixData);
}
// normalizing saaty's scale in the range[‐1,1]
public static double[] normalizeVector(double[] vector) {
    // double[] normalizedVector = new double[vector.length];
    for (int i = 0; i < vector.length; i++) {
    if (vector[i] != -2) {
     for (double value : SSV) {
      if (value == vector[i]) {
       // linear conversion of range [1/9, 9] to [‐1, 1]
       // equation will be a*(1/9) + b = ‐1
       // and another is a*(9)+b = 1
       // solving these two equation will give a = 9/40 and
       // b=‐41/40
       vector[i] = 9.0 / 40.0 * value - 41.0 / 40.0;
      } // end if
     } // end for
    } // end if
    }
    return vector;
}
public static double[] deNormalizeVector(double[] vector) {
    double[] deNormalizedVector = new double[vector.length];
    double difference = Double.MAX_VALUE;
    for (int i = 0; i < vector.length; i++) {
    difference = Double.MAX_VALUE;
    for (int j = 0; j < SSV.length; j++) {
     double val = 9.0 / 40.0 * SSV[j] - 41.0 / 40.0;
     if (Math.abs(val - vector[i]) < difference) {
      difference = Math.abs(val - vector[i]);
      deNormalizedVector[i] = SSV[j];
     }
    }
    }
    return deNormalizedVector;
}
public static double computeMatrixCR(RealMatrix m) {
    // doc get lambdaMax and n from matrix
    SingularValueDecomposition s;
    s = new SingularValueDecomposition(m);
    RealMatrix U = s.getU();
    RealMatrix V = s.getV();
    RealMatrix S = s.getS();

    double[] svaluee =s.getSingularValues();
    RealMatrix mam = (RealMatrix) new Array2DRowRealMatrix(svaluee);
    EigenDecomposition ed = new EigenDecomposition(mam);
    int index = -1;
    int position = 0;
    double value = 0;
    // get the index of lambda‐max
    for (double d : ed.getRealEigenvalues()) {
    index++;
    if (d > value) {
     position = index;
     value = Double.parseDouble(df.format(d));
    }
    }
   
    lambdaMax = value;
    //System.out.println("lambda‐max :"+value);
    double lambdaMax = value;
    int n = m.getColumnDimension();
    // doc: compute consitency index C.I.
    double ci = (lambdaMax - n) / (n - 1);
    // doc: compute random index R.I from lookup table
    double ri = 0;
    switch (n) {
    case 1:
    ri = 0.00;
    break;
    case 2:
    ri = 0.00;
    break;
    case 3:
    ri = 0.58;
    break;
    case 4:
    ri = 0.90;
    break;
    case 5:
    ri = 1.12;
    break;
    case 6:
    ri = 1.24;
    break;
    case 7:
    ri = 1.32;
    break;
    case 8:
    ri = 1.41;
    break;
    case 9:
    ri = 1.45;
    break;
    case 10:
    ri = 1.49;
    break;
    case 11:
    ri = 1.51;
    break;
    case 12:
    ri = 1.48;
    break;
    case 13:
    ri = 1.56;
    break;
    case 14:
    ri = 1.57;
    break;
    case 15:
    ri = 1.59;
    break;
    default:
    ri = (1.98 * (n - 2)) / n; // ref:hamdy taha 'Operation Research'
           // 8th edition page 481‐482
    break;
    }
    // doc: compute consistency ratio C.R.
    double cr = ci / ri;
    return cr;
}
public static void printMatrix(RealMatrix matrix) {
    for (double a[] : matrix.getData()) {
    System.out.print("|");
    for (double b : a) {
     b = Double.parseDouble(df.format(b));
     System.out.printf("  %.3f  ", b);
    }
    System.out.print("|\n");
    }
}
public static RealVector computeEigenVector(RealMatrix m) {
    
    SingularValueDecomposition s;
    s = new SingularValueDecomposition(m);
    RealMatrix U = s.getU();
    RealMatrix V = s.getV();
    RealMatrix S = s.getS();

    double[] svaluee =s.getSingularValues();
    EigenDecomposition ed = new EigenDecomposition(U);
    int index = -1;
    int position = 0;
    double value = 0;
    // get the index of lambda‐max
    for (double d : ed.getRealEigenvalues()) {
    index++;
    if (d > value) {
     position = index;
     value = Double.parseDouble(df.format(d));
    }
    }
    RealVector v = ed.getEigenvector(position);
    // normalizing eigenvector
    double sum = 0;
    for (int j = 0; j < v.getDimension(); j++) {
    sum += v.getEntry(j);
    }
    for (int k = 0; k < v.getDimension(); k++) {
    v.setEntry(k, Double.parseDouble(df.format(v.getEntry(k) / sum)));
    }
    System.out.println(v);
    return v;
}
} 