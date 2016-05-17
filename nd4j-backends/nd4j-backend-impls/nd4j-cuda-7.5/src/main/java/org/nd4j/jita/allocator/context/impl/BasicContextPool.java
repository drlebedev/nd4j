package org.nd4j.jita.allocator.context.impl;

import org.apache.commons.lang3.RandomUtils;
import org.nd4j.jita.allocator.context.ContextPack;
import org.nd4j.jita.allocator.context.ContextPool;
import org.nd4j.jita.allocator.pointers.cuda.CUcontext;
import org.nd4j.jita.allocator.pointers.cuda.cublasHandle_t;
import org.nd4j.jita.allocator.pointers.cuda.cudaStream_t;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.jcublas.context.CudaContext;
import org.nd4j.nativeblas.NativeOps;
import org.nd4j.nativeblas.NativeOpsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * This is context pool implementation, addressing shared cublas allocations together with shared stream pools
 *
 * Each context given contains:
 * 1. Stream for custom kernel invocations.
 * 2. cuBLAS handle tied with separate stream.
 *
 * @author raver119@gmail.com
 */
public class BasicContextPool implements ContextPool {
    // TODO: number of max threads should be device-dependant
    protected static final int MAX_STREAMS_PER_DEVICE = Integer.MAX_VALUE - 1;

    protected volatile Map<Integer, CUcontext> cuPool = new ConcurrentHashMap<>();

    protected volatile Map<Integer, cublasHandle_t> cublasPool = new ConcurrentHashMap<>();

    protected volatile Map<Long, CudaContext> contextsPool = new ConcurrentHashMap<>();

    protected volatile Map<Integer, Map<Integer, CudaContext>> contextsForDevices = new ConcurrentHashMap<>();

    protected Semaphore lock = new Semaphore(1);

    protected static Logger logger = LoggerFactory.getLogger(BasicContextPool.class);

    protected NativeOps nativeOps = NativeOpsHolder.getInstance().getDeviceNativeOps();

    public boolean containsContextForThread(long threadId) {
        return contextsPool.containsKey(threadId);
    }

    public CudaContext getContextForDevice(Integer deviceId) {
        return acquireContextForDevice(deviceId);
    }

