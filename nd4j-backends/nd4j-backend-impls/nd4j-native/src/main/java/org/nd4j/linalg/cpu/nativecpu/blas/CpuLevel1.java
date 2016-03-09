package org.nd4j.linalg.cpu.nativecpu.blas;


import org.nd4j.linalg.api.blas.impl.BaseLevel1;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.complex.IComplexDouble;
import org.nd4j.linalg.api.complex.IComplexFloat;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.nativeblas.Nd4jBlas;



/**
 * @author Adam Gibson
 */
public class CpuLevel1 extends BaseLevel1 {
    private Nd4jBlas nd4jBlas = new Nd4jBlas();
    private static long[] DUMMY = new long[1];
    @Override
    protected float sdsdot(int N, float alpha, INDArray X, int incX, INDArray Y, int incY) {
        return nd4jBlas.sdsdot(DUMMY,N,alpha,X.data().address(),incX,Y.data().address(),incY);
    }

    @Override
    protected double dsdot(int N, INDArray X, int incX, INDArray Y, int incY) {
        return nd4jBlas.dsdot(DUMMY,N,X.data().address(),incX,Y.data().address(),incY);
    }

    @Override
    protected float sdot(int N, INDArray X, int incX, INDArray Y, int incY) {
        return nd4jBlas.sdot(DUMMY,N,X.data().address(),incX,Y.data().address(),incY);
    }

    @Override
    protected float sdot( int N, DataBuffer X, int offsetX, int incX, DataBuffer Y,  int offsetY, int incY){
        throw new UnsupportedOperationException();
    }

    @Override
    protected double ddot(int N, INDArray X, int incX, INDArray Y, int incY) {
        return nd4jBlas.ddot(DUMMY,N,X.data().address(),incX,Y.data().address(),incY);
    }

    @Override
    protected double ddot( int N, DataBuffer X, int offsetX, int incX, DataBuffer Y,  int offsetY, int incY){
        throw new UnsupportedOperationException();
    }

    @Override
    protected void cdotu_sub(int N, IComplexNDArray X, int incX, IComplexNDArray Y, int incY, IComplexNDArray dotu) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void cdotc_sub(int N, IComplexNDArray X, int incX, IComplexNDArray Y, int incY, IComplexNDArray dotc) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void zdotu_sub(int N, IComplexNDArray X, int incX, IComplexNDArray Y, int incY, IComplexNDArray dotu) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void zdotc_sub(int N, IComplexNDArray X, int incX, IComplexNDArray Y, int incY, IComplexNDArray dotc) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected float snrm2(int N, INDArray X, int incX) {
        return nd4jBlas.snrm2(DUMMY,N,X.data().address(),incX);

    }

    @Override
    protected float sasum(int N, INDArray X, int incX) {
        return nd4jBlas.sasum(DUMMY,N,X.data().address(),incX);
    }

    @Override
    protected float sasum(int N, DataBuffer X, int offsetX, int incX) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected double dnrm2(int N, INDArray X, int incX) {
        return nd4jBlas.dnrm2(DUMMY,N,X.data().address(),incX);
    }

    @Override
    protected double dasum(int N, INDArray X, int incX) {
        return nd4jBlas.dasum(DUMMY,N,X.data().address(),incX);
    }

    @Override
    protected double dasum(int N, DataBuffer X, int offsetX, int incX) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected float scnrm2(int N, IComplexNDArray X, int incX) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected float scasum(int N, IComplexNDArray X, int incX) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected double dznrm2(int N, IComplexNDArray X, int incX) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected double dzasum(int N, IComplexNDArray X, int incX) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected int isamax(int N, INDArray X, int incX) {
        return nd4jBlas.isamax(DUMMY,N,X.data().address(),incX);
    }

    @Override
    protected int isamax(int N, DataBuffer X, int offsetX, int incX){
        throw new UnsupportedOperationException();
    }

    @Override
    protected int idamax(int N, INDArray X, int incX) {
        return nd4jBlas.idamax(DUMMY,N,X.data().address(),incX);
    }

    @Override
    protected int idamax(int N, DataBuffer X, int offsetX, int incX){
        throw new UnsupportedOperationException();
    }

    @Override
    protected int icamax(int N, IComplexNDArray X, int incX) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int izamax(int N, IComplexNDArray X, int incX) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void sswap(int N, INDArray X, int incX, INDArray Y, int incY) {
        nd4jBlas.sswap(DUMMY,N,X.data().address(),incX,Y.data().address(),incY);
    }

    @Override
    protected void scopy(int N, INDArray X, int incX, INDArray Y, int incY) {
        nd4jBlas.scopy(DUMMY,N,X.data().address(),incX,Y.data().address(),incY);
    }

