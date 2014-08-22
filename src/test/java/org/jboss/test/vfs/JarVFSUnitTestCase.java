/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.vfs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jboss.vfs.VirtualFile;
import org.junit.Assert;

/**
 * Tests of the VFS implementation
 *
 * @author ales.justin@jboss.org
 */
public class JarVFSUnitTestCase extends AbstractVFSTest {
    public JarVFSUnitTestCase(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(JarVFSUnitTestCase.class);
    }

    public void testDuplicateName() throws Throwable {
        VirtualFile jar = getVirtualFile("/vfs/test/dup.jar");
        recursiveMount(jar);

        VirtualFile lower = jar.getChild("org/jboss/acme/Dummy.class");
        Assert.assertNotNull(lower);
        Assert.assertTrue(lower.exists());
        VirtualFile upper = jar.getChild("org/jboss/acme/DuMMy.class");
        Assert.assertNotNull(upper);
        Assert.assertTrue(upper.exists());
        String ll = readLine(lower);
        String ul = readLine(upper);
        Assert.assertFalse("Lines match", ll.equals(ul));
    }

    static String readLine(VirtualFile file) throws Throwable {
        InputStream is = file.openStream();
        try {
            return new BufferedReader(new InputStreamReader(is)).readLine();
        } finally {
            is.close();
        }
    }
}
