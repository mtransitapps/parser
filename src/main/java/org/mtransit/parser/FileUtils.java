package org.mtransit.parser;

import java.io.File;

@SuppressWarnings("unused")
public final class FileUtils {

	public static void mkdir(File file) {
		boolean success = file.mkdir();
		if (!success) {
			MTLog.logFatal("Creation of the directory '%s' failed!", file);
		}
	}

	public static void deleteIfExist(File file) {
		if (!file.exists()) {
			return; // no file to delete
		}
		boolean success = file.delete();
		if (!success) {
			MTLog.logFatal("Deletion of the directory '%s' failed!", file);
		}
	}

	public static void delete(File file) {
		boolean success = file.delete();
		if (!success) {
			MTLog.logFatal("Deletion of the directory '%s' failed!", file);
		}
	}
}
