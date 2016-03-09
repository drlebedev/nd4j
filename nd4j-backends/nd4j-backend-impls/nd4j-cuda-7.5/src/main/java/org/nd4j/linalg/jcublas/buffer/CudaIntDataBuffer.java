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

package org.nd4j.linalg.jcublas.buffer;

import io.netty.buffer.ByteBuf;
import jcuda.Sizeof;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.util.ArrayUtil;

import java.nio.ByteBuffer;

/**
 * Cuda int buffer
 *
 * @author Adam Gibson
 */
public class CudaIntDataBuffer extends BaseCudaDataBuffer {
    /**
     * Base constructor
     *
     * @param length the length of the buffer
     */
    public CudaIntDataBuffer(int length) {
        super(length, Sizeof.INT);
    }

    public CudaIntDataBuffer(int length, int elementSize) {
        super(length, elementSize);
    }

    public CudaIntDataBuffer(int length, int elementSize, int offset) {
        super(length, elementSize, offset);
    }

    public CudaIntDataBuffer(DataBuffer underlyingBuffer, int length, int offset) {
        super(underlyingBuffer, length, offset);
    }

    public CudaIntDataBuffer(int[] data) {
        this(data.length);
        setData(data);
    }

    public CudaIntDataBuffer(int[] data, boolean copy) {
        super(data, copy);
    }

    public CudaIntDataBuffer(int[] data, boolean copy, int offset) {
        super(data, copy, offset);
    }


    public CudaIntDataBuffer(ByteBuf buf, int length) {
        super(buf, length);
    }

    public CudaIntDataBuffer(ByteBuf buf, int length, int offset) {
        super(buf, length, offset);
    }

    public CudaIntDataBuffer(byte[] data, int length) {
        super(data, length);
    }

    public CudaIntDataBuffer(double[] data) {
        super(data);
    }

    public CudaIntDataBuffer(double[] data, boolean copy) {
        super(data, copy);
    }

    public CudaIntDataBuffer(double[] data, boolean copy, int offset) {
        super(data, copy, offset);
    }

    public CudaIntDataBuffer(float[] data) {
        super(data);
    }

    public CudaIntDataBuffer(float[] data, boolean copy) {
        super(data, copy);
    }

    public CudaIntDataBuffer(float[] data, boolean copy, int offset) {
        super(data, copy, offset);
    }

    public CudaIntDataBuffer(ByteBuffer buffer, int length) {
        super(buffer, length);
    }

    public CudaIntDataBuffer(ByteBuffer buffer, int length, int offset) {
        super(buffer, length, offset);
    }

    @Override
    public void assign(int[] indices, float[] data, boolean contiguous, int inc) {
        if (indices.length != data.length)
            throw new IllegalArgumentException("Indices and data length must be the same");
        if (indices.length > length())
            throw new IllegalArgumentException("More elements than space to assign. This buffer is of length " + length() + " where the indices are of length " + data.length);

        if (!contiguous)
            throw new UnsupportedOperationException("Non contiguous is not supported");

    }

    @Override
    public void assign(int[] indices, double[] data, boolean contiguous, int inc) {
        if (indices.length != data.length)
            throw new IllegalArgumentException("Indices and data length must be the same");
        if (indices.length > length())
            throw new IllegalArgumentException("More elements than space to assign. This buffer is of length " + length() + " where the indices are of length " + data.length);

        if (!contiguous)
            throw new UnsupportedOperationException("Non contiguous is not supported");

    }

    @Override
    public Type dataType() {
        return Type.INT;
    }

    @Override
    protected DataBuffer create(int length) {
        return new CudaIntDataBuffer(length);
    }

    @Override
    public DataBuffer create(double[] data) {
        return new CudaIntDataBuffer(ArrayUtil.toInts(data));
    }

    @Override
    public DataBuffer create(float[] data) {
        return new CudaIntDataBuffer(ArrayUtil.toInts(data));
    }

    @Override
    public DataBuffer create(int[] data) {
        return new CudaIntDataBuffer(data);
    }

    @Override
    public DataBuffer create(ByteBuf buf, int length) {
        return new CudaIntDataBuffer(buf,length);
    }


    private void writeObject(java.io.ObjectOutputStream stream)
            throws java.io.IOException {
        stream.defaultWriteObject();

        if (getHostPointer() == null) {
            stream.writeInt(0);
        } else {
            int[] arr = this.asInt();

            stream.writeInt(arr.length);
            for (int i = 0; i < arr.length; i++) {
                stream.writeInt(arr[i]);
            }
        }
    }

    @Override
    public int getElementSize() {
        return Sizeof.INT;
    }

    private void readObject(java.io.ObjectInputStream stream)
            throws java.io.IOException, ClassNotFoundException {
        stream.defaultReadObject();

        int n = stream.readInt();
        int[] arr = new int[n];

        for (int i = 0; i < n; i++) {
            arr[i] = stream.readInt();
        }
        setData(arr);
    }


}
