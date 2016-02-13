package jcuda.jcublas.ops;

import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.jcublas.buffer.allocation.PinnedMemoryStrategy;
import org.nd4j.linalg.jcublas.context.ContextHolder;

import static org.junit.Assert.assertEquals;

/**
 * @author raver119@gmail.com
 */
public class CudaBroadcastTests {

    @Test
    public void testPinnedAddiRowVector() throws Exception {
        // simple way to stop test if we're not on CUDA backend here
        assertEquals("JcublasLevel1", Nd4j.getBlasWrapper().level1().getClass().getSimpleName());

        // reset to default MemoryStrategy, most probable is Pinned
        ContextHolder.getInstance().forceMemoryStrategyForThread(new PinnedMemoryStrategy());

        assertEquals("PinnedMemoryStrategy", ContextHolder.getInstance().getMemoryStrategy().getClass().getSimpleName());

        INDArray array1 = Nd4j.zeros(15,15);
        INDArray array2 = Nd4j.create(new float[]{2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f});

        array1.addiRowVector(array2);

        System.out.println("Array1: " + array1);
        System.out.println("Array2: " + array2);

        assertEquals(2.0f, array1.getRow(0).getFloat(0), 0.01);
    }

    @Test
    public void testPinnedSubiRowVector() throws Exception {
        // simple way to stop test if we're not on CUDA backend here
        assertEquals("JcublasLevel1", Nd4j.getBlasWrapper().level1().getClass().getSimpleName());

        // reset to default MemoryStrategy, most probable is Pinned
        ContextHolder.getInstance().forceMemoryStrategyForThread(new PinnedMemoryStrategy());

        assertEquals("PinnedMemoryStrategy", ContextHolder.getInstance().getMemoryStrategy().getClass().getSimpleName());

        INDArray array1 = Nd4j.zeros(15,15);
        INDArray array2 = Nd4j.create(new float[]{2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f});

        array1.subiRowVector(array2);

        System.out.println("Array1: " + array1);
        System.out.println("Array2: " + array2);

        assertEquals(-2.0f, array1.getRow(0).getFloat(0), 0.01);
    }

    @Test
    public void testPinnedRSubiRowVector() throws Exception {
        // simple way to stop test if we're not on CUDA backend here
        assertEquals("JcublasLevel1", Nd4j.getBlasWrapper().level1().getClass().getSimpleName());

        // reset to default MemoryStrategy, most probable is Pinned
        ContextHolder.getInstance().forceMemoryStrategyForThread(new PinnedMemoryStrategy());

        assertEquals("PinnedMemoryStrategy", ContextHolder.getInstance().getMemoryStrategy().getClass().getSimpleName());

        INDArray array1 = Nd4j.zeros(15,15);
        INDArray array2 = Nd4j.create(new float[]{2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f});

        array1.rsubiRowVector(array2);

        System.out.println("Array1: " + array1);
        System.out.println("Array2: " + array2);

        assertEquals(2.0f, array1.getRow(0).getFloat(0), 0.01);
    }

    @Test
    public void testPinnedMulRowVector() throws Exception {
        // simple way to stop test if we're not on CUDA backend here
        assertEquals("JcublasLevel1", Nd4j.getBlasWrapper().level1().getClass().getSimpleName());

        // reset to default MemoryStrategy, most probable is Pinned
        ContextHolder.getInstance().forceMemoryStrategyForThread(new PinnedMemoryStrategy());

        assertEquals("PinnedMemoryStrategy", ContextHolder.getInstance().getMemoryStrategy().getClass().getSimpleName());

        INDArray array1 = Nd4j.zeros(15,15);
        array1.putRow(0, Nd4j.create(new float[]{2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f}));
        array1.putRow(1, Nd4j.create(new float[]{2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f}));
        INDArray array2 = Nd4j.create(new float[]{2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f});

        array1.muliRowVector(array2);

        System.out.println("Array1: " + array1);
        System.out.println("Array2: " + array2);

        assertEquals(4.0f, array1.getRow(0).getFloat(0), 0.01);
    }

    @Test
    public void testPinnedDivRowVector() throws Exception {
        // simple way to stop test if we're not on CUDA backend here
        assertEquals("JcublasLevel1", Nd4j.getBlasWrapper().level1().getClass().getSimpleName());

        // reset to default MemoryStrategy, most probable is Pinned
        ContextHolder.getInstance().forceMemoryStrategyForThread(new PinnedMemoryStrategy());

        assertEquals("PinnedMemoryStrategy", ContextHolder.getInstance().getMemoryStrategy().getClass().getSimpleName());

        INDArray array1 = Nd4j.zeros(15,15);
        array1.putRow(0, Nd4j.create(new float[]{2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f}));
        array1.putRow(1, Nd4j.create(new float[]{2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f}));
        INDArray array2 = Nd4j.create(new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f});

        array1.diviRowVector(array2);

        System.out.println("Array1: " + array1);
        System.out.println("Array2: " + array2);

        assertEquals(2.0f, array1.getRow(0).getFloat(0), 0.01);
    }

    @Test
    public void testPinnedRDivRowVector() throws Exception {
        // simple way to stop test if we're not on CUDA backend here
        assertEquals("JcublasLevel1", Nd4j.getBlasWrapper().level1().getClass().getSimpleName());

        // reset to default MemoryStrategy, most probable is Pinned
        ContextHolder.getInstance().forceMemoryStrategyForThread(new PinnedMemoryStrategy());

        assertEquals("PinnedMemoryStrategy", ContextHolder.getInstance().getMemoryStrategy().getClass().getSimpleName());

        INDArray array1 = Nd4j.zeros(15,15);
        array1.putRow(0, Nd4j.create(new float[]{2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f}));
        array1.putRow(1, Nd4j.create(new float[]{2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f, 2.0f}));
        INDArray array2 = Nd4j.create(new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f});

        array1.rdiviRowVector(array2);

        System.out.println("Array1: " + array1);
        System.out.println("Array2: " + array2);

        assertEquals(0.5f, array1.getRow(0).getFloat(0), 0.01);
    }
}
