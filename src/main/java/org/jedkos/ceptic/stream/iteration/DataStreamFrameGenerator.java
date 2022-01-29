package org.jedkos.ceptic.stream.iteration;

import org.jedkos.ceptic.stream.StreamFrame;

import java.util.Iterator;
import java.util.UUID;

public class DataStreamFrameGenerator implements Iterable<StreamFrame> {

    protected final UUID streamId;
    protected byte[] data;
    protected boolean isFirstHeader;
    protected boolean isResponse;
    protected final int frameSize;


    public DataStreamFrameGenerator(UUID streamId, byte[] data, int frameSize, boolean isFirstHeader, boolean isResponse) {
        this.streamId = streamId;
        this.data = data;
        this.frameSize = frameSize / 2; // cut in half for generator to account for possible encoding size increase
        this.isFirstHeader = isFirstHeader;
        this.isResponse = isResponse;
    }

    @Override
    public Iterator<StreamFrame> iterator() {
        return new DataStreamFrameGeneratorIterator(this);
    }

}
