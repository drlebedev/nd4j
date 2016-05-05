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

package org.nd4j.linalg.api.ops;

import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.nio.Buffer;

/**
 * Base op. An op involves iterating over 2 buffers (x,y)  up to n elements
 * and applying a transform or accumulating a result.
 *
 * @author Adam Gibson
 */
public abstract class BaseOp implements Op {

    protected INDArray x, y, z;
    protected long n;
    protected long numProcessed;
    protected Object[] extraArgs;
    protected boolean passThrough;


    public BaseOp() {
    }

    @Override
    public boolean isExecSpecial() {
        return false;
    }

    @Override
    public DataBuffer extraArgsDataBuff() {
        if(extraArgs != null) {
            DataBuffer retBuff;
            if(x.data().dataType() == DataBuffer.Type.FLOAT) {
                retBuff = Nd4j.createBuffer(new float[extraArgs.length]);
                for(int i = 0; i < extraArgs.length; i++) {
                    Number arg = (Number) extraArgs[i];
                    float val = arg.floatValue();
                    retBuff.put(i,val);
                }
                return retBuff;
            }
            else {
                retBuff = Nd4j.createBuffer(new double[extraArgs.length]);
                for(int i = 0; i < extraArgs.length; i++) {
                    Number arg = (Number) extraArgs[i];
                    double val = arg.doubleValue();
                    retBuff.put(i, val);
                }
                return retBuff;
            }
        }
        return null;
    }

    @Override
    public Buffer extraArgsBuff() {
        if(extraArgs != null) {
            DataBuffer retBuff;
            if(x.data().dataType() == DataBuffer.Type.FLOAT) {
                retBuff = Nd4j.createBuffer(new float[extraArgs.length]);
                for(int i = 0; i < extraArgs.length; i++) {
                    Number val = (Number) extraArgs[i];
                    retBuff.put(i,val.floatValue());
                }
                return retBuff.asNioFloat();
            }
            else {
                retBuff = Nd4j.createBuffer(new double[extraArgs.length]);
                for(int i = 0; i < extraArgs.length; i++) {
                    Number val = (Number) extraArgs[i];
                    retBuff.put(i, val.doubleValue());
                }
                return retBuff.asNioDouble();
            }


        }
        return null;
    }

    @Override
    public boolean isPassThrough() {
        return passThrough;
    }

    @Override
    public void setX(INDArray x) {
        if(x == null)
            throw new IllegalArgumentException("X must not be null");
        this.x = x;
        numProcessed = 0;
    }

    @Override
    public void setZ(INDArray z) {
        if(z == null)
            throw new IllegalArgumentException("Z must not be null");
        this.z = z;
        numProcessed = 0;
    }

    @Override
    public void setY(INDArray y) {
        if(y == null)
            throw new IllegalArgumentException("Y must not be null");
        this.y = y;
        numProcessed = 0;
    }

    /**
     * Specify an alternative result array
     *
     * @param x the input
     * @param z the output array
     */
    public BaseOp(INDArray x, INDArray z) {
        this(x, z, x.lengthLong());
    }

    /**
     * Specify an alternative output array
     *
     * @param x the input
     * @param z the output
     * @param n the number of elements to iterate on
     */
    public BaseOp(INDArray x, INDArray z, long n) {
        this(x, null, z, n);
    }


    public BaseOp(INDArray x, INDArray y, INDArray z, long n) {
        this.n = n;
        init(x, y, z, n);
    }



    /**
     * An op for one ndarray
     *
     * @param x the ndarray
     */
    public BaseOp(INDArray x) {
        this(x, null, x, x.lengthLong());
    }

    @Override
    public Object[] extraArgs() {
        return extraArgs;
    }

    @Override
    public INDArray x() {
        return x;
    }

    @Override
    public INDArray y() {
        return y;
    }


    @Override
    public INDArray z() {
        return z;
    }

    @Override
    public long n() {
        return n;
    }


    @Override
    public void init(INDArray x, INDArray y, INDArray z, long n) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.n = n;

    }

    @Override
    public void setN(long n) {
        this.n = n;
    }

    @Override
    public long numProcessed() {
        return numProcessed;
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public void exec() {
        //no-op
    }

    @Override
    public void exec(int... dimensions) {
        //no-op
    }
}
