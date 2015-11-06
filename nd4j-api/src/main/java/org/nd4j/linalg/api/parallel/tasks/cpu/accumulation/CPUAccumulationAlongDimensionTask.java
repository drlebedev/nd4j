package org.nd4j.linalg.api.parallel.tasks.cpu.accumulation;

import lombok.AllArgsConstructor;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.Accumulation;
import org.nd4j.linalg.api.ops.executioner.OpExecutionerUtil;
import org.nd4j.linalg.api.parallel.tasks.BaseTask;
import org.nd4j.linalg.api.parallel.tasks.Task;
import org.nd4j.linalg.api.parallel.tasks.TaskExecutorProvider;
import org.nd4j.linalg.api.parallel.tasks.cpu.BaseCPUTask;
import org.nd4j.linalg.api.shape.tensor.TensorCalculator;
import org.nd4j.linalg.api.shape.tensor.TensorCalculatorFactory;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;


public class CPUAccumulationAlongDimensionTask extends BaseCPUTask<INDArray> {
    protected final Accumulation op;
    protected final int[] dimensions;

    protected List<Task<Double>> subTasks;

    public CPUAccumulationAlongDimensionTask(Accumulation op, int parallelThreshold, int... dimensions) {
        super(op, parallelThreshold);
        this.op = op;
        this.dimensions = dimensions;
    }

    @Override
    public INDArray blockUntilComplete() {
        if (future == null) {
            //invokeAsync() not called?
            invokeAsync();
        }

        INDArray ret = null;
        try {
            ret = future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(ret != null) return ret; //ForkJoin

        //ExecutorService
        int[] retShape = ArrayUtil.removeIndex(op.x().shape(), dimensions);
        INDArray out = Nd4j.create(retShape);
        int i = 0;
        for (Task<Double> task : subTasks) {
            out.putScalar(i++, task.blockUntilComplete());
        }
        op.setZ(out);
        return out;
    }

    @Override
    public INDArray call() {
        //Callable: Iterative decomposition
        int nTensors = op.x().tensorssAlongDimension(dimensions);
        subTasks = new ArrayList<>(nTensors);

        for (int i = 0; i < nTensors; i++) {
            Task<Double> task = new OpForDimTask(i);
            task.invokeAsync();
            subTasks.add(task);
        }
        return null;
    }

    @Override
    public INDArray compute() {
        //Fork Join: Recursive decomposition

        if(dimensions.length == 1 && !op.isPassThrough()){
            TensorCalculator tCalcx = TensorCalculatorFactory.getTensorCalculator(op.x(), dimensions[0]);
            TensorCalculator tCalcy;
            if(op.y() != null) tCalcy = TensorCalculatorFactory.getTensorCalculator(op.y(), dimensions[0]);
            else tCalcy = null;

            int[] retShape = ArrayUtil.removeIndex(op.x().shape(), dimensions);
            INDArray out = Nd4j.create(retShape);

            RecursiveAction action = new CPUAccumulations1dAction(op,threshold,tCalcx, tCalcy, 0,
                    tCalcx.getNumTensors()-1, out);
            action.invoke();
            op.setZ(out);
            return out;

        } else {

            int nTensors = op.x().tensorssAlongDimension(dimensions);
            List<RecursiveTask<Double>> subTasks = new ArrayList<>(nTensors);

            for (int i = 0; i < nTensors; i++) {
                RecursiveTask<Double> task = new OpForDimTaskFJ(i);
                task.fork();
                subTasks.add(task);
            }

            int[] retShape = ArrayUtil.removeIndex(op.x().shape(), dimensions);
            INDArray out = Nd4j.create(retShape);
            int i = 0;
            for (RecursiveTask<Double> task : subTasks) {
                out.putScalar(i++, task.join());
            }
            op.setZ(out);
            return out;
        }
    }

    private class OpForDimTask extends BaseTask<Double>  {
        private int tensorNum;
        private BaseCPUTask<Double> subTask;
        private Future<Double> future;

        public OpForDimTask(int tensorNum){
            this.tensorNum = tensorNum;
        }

        @Override
        public void invokeAsync() {
            this.future = TaskExecutorProvider.getTaskExecutor().executeAsync(this);
        }

        @Override
        public Double blockUntilComplete() {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return subTask.blockUntilComplete();
        }

        @Override
        public Double call() {
            Accumulation opOnDimension = (Accumulation) op.opForDimension(tensorNum, dimensions);
            INDArray x2 = opOnDimension.x();
            INDArray y2 = opOnDimension.y();

            boolean canDoDirectly;
            if (y2 == null) canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x2);
            else canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x2, y2);

            if (canDoDirectly) {
                subTask = new CPUAccumulationTask(opOnDimension, threshold, true);
            } else {
                subTask = new CPUAccumulationViaTensorTask(opOnDimension, threshold, true);
            }
            subTask.invokeAsync();
            return null;
        }
    }

    private class OpForDimTaskFJ extends RecursiveTask<Double> implements Task<Double> {
        private int tensorNum;
        private BaseCPUTask<Double> subTask;
        private Future<Double> future;

        public OpForDimTaskFJ(int tensorNum){
            this.tensorNum = tensorNum;
        }

        @Override
        public Double invokeBlocking() {
            invokeAsync();
            return blockUntilComplete();
        }

        @Override
        public void invokeAsync() {
            this.future = TaskExecutorProvider.getTaskExecutor().executeAsync(this);
        }

        @Override
        public Double blockUntilComplete() {
            return null;
        }

        @Override
        public Double call() {
            //Callable (should never be called)
            throw new RuntimeException("Callable.call() called as part of ForkJoin task");
        }

        @Override
        protected Double compute() {
            //Fork join
            Accumulation opOnDimension = (Accumulation) op.opForDimension(tensorNum, dimensions);
            INDArray x2 = opOnDimension.x();
            INDArray y2 = opOnDimension.y();

            boolean canDoDirectly;
            if (y2 == null) canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x2);
            else canDoDirectly = OpExecutionerUtil.canDoOpDirectly(x2, y2);

            if (canDoDirectly) {
                subTask = new CPUAccumulationTask(opOnDimension, threshold, true);
            } else {
                subTask = new CPUAccumulationViaTensorTask(opOnDimension, threshold, true);
            }
            return subTask.invoke();
        }
    }
}