    @Override
    protected void scopy(int n, DataBuffer x, int offsetX, int incrX, DataBuffer y, int offsetY, int incrY ){
        throw new UnsupportedOperationException();
    }

    @Override
    protected void saxpy(int N, float alpha, INDArray X, int incX, INDArray Y, int incY) {
        nd4jBlas.saxpy(DUMMY,N,alpha,X.data().address(),incX,Y.data().address(),incY);
    }

    @Override
    public void saxpy(int n,float alpha, DataBuffer x, int offsetX, int incrX, DataBuffer y, int offsetY, int incrY ){
        throw new UnsupportedOperationException();
    }


    @Override
    protected void dswap(int N, INDArray X, int incX, INDArray Y, int incY) {
        nd4jBlas.dswap(DUMMY,N,X.data().address(),incX,Y.data().address(),incY);
    }

    @Override
    protected void dcopy(int N, INDArray X, int incX, INDArray Y, int incY) {
        nd4jBlas.dcopy(DUMMY,N,X.data().address(),incX,Y.data().address(),incY);
    }

    @Override
    protected void dcopy(int n, DataBuffer x, int offsetX, int incrX, DataBuffer y, int offsetY, int incrY) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void daxpy(int N, double alpha, INDArray X, int incX, INDArray Y, int incY) {
        nd4jBlas.daxpy(DUMMY,N,alpha,X.data().address(),incX,Y.data().address(),incY);

    }

    @Override
    public void daxpy(int n,double alpha, DataBuffer x, int offsetX, int incrX, DataBuffer y, int offsetY, int incrY){
        throw new UnsupportedOperationException();
    }

    @Override
    protected void cswap(int N, IComplexNDArray X, int incX, IComplexNDArray Y, int incY) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void ccopy(int N, IComplexNDArray X, int incX, IComplexNDArray Y, int incY) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void caxpy(int N, IComplexFloat alpha, IComplexNDArray X, int incX, IComplexNDArray Y, int incY) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void zswap(int N, IComplexNDArray X, int incX, IComplexNDArray Y, int incY) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void zcopy(int N, IComplexNDArray X, int incX, IComplexNDArray Y, int incY) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void zaxpy(int N, IComplexDouble alpha, IComplexNDArray X, int incX, IComplexNDArray Y, int incY) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void srotg(float a, float b, float c, float s) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void srotmg(float d1, float d2, float b1, float b2, INDArray P) {
        DataBuffer addr = Nd4j.createBuffer(new float[]{d1,d2,b1,b2});
        nd4jBlas.srotmg(DUMMY,addr.address(),P.data().address());
    }

    @Override
    protected void srot(int N, INDArray X, int incX, INDArray Y, int incY, float c, float s) {
        nd4jBlas.srot(DUMMY,N,X.data().address(),incX,Y.data().address(),incY,c,s);
    }

    @Override
    protected void srotm(int N, INDArray X, int incX, INDArray Y, int incY, INDArray P) {
        nd4jBlas.srotm(DUMMY,N,X.data().address(),incX,Y.data().address(),incY,P.data().address());

    }

    @Override
    protected void drotg(double a, double b, double c, double s) {
        DataBuffer buff = Nd4j.createBuffer(new double[]{a, b, c, s});
        nd4jBlas.drotg(DUMMY,buff.address());
    }

    @Override
    protected void drotmg(double d1, double d2, double b1, double b2, INDArray P) {
        DataBuffer buff = Nd4j.createBuffer(new double[]{d1, d2, b1, b2});
        nd4jBlas.drotmg(DUMMY,buff.address(),P.data().address());
    }

    @Override
    protected void drot(int N, INDArray X, int incX, INDArray Y, int incY, double c, double s) {
        nd4jBlas.drot(DUMMY,N,X.data().address(),incX,Y.data().address(),incY,c,s);
    }


    @Override
    protected void drotm(int N, INDArray X, int incX, INDArray Y, int incY, INDArray P) {
        nd4jBlas.drotm(DUMMY,N,X.data().address(),incX,Y.data().address(),incY,P.data().address());
    }

    @Override
    protected void sscal(int N, float alpha, INDArray X, int incX) {
        nd4jBlas.sscal(DUMMY,N,alpha,X.data().address(),incX);
    }

    @Override
    protected void dscal(int N, double alpha, INDArray X, int incX) {
        nd4jBlas.dscal(DUMMY,N,alpha,X.data().address(),incX);
    }

    @Override
    protected void cscal(int N, IComplexFloat alpha, IComplexNDArray X, int incX) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void zscal(int N, IComplexDouble alpha, IComplexNDArray X, int incX) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void csscal(int N, float alpha, IComplexNDArray X, int incX) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void zdscal(int N, double alpha, IComplexNDArray X, int incX) {
        throw new UnsupportedOperationException();

    }
}
