package eu.openanalytics.phaedra.ui.cellprofiler.wizard;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;

import au.com.bytecode.opencsv.CSVReader;
import eu.openanalytics.phaedra.ui.cellprofiler.wizard.CellprofilerProtocolWizard.WizardState;

public class CellprofilerAnalyzer {

	public void analyzeFolder(WizardState state, IProgressMonitor monitor) throws InvocationTargetException {
		try {
			monitor.beginTask("Analyzing " + state.selectedFolder.getFileName(), IProgressMonitor.UNKNOWN);
			if (!Files.isDirectory(state.selectedFolder)) throw new IOException("Not a valid folder");
			
			List<Path> csvFiles = getChildren(state.selectedFolder, this::isCSVFile);
			if (csvFiles.isEmpty()) throw new IOException("No data files found in the folder");
			state.wellDataCandidates = csvFiles.stream().toArray(i -> new Path[i]);
			
			monitor.done();
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}
	
	public void analyzeWelldataFile(WizardState state, IProgressMonitor monitor) throws InvocationTargetException {
		try {
			monitor.beginTask("Analyzing " + state.selectedWellDataFile.getFileName(), IProgressMonitor.UNKNOWN);
			
			try (CSVReader reader = new CSVReader(new FileReader(state.selectedWellDataFile.toFile()))) {
				state.wellDataHeaders = reader.readNext();
				Arrays.sort(state.wellDataHeaders);
			}
			
			if (state.wellDataHeaders == null || state.wellDataHeaders.length == 0) {
				throw new IOException("No valid headers found in file");
			}
			
			monitor.done();
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}
	
	private List<Path> getChildren(Path path, Predicate<Path> condition) {
		try (Stream<Path> matches = Files.find(path, Integer.MAX_VALUE, (p, a) -> condition.test(p))) {
			return matches.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private boolean isCSVFile(Path path) {
		return Pattern.matches(".*\\.(csv|txt)", path.toFile().getName().toLowerCase());
	}
}
