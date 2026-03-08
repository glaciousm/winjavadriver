package io.github.winjavadriver;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.AbstractDriverOptions;

import java.util.Collections;
import java.util.Set;

/**
 * Options for configuring a WinJavaDriver session.
 * Extends Selenium's AbstractDriverOptions for seamless integration.
 *
 * <p>Capabilities are sent as flat vendor-prefixed keys in alwaysMatch:
 * {@code "winjavadriver:app"}, {@code "winjavadriver:appArguments"}, etc.
 */
public class WinJavaOptions extends AbstractDriverOptions<WinJavaOptions> {

    public static final String BROWSER_NAME = "winjavadriver";

    public WinJavaOptions() {
        setCapability("browserName", BROWSER_NAME);
        setCapability("platformName", "windows");
        setCapability("winjavadriver:waitForAppLaunch", 5);
        setCapability("winjavadriver:shouldCloseApp", true);
    }

    /**
     * Set the path to the application executable to launch.
     */
    public WinJavaOptions setApp(String app) {
        setCapability("winjavadriver:app", app);
        return this;
    }

    /**
     * Set command-line arguments for the application.
     */
    public WinJavaOptions setAppArguments(String args) {
        setCapability("winjavadriver:appArguments", args);
        return this;
    }

    /**
     * Set the working directory for the application.
     */
    public WinJavaOptions setAppWorkingDir(String dir) {
        setCapability("winjavadriver:appWorkingDir", dir);
        return this;
    }

    /**
     * Set the window handle to attach to (hex string like "0x1A2B3C").
     * Use this instead of app to attach to an already-running application.
     */
    public WinJavaOptions setAppTopLevelWindow(String handle) {
        setCapability("winjavadriver:appTopLevelWindow", handle);
        return this;
    }

    /**
     * Set seconds to wait for the app to launch and show its main window.
     * Default: 5 seconds.
     */
    public WinJavaOptions setWaitForAppLaunch(int seconds) {
        setCapability("winjavadriver:waitForAppLaunch", seconds);
        return this;
    }

    /**
     * Set whether to close the app when the session ends.
     * Default: true.
     */
    public WinJavaOptions setShouldCloseApp(boolean close) {
        setCapability("winjavadriver:shouldCloseApp", close);
        return this;
    }

    /**
     * Set the Windows username for remote session authentication.
     * Used when connecting to a WinJavaDriver Agent on a remote machine.
     */
    public WinJavaOptions setRemoteUser(String username) {
        setCapability("winjavadriver:remoteUser", username);
        return this;
    }

    /**
     * Set the Windows password for remote session authentication.
     * Used when connecting to a WinJavaDriver Agent on a remote machine.
     */
    public WinJavaOptions setRemotePassword(String password) {
        setCapability("winjavadriver:remotePassword", password);
        return this;
    }

    /**
     * Set the Windows domain for remote session authentication.
     * Used when connecting to a WinJavaDriver Agent on a remote machine.
     * If not set, the local machine name is used as domain.
     */
    public WinJavaOptions setRemoteDomain(String domain) {
        setCapability("winjavadriver:remoteDomain", domain);
        return this;
    }

    @Override
    protected Set<String> getExtraCapabilityNames() {
        return Collections.emptySet();
    }

    @Override
    protected Object getExtraCapability(String capabilityName) {
        return null;
    }

    @Override
    public WinJavaOptions merge(Capabilities extraCapabilities) {
        WinJavaOptions merged = new WinJavaOptions();
        this.asMap().forEach(merged::setCapability);
        if (extraCapabilities != null) {
            extraCapabilities.asMap().forEach(merged::setCapability);
        }
        return merged;
    }

    // Getters for internal use
    public String getApp() { return (String) getCapability("winjavadriver:app"); }
    public String getAppArguments() { return (String) getCapability("winjavadriver:appArguments"); }
    public String getAppWorkingDir() { return (String) getCapability("winjavadriver:appWorkingDir"); }
    public String getAppTopLevelWindow() { return (String) getCapability("winjavadriver:appTopLevelWindow"); }

    public int getWaitForAppLaunch() {
        Object val = getCapability("winjavadriver:waitForAppLaunch");
        return val instanceof Number ? ((Number) val).intValue() : 5;
    }

    public boolean isShouldCloseApp() {
        Object val = getCapability("winjavadriver:shouldCloseApp");
        return val instanceof Boolean ? (Boolean) val : true;
    }

    public String getRemoteUser() { return (String) getCapability("winjavadriver:remoteUser"); }
    public String getRemotePassword() { return (String) getCapability("winjavadriver:remotePassword"); }
    public String getRemoteDomain() { return (String) getCapability("winjavadriver:remoteDomain"); }
}
