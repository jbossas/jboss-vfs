/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.security.PermissionCollection;
import junit.framework.TestCase;
import org.jboss.vfs.VirtualFilePermission;

import static org.jboss.vfs.VirtualFilePermission.FLAG_DELETE;
import static org.jboss.vfs.VirtualFilePermission.FLAG_GET_FILE;
import static org.jboss.vfs.VirtualFilePermission.FLAG_READ;

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
