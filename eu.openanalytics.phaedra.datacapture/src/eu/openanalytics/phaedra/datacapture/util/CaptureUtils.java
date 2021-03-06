package eu.openanalytics.phaedra.datacapture.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import au.com.bytecode.opencsv.CSVReader;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.config.CaptureConfig;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;

/**
 * A collection of utilities for data capture scripts.
 */
public class CaptureUtils {
	
	/**
	 * Sort readings by their ID.
	 */
	public final static Comparator<PlateReading> READING_ID_SORTER = new Comparator<PlateReading>() {
		@Override
		public int compare(PlateReading o1, PlateReading o2) {
			if (o1 == null) return -1;
			if (o2 == null) return 1;
			return (int)(o1.getId() - o2.getId());
		}
	};
	
	/**
	 * Sort readings by their creation date.
	 */
	public final static Comparator<PlateReading> READING_DATE_SORTER = new Comparator<PlateReading>() {
		@Override
		public int compare(PlateReading o1, PlateReading o2) {
			if (o1 == null) return -1;
			if (o2 == null) return 1;
			return o1.getDate().compareTo(o2.getDate());
		}
	};

	/**
	 * Create a CSV reader on the specified file.
	 * Keep in mind that the reader must be closed after use by calling reader.close().
	 * 
	 * @param path The path to the CSV file. 
	 * @return A CSV reader, ready for use.
	 * @throws FileNotFoundException If the file does not exist.
	 */
	public static CSVReader getCSVReader(String path) throws FileNotFoundException {
		return new CSVReader(new FileReader(path));
	}
	
	/**
	 * Create a CSV reader on the specified file.
	 * Keep in mind that the reader must be closed after use by calling reader.close().
	 * 
	 * @param path The path to the CSV file.
	 * @param columnSeparator The separator character, default a comma ','.
	 * @param quoteChar The string quote character, default a double quote '"'.
	 * @return A CSV reader, ready for use.
	 * @throws FileNotFoundException If the file does not exist.
	 */
	public static CSVReader getCSVReader(String path, char columnSeparator, char quoteChar) throws FileNotFoundException {
		return new CSVReader(new FileReader(path), columnSeparator, quoteChar);
	}
	
	/**
	 * Parse an Excel workbook (XLS or XLSX) from an InputStream.
	 * 
	 * @param input The InputStream containing the Excel workbook.
	 * @param fileName The name of the originating file, if known. May be null.
	 * @return The parsed workbook.
	 * @throws IOException If the data cannot be parsed.
	 */
	public static Workbook parseExcelWorkbook(InputStream input, String fileName) throws IOException {
		if (fileName == null) {
			// Try to parse as XLSX
			return new XSSFWorkbook(input);
		}
		
		boolean xlsx = FileUtils.getExtension(fileName).equalsIgnoreCase("xlsx");
		return xlsx ? new XSSFWorkbook(input) : new HSSFWorkbook(input);
	}
	