    @Override
    public CudaContext acquireContextForDevice(Integer deviceId) {
        /*
            We should check, if we have context for this specific thread/device
            If we don't have context for this thread - we should stick to one of existent contexts available at pool
         */
        Long threadId = Thread.currentThread().getId();
        if (!contextsPool.containsKey(threadId)) {
            // we don't have attached context for this thread. we should pick up existing context for target device (if any).

            try {
                // this is lockable thing, but since it locks once per thread initialization, performance impact won't be big
                lock.acquire();
                // we create 1 CUcontext per device, which will be shared for all threads/streams on this device
/*
                if (!cuPool.containsKey(deviceId)) {
                    CUcontext cuContext = createNewContext(deviceId);
                    cuPool.put(deviceId, cuContext);
                }

                int result = JCudaDriver.cuCtxSetCurrent(cuPool.get(deviceId));
                if (result != CUresult.CUDA_SUCCESS) {
                    throw new RuntimeException("Failed to set context on assigner");
                }
*/
                if (!contextsForDevices.containsKey(deviceId)) {
                    contextsForDevices.put(deviceId, new ConcurrentHashMap<Integer, CudaContext>());
                }

                // if we hadn't hit MAX_STREAMS_PER_DEVICE limit - we add new stream. Otherwise we use random one.
                if (contextsForDevices.get(deviceId).size() < MAX_STREAMS_PER_DEVICE) {
                    logger.debug("Creating new context...");
                    CudaContext context = createNewStream(deviceId);

                    getDeviceBuffers(context, deviceId);

                    if (contextsForDevices.get(deviceId).size() == 0) {
                        // if we have no contexts created - it's just awesome time to attach cuBLAS handle here
                        logger.debug("Creating new cuBLAS handle for device ["+deviceId+"]...");

                        cudaStream_t cublasStream = createNewStream(deviceId).getOldStream();

                        cublasHandle_t handle = createNewCublasHandle(cublasStream);
                        context.setHandle(handle);
                        context.setCublasStream(cublasStream);

                        cublasPool.put(deviceId, handle);
                    } else {
                        // just pick handle out there
                        logger.debug("Reusing blas here...");
                        cublasHandle_t handle = cublasPool.get(deviceId);
                        context.setHandle(handle);

                        // TODO: actually we don't need this anymore
//                        cudaStream_t cublasStream = new cudaStream_t();
                  //      JCublas2.cublasGetStream(handle, cublasStream);
  //                      context.setCublasStream(cublasStream);
                    }

                    // we need this sync to finish memset
                    context.syncOldStream();

                    contextsPool.put(threadId, context);
                    contextsForDevices.get(deviceId).put(contextsForDevices.get(deviceId).size(), context);

                    return context;
                } else {
                    Integer rand = RandomUtils.nextInt(0, MAX_STREAMS_PER_DEVICE);
                    logger.debug("Reusing context: " + rand);

                    nativeOps.setDevice(deviceId);

                    CudaContext context = contextsForDevices.get(deviceId).get(rand);

                    contextsPool.put(threadId, context);
                    return context;
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.release();
            }
        }

        return contextsPool.get(threadId);
    }

    protected CudaContext createNewStream(Integer deviceId) {
        logger.debug("Creating new stream for device ["+deviceId+"]...");
        //JCuda.cudaSetDevice(deviceId);
        nativeOps.setDevice(deviceId);

        CudaContext context = new CudaContext();
        context.initOldStream();

        //context.initHandle();
        //context.associateHandle();
        //context.initStream();

        return context;
    }

    protected cublasHandle_t createNewCublasHandle(cudaStream_t stream) {
        cublasHandle_t handle = new cublasHandle_t(nativeOps.createBlasHandle());
        //JCublas2.cublasCreate(handle);
        //JCublas2.cublasSetStream(handle,stream);
        //nativeOps.setBlasStream(handle.address(), stream.address());

        return handle;
    }

    protected CUcontext createNewContext(Integer deviceId) {
        /*
        logger.debug("Creating new CUcontext...");
        CUdevice device = new CUdevice();
        CUcontext context = new CUcontext();

        //JCuda.cudaSetDevice(deviceId);


        int result = cuDeviceGet(device, deviceId);
        if (result != CUresult.CUDA_SUCCESS) {
            throw new RuntimeException("Failed to setDevice on driver");
        }

        result = cuCtxCreate(context, 0, device);
        if (result != CUresult.CUDA_SUCCESS) {
            throw new RuntimeException("Failed to create context on driver");
        }

        return context;
        */
        return null;
    }

    /**
     * This methods reset everything in pool, forcing recreation of all streams
     *
     * PLEASE NOTE: This is debugging-related method, and should NOT be used in real tasks
     */
    public synchronized void resetPool(int deviceId) {
        /*
        for (CUcontext cuContext: cuPool.values()) {
            logger.debug("Destroying context: " + cuContext);
            JCudaDriver.cuCtxDestroy(cuContext);
        }

        cuPool.clear();
        contextsForDevices.clear();
        contextsPool.clear();
        cublasPool.clear();

        acquireContextForDevice(deviceId);
        */
    }

    public CUcontext getCuContextForDevice(Integer deviceId) {
        return cuPool.get(deviceId);
    }

    /**
     * This method is used to allocate
     * @param context
     * @param deviceId
     */
    protected void getDeviceBuffers(CudaContext context, int deviceId) {
        NativeOps nativeOps = NativeOpsHolder.getInstance().getDeviceNativeOps(); //((JCudaExecutioner) Nd4j.getExecutioner()).getNativeOps();

        // we hardcode sizeOf to sizeOf(double)
        int sizeOf = 8;

        long  reductionPointer = nativeOps.mallocDevice(2049 * sizeOf * 2, deviceId, 0);
        if (reductionPointer == 0)
            throw new IllegalStateException("Can't allocate [DEVICE] reduction buffer memory!");

        nativeOps.memsetAsync(reductionPointer, 0, 2049 * sizeOf * 2, 0, context.getOldStream().address());

        context.syncOldStream();

        long  allocationPointer = nativeOps.mallocDevice(1024 * 1024, deviceId, 0);
        if (allocationPointer == 0)
            throw new IllegalStateException("Can't allocate [DEVICE] allocation buffer memory!");

        long  scalarPointer = nativeOps.mallocHost(1 * sizeOf, 0);
        if (scalarPointer == 0)
            throw new IllegalStateException("Can't allocate [HOST] scalar buffer memory!");

   //     Pointer dPtr = new Pointer();
   //     Pointer hPtr = new CudaPointer(scalarPointer);

/*
        JCuda.cudaHostGetDevicePointer(
                dPtr,
                hPtr,
                0);
*/
        context.setBufferScalar(scalarPointer);
        context.setBufferAllocation(allocationPointer);
        context.setBufferReduction(reductionPointer);

        long  specialPointer = nativeOps.mallocDevice(1024 * 1024 * sizeOf, deviceId, 0);
        if (specialPointer == 0)
            throw new IllegalStateException("Can't allocate [DEVICE] special buffer memory!");
/*
        dPtr = new Pointer();
        hPtr = new Pointer(specialPointer);

        JCuda.cudaHostGetDevicePointer(
                dPtr,
                hPtr,
                0);
*/
//        JCuda.cudaMemsetAsync(dPtr,0,65536 * sizeOf, context.getOldStream());
        nativeOps.memsetAsync(specialPointer, 0, 65536 * sizeOf, 0, context.getOldStream().address());

        context.setBufferSpecial(specialPointer);
    }

    public ContextPack acquireContextPackForDevice(Integer deviceId) {
        return new ContextPack(acquireContextForDevice(deviceId));
    }
}
