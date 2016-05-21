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

import io.netty.buffer.Unpooled;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.Getter;
import lombok.NonNull;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.indexer.DoubleIndexer;
import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.javacpp.indexer.IntIndexer;
import org.nd4j.jita.allocator.impl.AllocationPoint;
import org.nd4j.jita.allocator.impl.AllocationShape;
import org.nd4j.jita.allocator.impl.AtomicAllocator;
import org.nd4j.jita.allocator.pointers.CudaPointer;
import org.nd4j.linalg.api.buffer.BaseDataBuffer;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.complex.IComplexDouble;
import org.nd4j.linalg.api.complex.IComplexFloat;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.jcublas.context.ContextHolder;
import org.nd4j.linalg.jcublas.context.CudaContext;
import org.nd4j.linalg.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.*;
import java.util.*;

/**
 * Base class for a data buffer
 *
 * CUDA implementation for DataBuffer always uses JavaCPP as allocationMode, and device access is masked by appropriate allocator mover implementation.
 *
 * Memory allocation/deallocation is strictly handled by allocator, since JavaCPP alloc/dealloc has nothing to do with CUDA. But besides that, host pointers obtained from CUDA are 100% compatible with CPU
 *
 * @author Adam Gibson
 * @author raver119@gmail.com
 */
public abstract class BaseCudaDataBuffer extends BaseDataBuffer implements JCudaBuffer {

    @Getter protected transient AllocationPoint allocationPoint;

    private static AtomicAllocator allocator = AtomicAllocator.getInstance();

    private static Logger log = LoggerFactory.getLogger(BaseCudaDataBuffer.class);

    public BaseCudaDataBuffer() {

    }

    public BaseCudaDataBuffer(ByteBuf buf, long length) {
        throw new UnsupportedOperationException("Not implemented yet");
        //super(buf, length);
        // TODO: to be implemented, using ByteBuf.memoryAddress() and memcpy
    }

    public BaseCudaDataBuffer(ByteBuf buf, long length, long offset) {
        throw new UnsupportedOperationException("Not implemented yet");
        //super(buf, length, offset);
        // TODO: to be implemented, using ByteBuf.memoryAddress() and memcpy
    }

    public BaseCudaDataBuffer(float[] data, boolean copy) {
        //super(data, copy);
        this(data, copy, 0);
    }

    public BaseCudaDataBuffer(float[] data, boolean copy, long offset) {
        this(data.length, 4);
        this.offset = offset;
        this.originalOffset = offset;
        this.length = data.length - offset;
        this.underlyingLength = data.length;
        set(data, this.length, offset, offset);
    }

    public BaseCudaDataBuffer(double[] data, boolean copy) {
        this(data, copy, 0);
    }

    public BaseCudaDataBuffer(double[] data, boolean copy, long offset) {
        this(data.length , 8);
        this.offset = offset;
        this.originalOffset = offset;
        this.length = data.length - offset;
        this.underlyingLength = data.length;
        set(data, this.length, offset, offset);
    }

    public BaseCudaDataBuffer(int[] data, boolean copy) {
        this(data, copy, 0);
    }

    public BaseCudaDataBuffer(int[] data, boolean copy, long offset) {
        this(data.length, 4);
        this.offset = offset;
        this.originalOffset = offset;
        this.length = data.length - offset;
        this.underlyingLength = data.length;
        set(data, this.length, offset, offset);
    }



