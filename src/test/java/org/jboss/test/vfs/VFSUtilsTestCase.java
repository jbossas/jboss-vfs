/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.test.vfs;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

/**
 * Test to ensure the functionality of {@link VFSUtils} methods
 *
 * @author <a href="baileyje@gmail.com">John Bailey</a>
 */
public class VFSUtilsTestCase extends AbstractVFSTest {

    public VFSUtilsTestCase(String name) {
        super(name);
    }

    @Test
    public void testCopyChildrenRecursive() throws Exception {
        VirtualFile original = getVirtualFile("/vfs/test/jar1");
        VirtualFile target = VFS.getChild("/target-jar1");
        Closeable handle = null;
        try {
            handle = VFS.mountTemp(target, TempFileProvider.create("test", Executors.newSingleThreadScheduledExecutor()));
            VFSUtils.copyChildrenRecursive(original, target);
            assertChildren(original, target);

        } finally {
            VFSUtils.safeClose(handle);
        }
    }

    @Test
    public void testReadManifest() throws Exception {
        VirtualFile correctManifest = getVirtualFile("/vfs/test/manifest/correct.mf");        
        Manifest manifest = VFSUtils.readManifest(correctManifest);               
        assertManifest(manifest);
        VirtualFile incorrectManifest = getVirtualFile("/vfs/test/manifest/incorrect.mf");
        manifest = VFSUtils.readManifest(incorrectManifest);               
        assertManifest(manifest);
    }

    private void assertManifest(Manifest manifest) {
        Attributes attributes = manifest.getMainAttributes();
        assertEquals(9, attributes.size());
        assertEquals("Manifest-Version", "1.0", attributes.getValue("Manifest-Version"));
        assertEquals("Specification-Title", "filesonly-war", attributes.getValue("Specification-Title"));
        assertEquals("Specification-Version", "1.0.0.GA", attributes.getValue("Specification-Version"));
        assertEquals("Specification-Vendor", "JBoss Inc.", attributes.getValue("Specification-Vendor"));
        assertEquals("Implementation-Title", "filesonly-war", attributes.getValue("Implementation-Title"));
        assertEquals("Implementation-URL", "http://www.jboss.org", attributes.getValue("Implementation-URL"));
        assertEquals("Implementation-Version", "1.0.0.GA-jboss", attributes.getValue("Implementation-Version"));
        assertEquals("Implementation-Vendor", "JBoss Inc.", attributes.getValue("Implementation-Vendor"));
        assertEquals("Created-By", "${java.runtime.version} (${java.vendor})", attributes.getValue("Created-By"));
    }

    private void assertChildren(VirtualFile original, VirtualFile target) throws ArrayComparisonFailure, IOException {
        assertEquals("Original and target must have the same numer of children", original.getChildren().size(), target.getChildren().size());
        for (VirtualFile child : original.getChildren()) {
            VirtualFile targetChild = target.getChild(child.getName());
            assertTrue("Target should contain same children as original", targetChild.exists());
            if (child.isDirectory()) {
                assertChildren(child, targetChild);
            } else {
                assertContentEqual(child, targetChild);
            }
        }
    }
}
