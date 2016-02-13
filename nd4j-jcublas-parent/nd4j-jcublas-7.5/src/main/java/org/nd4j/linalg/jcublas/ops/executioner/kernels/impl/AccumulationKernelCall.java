package org.nd4j.linalg.jcublas.ops.executioner.kernels.impl;

import jcuda.jcublas.JCublas;
import jcuda.runtime.JCuda;
import org.nd4j.linalg.api.blas.BlasBufferUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.*;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.jcublas.buffer.JCudaBuffer;
import org.nd4j.linalg.jcublas.gpumetrics.GpuMetrics;
import org.nd4j.linalg.jcublas.kernel.KernelFunctions;
import org.nd4j.linalg.jcublas.ops.executioner.kernels.BaseGpuKernelCall;
import org.nd4j.linalg.jcublas.ops.executioner.kernels.GpuKernelCall;
import org.nd4j.linalg.jcublas.ops.executioner.kernels.args.KernelCallPointerArgs;
import org.nd4j.linalg.jcublas.ops.executioner.kernels.args.impl.AccumulationKernelCallPointerArgs;
import org.nd4j.linalg.jcublas.util.CudaArgs;
import org.nd4j.linalg.jcublas.util.KernelParamsWrapper;
import org.nd4j.linalg.jcublas.util.PointerUtil;
import org.nd4j.linalg.util.ArrayUtil;

import java.util.Arrays;

/**
 * Kernel call
 *  for accumulation
 *
 * @author Adam Gibson
 */
public class AccumulationKernelCall extends BaseGpuKernelCall {
    protected int[] dimension;
    protected int xStride;
    protected int yStride;
    protected boolean scalarResult;
    protected int[] multiDimension;
    protected int resultIndex,resultShapeInfoIndex;

    /**
     * Accumulation kernel call
     * @param op the op to use
     * @param dimension the dimensions for reduction
     */
    public AccumulationKernelCall(Op op,int[] dimension) {
        super(op);

        if(dimension == null)
            dimension = new int[] {Integer.MAX_VALUE};
        this.dimension = dimension;
        if(dimension[0] == Integer.MAX_VALUE) {
            scalarResult = true;
        }

        if(dimension.length > 1) {
            if(dimension.length == op.x().rank()) {
                this.dimension = new int[] {Integer.MAX_VALUE};
                scalarResult = true;
            }
            else {
                //the dimensions need to be in order
                Arrays.sort(dimension);
                this.multiDimension = dimension;
                //switch it to be being only the last dimension
                //the tad will be the prod of the previous dimensions
                this.dimension = new int[] {dimension[dimension.length - 1]};
            }

        }

        if(scalarResult)
            op.setZ(Nd4j.create(metrics.getGridSize()));
        else {
            op.setZ(Nd4j.create(ArrayUtil.removeIndex(op.x().shape(),dimension)));
        }

        createArgs();

    }



    @Override
    public void createMetrics() {
        String functionName = CudaArgs.getModuleNameFor(op);
        GpuMetrics metrics = GpuMetrics.blocksAndThreadsOccupancy(functionName, getType(op), op.n());
        if (dimension != null && dimension.length >= 1 && dimension[0] != Integer.MAX_VALUE) {
            int length = op.x().tensorssAlongDimension(dimension);
            if (length > 1000)
                length = 1000;
            int sharedMemBasedOnBlockSize = op.x().tensorAlongDimension(0, dimension).length() * 10 * op.x().data().getElementSize();
            if (sharedMemBasedOnBlockSize < 1024)
                sharedMemBasedOnBlockSize = 1024;
            metrics.setSharedMemoryNotOverMax(sharedMemBasedOnBlockSize);
        } else {
            int sharedMemBasedOnBlockSize = op.n() * op.x().data().getElementSize();
            if (sharedMemBasedOnBlockSize < 1024)
                sharedMemBasedOnBlockSize = 1024;
            metrics.setSharedMemoryNotOverMax(sharedMemBasedOnBlockSize);
            //setup a number of threads = the number of blocks being launched
        }

        this.metrics = metrics;


    }

