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

package org.nd4j.linalg.api.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.nd4j.linalg.api.complex.IComplexDouble;
import org.nd4j.linalg.api.complex.IComplexFloat;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.*;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for a data buffer
 * handling basic byte operations among other things.
 *
 * @author Adam Gibson
 */
public abstract class BaseDataBuffer implements DataBuffer {

    protected int length;
    protected int underlyingLength;
    protected int offset;
    protected int elementSize;
    protected transient ByteBuffer wrappedBuffer;
    protected DataBuffer wrappedDataBuffer;
    protected Collection<String> referencing = Collections.synchronizedSet(new HashSet<String>());
    protected transient WeakReference<DataBuffer> ref;
    protected boolean isPersist = false;
    protected AllocationMode allocationMode;
    protected double[] doubleData;
    protected int[] intData;
    protected float[] floatData;
    protected AtomicBoolean dirty = new AtomicBoolean(false);


    /**
     * Meant for creating another view of a buffer
     * @param underlyingBuffer the underlying buffer to create a view from
     * @param length the length of the view
     * @param offset the offset for the view
     */
    protected BaseDataBuffer(DataBuffer underlyingBuffer,int length,int offset) {
        this.length = length;
        this.offset = offset;
        this.allocationMode = underlyingBuffer.allocationMode();
        this.elementSize = underlyingBuffer.getElementSize();
        this.underlyingLength = underlyingBuffer.underlyingLength();
        this.wrappedDataBuffer = underlyingBuffer;

        if(underlyingBuffer.dataType() == Type.DOUBLE) {
            if(underlyingBuffer.allocationMode() == AllocationMode.HEAP) {
                double[] underlyingArray = (double[]) underlyingBuffer.array();
                this.doubleData = underlyingArray;
            }
            else {
                ByteBuffer underlyingBuff = underlyingBuffer.asNio();
                this.wrappedBuffer = underlyingBuff;
            }
        }
        else if(underlyingBuffer.dataType() == Type.FLOAT) {
            if(underlyingBuffer.allocationMode() == AllocationMode.HEAP) {
                float[] underlyingArray = (float[]) underlyingBuffer.array();
                this.floatData = underlyingArray;
            }
            else {
                ByteBuffer underlyingBuff = underlyingBuffer.asNio();
                this.wrappedBuffer = underlyingBuff;

            }
        }
        else if(underlyingBuffer.dataType() == Type.INT) {
            if(underlyingBuffer.allocationMode() == AllocationMode.HEAP) {
                int[] underlyingArray = (int[]) underlyingBuffer.array();
                this.intData = underlyingArray;
            }
            else {
                ByteBuffer underlyingBuff = underlyingBuffer.asNio();
                this.wrappedBuffer = underlyingBuff;

            }
        }
    }

    /**
     *
     * @param buf
     * @param length
     */
    protected BaseDataBuffer(ByteBuf buf,int length,int offset) {
        this(buf,length);
        this.offset = offset;
        this.length = length - offset;
        this.underlyingLength = length;
    }
    /**
     *
     * @param buf
     * @param length
     */
    protected BaseDataBuffer(ByteBuf buf,int length) {
        allocationMode = Nd4j.alloc;
        this.wrappedBuffer = buf.nioBuffer();
        this.length = length;
        this.underlyingLength = length;
    }
    /**
     *
     * @param data
     * @param copy
     */
    public BaseDataBuffer(float[] data, boolean copy,int offset) {
        this(data,copy);
        this.offset = offset;
        this.length = data.length - offset;
        this.underlyingLength = data.length;

    }
    /**
     *
     * @param data
     * @param copy
     */
    public BaseDataBuffer(float[] data, boolean copy) {
        allocationMode = Nd4j.alloc;
        if(allocationMode == AllocationMode.HEAP) {
            if(copy) {
                floatData = ArrayUtil.copy(data);
            }
            else {
                this.floatData = data;
            }
        }
        else {
            wrappedBuffer =  ByteBuffer.allocateDirect(4 * data.length);
            wrappedBuffer.order(ByteOrder.nativeOrder());
            FloatBuffer buffer = wrappedBuffer.asFloatBuffer();
            for(int i = 0; i < data.length; i++) {
                buffer.put(i,data[i]);
            }
        }

        length = data.length;
        underlyingLength = data.length;

    }