    /**
     * Base constructor. It's used within all constructors internally
     *
     * @param length      the length of the buffer
     * @param elementSize the size of each element
     */
    public BaseCudaDataBuffer(long length, int elementSize) {
        this.allocationMode = AllocationMode.JAVACPP;

        this.allocationPoint = AtomicAllocator.getInstance().allocateMemory(this, new AllocationShape(length, elementSize));
        this.length = length;
        //allocationPoint.attachBuffer(this);
        this.elementSize = elementSize;
        this.trackingPoint = allocationPoint.getObjectId();
        this.offset = 0;
        this.originalOffset = 0;


//        log.info("ElementSize: " + this.elementSize);
//        log.info("Host pointer: " + allocationPoint.getPointers().getHostPointer().address());
//        log.info("Device pointer: " + allocationPoint.getPointers().getDevicePointer().address());

//        log.info("Creating fresh buffer: length: ["+length+"], elementSize: ["+ elementSize+"]");

        if (dataType() == Type.DOUBLE) {
            this.pointer = new CudaPointer(allocationPoint.getPointers().getHostPointer(), length,0).asDoublePointer();
            indexer = DoubleIndexer.create((DoublePointer) pointer);
        } else if (dataType() == Type.FLOAT){
            this.pointer = new CudaPointer(allocationPoint.getPointers().getHostPointer(), length,0).asFloatPointer();
            indexer = FloatIndexer.create((FloatPointer) pointer);
        } else if (dataType() == Type.INT){
            this.pointer = new CudaPointer(allocationPoint.getPointers().getHostPointer(), length,0).asIntPointer();
            indexer = IntIndexer.create((IntPointer) pointer);
        }

        this.wrappedBuffer = this.pointer.asByteBuffer();

        if (this.wrappedBuffer == null) {
            throw new IllegalStateException("WrappedBuffer is NULL");
        }

        // TODO: probably this one could be reconsidered
//        Long tmpPoint = AtomicAllocator.getInstance().pickupSpan(allocationPoint);

//        log.info("BCDB wrappedBuffer params: " + wrappedBuffer.capacity() + " position: ["+ wrappedBuffer+"]");
    }

    public BaseCudaDataBuffer(long length, int elementSize, long offset) {
        this(length, elementSize);
        this.offset = offset;
        this.originalOffset = offset;
    }

    public BaseCudaDataBuffer(@NonNull DataBuffer underlyingBuffer, long length, long offset) {
        //this(length, underlyingBuffer.getElementSize(), offset);
        this.allocationMode = AllocationMode.JAVACPP;

        this.wrappedDataBuffer = underlyingBuffer;
        this.originalBuffer = underlyingBuffer.originalDataBuffer() == null ? underlyingBuffer : underlyingBuffer.originalDataBuffer();
        this.length = length;
        this.offset = offset;
        this.originalOffset = offset;
        this.trackingPoint = underlyingBuffer.getTrackingPoint();
        this.elementSize = underlyingBuffer.getElementSize();
        this.allocationPoint = ((BaseCudaDataBuffer) underlyingBuffer).allocationPoint;

//        log.info("BCDB create for view: length: ["+ length+"], offset: ["+ offset+"], originalOffset: ["+ underlyingBuffer.originalOffset() +"], elementSize: ["+elementSize+"]");

        if (underlyingBuffer.dataType() == Type.DOUBLE) {
            this.pointer = new CudaPointer(allocationPoint.getPointers().getHostPointer(), originalBuffer.length()).asDoublePointer();
            indexer = DoubleIndexer.create((DoublePointer) pointer);
        } else if (underlyingBuffer.dataType() == Type.FLOAT){
            this.pointer = new CudaPointer(allocationPoint.getPointers().getHostPointer(), originalBuffer.length()).asFloatPointer();
            indexer = FloatIndexer.create((FloatPointer) pointer);
        } else if (underlyingBuffer.dataType() == Type.INT){
            this.pointer = new CudaPointer(allocationPoint.getPointers().getHostPointer(), originalBuffer.length()).asIntPointer();
            indexer = IntIndexer.create((IntPointer) pointer);
        }

        this.wrappedBuffer = this.pointer.asByteBuffer();
//        log.info("Buffer info: dataType: ["+underlyingBuffer.dataType()+"], capacity: [" + this.wrappedBuffer.capacity()+ "], limit: ["+this.wrappedBuffer.limit()+"], position: ["+ this.wrappedBuffer.position() + "]");

        // TODO: make sure we're getting pointer with offset at allocator
    }

