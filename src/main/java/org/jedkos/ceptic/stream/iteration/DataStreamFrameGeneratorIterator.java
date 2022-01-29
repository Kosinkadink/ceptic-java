package org.jedkos.ceptic.stream.iteration;

import org.jedkos.ceptic.stream.StreamFrame;
import org.jedkos.ceptic.stream.StreamFrameHelper;

import java.util.Arrays;
import java.util.Iterator;

public class DataStreamFrameGeneratorIterator implements Iterator<StreamFrame> {

    private final DataStreamFrameGenerator generator;
    private int index = 0;

    public DataStreamFrameGeneratorIterator(DataStreamFrameGenerator generator) {
        this.generator = generator;
    }

    @Override
    public boolean hasNext() {
        return generator.data.length-1 > index;
    }

    @Override
    public StreamFrame next() {
        // get chunk of data
        byte[] chunk = Arrays.copyOfRange(generator.data, index,
                index+Math.min(generator.frameSize, generator.data.length-index));
        // iterate next chunk's starting index
        index += generator.frameSize;
        // if next chunk will be out of bounds, yield last frame
        if (!hasNext()) {
            if (generator.isFirstHeader) {
                return StreamFrameHelper.createHeaderLast(generator.streamId, chunk);
            }
            else {
                if (generator.isResponse) {
                    return StreamFrameHelper.createResponseLast(generator.streamId, chunk);
                } else {
                    return StreamFrameHelper.createDataLast(generator.streamId, chunk);
                }
            }
        }
        // otherwise yield continued frame
        if (generator.isFirstHeader) {
            generator.isFirstHeader = false;
            return StreamFrameHelper.createHeaderContinued(generator.streamId, chunk);
        } else {
            if (generator.isResponse) {
                generator.isResponse = false;
                return StreamFrameHelper.createResponseContinued(generator.streamId, chunk);
            } else {
                return StreamFrameHelper.createDataContinued(generator.streamId, chunk);
            }
        }
    }
}
