package org.jboss.vfs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class TempFileProviderTest {
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(2);

    @Test
    public void testCleanup() throws Exception {
        TempFileProvider tempFileProvider = TempFileProvider.create("temp", executorService, true);
        TempDir tmp = tempFileProvider.createTempDir("test-dir");
        tmp.createFile("test.txt", new ByteArrayInputStream("test".getBytes("utf-8")));
        tempFileProvider = TempFileProvider.create("temp", executorService, true);
        tempFileProvider.createTempDir("blah");
        File parent = tempFileProvider.getProviderRoot().toPath().getParent().toFile();
        Assert.assertEquals("There should be only one entry in directory", 1, parent.list().length);
    }


}
