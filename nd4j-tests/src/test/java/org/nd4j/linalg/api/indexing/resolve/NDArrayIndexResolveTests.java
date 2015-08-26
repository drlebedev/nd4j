package org.nd4j.linalg.api.indexing.resolve;

import org.junit.Test;
import org.nd4j.linalg.BaseNd4jTest;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.Nd4jBackend;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;

import static org.junit.Assert.*;

/**
 * @author Adam Gibson
 */
public class NDArrayIndexResolveTests extends BaseNd4jTest {

    public NDArrayIndexResolveTests(String name, Nd4jBackend backend) {
        super(name, backend);
    }

    @Test
    public void testResolvePoint() {
        INDArray arr = Nd4j.linspace(1,4,4).reshape(2, 2);
        INDArrayIndex[] test = NDArrayIndex.resolve(arr.shape(), NDArrayIndex.point(1));
        INDArrayIndex[] assertion = {NDArrayIndex.point(1),NDArrayIndex.all()};
        assertArrayEquals(assertion,test);

        INDArrayIndex[] allAssertion = {NDArrayIndex.all(),NDArrayIndex.all()};
        assertArrayEquals(allAssertion, NDArrayIndex.resolve(arr.shape(),NDArrayIndex.all()));

        INDArrayIndex[] allAndOne = new INDArrayIndex[] {NDArrayIndex.all(),NDArrayIndex.point(1)};
        assertArrayEquals(allAndOne,NDArrayIndex.resolve(arr.shape(),allAndOne));
    }


    @Test
    public void testResolvePointVector() {
        INDArray arr = Nd4j.linspace(1,4,4);
        INDArrayIndex[] getPoint  = {NDArrayIndex.point(1)};
        assertArrayEquals(getPoint,NDArrayIndex.resolve(arr.shape(),getPoint));
    }

    @Override
    public char ordering() {
        return 'f';
    }
}