    public BaseCudaDataBuffer(long length) {
        this(length, Nd4j.dataType() == Type.DOUBLE ? 8 : 4);
    }

    public BaseCudaDataBuffer(float[] data) {
        //super(data);
        this(data.length, Nd4j.dataType() == Type.DOUBLE ? 8 : 4);
        set(data, data.length, 0, 0);
    }

    public BaseCudaDataBuffer(int[] data) {
        //super(data);
        this(data.length, Nd4j.dataType() == Type.DOUBLE ? 8 : 4);
        set(data, data.length, 0, 0);
    }

    public BaseCudaDataBuffer(double[] data) {
       // super(data);
        this(data.length, Nd4j.dataType() == Type.DOUBLE ? 8 : 4);
        set(data, data.length, 0, 0);
    }

    public BaseCudaDataBuffer(byte[] data, long length) {
        this(Unpooled.wrappedBuffer(data),length);
    }

    public BaseCudaDataBuffer(ByteBuffer buffer, long length) {
        //super(buffer,length);
        throw new UnsupportedOperationException("OOPS 3");
    }

    public BaseCudaDataBuffer(ByteBuffer buffer, long length, long offset) {
        //super(buffer, length, offset);
        throw new UnsupportedOperationException("OOPS 4");
    }

    /**
     * This method always returns host pointer
     *
     * @return
     */
    @Override
    public long address() {
        return allocationPoint.getPointers().getHostPointer().address();
    }

    /**
     *
     * PLEASE NOTE: length, srcOffset, dstOffset are considered numbers of elements, not byte offsets
     *
     * @param data
     * @param length
     * @param srcOffset
     * @param dstOffset
     */
    public void set(int[] data, long length, long srcOffset, long dstOffset) {
        // TODO: make sure getPointer returns proper pointer
        if (dataType() == Type.DOUBLE) {
            //Pointer dstPtr = dstOffset > 0 ? new Pointer(allocator.getPointer(this).address()).withByteOffset(dstOffset * 4) : new Pointer(allocator.getPointer(this).address());
            //Pointer srcPtr = srcOffset > 0 ? Pointer.to(ArrayUtil.toDoubles(data)).withByteOffset(srcOffset * elementSize) : Pointer.to(ArrayUtil.toDoubles(data));
            Pointer srcPtr = new CudaPointer(new DoublePointer(ArrayUtil.toDoubles(data)).address() + (dstOffset * elementSize));

            allocator.memcpyAsync(this, srcPtr, length * elementSize, dstOffset * elementSize);
        } else if (dataType() == Type.FLOAT) {
            //Pointer srcPtr = srcOffset > 0 ? Pointer.to(ArrayUtil.toFloats(data)).withByteOffset(srcOffset * elementSize) : Pointer.to(ArrayUtil.toFloats(data));
            Pointer srcPtr = new CudaPointer(new FloatPointer(ArrayUtil.toFloats(data)).address() + (dstOffset * elementSize));

            allocator.memcpyAsync(this, srcPtr, length * elementSize, dstOffset * elementSize);
        } else if (dataType() == Type.INT) {
            //Pointer srcPtr = srcOffset > 0 ? Pointer.to(data).withByteOffset(srcOffset * elementSize) : Pointer.to(data);
            Pointer srcPtr = new CudaPointer(new IntPointer(data).address() + (dstOffset * elementSize));

            allocator.memcpyAsync(this, srcPtr, length * elementSize, dstOffset * elementSize);
        }
     //   allocator.synchronizeHostData(this);
    }

