package io.github.winjavadriver;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.service.DriverService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages the lifecycle of the winjavadriver.exe process.
 * Extends Selenium's DriverService for standard integration.
 *
 * <p>Auto-discovery order:
 * <ol>
 *   <li>System property {@code webdriver.winjavadriver.driver}</li>
 *   <li>PATH lookup for {@code winjavadriver.exe}</li>
 *   <li>Known locations (relative publish dir, %LOCALAPPDATA%)</li>
 * </ol>
 */
public class WinJavaDriverService extends DriverService {

    public static final String WINJAVADRIVER_EXE = "winjavadriver.exe";
    public static final String WINJAVADRIVER_DRIVER_PROPERTY = "webdriver.winjavadriver.driver";
    public static final int DEFAULT_PORT = 9515;
    public static final String GITHUB_RELEASES_URL =
            "https://github.com/glaciousm/winjavadriver/releases";
    public static final String VERSION = "1.0.3";

    public WinJavaDriverService(
            File executable,
            int port,
            Duration timeout,
            List<String> args,
            Map<String, String> environment) throws IOException {
        super(executable, port, timeout, args, environment);
    }

    /**
     * Create a default service that auto-discovers the driver executable.
     */
    public static WinJavaDriverService createDefaultService() {
        return new Builder().build();
    }

    @Override
    protected Capabilities getDefaultDriverOptions() {
        return new WinJavaOptions();
    }

    /**
     * Builder for WinJavaDriverService.
     */
    public static class Builder extends DriverService.Builder<WinJavaDriverService, Builder> {

        private boolean verbose = false;

        /**
         * Enable verbose logging on the server.
         */
        public Builder withVerboseLogging(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        @Override
        public int score(Capabilities capabilities) {
            int score = 0;
            if (WinJavaOptions.BROWSER_NAME.equals(capabilities.getBrowserName())) {
                score++;
            }
            if ("windows".equalsIgnoreCase(String.valueOf(capabilities.getCapability("platformName")))) {
                score++;
            }
            return score;
        }

        @Override
        protected void loadSystemProperties() {
            if (exe == null) {
                exe = findDefaultExecutable();
            }
        }

        @Override
        protected List<String> createArgs() {
            List<String> args = new ArrayList<>();
            args.add("--port");
            args.add(String.valueOf(getPort()));
            if (verbose) {
                args.add("--verbose");
            }
            return Collections.unmodifiableList(args);
        }

        @Override
        protected WinJavaDriverService createDriverService(
                File exe,
                int port,
                Duration timeout,
                List<String> args,
                Map<String, String> environment) {
            try {
                return new WinJavaDriverService(exe, port, timeout, args, environment);
            } catch (IOException e) {
                throw new WebDriverException("Failed to create WinJavaDriverService", e);
            }
        }

        protected File findDefaultExecutable() {
            // 1. System property
            String sysProp = System.getProperty(WINJAVADRIVER_DRIVER_PROPERTY);
            if (sysProp != null) {
                File f = new File(sysProp);
                if (f.exists() && f.canExecute()) {
                    return f;
                }
            }

            // 2. PATH lookup
            String path = System.getenv("PATH");
            if (path != null) {
                for (String dir : path.split(File.pathSeparator)) {
                    File f = new File(dir, WINJAVADRIVER_EXE);
                    if (f.exists() && f.canExecute()) {
                        return f;
                    }
                }
            }

            // 3. Known locations
            String[] knownPaths = {
                "server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/publish/" + WINJAVADRIVER_EXE,
                "../../server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/publish/" + WINJAVADRIVER_EXE,
            };

            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                knownPaths = appendToArray(knownPaths,
                        localAppData + "\\WinJavaDriver\\" + WINJAVADRIVER_EXE);
            }

            for (String p : knownPaths) {
                File f = new File(p);
                if (f.exists() && f.canExecute()) {
                    return f;
                }
            }

            // 4. Auto-download from GitHub Releases
            File downloaded = downloadFromGitHubReleases();
            if (downloaded != null) {
                return downloaded;
            }

            throw new WebDriverException(
                    "Unable to find " + WINJAVADRIVER_EXE + ". Set the '"
                            + WINJAVADRIVER_DRIVER_PROPERTY + "' system property, "
                            + "place it on your PATH, or use usingDriverExecutable().");
        }

        private File downloadFromGitHubReleases() {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData == null) {
                return null;
            }

            Path installDir = Paths.get(localAppData, "WinJavaDriver");
            File exe = installDir.resolve(WINJAVADRIVER_EXE).toFile();

            // Already downloaded
            if (exe.exists() && exe.canExecute()) {
                return exe;
            }

            String zipName = "winjavadriver-v" + VERSION + "-win-x64.zip";
            String downloadUrl = GITHUB_RELEASES_URL + "/download/v" + VERSION + "/" + zipName;

            System.out.println("[WinJavaDriver] Downloading " + WINJAVADRIVER_EXE
                    + " v" + VERSION + " from GitHub Releases...");

            try {
                Files.createDirectories(installDir);
                Path zipPath = installDir.resolve(zipName);

                // Download zip — follow redirects (GitHub redirects to S3)
                downloadFile(downloadUrl, zipPath);

                // Extract
                extractZip(zipPath, installDir);

                // Clean up zip
                Files.deleteIfExists(zipPath);

                if (exe.exists()) {
                    System.out.println("[WinJavaDriver] Downloaded to " + exe.getAbsolutePath());
                    return exe;
                }
            } catch (Exception e) {
                System.err.println("[WinJavaDriver] Auto-download failed: " + e.getMessage());
            }

            return null;
        }

        private static void downloadFile(String urlStr, Path target) throws IOException {
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(120_000);

            // Handle GitHub's redirect chain (302 → S3)
            int status = conn.getResponseCode();
            if (status == 302 || status == 301) {
                String redirect = conn.getHeaderField("Location");
                conn.disconnect();
                conn = (HttpURLConnection) URI.create(redirect).toURL().openConnection();
                conn.setConnectTimeout(30_000);
                conn.setReadTimeout(120_000);
            }

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                conn.disconnect();
            }
        }

        private static void extractZip(Path zipPath, Path targetDir) throws IOException {
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    Path outPath = targetDir.resolve(entry.getName()).normalize();
                    // Security: prevent zip slip
                    if (!outPath.startsWith(targetDir)) {
                        throw new IOException("Zip entry outside target dir: " + entry.getName());
                    }
                    Files.createDirectories(outPath.getParent());
                    Files.copy(zis, outPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        private static String[] appendToArray(String[] array, String element) {
            String[] result = new String[array.length + 1];
            System.arraycopy(array, 0, result, 0, array.length);
            result[array.length] = element;
            return result;
        }
    }
}
