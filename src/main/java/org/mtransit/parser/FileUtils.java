package org.mtransit.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@SuppressWarnings("unused")
public final class FileUtils {

	public static void mkdir(@NotNull File file) {
		boolean success = file.mkdir();
		if (!success) {
			throw new MTLog.Fatal("Creation of the directory '%s' failed!", file);
		}
	}

	public static void deleteIfExist(@NotNull File file) {
		if (!file.exists()) {
			return; // no file to delete
		}
		boolean success = file.delete();
		if (!success) {
			throw new MTLog.Fatal("Deletion of the directory '%s' failed!", file);
		}
	}

	public static void delete(@NotNull File file) {
		boolean success = file.delete();
		if (!success) {
			throw new MTLog.Fatal("Deletion of the directory '%s' failed!", file);
		}
	}

	@Nullable
	public static Long size(@NotNull File file) {
		try {
			return Files.size(file.toPath());
		} catch (IOException ioe) {
			MTLog.logNonFatal(ioe, "Error while reading size of %s!", file);
			return null;
		}
	}

	@Nullable
	public static String sizeToDiplayString(@Nullable Long size) {
		try {
			return org.apache.commons.io.FileUtils.byteCountToDisplaySize(size == null ? 0L : size);
		} catch (Exception e) {
			MTLog.logNonFatal(e, "Error while converting size of %s!", size);
			return null;
		}
	}
}