    /**
     *
     * PLEASE NOTE: length, srcOffset, dstOffset are considered numbers of elements, not byte offsets
     *
     * @param data
     * @param length
     * @param srcOffset
     * @param dstOffset
     */
    public void set(float[] data, long length, long srcOffset, long dstOffset) {
        // TODO: make sure getPointer returns proper pointer
        if (dataType() == Type.DOUBLE) {
            //Pointer dstPtr = dstOffset > 0 ? new Pointer(allocator.getPointer(this).address()).withByteOffset(dstOffset * 4) : new Pointer(allocator.getPointer(this).address());
            //Pointer srcPtr = srcOffset > 0 ? Pointer.to(ArrayUtil.toDoubles(data)).withByteOffset(srcOffset * elementSize) : Pointer.to(ArrayUtil.toDoubles(data));
            Pointer srcPtr = new CudaPointer(new DoublePointer(ArrayUtil.toDoubles(data)).address() + (dstOffset * elementSize));

            //memcpyAsync(dstPtr, srcPtr, length * 4);
            allocator.memcpyAsync(this, srcPtr, length * elementSize, dstOffset * elementSize);
        } else if (dataType() == Type.FLOAT) {
            //Pointer srcPtr = srcOffset > 0 ? Pointer.to(data).withByteOffset(srcOffset * elementSize) : Pointer.to(data);
            Pointer srcPtr = new CudaPointer(new FloatPointer(data).address() + (dstOffset * elementSize));

            //log.info("Memcpy params: byteLength: ["+(length * elementSize)+"], srcOffset: ["+(srcOffset * elementSize)+"], dstOffset: [" +(dstOffset* elementSize) + "]" );
            allocator.memcpyAsync(this, srcPtr, length * elementSize, dstOffset * elementSize);
        } else if (dataType() == Type.INT) {
            //Pointer srcPtr = srcOffset > 0 ? Pointer.to(ArrayUtil.toInts(data)).withByteOffset(srcOffset * elementSize) : Pointer.to(ArrayUtil.toInts(data));
            Pointer srcPtr = new CudaPointer(new IntPointer(ArrayUtil.toInts(data)).address() + (dstOffset * elementSize));

            allocator.memcpyAsync(this, srcPtr, length * elementSize, dstOffset * elementSize);
        }
    }

    /**
     *
     * PLEASE NOTE: length, srcOffset, dstOffset are considered numbers of elements, not byte offsets
     *
     * @param data
     * @param length
     * @param srcOffset
     * @param dstOffset
     */
    public void set(double[] data, long length, long srcOffset, long dstOffset) {
        // TODO: make sure getPointer returns proper pointer
        if (dataType() == Type.DOUBLE) {
            //Pointer dstPtr = dstOffset > 0 ? new Pointer(allocator.getPointer(this).address()).withByteOffset(dstOffset * 4) : new Pointer(allocator.getPointer(this).address());
            //Pointer srcPtr = srcOffset > 0 ? Pointer.to(data).withByteOffset(srcOffset * elementSize) : Pointer.to(data);
            Pointer srcPtr = new CudaPointer(new DoublePointer(data).address() + (dstOffset * elementSize));

            //memcpyAsync(dstPtr, srcPtr, length * 4);
            allocator.memcpyAsync(this, srcPtr, length * elementSize, dstOffset * elementSize);
        } else if (dataType() == Type.FLOAT) {
            //Pointer srcPtr = srcOffset > 0 ? Pointer.to(ArrayUtil.toFloats(data)).withByteOffset(srcOffset * elementSize) : Pointer.to(ArrayUtil.toFloats(data));
            Pointer srcPtr = new CudaPointer(new FloatPointer(ArrayUtil.toFloats(data)).address() + (dstOffset * elementSize));

            allocator.memcpyAsync(this, srcPtr, length * elementSize, dstOffset * elementSize);
        } else if (dataType() == Type.INT) {
            //Pointer srcPtr = srcOffset > 0 ? Pointer.to(ArrayUtil.toInts(data)).withByteOffset(srcOffset * elementSize) : Pointer.to(ArrayUtil.toInts(data));
            Pointer srcPtr = new CudaPointer(new IntPointer(ArrayUtil.toInts(data)).address() + (dstOffset * elementSize));

            allocator.memcpyAsync(this, srcPtr, length * elementSize, dstOffset * elementSize);
        }
    }

