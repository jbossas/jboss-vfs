package org.jboss.vfs;


import java.io.File;
import java.io.IOException;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
@MessageBundle(projectCode = "VFS")
public interface VFSMessages {
    /**
     * The messages
     */
    VFSMessages MESSAGES = Messages.getBundle(VFSMessages.class);

    @Message(id = 10, value = "Can't set up temp file provider")
    RuntimeException cantSetupTempFileProvider(@Cause Throwable cause);

    @Message(id = 11, value = "Temp directory closed")
    IOException tempDirectoryClosed();

    @Message(id = 12, value = "Temp file provider closed")
    IOException tempFileProviderClosed();

    //  Retired
    //    @Message(id = 13, value = "Failed to clean existing content for temp file provider of type %s")
    //    IOException failedToCleanExistingContentForTempFileProvider(String providerType);

    @Message(id = 14, value = "Could not create directory for root '%s' (prefix '%s', suffix '%s') after %d attempts")
    IOException couldNotCreateDirectoryForRoot(File root, String prefix, String suffix, int retries);

    @Message(id = 15, value = "Could not create directory for original name '%s' after %d attempts")
    IOException couldNotCreateDirectory(String originalName, int retries);

    @Message(id = 16, value = "Root filesystem already mounted")
    IOException rootFileSystemAlreadyMounted();

    @Message(id = 17, value = "Filesystem already mounted at mount point \"%s\"")
    IOException fileSystemAlreadyMountedAtMountPoint(VirtualFile mountPoint);

    @Message(id = 18, value = "Stream is closed")
    IOException streamIsClosed();

    @Message(id = 19, value = "Not a file: '%s'")
    IOException notAFile(String path);

    @Message(id = 20, value = "Remote host access not supported for URLs of type '%s'")
    IOException remoteHostAccessNotSupportedForUrls(String protocol);

    @Message(id = 21, value = "%s must not be null")
    IllegalArgumentException nullArgument(String name);

    @Message(id = 22, value = "Null or empty %s")
    IllegalArgumentException nullOrEmpty(String name);

    @Message(id = 23, value = "Given parent (%s) is not an ancestor of this virtual file")
    IllegalArgumentException parentIsNotAncestor(VirtualFile parent);

    @Message(id = 24, value = "Problems creating new directory: %s")
    IllegalArgumentException problemCreatingNewDirectory(VirtualFile targetChild);

    @Message(id = 25, value = "Invalid Win32 path: %s")
    IllegalArgumentException invalidWin32Path(String path);

    @Message(id = 26, value = "Cannot decode: %s [%s]")
    IllegalArgumentException cannotDecode(String path, String encoding, @Cause Exception e);

    @Message(id = 27, value = "Invalid jar signature %s should be %s")
    IOException invalidJarSignature(String bytes, String expectedHeader);

    @Message(id = 28, value = "Invalid actions string: %s")
    IllegalArgumentException invalidActionsString(String actions);

    @Message(id = 29, value = "The totalBufferLength must be larger than: %s")
    IllegalArgumentException bufferMustBeLargerThan(int minimumBufferLength);

    @Message(id = 30, value = "Buffer does not have enough capacity")
    IllegalArgumentException bufferDoesntHaveEnoughCapacity();

    @Message(id = 31, value = "The preconfigured attributes are immutable")
    IllegalStateException preconfiguredAttributesAreImmutable();

    @Message(id = 32, value = ".. on root path")
    IllegalStateException onRootPath();
}