	/**
	 * Evaluate a value against a regular expression. If it matches,
	 * the specified group number is returned.
	 * 
	 * @param value The value to evaluate.
	 * @param regex The expression to evaluate against.
	 * @param group The group number to return, 0 to return the entire match.
	 * @return The matched substring of value.
	 */
	public static String getMatch(String value, String regex, int group) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(value);
		String match = null;
		if (matcher.matches()) {
			match = matcher.group(group);
		}
		return match;
	}
	
	/**
	 * Obtain the file under the specified parent path that matches the
	 * given regular expression.
	 * Both the parent path and the regex may contain variable references.
	 * 
	 * @param parentPath The parent folder to look in.
	 * @param regex The regular expression to match.
	 * @param config The configuration object holding parameters.
	 * @return The matching child's path, or null if no match is found.
	 */
	public static String getMatchingChild(String parentPath, String regex, DataCaptureContext ctx) {
		parentPath = resolveVars(parentPath, false, ctx);
		regex = resolveVars(regex, true, ctx);
		
		Pattern pattern = Pattern.compile(regex);
		File parentFile = new File(parentPath);
		File[] children = parentFile.listFiles();
		if (children == null) return null;
		for (File child: children) {
			Matcher matcher = pattern.matcher(child.getName());
			if (matcher.matches()) {
				return child.getAbsolutePath();
			}
		}
		return null;
	}
	
	/**
	 * Obtain the files/folders under the specified parent path that match
	 * the given regular expression.
	 * Both the parent path and the regex may contain variable references.
	 * 
	 * @param parentPath The parent folder to look in.
	 * @param regex The regular expression to match.
	 * @return The matching children paths.
	 */
	public static String[] getMatchingChildren(String parentPath, String regex, DataCaptureContext ctx) {
		parentPath = resolveVars(parentPath, false, ctx);
		regex = resolveVars(regex, true, ctx);
		
		Pattern pattern = Pattern.compile(regex);
		File parentFile = new File(parentPath);
		File[] children = parentFile.listFiles();
		if (children == null) return new String[0];
		List<String> matchingChildren = new ArrayList<String>();
		for (File child: children) {
			Matcher matcher = pattern.matcher(child.getName());
			if (matcher.matches()) {
				matchingChildren.add(child.getAbsolutePath());
			}
		}
		return matchingChildren.toArray(new String[matchingChildren.size()]);
	}
	
	/**
	 * Resolve a path.
	 * The path may be absolute - in which case it is returned as it is,
	 * or relative - in which case it is evaluated against the currentPath.
	 * A path may contain variables.
	 *  
	 * @param path The path that needs resolving.
	 * @param currentPath The currentPath, used by relative paths.
	 * @return The resolved path.
	 */
	public static String resolvePath(String path, String currentPath, DataCaptureContext ctx) {
		if (path == null || path.isEmpty()) return null;
		
		// "." points to the current path.
		if (path.equals(".")) return currentPath;

		// ".." points to the parent of the current path, which is assumed to exist.
		if (path.equals("..")) return new File(currentPath).getParentFile().getAbsolutePath();
		
		// First, resolve any variables in the path.
		path = resolveVars(path, false, ctx);
		
		// Get rid of backslash separators.
		if (currentPath.startsWith("\\\\")) {
			currentPath = "\\\\" + currentPath.substring(2).replace('\\', '/');
		} else {
			currentPath = currentPath.replace('\\', '/');
		}
		if (path.startsWith("\\\\")) {
			path = "\\\\" + path.substring(2).replace('\\', '/');
		} else {
			path = path.replace('\\', '/');
		}
		
		// Remove trailing slash, if any.
		if (currentPath.endsWith("/")) currentPath = currentPath.substring(0, currentPath.length()-1);

		String resolvedPath = null;
		if (path.startsWith("../")) {
			String parentPath = new File(currentPath).getParentFile().getAbsolutePath();
			resolvedPath = parentPath + path.substring(2);
		} else if (path.startsWith("./")) {
			resolvedPath = currentPath + path.substring(1);
		} else {
			// Assume an absolute path is given.
			resolvedPath = path;
		}
		
		return resolvedPath;
	}
	
	/**
	 * Resolve a variable in the given data capture context.
	 * 
	 * @param varName The  name of the variable to resolve.
	 * @param ctx The data capture context to look in.
	 * @return The resolved string value, or null if no match was found.
	 */
	public static String resolveVar(String varName, DataCaptureContext ctx) {
		Object value = VariableResolver.get(varName, ctx);
		return (value == null) ? null : value.toString();
	}
	
	/**
	 * Resolve all variable references in the given string against the given data capture context.
	 * 
	 * @param unresolvedValue The string to resolve.
	 * @param escapeValuesForRegex If the resolved value will be used as a regular expression pattern, set to true to escape any regex characters.
	 * @param ctx The data capture context to look in.
	 * @return The resolved string value.
	 */
	public static String resolveVars(String unresolvedValue, boolean escapeValuesForRegex, DataCaptureContext ctx) {
		return resolveVars(unresolvedValue, escapeValuesForRegex, ctx, null);
	}
	
	/**
	 * Resolve all variable references in the given string against the given data capture context.
	 * 
	 * @param unresolvedValue The string to resolve.
	 * @param escapeValuesForRegex If the resolved value will be used as a regular expression pattern, set to true to escape any regex characters.
	 * @param ctx The data capture context to look in.
	 * @param localVariables An optional map of variables to look in while resolving variables.
	 * @return The resolved string value.
	 */
	public static String resolveVars(String unresolvedValue, boolean escapeValuesForRegex, DataCaptureContext ctx, Map<String,String> localVariables) {
		if (unresolvedValue == null || unresolvedValue.isEmpty()) return unresolvedValue;
		
		char varStart = '$';
		char varStart2 = '{';
		char varEnd = '}';
		
		StringBuilder resolvedValue = new StringBuilder();
		int len = unresolvedValue.length();
		int position = 0;
		while (position < len) {
			char currentChar = unresolvedValue.charAt(position);
			
			boolean isVarStart = false;
			if (currentChar == varStart && position+1 < len) {
				char nextChar = unresolvedValue.charAt(position+1);
				if (nextChar == varStart2) {
					isVarStart = true;
				}
			}
			
			if (isVarStart) {
				// Consume & replace var
				int varStartPosition = position + 2;
				
				boolean hasVarEnd = true;
				int varEndPosition = varStartPosition;
				while (unresolvedValue.charAt(varEndPosition) != varEnd) {
					varEndPosition++;
					if (varEndPosition == len) {
						hasVarEnd = false;
						break;
					}
				}
				if (hasVarEnd) {
					String varName = unresolvedValue.substring(varStartPosition, varEndPosition);
					String varValue = null;
					if (localVariables != null) {
						varValue = localVariables.get(varName);
					}
					if (varValue == null) {
						Object varValueObject = VariableResolver.get(varName, ctx);
						if (varValueObject != null) varValue = varValueObject.toString();	
					}
					if (varValue == null) {
						varValue = "" + varStart + varStart2 + varName + varEnd;
					}
					
					//Fix: var value may contain reserved regex characters.
					if (escapeValuesForRegex) varValue = escapeRegexChars(varValue);
					
					resolvedValue.append(varValue);
					position = varEndPosition;
				} else {
					resolvedValue.append("" + varStart);
				}
			} else {
				resolvedValue.append(currentChar);
			}
			
			position++;
		}
		
		// Keep resolving until there are no more variables to resolve.
		// I.e. a variable might resolve into a String containing more variables.
		String resolvedString = resolvedValue.toString();
		String previousValue = unresolvedValue;
		while (!resolvedString.equals(previousValue)) {
			previousValue = resolvedString;
			resolvedString = resolveVars(resolvedString, escapeValuesForRegex, ctx);
		}
		
		return resolvedString;
	}
	
	/**
	 * Generate and throw a data capture exception with the given message.
	 * 
	 * @param message The message to include in the exception.
	 * @throws DataCaptureException The exception that will be generated and thrown.
	 */
	public static void doError(String message) throws DataCaptureException {
		throw new DataCaptureException(message);
	}
	
	/**
	 * Create a map containing a copy of all parameters of the given data capture module.
	 * 
	 * @param config The data capture module's configuration.
	 * @return A map containing all parameters from the module.
	 */
	public static Map<String,String> createParamMap(ModuleConfig config) {
		Map<String,String> params = new HashMap<String, String>();
		if (config != null) {
			String[] keys = config.getParameters().getParameterKeys();
			for (String key: keys) {
				Object value = config.getParameters().getParameter(key);
				if (value != null) params.put(key, value.toString());
			}
		}
		return params;
	}
	
	/**
	 * Create a new feature definition, to be used when a data capture job
	 * detects new features in the data source.
	 * 
	 * @param name The name of the new feature definition.
	 * @return A new feature definition.
	 */
	public static FeatureDefinition newFeatureDef(String name) {
		return new FeatureDefinition(name);
	}
	
	private static final String WELLDATA_IMPORTER_ID = "gather.welldata";
	private static final String SUBWELLDATA_IMPORTER_ID = "gather.subwelldata";
	private static final String IMAGEDATA_IMPORTER_ID = "gather.imagedata";
	
	public static String[] createModuleFilter(String captureConfigId, Boolean captureWellData, Boolean captureSubWellData, Boolean captureImageData) {
		try {
			CaptureConfig captureConfig = DataCaptureService.getInstance().getCaptureConfig(captureConfigId);
			
			String[] wellModules 	= getModules(WELLDATA_IMPORTER_ID, captureConfig);
			String[] subwellModules = getModules(SUBWELLDATA_IMPORTER_ID, captureConfig);
			String[] imageModules 	= getModules(IMAGEDATA_IMPORTER_ID, captureConfig);
			
			List<String> filter = new ArrayList<String>();
			ModuleConfig[] configs = captureConfig.getModuleConfigs();
			for (ModuleConfig cfg: configs) {
				String id = cfg.getId();

				// Skip module only if explicitly disabled. I.e. by default, execute everything.
				if (captureWellData != null && !captureWellData && CollectionUtils.find(wellModules, id) != -1) continue;
				if (captureSubWellData != null && !captureSubWellData && CollectionUtils.find(subwellModules, id) != -1) continue;
				if (captureImageData != null && !captureImageData && CollectionUtils.find(imageModules, id) != -1) continue;
				
				filter.add(id);
			}

			return filter.toArray(new String[filter.size()]);
		} catch (IOException e) {
			return null;
		} catch(DataCaptureException e) {
			throw new RuntimeException(e.getMessage(), e.getCause());
		}
	}
	
	private static String[] getModules(String baseId, CaptureConfig cfg) {
		if (baseId == null || baseId.isEmpty()) return new String[0];
		baseId = baseId.trim().toLowerCase();
		List<String> matchingIds = new ArrayList<String>();
		for (ModuleConfig mod: cfg.getModuleConfigs()) {
			String id = mod.getId();
			if (id.toLowerCase().startsWith(baseId)) matchingIds.add(id);
		}
		return matchingIds.toArray(new String[matchingIds.size()]);
	}
	
	private static String escapeRegexChars(String value) {
		char[] charsToReplace = new char[]{'(',')','.','-'};
		for (char c: charsToReplace) {
			value = value.replace(""+c, "\\"+c);
		}
		return value;
	}
}
