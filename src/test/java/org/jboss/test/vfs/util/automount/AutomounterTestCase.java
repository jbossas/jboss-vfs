/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.test.vfs.util.automount;

import java.io.File;

import org.jboss.test.vfs.AbstractVFSTest;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.util.automount.Automounter;
import org.jboss.vfs.util.automount.MountOption;
import org.jboss.vfs.util.automount.MountOwner;
import org.jboss.vfs.util.automount.SimpleMountOwner;
import org.jboss.vfs.util.automount.VirtualFileOwner;

/**
 * Test for {@link Automounter}
 *
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 */
public class AutomounterTestCase extends AbstractVFSTest {

    public AutomounterTestCase(String name) {
        super(name);
    }

    public void testMountAndCleanup() throws Exception {
        VirtualFile virtualFile = getVirtualFile("/vfs/test/simple.ear");
        MountOwner owner = new VirtualFileOwner(virtualFile);
        Automounter.mount(owner, virtualFile);
        assertTrue(Automounter.isMounted(virtualFile));
        Automounter.cleanup(owner);
        assertFalse(Automounter.isMounted(virtualFile));
    }

    public void testCleanupWithOwner() throws Exception {
        VirtualFile earVirtualFile = getVirtualFile("/vfs/test/simple.ear");
        Automounter.mount(earVirtualFile);

        VirtualFileOwner owner = new VirtualFileOwner(earVirtualFile);

        VirtualFile jarVirtualFile = earVirtualFile.getChild("archive.jar");
        Automounter.mount(owner, jarVirtualFile);

        VirtualFile warVirtualFile = earVirtualFile.getChild("simple.war");
        Automounter.mount(owner, warVirtualFile);

        assertTrue(Automounter.isMounted(earVirtualFile));
        assertTrue(Automounter.isMounted(warVirtualFile));
        assertTrue(Automounter.isMounted(jarVirtualFile));

        Automounter.cleanup(owner);

        assertFalse(Automounter.isMounted(earVirtualFile));
        assertFalse(Automounter.isMounted(warVirtualFile));
        assertFalse(Automounter.isMounted(jarVirtualFile));
    }

    public void testCleanupRecursive() throws Exception {
        VirtualFile earVirtualFile = getVirtualFile("/vfs/test/simple.ear");
        Automounter.mount(earVirtualFile);

        VirtualFile jarVirtualFile = earVirtualFile.getChild("archive.jar");
        Automounter.mount(jarVirtualFile);

        VirtualFile warVirtualFile = earVirtualFile.getChild("simple.war");
        Automounter.mount(warVirtualFile);

        assertTrue(Automounter.isMounted(earVirtualFile));
        assertTrue(Automounter.isMounted(warVirtualFile));
        assertTrue(Automounter.isMounted(jarVirtualFile));

        Automounter.cleanup(new VirtualFileOwner(earVirtualFile));

        assertFalse(Automounter.isMounted(earVirtualFile));
        assertFalse(Automounter.isMounted(warVirtualFile));
        assertFalse(Automounter.isMounted(jarVirtualFile));
    }

    public void testCleanupRefereces() throws Exception {
        VirtualFile earVirtualFile = getVirtualFile("/vfs/test/simple.ear");
        Automounter.mount(earVirtualFile);

        VirtualFileOwner owner = new VirtualFileOwner(earVirtualFile);

        VirtualFile jarVirtualFile = getVirtualFile("/vfs/test/jar1.jar");
        Automounter.mount(owner, jarVirtualFile);

        VirtualFile warVirtualFile = getVirtualFile("/vfs/test/filesonly.war");
        Automounter.mount(owner, warVirtualFile);

        assertTrue(Automounter.isMounted(earVirtualFile));
        assertTrue(Automounter.isMounted(warVirtualFile));
        assertTrue(Automounter.isMounted(jarVirtualFile));

        VirtualFile otherEarVirtualFile = getVirtualFile("/vfs/test/spring-ear.ear");
        Automounter.mount(otherEarVirtualFile, jarVirtualFile);

        Automounter.cleanup(owner);

        assertFalse(Automounter.isMounted(earVirtualFile));
        assertFalse(Automounter.isMounted(warVirtualFile));
        assertTrue("Should not have unmounted the reference from two locations", Automounter.isMounted(jarVirtualFile));

        Automounter.cleanup(otherEarVirtualFile);
        assertFalse(Automounter.isMounted(jarVirtualFile));
    }

    public void testCleanupReferecesSameVF() throws Exception {
        VirtualFile earVirtualFile = getVirtualFile("/vfs/test/simple.ear");
        Automounter.mount(earVirtualFile);

        VirtualFileOwner owner = new VirtualFileOwner(earVirtualFile);

        VirtualFile jarVirtualFile = getVirtualFile("/vfs/test/jar1.jar");
        Automounter.mount(owner, jarVirtualFile);

        VirtualFileOwner otherOwner = new VirtualFileOwner(earVirtualFile);
        Automounter.mount(otherOwner, jarVirtualFile);

        Automounter.cleanup(owner);

        assertFalse("Should have been unmounted since the VirtualFile is the same", Automounter.isMounted(jarVirtualFile));
    }

    public void testCleanupReferecesSimpleOwner() throws Exception {
        MountOwner owner = new SimpleMountOwner(new Object());

        VirtualFile jarVirtualFile = getVirtualFile("/vfs/test/jar1.jar");
        Automounter.mount(owner, jarVirtualFile);

        MountOwner otherOwner = new SimpleMountOwner(new Object());
        Automounter.mount(otherOwner, jarVirtualFile);

        Automounter.cleanup(owner);

        assertTrue("Should not have unmounted the reference from two locations", Automounter.isMounted(jarVirtualFile));

        Automounter.cleanup(otherOwner);
        assertFalse(Automounter.isMounted(jarVirtualFile));
    }

    public void testCleanupReferecesSimpleOwnerSameObj() throws Exception {
        Object ownerObject = new Object();

        VirtualFile jarVirtualFile = getVirtualFile("/vfs/test/jar1.jar");
        Automounter.mount(ownerObject, jarVirtualFile);

        Automounter.mount(ownerObject, jarVirtualFile);

        Automounter.cleanup(ownerObject);

        assertFalse("Should have been unmounted since the owner object is the same", Automounter.isMounted(jarVirtualFile));
    }

    public void testMountWithCopy() throws Exception {
        VirtualFile jarVirtualFile = getVirtualFile("/vfs/test/jar1.jar");
        File originalFile = jarVirtualFile.getPhysicalFile();
        Automounter.mount(jarVirtualFile, MountOption.COPY);

        File copiedFile = jarVirtualFile.getPhysicalFile();

        assertFalse(copiedFile.equals(originalFile));

        Automounter.cleanup(jarVirtualFile);
        assertFalse("Should have been unmounted since the owner object is the same", Automounter.isMounted(jarVirtualFile));
    }

}