    @Override
    public void createArgs() {
        if (op.y() != null) {


            metrics.setSharedMemoryNotOverMax(metrics.getSharedMemory() * 2);
            xStride = BlasBufferUtil.getBlasStride(dimension == null ? op.x() : op.x().tensorAlongDimension(0, dimension));
            if (xStride < 0) {
                op.setX(op.x().dup());
                xStride = BlasBufferUtil.getBlasStride(dimension == null ? op.x() : op.x().tensorAlongDimension(0, dimension));
                if (xStride < 0)
                    throw new IllegalStateException("Unable to compute element wise stride");

            }

            yStride = BlasBufferUtil.getBlasStride(dimension == null ? op.y() : op.y().tensorAlongDimension(0, dimension));
            if (op.y().ordering() != op.x().ordering()) {
                op.setY(op.y().dup(op.x().ordering()));
                yStride = BlasBufferUtil.getBlasStride(dimension == null ? op.y() : op.y().tensorAlongDimension(0, dimension));
                if (yStride < 0)
                    throw new IllegalStateException("Unable to compute element wise stride");

            }
            resultIndex = 5;
            resultShapeInfoIndex = resultIndex + 1;
            //result index for the pointer to use when invoking the post process method
            args = new Object[] {
                    CudaArgs.getOpCode(op),
                    op.n(),
                    op.x(),
                    KernelFunctions.alloc(PointerUtil.toShapeInfoBuffer(op.x(), dimension)),
                    op.y(),
                    KernelFunctions.alloc(PointerUtil.toShapeInfoBuffer(op.y(), dimension)),
                    toArgs(op.extraArgs(),
                            getType(op)),
                    op.z(),
                    KernelFunctions.alloc(PointerUtil.toShapeInfoBuffer(op.z())),
                    KernelFunctions.alloc(metrics.getGpuDefinitionInfo()),
                    KernelFunctions.alloc(dimension == null ? new int[]{Integer.MAX_VALUE} : dimension),
                    dimension == null ? 1 : dimension.length,
                    //if the whole buffer is to be used don't do final aggregation this happens
                    //by aggregating blocks on cpu first
                    toInt((dimension == null || dimension[0] == Integer.MAX_VALUE))
            };


        } else {
            INDArray firstTad = null;
            //handle case where the tad is actually the whole array
            if (!scalarResult) {
                firstTad = op.x().tensorAlongDimension(0, dimension);
                if (firstTad.length() == op.x().length()) {
                    dimension = null;
                }
            }

            xStride = BlasBufferUtil.getBlasStride(scalarResult ? op.x() : firstTad);
            if (xStride < 0) {
                op.setX(op.x().dup());
                xStride = BlasBufferUtil.getBlasStride(scalarResult ? op.x() : firstTad);
                //dup didn't handle it
                if (xStride < 0) {
                    throw new IllegalStateException("Unable to compute element wise stride for x");}
            }

            int sharedMemBasedOnBlockSize = op.n() * op.x().data().getElementSize();
            if (sharedMemBasedOnBlockSize < 1024)
                sharedMemBasedOnBlockSize = 1024;
            metrics.setSharedMemoryNotOverMax(sharedMemBasedOnBlockSize);


            int length = op.x().data().length();
            if (dimension == null && xStride == 1 && op.x().offset() == 0)
                length = op.n();
            resultIndex = 4;
            resultShapeInfoIndex = resultIndex + 1;

            //result index for the pointer to use when invoking the post process method
            args = new Object[] {
                    CudaArgs.getOpCode(op),
                    length,
                    op.x(),
                    KernelFunctions.alloc(PointerUtil.toShapeInfoBuffer(op.x(), dimension)),
                    toArgs(op.extraArgs(), getType(op)),
                    op.z(),
                    KernelFunctions.alloc(PointerUtil.toShapeInfoBuffer(op.z())),
                    KernelFunctions.alloc(metrics.getGpuDefinitionInfo()),
                    KernelFunctions.alloc(scalarResult ? new int[]{Integer.MAX_VALUE} : dimension),
                    scalarResult ? 1 : dimension.length,
                    //if the whole buffer is to be used don't do final aggregation this happens
                    //by aggregating blocks on cpu first
                    toInt(scalarResult)
            };
        }
    }



