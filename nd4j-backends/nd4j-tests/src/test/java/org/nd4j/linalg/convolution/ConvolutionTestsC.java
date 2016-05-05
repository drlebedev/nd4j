/*
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 *
 */

package org.nd4j.linalg.convolution;
import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nd4j.linalg.BaseNd4jTest;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.util.AllocUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.Nd4jBackend;

import java.util.Arrays;

/**
 * Created by agibsonccc on 9/6/14.
 */
@RunWith(Parameterized.class)
public  class ConvolutionTestsC extends BaseNd4jTest {

    public ConvolutionTestsC(Nd4jBackend backend) {
        super(backend);
    }


    @Test
    public void testConvOutWidthAndHeight() {
        int outSize = Convolution.outSize(2,1,1,2,false);
        assertEquals(6, outSize);
    }

    @Test
    public void testIm2Col() {
        INDArray linspaced = Nd4j.linspace(1,16,16).reshape(2, 2, 2, 2);
        INDArray ret = Convolution.im2col(linspaced, 1, 1, 1, 1, 2, 2, 0, false);
        INDArray im2colAssertion = Nd4j.create(new double[]{
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 6.0, 0.0, 0.0, 0.0, 0.0, 7.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, 10.0, 0.0, 0.0, 0.0, 0.0, 11.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 13.0, 14.0, 0.0, 0.0, 0.0, 0.0, 15.0, 16.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        }, new int[]{2, 2, 1, 1, 6, 6});
        assertEquals(im2colAssertion, ret);
        INDArray col2ImAssertion = Nd4j.create(new double[] {
                1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0,10.0,11.0,12.0,13.0,14.0,15.0,16.0

        }, new int[]{2,2,2,2});

        INDArray otherConv = Convolution.col2im(ret, 1, 1, 2, 2, 2, 2);
        assertEquals(col2ImAssertion,otherConv);

    }

    @Test
    public void testIm2Col2() {
        int kh = 2;
        int kw = 2;
        int ph = 0;
        int pw = 0;
        int sy = 2;
        int sx = 2;
        int depth = 2;
        INDArray assertion  = Nd4j.create(new double[]{
                1, 1, 1, 1, 3, 3, 3, 3, 1, 1, 1, 1, 3, 3, 3, 3, 1, 1, 1, 1, 3, 3, 3, 3, 1, 1, 1, 1, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4, 2, 2, 2, 2, 4, 4, 4, 4, 2, 2, 2, 2, 4, 4, 4, 4, 2, 2, 2, 2, 4, 4, 4, 4
        }, new int[]{1, 1, 2, 2, 4, 4});
        INDArray ret = Nd4j.create(new double[]{
                1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3,
                3, 4, 4, 4, 4, 4, 4, 4, 4, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2,
                2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4
        }, new int[]{1, 1, 8, 8});

        INDArray test = Convolution.im2col(ret, kh, kw, sy, sx, ph, pw, 0, false);
        assertEquals(assertion,test);

    }