    /**
     *
     * @param data
     * @param copy
     */
    public BaseDataBuffer(double[] data, boolean copy,int offset) {
        this(data,copy);
        this.offset = offset;
        this.underlyingLength = data.length;
        this.length = underlyingLength - offset;
    }

    /**
     *
     * @param data
     * @param copy
     */
    public BaseDataBuffer(double[] data, boolean copy) {
        allocationMode = Nd4j.alloc;
        if(allocationMode == AllocationMode.HEAP) {
            if(copy) {
                doubleData = ArrayUtil.copy(data);
            }
            else {
                this.doubleData = data;
            }
        }
        else {
            wrappedBuffer =  ByteBuffer.allocateDirect(8 * data.length);
            wrappedBuffer.order(ByteOrder.nativeOrder());
            DoubleBuffer buffer = wrappedBuffer.asDoubleBuffer();
            for(int i = 0; i < data.length; i++) {
                buffer.put(i,data[i]);
            }
        }

        length = data.length;
        underlyingLength = data.length;
    }


    /**
     *
     * @param data
     * @param copy
     */
    public BaseDataBuffer(int[] data, boolean copy,int offset) {
        this(data,copy);
        this.offset = offset;
        this.length = data.length - offset;
        this.underlyingLength = data.length;
    }
    /**
     *
     * @param data
     * @param copy
     */
    public BaseDataBuffer(int[] data, boolean copy) {
        allocationMode = Nd4j.alloc;
        if(allocationMode == AllocationMode.HEAP) {
            if(copy)
                intData = ArrayUtil.copy(data);

            else
                this.intData = data;

        }
        else {
            wrappedBuffer =  ByteBuffer.allocateDirect(4 * data.length);
            wrappedBuffer.order(ByteOrder.nativeOrder());
            IntBuffer buffer = wrappedBuffer.asIntBuffer();
            for(int i = 0; i < data.length; i++) {
                buffer.put(i,data[i]);
            }
        }

        length = data.length;
        underlyingLength = data.length;
    }

    /**
     *
     * @param data
     */
    public BaseDataBuffer(double[] data) {
        this(data,Nd4j.copyOnOps);
    }

    /**
     *
     * @param data
     */
    public BaseDataBuffer(int[] data) {
        this(data, Nd4j.copyOnOps);
    }

    /**
     *
     * @param data
     */
    public BaseDataBuffer(float[] data) {
        this(data,Nd4j.copyOnOps);
    }
    /**
     *
     * @param length
     * @param elementSize
     */
    public BaseDataBuffer(int length, int elementSize,int offset) {
        this(length,elementSize);
        this.offset = offset;
        this.length = length - offset;
        this.underlyingLength = length;
    }

    /**
     *
     * @param length
     * @param elementSize
     */
    public BaseDataBuffer(int length, int elementSize) {
        allocationMode = Nd4j.alloc;
        this.length = length;
        this.underlyingLength = length;
        this.elementSize = elementSize;
        if(allocationMode() == AllocationMode.DIRECT) {
            //allows for creation of the nio byte buffer to be overridden
            setNioBuffer();
        }
        else if(dataType() == Type.DOUBLE) {
            doubleData = new double[length];
        }
        else if(dataType() == Type.FLOAT) {
            floatData = new float[length];
        }
        else if(dataType() == Type.INT)
            intData = new int[length];
    }
    /**
     * Create a data buffer from
     * the given length
     *
     * @param buffer
     * @param length
     */
    public BaseDataBuffer(ByteBuffer buffer,int length,int offset) {
        this(buffer,length);
        this.offset = offset;
        this.underlyingLength = length;
        this.length = length - offset;

    }
    /**
     * Create a data buffer from
     * the given length
     *
     * @param buffer
     * @param length
     */
    public BaseDataBuffer(ByteBuffer buffer,int length) {
        allocationMode = Nd4j.alloc;
        this.length = length;
        this.underlyingLength = length;
        buffer.order(ByteOrder.nativeOrder());
        if(allocationMode() == AllocationMode.DIRECT) {
            this.wrappedBuffer = buffer;
        }
        else if(dataType() == Type.INT) {
            intData = new int[length];
            IntBuffer intBuffer = buffer.asIntBuffer();
            for(int i = 0; i < length; i++) {
                intData[i] = intBuffer.get(i);
            }
        }
        else if(dataType() == Type.DOUBLE) {
            doubleData = new double[length];
            DoubleBuffer doubleBuffer = buffer.asDoubleBuffer();
            for(int i = 0; i < length; i++) {
                doubleData[i] = doubleBuffer.get(i);
            }


        }
        else if(dataType() == Type.FLOAT) {
            floatData = new float[length];
            FloatBuffer floatBuffer = buffer.asFloatBuffer();
            for(int i = 0; i < length; i++) {
                floatData[i] = floatBuffer.get(i);
            }
        }
    }

