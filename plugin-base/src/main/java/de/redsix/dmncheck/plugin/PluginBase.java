package de.redsix.dmncheck.plugin;

import de.redsix.dmncheck.result.Severity;
import de.redsix.dmncheck.result.ValidationResult;
import de.redsix.dmncheck.util.ValidatorLoader;
import de.redsix.dmncheck.validators.core.Validator;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public interface PluginBase {

    PrettyPrintValidationResults.PluginLogger getPluginLogger();

    List<String> getExcludeList();

    List<String> getSearchPathList();

    String[] getValidatorPackages();

    String[] getValidatorClasses();

    default boolean validate() {
        final List<Path> searchPathObjects = getSearchPathList().stream().map(Paths::get).collect(Collectors.toList());
        final List<File> filesToTest = fetchFilesToTestFromSearchPaths(searchPathObjects);

        return testFiles(filesToTest);
    }

    default boolean testFiles(final List<File> files) {
        boolean encounteredError = false;
        for (File file : files) {
            encounteredError |= testFile(file);
        }

        return encounteredError;
    }

    default boolean testFile(final File file) {
        boolean encounteredError = false;

        try {
            final DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(file);
            final List<ValidationResult> validationResults = runValidators(dmnModelInstance);

            if (!validationResults.isEmpty()) {
                PrettyPrintValidationResults.logPrettified(file, validationResults, getPluginLogger());
                encounteredError = validationResults.stream()
                                                    .anyMatch(result -> Severity.ERROR.equals(result.getSeverity()));
            }
        }
        catch (Exception e) {
            getPluginLogger().error.accept(e.getMessage());
            encounteredError = true;
        }

        return encounteredError;
    }

    default List<ValidationResult> runValidators(final DmnModelInstance dmnModelInstance) {
        return getValidators().stream()
                              .flatMap(validator -> validator.apply(dmnModelInstance).stream())
                              .collect(Collectors.toList());
    }

    default List<File> fetchFilesToTestFromSearchPaths(final List<Path> searchPaths) {
        final List<Path> fileNames = getFileNames(searchPaths);
        final List<File> files = fileNames.stream().map(Path::toFile).collect(Collectors.toList());
        return files.stream().filter(file -> {
            if (getExcludeList().contains(file.getName())) {
                getPluginLogger().info.accept("Skipped File: " + file);
                return false;
            } else {
                return true;
            }
        }).collect(Collectors.toList());
    }

    default List<Path> getFileNames(final List<Path> dirs) {
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.dmn");

        return dirs.stream().flatMap(dir -> {
            try {
                return Files.walk(dir)
                            .filter(Files::isRegularFile)
                            .filter(matcher::matches);
            } catch (IOException e) {
                throw new RuntimeException("Could not determine DMN files.", e);
            }
        }).collect(Collectors.toList());
    }

    default List<Validator> getValidators() {
        return ValidatorLoader.getValidators(getValidatorPackages(), getValidatorClasses());
    }
}
