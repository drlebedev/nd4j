package org.nd4j.linalg.api.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.Test;
import org.nd4j.linalg.BaseNd4jTest;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.iter.NdIndexIterator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.Nd4jBackend;

public class TestReadWrite extends BaseNd4jTest {
    public TestReadWrite() {
    }

    public TestReadWrite(Nd4jBackend backend) {
        super(backend);
    }

    public TestReadWrite(String name) {
        super(name);
    }

    public TestReadWrite(String name, Nd4jBackend backend) {
        super(name, backend);
    }

    @Test
    public void testLoadingSavingDouble() throws Exception {
        Nd4j.factory().setDType(DataBuffer.Type.DOUBLE);
        Nd4j.dtype = DataBuffer.Type.DOUBLE;
        INDArray randF = Nd4j.create(new int[]{5, 3}, 'f');
        INDArray randC = Nd4j.create(new int[]{5, 3}, 'c');
        randF.assign(Nd4j.rand(new int[]{5, 3}));
        randC.assign(randF);
        assertEquals(randF, randC);

        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        DataOutputStream dos1 = new DataOutputStream(baos1);
        Nd4j.write(randF, dos1);
        dos1.close();

        byte[] bytes1 = baos1.toByteArray();
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        DataOutputStream dos2 = new DataOutputStream(baos2);
        Nd4j.write(randF, dos2);
        dos2.close();

        byte[] bytes2 = baos2.toByteArray();
        ByteArrayInputStream bais1 = new ByteArrayInputStream(bytes1);
        DataInputStream dis1 = new DataInputStream(bais1);
        INDArray randFCopy = Nd4j.read(dis1);

        ByteArrayInputStream bais2 = new ByteArrayInputStream(bytes2);
        DataInputStream dis2 = new DataInputStream(bais2);
        INDArray randCCopy = Nd4j.read((DataInputStream) dis2);

        //Should be no issues with lost precision etc (should be byte-for-byte copy)
        TestReadWrite.assertEquals(randF, randFCopy);
        TestReadWrite.assertEquals(randC, randCCopy);

        //do double -> float
        Nd4j.factory().setDType(DataBuffer.Type.FLOAT);
        Nd4j.dtype = DataBuffer.Type.FLOAT;
        bais1 = new ByteArrayInputStream(bytes1);
        dis1 = new DataInputStream(bais1);
        INDArray randFCopyFloat = Nd4j.read(dis1);
        TestReadWrite.assertEquals(randFCopyFloat.data().dataType(), DataBuffer.Type.FLOAT);

        if (randFCopyFloat.data().allocationMode() == DataBuffer.AllocationMode.HEAP) {
            TestReadWrite.assertTrue(randFCopyFloat.data().array() instanceof float[]);
        }

        NdIndexIterator iter = new NdIndexIterator(randF.shape());
        while (iter.hasNext()) {
            int[] next = iter.next();
            TestReadWrite.assertEquals(randF.getDouble(next), randFCopyFloat.getDouble(next), 1.0E-4);
        }

        bais2 = new ByteArrayInputStream(bytes2);
        dis2 = new DataInputStream(bais2);
        INDArray randCCopyFloat = Nd4j.read(dis2);
        TestReadWrite.assertEquals(randCCopyFloat.data().dataType(), DataBuffer.Type.FLOAT);
        if (randCCopyFloat.data().allocationMode() == DataBuffer.AllocationMode.HEAP) {
            TestReadWrite.assertTrue(randCCopyFloat.data().array() instanceof float[]);
        }
        iter = new NdIndexIterator(randC.shape());
        while (iter.hasNext()) {
            int[] next = iter.next();
            TestReadWrite.assertEquals(randC.getDouble(next), randCCopyFloat.getDouble(next), 1.0E-4);
        }
    }

    public char ordering() {
        return 'c';
    }
}