    @Test
    @Ignore
    public void testCompareIm2ColImpl() {

        int[] miniBatches = {1, 3, 5};
        int[] depths = {1, 3, 5};
        int[] inHeights = {5,21};
        int[] inWidths = {5,21};
        int[] strideH = {1,2};
        int[] strideW = {1,2};
        int[] sizeW = {1,2,3};
        int[] sizeH = {1,2,3};
        int[] padH = {0,1,2};
        int[] padW = {0,1,2};
        boolean[] coverall = {false,true};

        DataBuffer.Type[] types = new DataBuffer.Type[]{DataBuffer.Type.FLOAT, DataBuffer.Type.DOUBLE, DataBuffer.Type.FLOAT, DataBuffer.Type.DOUBLE};
        DataBuffer.AllocationMode[] modes = new DataBuffer.AllocationMode[]{DataBuffer.AllocationMode.HEAP, DataBuffer.AllocationMode.HEAP,
                DataBuffer.AllocationMode.DIRECT, DataBuffer.AllocationMode.DIRECT};

        String factoryClassName = Nd4j.factory().getClass().toString().toLowerCase();
        if( factoryClassName.contains("jcublas") || factoryClassName.contains("cuda") ){
            //Only test direct for CUDA; test all for CPU
            types = new DataBuffer.Type[]{DataBuffer.Type.FLOAT, DataBuffer.Type.DOUBLE};
            modes = new DataBuffer.AllocationMode[]{DataBuffer.AllocationMode.DIRECT, DataBuffer.AllocationMode.DIRECT};
        }

        for( int i=0; i<types.length; i++ ) {
            DataBuffer.Type type = types[i];
            DataBuffer.AllocationMode mode = modes[i];

            Nd4j.factory().setDType(type);
            Nd4j.dtype = type;
            Nd4j.alloc = mode;

            AllocUtil.setAllocationModeForContext(mode);

            for (int m : miniBatches) {
                for (int d : depths) {
                    for (int h : inHeights) {
                        for (int w : inWidths) {
                            for (int sh : strideH) {
                                for (int sw : strideW) {
                                    for (int kh : sizeH) {
                                        for (int kw : sizeW) {
                                            for (int ph : padH) {
                                                for (int pw : padW) {
                                                    if ((w - kw + 2 * pw) % sw != 0 || (h - kh + 2 * ph) % sh != 0)
                                                        continue;   //(w-kp+2*pw)/sw + 1 is not an integer,  i.e., number of outputs doesn't fit

                                                    System.out.println("Running " + m + " " + d + " " + h + " " + w);
                                                    for( boolean cAll : coverall ) {

                                                        INDArray in = Nd4j.rand(new int[]{m, d, h, w});
                                                        //assertEquals(in.data().allocationMode(), mode);
                                                        //assertEquals(in.data().dataType(), type);

                                                        INDArray outOrig = OldConvolution.im2col(in, kh, kw, sh, sw, ph, pw, -1, cAll); //Old implementation
                                                        INDArray outNew = Convolution.im2col(in, kh, kw, sh, sw, ph, pw, cAll);         //Current implementation

                                                        assertEquals(outOrig,outNew);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Test
    public void testMoreIm2Col2() {
        int kh = 2;
        int kw = 2;
        int ph = 0;
        int pw = 0;
        int sy = 2;
        int sx = 2;

        INDArray ret = Nd4j.create(new double[]{
                1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3,
                3, 4, 4, 4, 4, 4, 4, 4, 4, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2,
                2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4
        }, new int[]{1, 1, 8, 8});

        INDArray assertion = Nd4j.create(new double[]{1,1,1,1,3,3,3,3,1,1,1,1,3,3,3,3,1,1,1,1,3,3,3,3,1,1,1,1,3,3,3,3
                ,2,2,2,2,4,4,4,4,2,2,
                2,2,4,4,4,4,2,2,2,2,4,4,4,4,2,2,2,2,4,4,4,4},new int[] {1, 1, 2, 2, 4, 4});
        INDArray im2colTest = Convolution.im2col(ret, kh, kw, sy, sx, ph, pw, 0, false);
        assertEquals(assertion,im2colTest);
    }


    @Test
    public void testCol2Im() {
        int kh = 1;
        int kw = 1;
        int sy = 1;
        int sx = 1;
        int ph = 1;
        int pw = 1;
        INDArray linspaced = Nd4j.linspace(1,64,64).reshape(2,2,2,2,2,2);
        INDArray newTest = Convolution.col2im(linspaced,sy,sx,ph,pw,2,2);
        INDArray assertion = OldConvolution.col2im(linspaced,sy,sx,ph,pw,2,2);

        System.out.println("Assertion dimensions: " + Arrays.toString(assertion.shape()));
        assertEquals(assertion,newTest);
    }

    @Test
    public void testimcolim() {
        int nEx = 2;
        int depth = 3;
        int width = 7;
        int height = 7;
        int [] kernel = {3,2} ;
        int [] stride = {2,3} ;
        int [] padding = {1,2} ;
        int prod = nEx*depth*width*height;

        INDArray in = Nd4j.linspace(1,prod,prod).reshape(nEx,depth,width,height);

        INDArray assertim2col = OldConvolution.im2col(in, kernel, stride, padding);
        INDArray im2col = Convolution.im2col(in, kernel, stride, padding);
        assertEquals(assertim2col,im2col);

        INDArray assertcol2im = OldConvolution.col2im(im2col,stride,padding,height,width);
        INDArray col2im = Convolution.col2im(im2col,stride,padding,height,width);
        assertEquals(assertcol2im,col2im);
    }

    @Override
    public char ordering() {
        return 'c';
    }
}
