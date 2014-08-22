/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import static org.jboss.vfs.VirtualFilePermission.FLAG_DELETE;
import static org.jboss.vfs.VirtualFilePermission.FLAG_GET_FILE;
import static org.jboss.vfs.VirtualFilePermission.FLAG_READ;

import java.security.PermissionCollection;

import junit.framework.TestCase;
import org.jboss.vfs.VirtualFilePermission;

public final class VirtualFilePermissionTestCase extends TestCase {

    public void testFlagParsing() {
        assertEquals(new VirtualFilePermission("foo", "read").getActionFlags(), FLAG_READ);
        assertEquals(new VirtualFilePermission("foo", "delete").getActionFlags(), FLAG_DELETE);
        assertEquals(new VirtualFilePermission("foo", "getfile").getActionFlags(), FLAG_GET_FILE);
        assertEquals(new VirtualFilePermission("foo", "read,delete").getActionFlags(), FLAG_READ | FLAG_DELETE);
        assertEquals(new VirtualFilePermission("foo", "read,getfile").getActionFlags(), FLAG_READ | FLAG_GET_FILE);
        assertEquals(new VirtualFilePermission("foo", "delete,getfile").getActionFlags(), FLAG_DELETE | FLAG_GET_FILE);
        assertEquals(new VirtualFilePermission("foo", "delete,getfile,read").getActionFlags(), FLAG_DELETE | FLAG_GET_FILE | FLAG_READ);
        assertEquals(new VirtualFilePermission("foo", "*").getActionFlags(), FLAG_DELETE | FLAG_GET_FILE | FLAG_READ);
        assertEquals(new VirtualFilePermission("foo", "").getActionFlags(), 0);
        try {
            new VirtualFilePermission("foo", "blah");
            fail("expected exception");
        } catch (IllegalArgumentException expected) {}
    }

    public void testPathCanonicalization() {
        assertEquals(new VirtualFilePermission("/simple/test", 0).getName(), "/simple/test");
        assertEquals(new VirtualFilePermission("/simple//test", 0).getName(), "/simple/test");
        assertEquals(new VirtualFilePermission("//simple//test", 0).getName(), "/simple/test");
        assertEquals(new VirtualFilePermission("/./simple//test", 0).getName(), "/simple/test");
        assertEquals(new VirtualFilePermission("/simple/./test", 0).getName(), "/simple/test");
        assertEquals(new VirtualFilePermission("/simple/not/../test", 0).getName(), "/simple/test");
        assertEquals(new VirtualFilePermission("/simple/not/./../test", 0).getName(), "/simple/test");
        assertEquals(new VirtualFilePermission("/simple/not/.././test", 0).getName(), "/simple/test");
        assertEquals(new VirtualFilePermission("simple/not/.././test", 0).getName(), "simple/test");
        assertEquals(new VirtualFilePermission("/../../..", 0).getName(), "/");
        assertEquals(new VirtualFilePermission("/././.", 0).getName(), "/");
        assertEquals(new VirtualFilePermission("/..", 0).getName(), "/");
        assertEquals(new VirtualFilePermission("/.", 0).getName(), "/");
        assertEquals(new VirtualFilePermission("../../..", 0).getName(), "");
        assertEquals(new VirtualFilePermission("..", 0).getName(), "");
        assertEquals(new VirtualFilePermission(".", 0).getName(), "");
    }

    public void testImpliesSimple() {
        assertTrue(new VirtualFilePermission("foo", "read").implies(new VirtualFilePermission("foo", FLAG_READ)));
        assertTrue(new VirtualFilePermission("foo", "delete").implies(new VirtualFilePermission("foo", FLAG_DELETE)));
        assertTrue(new VirtualFilePermission("foo", "getfile").implies(new VirtualFilePermission("foo", FLAG_GET_FILE)));
        assertTrue(new VirtualFilePermission("foo", "read,delete").implies(new VirtualFilePermission("foo", FLAG_READ | FLAG_DELETE)));
        assertTrue(new VirtualFilePermission("foo", "read,getfile").implies(new VirtualFilePermission("foo", FLAG_READ | FLAG_GET_FILE)));
        assertTrue(new VirtualFilePermission("foo", "delete,getfile").implies(new VirtualFilePermission("foo", FLAG_DELETE | FLAG_GET_FILE)));
        assertTrue(new VirtualFilePermission("foo", "delete,getfile,read").implies(new VirtualFilePermission("foo", FLAG_DELETE | FLAG_GET_FILE | FLAG_READ)));
        assertTrue(new VirtualFilePermission("foo", "*").implies(new VirtualFilePermission("foo", FLAG_DELETE | FLAG_GET_FILE | FLAG_READ)));
        assertTrue(new VirtualFilePermission("foo", "").implies(new VirtualFilePermission("foo", 0)));
    }