    @Override
    public void setData(int[] data) {
        set(data, data.length, 0, 0);
    }

    @Override
    public void setData(float[] data) {
        set(data, data.length, 0, 0);
    }

    @Override
    public void setData(double[] data) {
        set(data, data.length, 0, 0);
    }



    @Override
    protected void setNioBuffer() {
        throw new UnsupportedOperationException("setNioBuffer() is not supported for CUDA backend");
        /*
        wrappedBuffer = ByteBuffer.allocateDirect(elementSize * length);
        wrappedBuffer.order(ByteOrder.nativeOrder());
        */
    }

    @Override
    public void copyAtStride(DataBuffer buf, long n, long stride, long yStride, long offset, long yOffset) {
        allocator.synchronizeHostData(this);
        allocator.synchronizeHostData(buf);
        super.copyAtStride(buf, n, stride, yStride, offset, yOffset);
    }

    @Override
    public AllocationMode allocationMode() {
        return allocationMode;
    }

    @Override
    public ByteBuffer getHostBuffer() {
        return wrappedBuffer;
    }

    @Override
    public Pointer getHostPointer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pointer getHostPointer(int offset) {
        throw new UnsupportedOperationException();
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
    public int getElementSize() {
        return elementSize;
    }


    @Override
    public void addReferencing(String id) {
        referencing.add(id);
    }

    @Override
    public void put(long i, IComplexNumber result) {
        throw new UnsupportedOperationException("ComplexNumbers are not supported yet");
        /*
        modified.set(true);
        if (dataType() == DataBuffer.Type.FLOAT) {
            JCublas2.cublasSetVector(
                    (int) length(),
                    getElementSize(),
                    PointerUtil.getPointer(CudaComplexConversion.toComplex(result.asFloat()))
                    , 1
                    , getHostPointer()
                    , 1);
        }
        else {
            JCublas2.cublasSetVector(
                    (int) length(),
                    getElementSize(),
                    PointerUtil.getPointer(CudaComplexConversion.toComplexDouble(result.asDouble()))
                    , 1
                    , getHostPointer()
                    , 1);
        }
        */
    }


    @Deprecated
    public Pointer getHostPointer(INDArray arr,int stride, int offset,int length) {
        throw new UnsupportedOperationException("This method is deprecated");
    }

    @Deprecated
    public void set(Pointer pointer) {
        throw new UnsupportedOperationException("set(Pointer) is not supported");
        //modified.set(true);

        /*
        if (dataType() == DataBuffer.Type.DOUBLE) {
            JCublas2.cublasDcopy(
                    ContextHolder.getInstance().getHandle(),
                    length(),
                    pointer,
                    1,
                    getHostPointer(),
                    1
            );
        } else {
            JCublas2.cublasScopy(
                    ContextHolder.getInstance().getHandle(),
                    length(),
                    pointer,
                    1,
                    getHostPointer(),
                    1
            );
        }
        */
    }

    @Override
    public void put(long i, float element) {
        allocator.synchronizeHostData(this);
        allocator.tickHostWrite(this);
        super.put(i, element);
    }

    @Override
    public void put(long i, double element) {
        allocator.synchronizeHostData(this);
        allocator.tickHostWrite(this);
        super.put(i, element);
    }

    @Override
    public void put(long i, int element) {
        allocator.synchronizeHostData(this);
        allocator.tickHostWrite(this);
        super.put(i, element);
    }

    @Override
    public IComplexFloat getComplexFloat(long i) {
        return Nd4j.createFloat(getFloat(i), getFloat(i + 1));
    }

    @Override
    public IComplexDouble getComplexDouble(long i) {
        return Nd4j.createDouble(getDouble(i), getDouble(i + 1));
    }

    @Override
    public IComplexNumber getComplex(long i) {
        return dataType() == Type.FLOAT ? getComplexFloat(i) : getComplexDouble(i);
    }

    /**
     * Set an individual element
     *
     * @param index the index of the element
     * @param from  the element to get data from
     */
    @Deprecated
    protected void set(long index, long length, Pointer from, long inc) {


        long offset = getElementSize() * index;
        if (offset >= length() * getElementSize())
            throw new IllegalArgumentException("Illegal offset " + offset + " with index of " + index + " and length " + length());

        // TODO: fix this
        throw new UnsupportedOperationException("Deprecated set() call");
    }

    /**
     * Set an individual element
     *
     * @param index the index of the element
     * @param from  the element to get data from
     */
    @Deprecated
    protected void set(long index, long length, Pointer from) {
        set(index, length, from, 1);
    }

    @Override
    public void assign(DataBuffer data) {
        /*JCudaBuffer buf = (JCudaBuffer) data;
        set(0, buf.getHostPointer());
        */
        /*
        memcpyAsync(
                new Pointer(allocator.getPointer(this).address()),
                new Pointer(allocator.getPointer(data).address()),
                data.length()
        );*/
        allocator.memcpy(this, data);
    }





    /**
     * Set an individual element
     *
     * @param index the index of the element
     * @param from  the element to get data from
     */
    @Deprecated
    protected void set(long index, Pointer from) {
        set(index, 1, from);
    }

    @Override
    public void flush() {
        //
    }


    @Override
    public void destroy() {
    }

    private void writeObject(java.io.ObjectOutputStream stream)
            throws IOException {
        stream.defaultWriteObject();
        write(stream);
    }

    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        doReadObject(stream);
        // TODO: to be implemented
        /*
        copied = new HashMap<>();
        pointersToContexts = HashBasedTable.create();
        ref = new WeakReference<DataBuffer>(this,Nd4j.bufferRefQueue());
        freed = new AtomicBoolean(false);
        */
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for(int i = 0; i < length(); i++) {
            sb.append(getDouble(i));
            if(i < length() - 1)
                sb.append(",");
        }
        sb.append("]");
        return sb.toString();

    }

