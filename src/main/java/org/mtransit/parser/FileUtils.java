package org.mtransit.parser;

import org.jetbrains.annotations.NotNull;

import java.io.File;

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
}
