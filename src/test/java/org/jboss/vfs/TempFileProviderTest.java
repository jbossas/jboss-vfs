package org.jboss.vfs;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
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

    @Test
    public void testMountingZip() throws Exception {
        TempFileProvider tempFileProvider = TempFileProvider.create("temp", executorService, true);

        VirtualFile deploymentRoot = VFS.getChild(Thread.currentThread().getContextClassLoader().getResource("vfs/content").toURI());
        VirtualFile content = VFS.getChild(Thread.currentThread().getContextClassLoader().getResource("vfs/test/jar1.jar").toURI());

        Closeable mountPoint = VFS.mountZip(content, deploymentRoot, tempFileProvider);
        Assert.assertNotNull("mount should work", mountPoint);
        mountPoint.close();

        VirtualFile test = VFS.getChild("C:\\development\\java\\wildfly\\testsuite\\integration\\smoke\\target\\workdir\\target\\auto-deployments\\test-deployment.sar");
        Assert.assertEquals("should got just name","test-deployment.sar",test.getName());
    }
}
