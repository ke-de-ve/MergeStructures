package community.redrover.merge.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

public class FileUtilsTest {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TestConfig {
        public String name;
        public int version;

        @Override
        public String toString() {
            return "TestConfig{name='" + name + "', version=" + version + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            TestConfig that = (TestConfig) o;
            return version == that.version && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + version;
            return result;
        }
    }

    private Path createTempFileWithContent(String prefix, String suffix, String content) throws IOException {
        Path tempFile = Files.createTempFile(prefix, suffix);
        Files.writeString(tempFile, content);

        return tempFile;
    }

    private void deleteTempFile(Path file) {
        assertDoesNotThrow(() -> Files.delete(file), "Failed to delete temp file: " + file);
    }

    private void deleteTempFileIfExists(String prefix, String suffix) throws IOException {
        try {
            Files.deleteIfExists(Path.of(prefix + suffix));
        } catch (IOException e) {
            throw new IOException("Failed to delete existing temp file", e);
        }
    }

    @Test
    void testEmptyFileThrowsIOException() throws IOException {
        Path emptyFile = createTempFileWithContent("empty_file", ".test", "");

        UncheckedIOException exception = assertThrows(
                UncheckedIOException.class, () -> FileUtils.loadFileToMap(emptyFile));

        assertTrue(exception.getMessage().contains("File is empty"));

        deleteTempFile(emptyFile);
    }

    @Test
    void testValidJsonFileReturnsExpectedMap() throws IOException {
        Path tempJsonFile = createTempFileWithContent("test", ".json", """
                {
                  "key1": "value1",
                  "key2": {
                    "nestedKey": "nestedValue"
                  }
                }
                """);

        Map<String, Object> result = FileUtils.loadFileToMap(tempJsonFile);

        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("nestedValue", ((Map<?, ?>) result.get("key2")).get("nestedKey"));

        deleteTempFile(tempJsonFile);
    }

    @Test
    void testValidYamlFileReturnsExpectedMap() throws IOException {
        Path tempYamlFile = createTempFileWithContent("test", ".yaml", """
                key1: value1
                key2:
                  nestedKey: nestedValue
                """);

        Map<String, Object> result = FileUtils.loadFileToMap(tempYamlFile);

        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("nestedValue", ((Map<?, ?>) result.get("key2")).get("nestedKey"));

        deleteTempFile(tempYamlFile);
    }

    @Test
    void testFileWithoutExtensionThrowsException() throws IOException {
        Path noExtTempFile = createTempFileWithContent("test", "", "key1: value1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> FileUtils.loadFileToMap(noExtTempFile));
        assertTrue(exception.getMessage().contains("File does not have a valid extension"));

        deleteTempFile(noExtTempFile);
    }

    @Test
    void testFileWithEndingDotThrowsException() throws IOException {
        Path endDotTempFile = createTempFileWithContent("test", ".", "key1: value1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> FileUtils.loadFileToMap(endDotTempFile));
        assertTrue(exception.getMessage().contains("File does not have a valid extension"));

        deleteTempFile(endDotTempFile);
    }

    @Test
    void testUnsupportedFileFormatThrowsException() throws IOException {
        Path tempTxtFile = createTempFileWithContent("test", ".txt", "key1: value1");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> FileUtils.loadFileToMap(tempTxtFile));
        assertTrue(exception.getMessage().contains("Unsupported file format"));

        deleteTempFile(tempTxtFile);
    }

    @Test
    void testNonExistentFileThrowsException() {
        Path nonExistentFilePath = Path.of("/path/to/nonexistent/file.json");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> FileUtils.loadFileToMap(nonExistentFilePath));
        assertTrue(exception.getMessage().contains("Nonexisting file path provided"));
    }

    @Test
    void testWriteJsonFileFromMap() throws IOException {
        Map<String, Object> data = Map.of(
                "key1", "value1",
                "key2", Map.of("nestedKey", "nestedValue")
        );

        Path tempJsonFile = Files.createTempFile("write_test", ".json");
        FileUtils.writeMapToFile(tempJsonFile, data);

        assertEquals(data, FileUtils.loadFileToMap(tempJsonFile));

        deleteTempFile(tempJsonFile);
    }

    @Test
    void testWriteYamlFileFromMap() throws IOException {
        Map<String, Object> data = Map.of(
                "key1", "value1",
                "key2", Map.of("nestedKey", "nestedValue")
        );

        Path tempYamlFile = Files.createTempFile("write_test", ".yaml");

        FileUtils.writeMapToFile(tempYamlFile, data);

        assertEquals(data, FileUtils.loadFileToMap(tempYamlFile));

        deleteTempFile(tempYamlFile);
    }

    @Test
    void testWriteUnsupportedFormatThrowsException() throws IOException {

        Path tempTxtFile = Files.createTempFile("write_test", ".txt");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                FileUtils.writeMapToFile(tempTxtFile, Map.of("key", "value")));

        assertTrue(ex.getMessage().contains("Unsupported file format"));

        deleteTempFile(tempTxtFile);
    }

    @Test
    void testLoadJsonFileToObject() throws IOException {
        Path tempYamlFile = createTempFileWithContent("test", ".json", """
                {
                  "name": "John",
                  "version": 1
                }
                """);

        final TestConfig testConfig = new TestConfig("John", 1);

        File yamlFile = new File(tempYamlFile.toString());

        TestConfig yamlConfig = FileUtils.loadFileToObject(yamlFile.getPath(), TestConfig.class);
        deleteTempFileIfExists("test", ".json");

        Assertions.assertNotNull(yamlConfig);
        Assertions.assertEquals(testConfig, yamlConfig);
    }


    @Test
    void testLoadYamlFileToObject() throws IOException {
        Path tempYamlFile = createTempFileWithContent("test", ".yaml", """
                name: "John"
                version: 1
                """);

        File yamlFile = new File(tempYamlFile.toString());
        TestConfig yamlConfig = FileUtils.loadFileToObject(yamlFile.getPath(), TestConfig.class);
        deleteTempFileIfExists("test", ".yaml");

        Assertions.assertNotNull(yamlConfig);
        Assertions.assertEquals(new TestConfig("John", 1), yamlConfig);
    }

    @Test
    void testLoadFileToObjectNegatives() throws IOException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtils.loadFileToObject("", TestConfig.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtils.loadFileToObject("Wrong_name", TestConfig.class));

        final Path wrongExtensionFile = createTempFileWithContent("wrong", ".ext", "dummy values");
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtils.loadFileToObject(wrongExtensionFile.toString(), TestConfig.class));
        deleteTempFileIfExists("wrong", ".ext");

        final Path emptyJsonFile = createTempFileWithContent("empty_file", ".json", "");
        Assertions.assertThrows(UncheckedIOException.class, () -> FileUtils.loadFileToObject(emptyJsonFile.toString(), TestConfig.class));
        deleteTempFileIfExists("empty_file", ".json");

        final Path emptyYamlFile1 = createTempFileWithContent("empty_file", ".yaml", "");
        Assertions.assertThrows(UncheckedIOException.class, () -> FileUtils.loadFileToObject(emptyYamlFile1.toString(), TestConfig.class));
        deleteTempFileIfExists("empty_file", ".yaml");

        final Path emptyYamlFile2 = createTempFileWithContent("empty_file", ".yml", "");
        Assertions.assertThrows(UncheckedIOException.class, () -> FileUtils.loadFileToObject(emptyYamlFile2.toString(), TestConfig.class));
        deleteTempFileIfExists("empty_file", ".yml");
    }
}