    public void testImpliesMatch() {
        assertTrue(new VirtualFilePermission("/foo/bar/*", FLAG_READ).implies(new VirtualFilePermission("/foo/bar/baz", "read")));
        assertTrue(new VirtualFilePermission("/foo/bar/*", FLAG_READ).implies(new VirtualFilePermission("/foo/bar/*", "read")));
        assertFalse(new VirtualFilePermission("/foo/bar/*", FLAG_READ).implies(new VirtualFilePermission("/foo/bar/-", "read")));
        assertFalse(new VirtualFilePermission("/foo/bar/*", FLAG_READ).implies(new VirtualFilePermission("/foo/bar", "read")));
        assertTrue(new VirtualFilePermission("/foo/bar/-", FLAG_READ).implies(new VirtualFilePermission("/foo/bar/baz", "read")));
        assertTrue(new VirtualFilePermission("/foo/bar/-", FLAG_READ).implies(new VirtualFilePermission("/foo/bar/*", "read")));
        assertTrue(new VirtualFilePermission("/foo/bar/-", FLAG_READ).implies(new VirtualFilePermission("/foo/bar/-", "read")));
        assertFalse(new VirtualFilePermission("/foo/bar/-", FLAG_READ).implies(new VirtualFilePermission("/foo/bar", "read")));

        assertTrue(new VirtualFilePermission("/*", FLAG_READ).implies(new VirtualFilePermission("/baz", "read")));
        assertTrue(new VirtualFilePermission("/*", FLAG_READ).implies(new VirtualFilePermission("/*", "read")));
        assertFalse(new VirtualFilePermission("/*", FLAG_READ).implies(new VirtualFilePermission("/-", "read")));
        assertFalse(new VirtualFilePermission("/*", FLAG_READ).implies(new VirtualFilePermission("", "read")));
        assertTrue(new VirtualFilePermission("/-", FLAG_READ).implies(new VirtualFilePermission("/baz", "read")));
        assertTrue(new VirtualFilePermission("/-", FLAG_READ).implies(new VirtualFilePermission("/*", "read")));
        assertTrue(new VirtualFilePermission("/-", FLAG_READ).implies(new VirtualFilePermission("/-", "read")));
        assertFalse(new VirtualFilePermission("/-", FLAG_READ).implies(new VirtualFilePermission("", "read")));

        assertTrue(new VirtualFilePermission("*", FLAG_READ).implies(new VirtualFilePermission("baz", "read")));
        assertTrue(new VirtualFilePermission("*", FLAG_READ).implies(new VirtualFilePermission("*", "read")));
        assertFalse(new VirtualFilePermission("*", FLAG_READ).implies(new VirtualFilePermission("-", "read")));
        assertFalse(new VirtualFilePermission("*", FLAG_READ).implies(new VirtualFilePermission("", "read")));
        assertTrue(new VirtualFilePermission("-", FLAG_READ).implies(new VirtualFilePermission("baz", "read")));
        assertTrue(new VirtualFilePermission("-", FLAG_READ).implies(new VirtualFilePermission("*", "read")));
        assertTrue(new VirtualFilePermission("-", FLAG_READ).implies(new VirtualFilePermission("-", "read")));
        assertFalse(new VirtualFilePermission("-", FLAG_READ).implies(new VirtualFilePermission("", "read")));
    }

    public void testCollection() {
        final PermissionCollection collection = new VirtualFilePermission("foo", 0).newPermissionCollection();
        collection.add(new VirtualFilePermission("/foo/bar/*", "read"));
        collection.add(new VirtualFilePermission("/foo/bar/-", "delete"));
        collection.add(new VirtualFilePermission("/foo/bar/baz", "getfile,read"));
        collection.add(new VirtualFilePermission("foo/bar/baz", "delete"));

        assertTrue(collection.implies(new VirtualFilePermission("/foo/bar/blah", "read,delete")));
        assertFalse(collection.implies(new VirtualFilePermission("/foo/bar/blah", "read,delete,getfile")));
        assertTrue(collection.implies(new VirtualFilePermission("/foo/bar/baz/zap", "delete")));
        assertFalse(collection.implies(new VirtualFilePermission("/foo/bar/baz/zap", "read")));
        assertFalse(collection.implies(new VirtualFilePermission("/foo/bar", "read")));
        assertFalse(collection.implies(new VirtualFilePermission("/foo", "read")));
    }
}
