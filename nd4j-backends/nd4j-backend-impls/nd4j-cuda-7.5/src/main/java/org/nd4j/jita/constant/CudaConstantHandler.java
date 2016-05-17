package org.nd4j.jita.constant;

import org.nd4j.jita.allocator.impl.AllocationPoint;
import org.nd4j.jita.allocator.impl.AtomicAllocator;
import org.nd4j.jita.allocator.pointers.CudaPointer;
import org.nd4j.jita.allocator.utils.AllocationUtils;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.jcublas.context.CudaContext;
import org.nd4j.nativeblas.NativeOps;
import org.nd4j.nativeblas.NativeOpsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author raver119@gmail.com
 */
public class CudaConstantHandler implements ConstantHandler {
    private static Logger logger = LoggerFactory.getLogger(CudaConstantHandler.class);
    protected Map<Integer, AtomicLong> constantOffsets = new HashMap<>();
    protected Map<Integer, Semaphore> deviceLocks = new ConcurrentHashMap<>();

    protected Map<Integer, Map<ArrayDescriptor, DataBuffer>> buffersCache = new HashMap<>();
    protected Map<Integer, Long> deviceAddresses = new HashMap<>();

    protected NativeOps nativeOps = NativeOpsHolder.getInstance().getDeviceNativeOps();

    protected Semaphore lock = new Semaphore(1);

    @Override
    public long moveToConstantSpace(DataBuffer dataBuffer) {
        // now, we move things to constant memory
        Integer deviceId = AtomicAllocator.getInstance().getDeviceId();
        ensureMaps(deviceId);

        AllocationPoint point = AtomicAllocator.getInstance().getAllocationPoint(dataBuffer);

        long requiredMemoryBytes = AllocationUtils.getRequiredMemory(point.getShape());
        // and release device memory :)

        long currentOffset = constantOffsets.get(deviceId).get();
        CudaContext context = (CudaContext) AtomicAllocator.getInstance().getDeviceContext().getContext();
        if (currentOffset >= 49152 || requiredMemoryBytes > 272)  {
            nativeOps.memcpyAsync(point.getPointers().getDevicePointer().address(), point.getPointers().getHostPointer().address(), requiredMemoryBytes, 1, context.getOldStream().getNativePointer());
            point.setConstant(true);
            point.tickDeviceWrite();
            point.tickHostRead();
            return 0;
        }

        currentOffset = constantOffsets.get(deviceId).getAndAdd(requiredMemoryBytes);
        if (currentOffset >= 49152)  {
            nativeOps.memcpyAsync(point.getPointers().getDevicePointer().address(), point.getPointers().getHostPointer().address(), requiredMemoryBytes, 1, context.getOldStream().getNativePointer());
            point.setConstant(true);
            point.tickDeviceWrite();
            point.tickHostRead();
            return 0;
        }


        nativeOps.memcpyConstantAsync(currentOffset, point.getPointers().getHostPointer().address(), requiredMemoryBytes, 1, context.getOldStream().getNativePointer());
        long cAddr = deviceAddresses.get(deviceId).longValue()  + currentOffset;
        point.getPointers().setDevicePointer(new CudaPointer(cAddr));
        point.setConstant(true);
        point.tickDeviceWrite();
        point.tickHostRead();

        return cAddr;
    }

    private void ensureMaps(Integer deviceId) {
        if (!buffersCache.containsKey(deviceId)) {
            try {
                lock.acquire();
                if (!buffersCache.containsKey(deviceId)) {
                    buffersCache.put(deviceId, new ConcurrentHashMap<ArrayDescriptor, DataBuffer>());
                    constantOffsets.put(deviceId, new AtomicLong(0));
                    deviceLocks.put(deviceId, new Semaphore(1));

                    long cAddr = nativeOps.getConstantSpace();
         //           logger.info("Got constant address: [{}]", cAddr);

                    deviceAddresses.put(deviceId, cAddr);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.release();
            }
        }
    }

    @Override
    public DataBuffer getConstantBuffer(int[] array) {
      //  logger.info("getConstantBuffer(int[]) called");
        ArrayDescriptor descriptor = new ArrayDescriptor(array);

        Integer deviceId = AtomicAllocator.getInstance().getDeviceId();

        ensureMaps(deviceId);

        if (!buffersCache.get(deviceId).containsKey(descriptor)) {
            // we create new databuffer
        //    logger.info("Creating new constant buffer...");
            DataBuffer buffer = Nd4j.createBuffer(array);

            // now we move data to constant memory, and keep happy
            moveToConstantSpace(buffer);

            buffersCache.get(deviceId).put(descriptor, buffer);
            return buffer;
        } //else logger.info("Reusing constant buffer...");

        return buffersCache.get(deviceId).get(descriptor);
    }

    @Override
    public DataBuffer getConstantBuffer(float[] array) {
     //   logger.info("getConstantBuffer(float[]) called");
        ArrayDescriptor descriptor = new ArrayDescriptor(array);

        Integer deviceId = AtomicAllocator.getInstance().getDeviceId();

        ensureMaps(deviceId);

        if (!buffersCache.get(deviceId).containsKey(descriptor)) {
            // we create new databuffer
       //     logger.info("Creating new constant buffer...");
            DataBuffer buffer = Nd4j.createBuffer(array);

            // now we move data to constant memory, and keep happy
            moveToConstantSpace(buffer);

            buffersCache.get(deviceId).put(descriptor, buffer);
            return buffer;
        } // else logger.info("Reusing constant buffer...");

        return buffersCache.get(deviceId).get(descriptor);
    }

    @Override
    public DataBuffer getConstantBuffer(double[] array) {
//        logger.info("getConstantBuffer(double[]) called");
        ArrayDescriptor descriptor = new ArrayDescriptor(array);

        Integer deviceId = AtomicAllocator.getInstance().getDeviceId();

        ensureMaps(deviceId);

        if (!buffersCache.get(deviceId).containsKey(descriptor)) {
            // we create new databuffer
            //logger.info("Creating new constant buffer...");
            DataBuffer buffer = Nd4j.createBuffer(array);

            // now we move data to constant memory, and keep happy
            moveToConstantSpace(buffer);

            buffersCache.get(deviceId).put(descriptor, buffer);
            return buffer;
        } //else logger.info("Reusing constant buffer...");

        return buffersCache.get(deviceId).get(descriptor);
    }
}
