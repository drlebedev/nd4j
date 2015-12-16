package org.nd4j.linalg.dataset;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.nd4j.linalg.BaseNd4jTest;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4jBackend;

import java.io.IOException;


/**
 * Created by agibsonccc on 9/10/15.
 */
public class MiniBatchFileDataSetIteratorTest extends BaseNd4jTest {
    public MiniBatchFileDataSetIteratorTest() {
    }

    public MiniBatchFileDataSetIteratorTest(Nd4jBackend backend) {
        super(backend);
    }

    public MiniBatchFileDataSetIteratorTest(String name) {
        super(name);
    }

    public MiniBatchFileDataSetIteratorTest(String name, Nd4jBackend backend) {
        super(name, backend);
    }

    @Test
    public void testMiniBatches() throws Exception {
        DataSet load = new IrisDataSetIterator(150,150).next();
        final MiniBatchFileDataSetIterator iter = new MiniBatchFileDataSetIterator(load,10,false);
        while(iter.hasNext())
            assertEquals(10,iter.next().numExamples());
        if(iter.getRootDir() == null)
            return;
        DataSetIterator existing = new ExistingMiniBatchDataSetIterator(iter.getRootDir());
        while(iter.hasNext())
            assertEquals(10,existing.next().numExamples());


        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileUtils.deleteDirectory(iter.getRootDir());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));

    }

    @Override
    public char ordering() {
        return 'f';
    }
}