    @Override
    public boolean sameUnderlyingData(DataBuffer buffer) {
            return buffer.getTrackingPoint() == getTrackingPoint();
    }

    /**
     * PLEASE NOTE: this method implies STRICT equality only.
     * I.e: this == object
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (this == o) return true;

        return false;
    }

    @Override
    public void read(DataInputStream s) {
        try {
            // skip allocationMode
            s.readUTF();
            allocationMode = AllocationMode.JAVACPP;
            length = s.readInt();
            Type t = Type.valueOf(s.readUTF());
    //        log.info("Restoring buffer ["+t+"] of length ["+ length+"]");
            if(t == Type.DOUBLE) {
                this.elementSize = 8;
                this.allocationPoint = AtomicAllocator.getInstance().allocateMemory(this, new AllocationShape(length, elementSize));
                //allocationPoint.attachBuffer(this);
                this.trackingPoint = allocationPoint.getObjectId();

                this.pointer = new CudaPointer(allocationPoint.getPointers().getHostPointer(), length).asDoublePointer();
                indexer = DoubleIndexer.create((DoublePointer) pointer);

                double[] array = new double[(int) length];

                for(int i = 0; i < length(); i++) {
                    array[i] = s.readDouble();
                }
                setData(array);

            } else if(t == Type.FLOAT) {
                this.elementSize = 4;
                this.allocationPoint = AtomicAllocator.getInstance().allocateMemory(this, new AllocationShape(length, elementSize));
                //allocationPoint.attachBuffer(this);
                this.trackingPoint = allocationPoint.getObjectId();

                this.pointer = new CudaPointer(allocationPoint.getPointers().getHostPointer(), length).asFloatPointer();
                indexer = FloatIndexer.create((FloatPointer) pointer);

                float[] array = new float[(int) length];

                for(int i = 0; i < length(); i++) {
                    array[i] = s.readFloat();
                }
                setData(array);

            } else if(t == Type.INT) {
                this.elementSize = 4;
                this.allocationPoint = AtomicAllocator.getInstance().allocateMemory(this, new AllocationShape(length, elementSize));
                this.trackingPoint = allocationPoint.getObjectId();

                this.pointer = new CudaPointer(allocationPoint.getPointers().getHostPointer(), length).asIntPointer();
                indexer = IntIndexer.create((IntPointer) pointer);

                int[] array = new int[(int) length];

                for(int i = 0; i < length(); i++) {
                    array[i] = s.readInt();
                }
                setData(array);

            } else throw new IllegalStateException("Unknown dataType: ["+ t.toString()+"]");



            this.wrappedBuffer = this.pointer .asByteBuffer();
            this.wrappedBuffer.order(ByteOrder.nativeOrder());

      //      log.info("wrappedBuffer: length: ["+ wrappedBuffer.capacity()+"]");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        allocator.synchronizeHostData(this);
    }

    @Override
    public byte[] asBytes() {
        allocator.synchronizeHostData(this);
        return super.asBytes();
    }

    @Override
    public double[] asDouble() {
        allocator.synchronizeHostData(this);
        return super.asDouble();
    }

    @Override
    public float[] asFloat() {
        allocator.synchronizeHostData(this);
        return super.asFloat();
    }

    @Override
    public int[] asInt() {
        allocator.synchronizeHostData(this);
        return super.asInt();
    }

    @Override
    public ByteBuf asNetty() {
        allocator.synchronizeHostData(this);
        return super.asNetty();
    }

    @Override
    public ByteBuffer asNio() {
        allocator.synchronizeHostData(this);
        return super.asNio();
    }

    @Override
    public DoubleBuffer asNioDouble() {
        allocator.synchronizeHostData(this);
        return super.asNioDouble();
    }

    @Override
    public FloatBuffer asNioFloat() {
        allocator.synchronizeHostData(this);
        return super.asNioFloat();
    }

    @Override
    public IntBuffer asNioInt() {
        allocator.synchronizeHostData(this);
        return super.asNioInt();
    }

    @Override
    public DataBuffer dup() {
        allocator.synchronizeHostData(this);
        DataBuffer buffer = create(this.length);
        allocator.memcpyBlocking(buffer, new CudaPointer(allocator.getHostPointer(this).address()), this.length * elementSize, 0 );
        return buffer;
    }

    @Override
    public Number getNumber(long i) {
        allocator.synchronizeHostData(this);
        return super.getNumber(i);
    }

    @Override
    public double getDouble(long i) {
        allocator.synchronizeHostData(this);
        return super.getDouble(i);
    }

    @Override
    public double[] getDoublesAt(long offset, long inc, int length) {
        return super.getDoublesAt(offset, inc, length);
    }

    @Override
    public double[] getDoublesAt(long offset, int length) {
        return super.getDoublesAt(offset, length);
    }

    @Override
    public float getFloat(long i) {
        allocator.synchronizeHostData(this);

        //log.info("Requesting data:  trackingPoint: ["+ trackingPoint+"], length: ["+length+"], offset: ["+ offset+ "], position: ["+ i  +"], elementSize: [" +getElementSize() + "], byteoffset: ["+ (offset + i) * getElementSize() + "], bufferCapacity: ["+this.wrappedBuffer.capacity()+"], dtype: ["+dataType()+"]");

        return super.getFloat(i);
        //return wrappedBuffer.getFloat((int)(offset + i) * getElementSize());
    }

    @Override
    public float[] getFloatsAt(long offset, long inc, int length) {
        return super.getFloatsAt(offset, inc, length);
    }

    @Override
    public float[] getFloatsAt(long offset, int length) {
        return super.getFloatsAt(offset, length);
    }

    @Override
    public int getInt(long ix) {
        allocator.synchronizeHostData(this);
        return super.getInt(ix);
    }
}