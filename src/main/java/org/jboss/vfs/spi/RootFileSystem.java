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

package org.jboss.vfs.spi;

import java.net.URI;
import java.net.URISyntaxException;
import org.jboss.vfs.VirtualFile;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.security.CodeSigner;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

/**
 * A special FileSystem which supports multiple roots.
 * 
 * This is currently accomplished by requiring that VirtualFile.getPathName()
 * produce output that is consumable by java.io.File as a path.
 */
public final class RootFileSystem implements FileSystem {

    private static final Logger log = Logger.getLogger("org.jboss.vfs.root");
    
    public static final RootFileSystem ROOT_INSTANCE = new RootFileSystem();
    
    private boolean forceCaseSensative = false;
    
    private RootFileSystem() {
    	forceCaseSensative = "true".equals(System.getProperty("jboss.vfs.forceCaseSensative"));
    }

    /**
     * {@inheritDoc}
     */
    public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return new FileInputStream(getFile(mountPoint, target));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadOnly() {
        return false;
    }

    private class ForceCaseSensativeFileFilter implements FilenameFilter {
    	private String virtualFileName = "";
    	
    	public ForceCaseSensativeFileFilter(String virtualFileName) {
    		this.virtualFileName = virtualFileName;
    		if (virtualFileName == null) {
    			virtualFileName = "";
    		}
    	}
    	
		@Override
		public boolean accept(File dir, String name) {
			return virtualFileName.equals(name);
		}    	
    }
    
    /**
     * {@inheritDoc}
     */
    public File getFile(VirtualFile mountPoint, VirtualFile target) {
    	File retFile = new File(target.getPathName()); 
        if (forceCaseSensative && retFile != null && retFile.isFile()) {
        	File parentFile = retFile.getParentFile();
        	if (parentFile != null) {
        		ForceCaseSensativeFileFilter wc = new ForceCaseSensativeFileFilter(target.getName());
        		if (parentFile.list(wc).length == 0) {
        			// Randomize the Filename so that it will map to a file that does not exist.
        			retFile = new File(target.getPathName() + UUID.randomUUID().toString());	
        		}        		
        	}        	
        }
        return retFile;
    }

    /**
     * {@inheritDoc}
     */
    public boolean delete(VirtualFile mountPoint, VirtualFile target) {
        return getFile(mountPoint, target).delete();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize(VirtualFile mountPoint, VirtualFile target) {
        return getFile(mountPoint, target).length();
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified(VirtualFile mountPoint, VirtualFile target) {
        return getFile(mountPoint, target).lastModified();
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(VirtualFile mountPoint, VirtualFile target) {
        return getFile(mountPoint, target).exists();
    }

    /** {@inheritDoc} */
    public boolean isFile(final VirtualFile mountPoint, final VirtualFile target) {
        return getFile(mountPoint, target).isFile();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) {
        return getFile(mountPoint, target).isDirectory();
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) {
        final String[] names = getFile(mountPoint, target).list();
        return names == null ? Collections.<String>emptyList() : Arrays.asList(names);
    }
    
    /**
     * {@inheritDoc}
     */
    public CodeSigner[] getCodeSigners(VirtualFile mountPoint, VirtualFile target) {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public File getMountSource() {
        return null;
    }

    public URI getRootURI() throws URISyntaxException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        // no operation - the root FS can't be closed
    }
}
