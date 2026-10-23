package io.macroservice.platform.demo;

import org.springframework.beans.factory.ObjectProvider;

/** Defers looking up the real {@link TraceStore}: post-processors are created before ordinary beans. */
final class LazyStore extends TraceStore {

    private final ObjectProvider<TraceStore> provider;

    LazyStore(ObjectProvider<TraceStore> provider) {
        super(0);
        this.provider = provider;
    }

    @Override
    public void add(TraceRecord record) {
        provider.getObject().add(record);
    }
}