    //sets the nio wrapped buffer (allows to be overridden for other use cases like cuda)
    protected void setNioBuffer() {
        wrappedBuffer = ByteBuffer.allocateDirect(elementSize * length);
        wrappedBuffer.order(ByteOrder.nativeOrder());

    }


    public BaseDataBuffer(byte[] data, int length) {
        this(Unpooled.wrappedBuffer(data),length);
    }

    @Override
    public DataBuffer underlyingDataBuffer() {
        return wrappedDataBuffer;
    }

    @Override
    public int offset() {
        return offset;
    }

    @Override
    public AllocationMode allocationMode() {
        return allocationMode;
    }

    @Override
    public void persist() {
        isPersist = true;
    }

    @Override
    public boolean isPersist() {
        return isPersist;
    }

    @Override
    public void unPersist() {
        isPersist = false;
    }

    /**
     * Instantiate a buffer with the given length
     *
     * @param length the length of the buffer
     */
    protected BaseDataBuffer(int length) {
        this.length = length;
        this.underlyingLength = length;
        allocationMode = Nd4j.alloc;
        if(length < 0)
            throw new IllegalArgumentException("Unable to create a buffer of length <= 0");

        ref = new WeakReference<DataBuffer>(this,Nd4j.bufferRefQueue());
        if(allocationMode == AllocationMode.HEAP) {
            if(length >= Integer.MAX_VALUE)
                throw new IllegalArgumentException("Length of data buffer can not be > Integer.MAX_VALUE for heap (array based storage) allocation");
            if(dataType() == Type.DOUBLE)
                doubleData = new double[length];
            else if(dataType() == Type.FLOAT)
                floatData = new float[length];
        }
        else {
            if(length * getElementSize() < 0)
                throw new IllegalArgumentException("Unable to create buffer of length " + length + " due to negative length specified");
            wrappedBuffer = ByteBuffer.allocateDirect(getElementSize() * length).order(ByteOrder.nativeOrder());
        }

    }

    @Override
    public void copyAtStride(DataBuffer buf, int n, int stride, int yStride, int offset, int yOffset) {
        if(dataType() == Type.FLOAT) {
            for(int i = 0; i < n; i++) {
                put(offset + i * stride,buf.getFloat(yOffset + i * yStride));
            }
        }
        else {
            for(int i = 0; i < n; i++) {
                put(offset + i * stride,buf.getDouble(yOffset + i * yStride));
            }
        }

    }

    @Override
    public void removeReferencing(String id) {
        referencing.remove(id);
    }

    @Override
    public Collection<String> references() {
        return referencing;
    }

    @Override
    public void addReferencing(String id) {
        referencing.add(id);
    }

    @Override
    public void assign(int[] indices, float[] data, boolean contiguous, int inc) {
        if (indices.length != data.length)
            throw new IllegalArgumentException("Indices and data length must be the same");
        if (indices.length > length())
            throw new IllegalArgumentException("More elements than space to assign. This buffer is of length " + length() + " where the indices are of length " + data.length);
        for (int i = 0; i < indices.length; i++) {
            put(indices[i], data[i]);
        }
    }



    @Override
    public void setData(int[] data) {
        if(intData != null)
            this.intData = data;
        else {
            for (int i = 0; i < data.length; i++) {
                put(i,data[i]);
            }
        }

    }

    @Override
    public void setData(float[] data) {
        if(floatData != null) {
            this.floatData = data;
        }
        else {
            for(int i = 0; i < data.length; i++)
                put(i,data[i]);
        }

    }

    @Override
    public void setData(double[] data) {
        if(doubleData != null) {
            this.doubleData = data;
        }
        else {
            for(int i = 0; i < data.length; i++)
                put(i, data[i]);
        }
    }


