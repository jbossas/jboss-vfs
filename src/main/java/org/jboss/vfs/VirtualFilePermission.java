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

package org.jboss.vfs;

import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;

/**
 * A permission to a file on the virtual file system.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @see java.io.FilePermission
 */
public final class VirtualFilePermission extends Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * @serial the action flags (must be within {@link #VALID_FLAGS})
     */
    private final int actionFlags;

    /**
     * The flag value for the "read" action.
     */
    public static final int FLAG_READ = Integer.parseInt("0000000000000001",2); //0b0000_0000_0000_0001
    /**
     * The flag value for the "delete" action.
     */
    public static final int FLAG_DELETE = Integer.parseInt("0000000000000010",2); //0b0000_0000_0000_0010;
    /**
     * The flag value for the "getfile" action.
     */
    public static final int FLAG_GET_FILE = Integer.parseInt("0000000000000100",2); //0b0000_0000_0000_0100;

    /**
     * The set of valid action flags for this permission.
     */
    public static final int VALID_FLAGS = Integer.parseInt("0000000000000111",2); //0b0000_0000_0000_0111;

    VirtualFilePermission(final String path, final int actionFlags, final boolean canonicalize) {
        super(canonicalize ? VFSUtils.canonicalize(path) : path);
        this.actionFlags = actionFlags & VALID_FLAGS;
    }

    /**
     * Construct a new instance.
     *
     * @param path    the path
     * @param actions the actions to grant
     */
    public VirtualFilePermission(final String path, final String actions) {
        this(path, parseActions(actions), true);
    }

    /**
     * Construct a new instance.  Any flags outside of {@link #VALID_FLAGS} are ignored.
     *
     * @param path        the path
     * @param actionFlags the action flags to set
     */
    public VirtualFilePermission(final String path, final int actionFlags) {
        this(path, actionFlags, true);
    }

    private static boolean in(char c, char t1, char t2) {
        return c == t1 || c == t2;
    }

    private static boolean lenIs(String s, int idx, int len, int wlen) {
        return idx == len - wlen || idx < len - wlen && s.charAt(idx + wlen) == ',';
    }

    static int parseActions(String actions) {
        final int len = actions.length();
        int res = 0;
        for (int i = 0; i < len; i++) {
            if (lenIs(actions, i, len, 4)
                    && in(actions.charAt(i), 'r', 'R')
                    && in(actions.charAt(i + 1), 'e', 'E')
                    && in(actions.charAt(i + 2), 'a', 'A')
                    && in(actions.charAt(i + 3), 'd', 'D')) {
                res |= FLAG_READ;
                i += 4;
            } else if (lenIs(actions, i, len, 6)
                    && in(actions.charAt(i), 'd', 'D')
                    && in(actions.charAt(i + 1), 'e', 'E')
                    && in(actions.charAt(i + 2), 'l', 'L')
                    && in(actions.charAt(i + 3), 'e', 'E')
                    && in(actions.charAt(i + 4), 't', 'T')
                    && in(actions.charAt(i + 5), 'e', 'E')) {
                res |= FLAG_DELETE;
                i += 6;
            } else if (lenIs(actions, i, len, 7)
                    && in(actions.charAt(i), 'g', 'G')
                    && in(actions.charAt(i + 1), 'e', 'E')
                    && in(actions.charAt(i + 2), 't', 'T')
                    && in(actions.charAt(i + 3), 'f', 'F')
                    && in(actions.charAt(i + 4), 'i', 'I')
                    && in(actions.charAt(i + 5), 'l', 'L')
                    && in(actions.charAt(i + 6), 'e', 'E')) {
                res |= FLAG_GET_FILE;
                i += 7;
            } else if (lenIs(actions, i, len, 1) && actions.charAt(i) == '*') {
                res |= FLAG_READ | FLAG_DELETE | FLAG_GET_FILE;
            } else {
                throw VFSMessages.MESSAGES.invalidActionsString(actions);
            }
        }
        return res;
    }

    public boolean implies(final Permission permission) {
        return permission instanceof VirtualFilePermission && implies((VirtualFilePermission) permission);
    }

    public boolean implies(final VirtualFilePermission permission) {
        return permission != null && impliesUnchecked(permission);
    }

    private boolean impliesUnchecked(final VirtualFilePermission permission) {
        final int theirFlags = permission.actionFlags;
        return (actionFlags & theirFlags) == theirFlags && impliesPath(getName(), permission.getName());
    }

    private static int ourIndexOf(String str, char ch, int start) {
        final int idx = str.indexOf(ch, start);
        return idx == -1 ? str.length() : idx;
    }

    static boolean impliesPath(String ourName, String theirName) {
        if ("<<ALL FILES>>".equals(ourName)) {
            return true;
        }
        return impliesPath(ourName, theirName, 0);
    }

    private static boolean impliesPath(String ourName, String theirName, int idx) {
        final int ourLen = ourName.length();
        final int theirLen = theirName.length();
        final int ei1 = ourIndexOf(ourName, '/', idx);
        final int ei2 = ourIndexOf(theirName, '/', idx);
        if (ei1 == idx + 1) {
            final char ch = ourName.charAt(idx);
            if (ch == '-') {
                // recursive wildcard...
                // if they are non-empty, match
                return theirLen > idx; // otherwise their segment is empty (no match)
            } else if (ch == '*') {
                // non-recursive wildcard...
                // if they are non-empty, and this is their last segment, match, unless they are '-'
                return theirLen > idx && ei2 == theirLen && (ei2 != ei1 || theirName.charAt(idx) != '-');
            }
        }
        if (ei1 == ei2) {
            if (ei1 == ourLen && ei2 == theirLen) {
                // exact match
                return true;
            } else {
                // leading sequence matches, check next segment
                return impliesPath(ourName, theirName, ei1 + 1);
            }
        } else {
            return false;
        }
    }

    public String getActions() {
        final StringBuilder builder = new StringBuilder();
        if ((actionFlags & FLAG_READ) != 0) {
            builder.append("read");
        }
        if ((actionFlags & FLAG_DELETE) != 0) {
            if (builder.length() > 0) { builder.append(','); }
            builder.append("delete");
        }
        if ((actionFlags & FLAG_GET_FILE) != 0) {
            if (builder.length() > 0) { builder.append(','); }
            builder.append("getfile");
        }
        return builder.toString();
    }

    /**
     * Get the action flags for this permission.
     *
     * @return the action flags for this permission
     */
    public int getActionFlags() {
        return actionFlags;
    }

    public PermissionCollection newPermissionCollection() {
        return new VirtualFilePermissionCollection();
    }

    public boolean equals(final Object permission) {
        return permission instanceof VirtualFilePermission && equals((VirtualFilePermission) permission);
    }

    public boolean equals(final Permission permission) {
        return permission instanceof VirtualFilePermission && equals((VirtualFilePermission) permission);
    }

    public boolean equals(final VirtualFilePermission permission) {
        return permission != null && permission.actionFlags == actionFlags && permission.getName().equals(permission.getName());
    }

    public int hashCode() {
        return getName().hashCode() * 11 + actionFlags;
    }
}
