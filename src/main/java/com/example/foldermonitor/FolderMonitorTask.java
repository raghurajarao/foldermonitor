package com.example.foldermonitor;

import static com.example.foldermonitor.MonitorConfig.EXCLUSION_LIST;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.stream.Stream;

public class FolderMonitorTask implements Runnable {
	private String monitorFolder;
	private Set<String> exclusion = Stream.of(EXCLUSION_LIST.split(",")).collect(toSet());
	
	public FolderMonitorTask (String folder) {
		this.monitorFolder = folder;
	}

	@Override
	public void run() {
		try {
			MonitorStats stats = new MonitorStats();
			log("Monitoring of folder %s started", this.monitorFolder);
			monitorSize(stats);
			purgeExecutables(stats);
			showMonitorStats(stats);
			log("Monitoring of floder %s ended", this.monitorFolder);
		} catch (Exception ex) {
			log("Exception occured while monitoring %s", ex.getMessage());
		}
	}

	private void monitorSize(MonitorStats stats) throws IOException {
		List<File> files = Files.list(Paths.get(this.monitorFolder)).map(p -> p.toFile()).filter(f -> isNonExecutableFile(f.getName())).collect(toList());
		long folderSize = files.stream().mapToLong(f -> f.length()).sum();
		if (folderSize > MonitorConfig.FOLDER_SIZE_MAXLIMIT) {
			log("Archiving folder as it exceeds max size limit of %d", MonitorConfig.FOLDER_SIZE_MAXLIMIT);
			folderSize = archiveFolder(files, folderSize, stats);
		}
		stats.setCurrentFolderSize(folderSize);
	}
	

	private long archiveFolder(List<File> files, long currentSize, MonitorStats stats) throws IOException {
		files.sort((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
		files.stream().map(f -> f.getName()).forEach(System.out::println);
		for (File file: files) {
			currentSize = currentSize - file.length();
			Files.move(file.toPath(), new File(MonitorConfig.MONITOR_ARCHIVE, file.getName()).toPath());
			stats.addArchive(file.getName());
			if (currentSize < MonitorConfig.FOLDER_SIZE_MAXLIMIT) {
				break;
			}
		}
		return currentSize;
	}

	private void purgeExecutables(MonitorStats stats) throws IOException {
		Files.list(Paths.get(monitorFolder)).map(p -> p.toFile()).filter(f -> !isNonExecutableFile(f.getName())).forEach((f) -> {
			stats.addDeletion(f.getName());
			f.delete();
		});
	}
	
	private boolean isNonExecutableFile(String filename) {
		Optional<String> fileExtension = Optional.ofNullable(filename).filter(f -> f.contains(".")).map(f -> f.substring(f.lastIndexOf(".") + 1));
		return !fileExtension.filter(f -> exclusion.contains(f)).isPresent();
	}
	
	private void showMonitorStats(MonitorStats stats) {
		log("Current Folder Size %s", stats.getCurrentFolderSize());
		if (stats.getAchiveFileCount() > 0) {
			log("Number of files archived " + stats.getAchiveFileCount());
			log("List of files archived " + stats.getArchiveFiles());
		}
		
		if (stats.getDeletedFilesCount() > 0) {
			log("Number of files deleted " + stats.getDeletedFilesCount());
			log("List of files deleted " + stats.getDeletedFiles());
		}
	}

	private void log(String message, Object... args) {
		System.out.println(String.format(message, args));
	}


	private class MonitorStats {
		List<String> archiveFiles = new ArrayList<>();
		List<String> deleteFiles = new ArrayList<>();
		long currentFolderSize = 0l;

		public void addArchive(String file) {
			archiveFiles.add(file);
		}
		
		public void addDeletion(String file) {
			deleteFiles.add(file);
		}

		public void setCurrentFolderSize(long size) {
			currentFolderSize = size;
		}
		
		public String getArchiveFiles() {
			return String.join(",", archiveFiles);
		}
		
		public int getAchiveFileCount() {
			return archiveFiles.size();
		}

		public long getCurrentFolderSize() {
			return currentFolderSize;
		}
		
		public int getDeletedFilesCount () {
			return deleteFiles.size();
		}

		public String getDeletedFiles() {
			return String.join(",", deleteFiles);
		}
	}
}