    /**
     * Calculates a reduction across blocks
     * @param op
     * @param resultAcrossBlocks
     */
    public static  void calculateBlockResult(Accumulation op,INDArray resultAcrossBlocks) {
        int oldN = op.n();
        INDArray oldX = op.x();
        op.setX(resultAcrossBlocks);
        op.setApplyFinalTransform(false);
        double result = op.zeroDouble();
        for(int i = 0; i < resultAcrossBlocks.length(); i++) {
            double firstVal = resultAcrossBlocks.data().getDouble(resultAcrossBlocks.offset() + i * resultAcrossBlocks.elementWiseStride());
            result = op.combineSubResults(firstVal,result);
        }

        if(resultAcrossBlocks.length() == 1)
            result = resultAcrossBlocks.getDouble(0);

        op.setFinalResult(result);
        op.setApplyFinalTransform(true);
        op.setN(oldN);
        op.setX(oldX);
        op.getAndSetFinalResult(op.getFinalResult().doubleValue());
    }



    @Override
    public void invoke() {
        Accumulation acc = (Accumulation) op;
        INDArray tempResult;

        JCudaBuffer tempResultInfo;
        //allocate the memory for the temp solution
        if(multiDimension != null) {
            tempResult = Nd4j.create(ArrayUtil.removeIndex(acc.x().shape(),dimension));
            args[resultIndex] = tempResult;
            op.setZ(tempResult);
            tempResultInfo = KernelFunctions.alloc(PointerUtil.toShapeInfoBuffer(op.z()));
            args[resultShapeInfoIndex] = tempResultInfo;
        }

        try(KernelParamsWrapper kParams = new KernelParamsWrapper(true,args).setResultOp(acc, op.z(),dimension)) {
            //setup the kernel parameters such that super.invoke() will call the kernel with the given parameters
            this.args = kParams.getKernelParameters();
            this.cudaContext = kParams.getContext();


            /*
                FIXME: At this place we assume that multiDimension CAN be null, and at the same time there's cycle for multiDimension.length
                temp workaround: check for null added, detailed algo investigation required
             */

            boolean collapseTad = multiDimension != null;
            if(collapseTad)
                acc.setApplyFinalTransform(false);
            KernelCallPointerArgs devicePointers = getPointers();

            if (multiDimension != null) for(int i = multiDimension.length - 1; i >= 0 ; i--) {
                //invoke basic reduce
                super.invoke();
            } else {
                super.invoke();
            }

            //invoke the collapse tad
            if(collapseTad) {
                System.out.println("cTAD");
                TadCollapseAccumulation collapseAccumulation = new TadCollapseAccumulation(this.op,multiDimension,this.dimension,false);
                GpuKernelCall collapseKernelCall = new CollapseAccumuationKernelCall(
                        getPointers(),collapseAccumulation,metrics,cudaContext);
                collapseKernelCall.invoke();
            }

            //collapse dimension result
            //dimension result
            /*if(dimension != null && dimension[0] != Integer.MAX_VALUE) {
                GpuKernelCall postProcessCall = new PostProcessKernelCall(
                        op,dimension,getPointers(),args,cudaContext);
                postProcessCall.invoke();

            }*/
        } catch(Exception e) {
            throw new RuntimeException("Could not execute kernel", e);
        }

    }

    @Override
    public KernelCallPointerArgs getPointers() {
        return new AccumulationKernelCallPointerArgs(op,args);
    }


    private int toInt(boolean val) {
        return val ? 1 : 0;
    }

}