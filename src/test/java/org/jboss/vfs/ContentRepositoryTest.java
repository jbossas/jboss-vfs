package org.jboss.vfs;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class ContentRepositoryTest {


    @Test
    public void testRelativeContentRoot() {
        final VirtualFile deploymentRoot = VFS.getChild("content/74b0411f31e4fb1d4a261c94aa8674da05c64f/content");
        assert deploymentRoot != null;
        Assert.assertEquals("Path should not be absolute", "/content/74b0411f31e4fb1d4a261c94aa8674da05c64f/content", deploymentRoot.getPathName());
        Assert.assertEquals("name should be just last part", "content", deploymentRoot.getName());
    }

}