    @Override
    public void assign(int[] indices, double[] data, boolean contiguous, int inc) {
        if (indices.length != data.length)
            throw new IllegalArgumentException("Indices and data length must be the same");
        if (indices.length > length())
            throw new IllegalArgumentException("More elements than space to assign. This buffer is of length " + length() + " where the indices are of length " + data.length);
        for (int i = 0; i < indices.length; i += inc) {
            put(indices[i], data[i]);
        }
    }

    @Override
    public void assign(DataBuffer data) {
        if (data.length() != length())
            throw new IllegalArgumentException("Unable to assign buffer of length " + data.length() + " to this buffer of length " + length());

        for (int i = 0; i < data.length(); i++) {
            put(i, data.getDouble(i));
        }
    }

    @Override
    public void assign(int[] indices, float[] data, boolean contiguous) {
        assign(indices, data, contiguous, 1);
    }

    @Override
    public void assign(int[] indices, double[] data, boolean contiguous) {
        assign(indices, data, contiguous, 1);
    }

    @Override
    public int underlyingLength() {
        return underlyingLength;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public void assign(Number value) {
        for(int i = 0; i < length(); i++)
            assign(value,i);
    }


    @Override
    public double[] getDoublesAt(int offset, int length) {
        return getDoublesAt(offset, 1, length);
    }

    @Override
    public float[] getFloatsAt(int offset, int inc, int length) {
        if (offset + length > length())
            length -= offset;
        float[] ret = new float[length];
        for (int i = 0; i < length; i++) {
            ret[i] = getFloat(i + offset);
        }
        return ret;
    }


    @Override
    public DataBuffer dup() {
        if(floatData != null) {
            return create(floatData);
        }
        else if(doubleData != null) {
            return create(doubleData);
        }
        else if(intData != null) {
            return create(intData);
        }

        DataBuffer ret = create(length);
        for(int i = 0; i < ret.length(); i++)
            ret.put(i, getDouble(i));

        return ret;
    }

    /**
     * Create with length
     * @param length a databuffer of the same type as
     *               this with the given length
     * @return a data buffer with the same length and datatype as this one
     */
    protected abstract  DataBuffer create(int length);


    /**
     * Create the data buffer
     * with respect to the given byte buffer
     * @param data the buffer to create
     * @return the data buffer based on the given buffer
     */
    public abstract DataBuffer create(double[] data);
    /**
     * Create the data buffer
     * with respect to the given byte buffer
     * @param data the buffer to create
     * @return the data buffer based on the given buffer
     */
    public abstract DataBuffer create(float[] data);

    /**
     * Create the data buffer
     * with respect to the given byte buffer
     * @param data the buffer to create
     * @return the data buffer based on the given buffer
     */
    public abstract DataBuffer create(int[] data);

    /**
     * Create the data buffer
     * with respect to the given byte buffer
     * @param buf the buffer to create
     * @return the data buffer based on the given buffer
     */
    public abstract DataBuffer create(ByteBuf buf,int length);

    @Override
    public double[] getDoublesAt(int offset, int inc, int length) {
        if (offset + length > length())
            length -= offset;

        double[] ret = new double[length];
        for (int i = 0; i < length; i++) {
            ret[i] = getDouble(i + offset);
        }


        return ret;
    }

    @Override
    public float[] getFloatsAt(int offset, int length) {
        return getFloatsAt(offset, 1, length);
    }

    @Override
    public IComplexFloat getComplexFloat(int i) {
        return Nd4j.createFloat(getFloat(i), getFloat(i + 1));
    }

    @Override
    public IComplexDouble getComplexDouble(int i) {
        return Nd4j.createDouble(getDouble(i), getDouble(i + 1));
    }

    @Override
    public IComplexNumber getComplex(int i) {
        return dataType() == DataBuffer.Type.FLOAT ? getComplexFloat(i) : getComplexDouble(i);
    }


    @Override
    public void put(int i, IComplexNumber result) {
        put(i, result.realComponent().doubleValue());
        put(i + 1, result.imaginaryComponent().doubleValue());
    }


    @Override
    public void assign(int[] offsets, int[] strides, DataBuffer... buffers) {
        assign(offsets, strides, length(), buffers);
    }

    @Override
    public byte[] asBytes() {
        if(allocationMode == AllocationMode.HEAP) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(getElementSize() * length());
            DataOutputStream dos = new DataOutputStream(bos);

            if(dataType() == Type.DOUBLE) {
                if(doubleData == null)
                    throw new IllegalStateException("Double array is null!");

                try {
                    for(int i = 0; i < doubleData.length; i++)
                        dos.writeDouble(doubleData[i]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
            else {
                if(floatData == null)
                    throw new IllegalStateException("Double array is null!");

                try {
                    for(int i = 0; i < floatData.length; i++)
                        dos.writeFloat(floatData[i]);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }

            return bos.toByteArray();

        }
        else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            if(dataType() == Type.DOUBLE) {
                for(int i = 0; i < length(); i++) {
                    try {
                        dos.writeDouble(getDouble(i));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                for(int i = 0; i < length(); i++) {
                    try {
                        dos.writeFloat(getFloat(i));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return bos.toByteArray();
        }
    }

    @Override
    public float[] asFloat() {
        if(allocationMode == AllocationMode.HEAP) {
            if(floatData != null) {
                return floatData;
            }
        }

        float[] ret = new float[length];
        for(int i = 0; i < length; i++)
            ret[i] = getFloat(i);
        return ret;

    }

    @Override
    public double[] asDouble() {
        if(allocationMode == AllocationMode.HEAP) {
            if(doubleData != null) {
                return doubleData;
            }
        }


        double[] ret = new double[length];
        for(int i = 0; i < length; i++)
            ret[i] = getDouble(i);
        return ret;

    }

    @Override
    public int[] asInt() {
        if(allocationMode == AllocationMode.HEAP) {
            if(intData != null) {
                return intData;
            }
        }
        return wrappedBuffer.asIntBuffer().array();
    }

    @Override
    public double getDouble(int i) {
        if(doubleData != null) {
            if(offset() + i >= doubleData.length)
                throw new IllegalStateException("Index out of bounds " + i);
            dirty.set(false);
            return doubleData[offset() + i];
        }
        else if(floatData != null) {
            if(offset() + i >= floatData.length)
                throw new IllegalStateException("Index out of bounds " + i);
            dirty.set(false);
            return (double) floatData[offset() + i];
        }
        else if(intData != null) {
            dirty.set(false);
            return (double) intData[offset() + i];
        }

        if(dataType() == Type.FLOAT) {
            dirty.set(false);
            return wrappedBuffer.asFloatBuffer().get(offset() + i);
        }

        else if(dataType() == Type.INT) {
            dirty.set(false);
            return wrappedBuffer.asIntBuffer().get(offset() + i);
        }
        else {
            dirty.set(false);
            return wrappedBuffer.asDoubleBuffer().get(offset() + i);
        }
    }

    @Override
    public float getFloat(int i) {
        if(doubleData != null) {
            if(i >= doubleData.length)
                throw new IllegalStateException("Index out of bounds " + i);
            dirty.set(false);
            return (float) doubleData[offset() + i];
        } else if(floatData != null) {
            if(i >= floatData.length)
                throw new IllegalStateException("Index out of bounds " + i);
            dirty.set(false);
            return floatData[offset() + i];
        }
        else if(intData != null) {
            dirty.set(false);
            return (float) intData[offset() + i];
        }

        if(dataType() == Type.DOUBLE) {
            dirty.set(false);
            return (float) wrappedBuffer.asDoubleBuffer().get(offset() + i);
        }

        dirty.getAndSet(true);
        return wrappedBuffer.asFloatBuffer().get(offset() + i);
    }

    @Override
    public Number getNumber(int i) {
        if(dataType() == Type.DOUBLE)
            return getDouble(i);
        else if(dataType() == Type.INT)
            return getInt(i);
        return getFloat(i);
    }

    @Override
    public void put(int i, float element) {
        put(i,(double) element);
    }

    @Override
    public void put(int i, double element) {
          if(doubleData != null)
            doubleData[offset() + i] = element;

        else if(floatData != null)
            floatData[offset() + i] = (float) element;

        else if(intData != null)
            intData[offset() + i] = (int) element;

        else {
            if(dataType() == Type.DOUBLE) {
                wrappedBuffer.asDoubleBuffer().put(offset() + i,element);
            }
            else if(dataType() == Type.INT) {
                wrappedBuffer.asIntBuffer().put(offset() + i,(int) element);
            }
            else {
                wrappedBuffer.asFloatBuffer().put(offset() + i,(float) element);
            }
        }

        dirty.set(true);
    }

    @Override
    public boolean dirty() {
        return dirty.get();
    }

    @Override
    public boolean sameUnderlyingData(DataBuffer buffer) {
        if(allocationMode() != buffer.allocationMode())
        return false;
        if(allocationMode() == AllocationMode.HEAP) {
            return array() == buffer.array();
        }
        else {
            return buffer.asNio() == asNio();
        }
    }

    @Override
    public IntBuffer asNioInt() {
        if(wrappedBuffer == null) {
            if(offset() > 0)
                return (IntBuffer) IntBuffer.wrap(intData).position(offset());
            else
                return IntBuffer.wrap(intData);
        }
        if(offset() == 0) {
            return wrappedBuffer.asIntBuffer();
        }
        else
            return (IntBuffer) wrappedBuffer.asIntBuffer().position(offset());
    }

    @Override
    public DoubleBuffer asNioDouble() {
        if(wrappedBuffer == null) {
            if(offset() == 0) {
                return DoubleBuffer.wrap(doubleData);
            }
            else
                return (DoubleBuffer) DoubleBuffer.wrap(doubleData).position(offset());
        }

        if(offset() == 0) {
            return wrappedBuffer.asDoubleBuffer();
        }
        else {
            ByteBuffer ret = (ByteBuffer) wrappedBuffer.slice().position(offset() * getElementSize());
            ByteBuffer convert =  ret.slice();
            return convert.asDoubleBuffer();
        }
    }

    @Override
    public FloatBuffer asNioFloat() {
        if(wrappedBuffer == null) {
            if(offset() == 0) {
                return FloatBuffer.wrap(floatData);
            }
            else
                return (FloatBuffer) FloatBuffer.wrap(floatData).position(offset());
        }
        if(offset() == 0) {
            return wrappedBuffer.asFloatBuffer();
        }
        else {
            ByteBuffer ret = (ByteBuffer) wrappedBuffer.slice().position(offset() * getElementSize());
            ByteBuffer convert =  ret.slice();
            return convert.asFloatBuffer();
        }

    }

    @Override
    public ByteBuffer asNio() {
       return wrappedBuffer;
    }

    @Override
    public ByteBuf asNetty() {
        if(wrappedBuffer != null)
            return Unpooled.wrappedBuffer(wrappedBuffer);
        else if(floatData != null)
            return Unpooled.copyFloat(floatData);
        else if(doubleData != null)
            return Unpooled.copyDouble(doubleData);
        throw new IllegalStateException("No data source defined");
    }

    @Override
    public void put(int i, int element) {
        //note here that the final put will take care of the offset
        put(i,(double) element);
    }

    @Override
    public void assign(Number value, int offset) {
        //note here that the final put will take care of the offset
        for(int i = offset; i < length(); i++)
            put(i, value.doubleValue());
    }

    @Override
    public void write(OutputStream dos) {
        if(dos instanceof DataOutputStream) {
            try {
                write((DataOutputStream) dos);
            } catch (IOException e) {
                throw new IllegalStateException("IO Exception writing buffer",e);
            }
        }
        else {
            DataOutputStream dos2 = new DataOutputStream(dos);
            try {

                write( dos2);
            } catch (IOException e) {
                throw new IllegalStateException("IO Exception writing buffer",e);
            }
        }

    }

    @Override
    public void read(InputStream is) {
        if(is instanceof DataInputStream) {
            read((DataInputStream) is);
        }

        else {
            DataInputStream dis2 = new DataInputStream(is);
            read(dis2);
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public int getInt(int ix) {
        return (int) getDouble(ix);
    }

    @Override
    public void assign(int[] offsets, int[] strides, long n, DataBuffer... buffers) {
        if (offsets.length != strides.length || strides.length != buffers.length)
            throw new IllegalArgumentException("Unable to assign buffers, please specify equal lengths strides, offsets, and buffers");
        int count = 0;
        for (int i = 0; i < buffers.length; i++) {
            //note here that the final put will take care of the offset
            for (int j = offsets[i]; j < buffers[i].length(); j += strides[i]) {
                put(count++, buffers[i].getDouble(j));
            }
        }

        if (count != n)
            throw new IllegalArgumentException("Strides and offsets didn't match up to length " + n);

    }

    @Override
    public void assign(DataBuffer... buffers) {
        int[] offsets = new int[buffers.length];
        int[] strides = new int[buffers.length];
        for (int i = 0; i < strides.length; i++)
            strides[i] = 1;
        assign(offsets, strides, buffers);
    }


    @Override
    public void destroy() {

    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof DataBuffer) {
            DataBuffer d = (DataBuffer) o;
            if(d.length() != length())
                return false;
            for(int i = 0; i < length(); i++) {
                double eps = Math.abs(getDouble(i) - d.getDouble(i));
                if(eps > Nd4j.EPS_THRESHOLD)
                    return false;
            }
        }

        return true;
    }

    private void readObject(ObjectInputStream s) {
        doReadObject(s);
    }

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        out.defaultWriteObject();
        write(out);
    }


    protected void doReadObject(ObjectInputStream s) {
        try {
            s.defaultReadObject();
            read(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }




    protected void read(DataInputStream s) {
        try {
            ref = new WeakReference<DataBuffer>(this,Nd4j.bufferRefQueue());
            referencing = Collections.synchronizedSet(new HashSet<String>());
            dirty = new AtomicBoolean(false);
            allocationMode = AllocationMode.valueOf(s.readUTF());
            length = s.readInt();
            Type t = Type.valueOf(s.readUTF());
            if(t == Type.DOUBLE) {
                if(allocationMode == AllocationMode.HEAP) {
                    if (this.dataType() == Type.FLOAT) { //DataBuffer type
                        // double -> float
                        floatData = new float[length()];
                    } else if (this.dataType() == Type.DOUBLE) {
                        //double -> double
                        doubleData = new double[length()];
                    } else {
                        //double -> int
                        intData = new int[length()];
                    }
                    for(int i = 0; i < length(); i++) {
                        put(i,s.readDouble());
                    }
                }
                else {
                    wrappedBuffer = ByteBuffer.allocateDirect(length() * getElementSize());
                    wrappedBuffer.order(ByteOrder.nativeOrder());
                    for(int i = 0; i < length(); i++) {
                        put(i,s.readDouble());
                    }
                }
            }
            else {
                if(allocationMode == AllocationMode.HEAP) {
                    if (this.dataType() == Type.FLOAT) { //DataBuffer type
                        // float -> float
                        floatData = new float[length()];
                    } else if (this.dataType() == Type.DOUBLE) {
                        //float -> double
                        doubleData = new double[length()];
                    } else {
                        //float-> int
                        intData = new int[length()];
                    }
                    for(int i = 0; i < length(); i++) {
                        put(i,s.readFloat());
                    }
                }
                else {
                    wrappedBuffer = ByteBuffer.allocateDirect(length() * getElementSize());
                    wrappedBuffer.order(ByteOrder.nativeOrder());
                    for(int i = 0; i < length(); i++) {
                        put(i,s.readFloat());
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    protected void write(DataOutputStream out) throws IOException {
        out.writeUTF(allocationMode.name());
        out.writeInt(length());
        out.writeUTF(dataType().name());
        if(dataType() == Type.DOUBLE) {
            for(int i = 0; i < length(); i++)
                out.writeDouble(getDouble(i));
        }
        else {
            for(int i = 0; i < length(); i++)
                out.writeFloat(getFloat(i));
        }

    }




    @Override
    public Object array() {
        if(floatData != null)
            return floatData;
        if(doubleData != null)
            return doubleData;
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append("[");
        for(int i = 0; i < length(); i++) {
            ret.append(getNumber(i));
            if(i < length() - 1)
                ret.append(",");
        }
        ret.append("]");

        return ret.toString();
    }

    @Override
    public int hashCode() {
        int result = length;
        result = 31 * result + (referencing != null ? referencing.hashCode() : 0);
        result = 31 * result + (ref != null ? ref.hashCode() : 0);
        result = 31 * result + (isPersist ? 1 : 0);
        result = 31 * result + (allocationMode != null ? allocationMode.hashCode() : 0);
        return result;
    }
}