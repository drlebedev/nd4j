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
 */

package org.nd4j.linalg.dataset;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nd4j.linalg.BaseNd4jTest;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.Nd4jBackend;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.util.ArrayUtil;
import org.nd4j.linalg.util.FeatureUtil;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class DataSetTest extends BaseNd4jTest {



    public DataSetTest(Nd4jBackend backend) {
        super(backend);
    }

    @Test
    public void testViewIterator() {
        DataSetIterator iter = new ViewIterator(new IrisDataSetIterator(150,150).next(),10);
        assertTrue(iter.hasNext());
        int count = 0;
        while(iter.hasNext()) {
            DataSet next = iter.next();
            count++;
            assertArrayEquals(new int[]{10,4},next.getFeatureMatrix().shape());
        }

        assertFalse(iter.hasNext());
        assertEquals(15,count);
        iter.reset();
        assertTrue(iter.hasNext());

    }



    @Test
    public void testSplitTestAndTrain() throws Exception{
        INDArray labels = FeatureUtil.toOutcomeMatrix(new int[]{0,0,0,0,0,0,0,0},1);
        DataSet data = new DataSet(Nd4j.rand(8,1),labels);

        SplitTestAndTrain train = data.splitTestAndTrain(6, new Random(1));
        assertEquals(train.getTrain().getLabels().length(),6);

        SplitTestAndTrain train2 = data.splitTestAndTrain(6, new Random(1));
        assertEquals(getFailureMessage(), train.getTrain().getFeatureMatrix(), train2.getTrain().getFeatureMatrix());

        DataSet x0 = new IrisDataSetIterator(150,150).next();
        SplitTestAndTrain testAndTrain = x0.splitTestAndTrain(10);
        assertArrayEquals(new int[]{10, 4}, testAndTrain.getTrain().getFeatureMatrix().shape());
        assertEquals(x0.getFeatureMatrix().getRows(ArrayUtil.range(0, 10)), testAndTrain.getTrain().getFeatureMatrix());
        assertEquals(x0.getLabels().getRows(ArrayUtil.range(0,10)),testAndTrain.getTrain().getLabels());


    }

    @Test
    public void testLabelCounts() {
        DataSet x0 = new IrisDataSetIterator(150,150).next();
        assertEquals(getFailureMessage(),0,x0.get(0).outcome());
        assertEquals(getFailureMessage(),0,x0.get(1).outcome());
        assertEquals(getFailureMessage(),2, x0.get(149).outcome());
        Map<Integer,Double> counts = x0.labelCounts();
        assertEquals(getFailureMessage(), 50, counts.get(0), 1e-1);
        assertEquals(getFailureMessage(),50,counts.get(1),1e-1);
        assertEquals(getFailureMessage(),50,counts.get(2),1e-1);

    }

    @Test
    public void testTimeSeriesMerge(){
        //Basic test for time series, all of the same length + no masking arrays
        int numExamples = 10;
        int inSize = 13;
        int labelSize = 5;
        int tsLength = 15;

        Nd4j.getRandom().setSeed(12345);
        List<DataSet> list = new ArrayList<>(numExamples);
        for( int i=0; i<numExamples; i++ ){
            INDArray in = Nd4j.rand(new int[]{1,inSize,tsLength});
            INDArray out = Nd4j.rand(new int[]{1,labelSize,tsLength});
            list.add(new DataSet(in,out));
        }

        DataSet merged = DataSet.merge(list);
        assertEquals(numExamples,merged.numExamples());

        INDArray f = merged.getFeatures();
        INDArray l = merged.getLabels();
        assertArrayEquals(new int[]{numExamples, inSize, tsLength}, f.shape());
        assertArrayEquals(new int[]{numExamples, labelSize, tsLength}, l.shape());

        for( int i=0; i<numExamples; i++ ){
            DataSet exp = list.get(i);
            INDArray expIn = exp.getFeatureMatrix();
            INDArray expL = exp.getLabels();

            INDArray fSubset = f.get(NDArrayIndex.interval(i,i+1), NDArrayIndex.all(), NDArrayIndex.all());
            INDArray lSubset = l.get(NDArrayIndex.interval(i,i+1), NDArrayIndex.all(),NDArrayIndex.all());

            assertEquals(expIn, fSubset);
            assertEquals(expL,lSubset);
        }
    }

    @Test
     public void testTimeSeriesMergeDifferentLength(){
        //Test merging of time series with different lengths -> no masking arrays on the input DataSets

        int numExamples = 10;
        int inSize = 13;
        int labelSize = 5;
        int minTSLength = 10;   //Lengths 10, 11, ..., 19

        Nd4j.getRandom().setSeed(12345);
        List<DataSet> list = new ArrayList<>(numExamples);
        for( int i=0; i<numExamples; i++ ){
            INDArray in = Nd4j.rand(new int[]{1,inSize,minTSLength+i});
            INDArray out = Nd4j.rand(new int[]{1,labelSize,minTSLength+i});
            list.add(new DataSet(in,out));
        }

        DataSet merged = DataSet.merge(list);
        assertEquals(numExamples,merged.numExamples());

        INDArray f = merged.getFeatures();
        INDArray l = merged.getLabels();
        int expectedLength = minTSLength+numExamples-1;
        assertArrayEquals(new int[]{numExamples,inSize,expectedLength},f.shape());
        assertArrayEquals(new int[]{numExamples, labelSize, expectedLength}, l.shape());

        assertTrue(merged.hasMaskArrays());
        assertNotNull(merged.getFeaturesMaskArray());
        assertNotNull(merged.getLabelsMaskArray());
        INDArray featuresMask = merged.getFeaturesMaskArray();
        INDArray labelsMask = merged.getLabelsMaskArray();
        assertArrayEquals(new int[]{numExamples,expectedLength}, featuresMask.shape());
        assertArrayEquals(new int[]{numExamples,expectedLength}, labelsMask.shape());

        //Check each row individually:
        for( int i=0; i<numExamples; i++ ){
            DataSet exp = list.get(i);
            INDArray expIn = exp.getFeatureMatrix();
            INDArray expL = exp.getLabels();

            int thisRowOriginalLength = minTSLength + i;

            INDArray fSubset = f.get(NDArrayIndex.interval(i,i+1), NDArrayIndex.all(), NDArrayIndex.all());
            INDArray lSubset = l.get(NDArrayIndex.interval(i,i+1), NDArrayIndex.all(),NDArrayIndex.all());

            for( int j=0; j<inSize; j++ ) {
                for (int k = 0; k < thisRowOriginalLength; k++) {
                    double expected = expIn.getDouble(0,j,k);
                    double act = fSubset.getDouble(0,j,k);
                    if(Math.abs(expected-act) > 1e-3){
                        System.out.println(expIn);
                        System.out.println(fSubset);
                    }
                    assertEquals(expected, act, 1e-3f);
                }

                //Padded values: should be exactly 0.0
                for( int k=thisRowOriginalLength; k<expectedLength; k++ ){
                    assertEquals(0.0, fSubset.getDouble(0,j,k), 0.0);
                }
            }

            for( int j=0; j<labelSize; j++ ) {
                for (int k = 0; k < thisRowOriginalLength; k++) {
                    double expected = expL.getDouble(0,j,k);
                    double act = lSubset.getDouble(0,j,k);
                    assertEquals(expected, act, 1e-3f);
                }

                //Padded values: should be exactly 0.0
                for( int k=thisRowOriginalLength; k<expectedLength; k++ ){
                    assertEquals(0.0, lSubset.getDouble(0,j,k), 0.0);
                }
            }

            //Check mask values:
            for( int j=0; j<expectedLength; j++){
                double expected = (j >= thisRowOriginalLength ? 0.0 : 1.0);
                double actFMask = featuresMask.getDouble(i,j);
                double actLMask = labelsMask.getDouble(i,j);

                if(expected != actFMask){
                    System.out.println(featuresMask);
                    System.out.println(j);
                }

                assertEquals(expected, actFMask, 0.0);
                assertEquals(expected, actLMask, 0.0);
            }
        }
    }


    @Test
    public void testTimeSeriesMergeWithMasking(){
        //Test merging of time series with (a) different lengths, and (b) mask arrays in the input DataSets

        int numExamples = 10;
        int inSize = 13;
        int labelSize = 5;
        int minTSLength = 10;   //Lengths 10, 11, ..., 19

        Random r = new Random(12345);

        Nd4j.getRandom().setSeed(12345);
        List<DataSet> list = new ArrayList<>(numExamples);
        for( int i=0; i<numExamples; i++ ){
            INDArray in = Nd4j.rand(new int[]{1,inSize,minTSLength+i});
            INDArray out = Nd4j.rand(new int[]{1,labelSize,minTSLength+i});

            INDArray inMask = Nd4j.create(1, minTSLength + i);
            INDArray outMask = Nd4j.create(1,minTSLength + i);
            for( int j=0; j<inMask.size(1); j++ ){
                inMask.putScalar(j, (r.nextBoolean() ? 1.0 : 0.0) );
                outMask.putScalar(j, (r.nextBoolean() ? 1.0 : 0.0) );
            }

            list.add(new DataSet(in,out,inMask,outMask));
        }

        DataSet merged = DataSet.merge(list);
        assertEquals(numExamples,merged.numExamples());

        INDArray f = merged.getFeatures();
        INDArray l = merged.getLabels();
        int expectedLength = minTSLength+numExamples-1;
        assertArrayEquals(new int[]{numExamples,inSize,expectedLength},f.shape());
        assertArrayEquals(new int[]{numExamples, labelSize, expectedLength}, l.shape());

        assertTrue(merged.hasMaskArrays());
        assertNotNull(merged.getFeaturesMaskArray());
        assertNotNull(merged.getLabelsMaskArray());
        INDArray featuresMask = merged.getFeaturesMaskArray();
        INDArray labelsMask = merged.getLabelsMaskArray();
        assertArrayEquals(new int[]{numExamples,expectedLength}, featuresMask.shape());
        assertArrayEquals(new int[]{numExamples,expectedLength}, labelsMask.shape());

        //Check each row individually:
        for( int i=0; i<numExamples; i++ ){
            DataSet original = list.get(i);
            INDArray expIn = original.getFeatureMatrix();
            INDArray expL = original.getLabels();
            INDArray origMaskF = original.getFeaturesMaskArray();
            INDArray origMaskL = original.getLabelsMaskArray();

            int thisRowOriginalLength = minTSLength + i;

            INDArray fSubset = f.get(NDArrayIndex.interval(i,i+1), NDArrayIndex.all(), NDArrayIndex.all());
            INDArray lSubset = l.get(NDArrayIndex.interval(i,i+1), NDArrayIndex.all(),NDArrayIndex.all());

            for( int j=0; j<inSize; j++ ) {
                for (int k = 0; k < thisRowOriginalLength; k++) {
                    double expected = expIn.getDouble(0,j,k);
                    double act = fSubset.getDouble(0,j,k);
                    if(Math.abs(expected-act) > 1e-3){
                        System.out.println(expIn);
                        System.out.println(fSubset);
                    }
                    assertEquals(expected, act, 1e-3f);
                }

                //Padded values: should be exactly 0.0
                for( int k=thisRowOriginalLength; k<expectedLength; k++ ){
                    assertEquals(0.0, fSubset.getDouble(0,j,k), 0.0);
                }
            }

            for( int j=0; j<labelSize; j++ ) {
                for (int k = 0; k < thisRowOriginalLength; k++) {
                    double expected = expL.getDouble(0,j,k);
                    double act = lSubset.getDouble(0,j,k);
                    assertEquals(expected, act, 1e-3f);
                }

                //Padded values: should be exactly 0.0
                for( int k=thisRowOriginalLength; k<expectedLength; k++ ){
                    assertEquals(0.0, lSubset.getDouble(0,j,k), 0.0);
                }
            }

            //Check mask values:
            for( int j=0; j<expectedLength; j++){
                double expectedF;
                double expectedL;
                if( j >= thisRowOriginalLength ){
                    //Outside of original data bounds -> should be 0
                    expectedF = 0.0;
                    expectedL = 0.0;
                } else {
                    //Value should be same as original mask array value
                    expectedF = origMaskF.getDouble(j);
                    expectedL = origMaskL.getDouble(j);
                }

                double actFMask = featuresMask.getDouble(i,j);
                double actLMask = labelsMask.getDouble(i,j);
                assertEquals(expectedF, actFMask, 0.0);
                assertEquals(expectedL, actLMask, 0.0);
            }
        }
    }

    @Test
    public void testCnnMerge(){
        //Test merging of CNN data sets
        int nOut = 3;
        int width = 5;
        int height = 4;
        int depth = 3;
        int nExamples1 = 2;
        int nExamples2 = 1;

        int length1 = width*height*depth*nExamples1;
        int length2 = width*height*depth*nExamples2;

        INDArray first = Nd4j.linspace(1,length1,length1).reshape('c',nExamples1,depth,width,height);
        INDArray second = Nd4j.linspace(1,length2,length2).reshape('c',nExamples2,depth,width,height).addi(0.1);

        INDArray labels1 = Nd4j.linspace(1,nExamples1*nOut,nExamples1*nOut).reshape('c',nExamples1,nOut);
        INDArray labels2 = Nd4j.linspace(1,nExamples2*nOut,nExamples2*nOut).reshape('c',nExamples2,nOut);

        DataSet ds1 = new DataSet(first,labels1);
        DataSet ds2 = new DataSet(second,labels2);

        DataSet merged = DataSet.merge(Arrays.asList(ds1,ds2));

        INDArray fMerged = merged.getFeatureMatrix();
        INDArray lMerged = merged.getLabels();

        assertArrayEquals(new int[]{nExamples1+nExamples2,depth,width,height}, fMerged.shape());
        assertArrayEquals(new int[]{nExamples1+nExamples2,nOut}, lMerged.shape());

        assertEquals(first, fMerged.get(NDArrayIndex.interval(0,nExamples1), NDArrayIndex.all(), NDArrayIndex.all(), NDArrayIndex.all()));
        assertEquals(second, fMerged.get(NDArrayIndex.interval(nExamples1,nExamples1+nExamples2), NDArrayIndex.all(), NDArrayIndex.all(), NDArrayIndex.all()));
        assertEquals(labels1, lMerged.get(NDArrayIndex.interval(0,nExamples1), NDArrayIndex.all()));
        assertEquals(labels2, lMerged.get(NDArrayIndex.interval(nExamples1,nExamples1+nExamples2), NDArrayIndex.all()));
    }

    @Test
    public void testMixedRnn2d(){
        //RNN input with 2d label output
        //Basic test for time series, all of the same length + no masking arrays
        int numExamples = 10;
        int inSize = 13;
        int labelSize = 5;
        int tsLength = 15;

        Nd4j.getRandom().setSeed(12345);
        List<DataSet> list = new ArrayList<>(numExamples);
        for( int i=0; i<numExamples; i++ ){
            INDArray in = Nd4j.rand(new int[]{1,inSize,tsLength});
            INDArray out = Nd4j.rand(new int[]{1,labelSize});
            list.add(new DataSet(in,out));
        }

        DataSet merged = DataSet.merge(list);
        assertEquals(numExamples,merged.numExamples());

        INDArray f = merged.getFeatures();
        INDArray l = merged.getLabels();
        assertArrayEquals(new int[]{numExamples, inSize, tsLength}, f.shape());
        assertArrayEquals(new int[]{numExamples, labelSize}, l.shape());

        for( int i=0; i<numExamples; i++ ){
            DataSet exp = list.get(i);
            INDArray expIn = exp.getFeatureMatrix();
            INDArray expL = exp.getLabels();

            INDArray fSubset = f.get(NDArrayIndex.interval(i,i+1), NDArrayIndex.all(), NDArrayIndex.all());
            INDArray lSubset = l.get(NDArrayIndex.interval(i,i+1), NDArrayIndex.all());

            assertEquals(expIn, fSubset);
            assertEquals(expL,lSubset);
        }
    }

    @Test
    public void testShuffle4d() {
        int nSamples = 10;
        int nChannels = 3;
        int imgRows = 4;
        int imgCols = 2;
        
        int nLabels = 5;
        int[] shape = new int[]{nSamples, nChannels, imgRows, imgCols};

        int entries = nSamples * nChannels * imgRows * imgCols;
        int labels = nSamples * nLabels;
        
        INDArray ds_data = Nd4j.linspace(1, entries, entries).reshape(nSamples, nChannels, imgRows, imgCols);
        INDArray ds_labels = Nd4j.linspace(1, labels, labels).reshape(nSamples, nLabels);
        DataSet ds = new DataSet(ds_data, ds_labels);
        ds.shuffle();
        for (int dim = 1; dim < 4; dim++) {
            //get tensor along dimension - the order in every dimension but zero should be preserved
            for (int tensorNum = 0; tensorNum < entries / shape[dim]; tensorNum++) {
                for (int i = 0, j = 1; j < shape[dim]; i++, j++) {
                    int f_element = ds.getFeatures().tensorAlongDimension(tensorNum, dim).getInt(i);
                    int f_next_element = ds.getFeatures().tensorAlongDimension(tensorNum, dim).getInt(j);
                    int f_element_diff = f_next_element - f_element;
                    assertTrue(f_element_diff == ds_data.stride(dim)); 
                }
            }
        }
    }

    @Test
    public void testShuffleNd() {
        int numDims = 7;
        int nLabels = 3;
        Random r = new Random();

        
        int[] shape = new int[numDims];
        int entries = 1;
        for (int i=0;i<numDims;i++) {
            //randomly generating shapes bigger than 1 
            shape[i] = r.nextInt(4)+ 2;
            entries *= shape[i];
        }
        int labels = shape[0] * nLabels;

        INDArray ds_data = Nd4j.linspace(1, entries, entries).reshape(shape);
        INDArray ds_labels = Nd4j.linspace(1, labels, labels).reshape(shape[0],nLabels);

        DataSet ds = new DataSet(ds_data, ds_labels);
        ds.shuffle();

        //Checking Nd dataset which is the data
        for (int dim = 1; dim < numDims; dim++) {
            //get tensor along dimension - the order in every dimension but zero should be preserved
            for (int tensorNum = 0; tensorNum < ds_data.tensorssAlongDimension(dim); tensorNum++) {
                //the difference between consecutive elements should be equal to the stride
                for (int i = 0, j = 1; j < shape[dim]; i++, j++) {
                    int f_element = ds.getFeatures().tensorAlongDimension(tensorNum, dim).getInt(i);
                    int f_next_element = ds.getFeatures().tensorAlongDimension(tensorNum, dim).getInt(j);
                    int f_element_diff = f_next_element - f_element;
                    assertTrue(f_element_diff == ds_data.stride(dim));
                }
            }
        }

        //Checking 2d, features
        int dim = 1;
        //get tensor along dimension - the order in every dimension but zero should be preserved
        for (int tensorNum = 0; tensorNum < ds_labels.tensorssAlongDimension(dim); tensorNum++) {
            //the difference between consecutive elements should be equal to the stride
            for (int i = 0, j = 1; j < nLabels; i++, j++) {
                int l_element = ds.getLabels().tensorAlongDimension(tensorNum, dim).getInt(i);
                int l_next_element = ds.getLabels().tensorAlongDimension(tensorNum, dim).getInt(j);
                int l_element_diff = l_next_element - l_element;
                assertTrue(l_element_diff == ds_labels.stride(dim));
            }
        }
    }




    @Override
    public char ordering() {
        return 'f';
    }
}
