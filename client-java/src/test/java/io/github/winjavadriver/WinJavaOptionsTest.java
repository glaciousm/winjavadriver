package io.github.winjavadriver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WinJavaOptions — verifies all capability setters/getters,
 * including the remote credential fields.
 */
class WinJavaOptionsTest {

    @Test
    @DisplayName("Default options should set browserName and platformName")
    void defaultsShouldSetBrowserAndPlatform() {
        var options = new WinJavaOptions();

        assertThat(options.getCapability("browserName")).isEqualTo("winjavadriver");
        assertThat(options.getCapability("platformName").toString()).isEqualToIgnoringCase("windows");
    }

    @Test
    @DisplayName("setApp should set winjavadriver:app capability")
    void setAppShouldSetCapability() {
        var options = new WinJavaOptions().setApp("calc.exe");

        assertThat(options.getApp()).isEqualTo("calc.exe");
        assertThat(options.getCapability("winjavadriver:app")).isEqualTo("calc.exe");
    }

    @Test
    @DisplayName("setAppArguments should set winjavadriver:appArguments capability")
    void setAppArgumentsShouldSetCapability() {
        var options = new WinJavaOptions().setAppArguments("--verbose");

        assertThat(options.getAppArguments()).isEqualTo("--verbose");
    }

    @Test
    @DisplayName("setAppTopLevelWindow should set winjavadriver:appTopLevelWindow capability")
    void setAppTopLevelWindowShouldSetCapability() {
        var options = new WinJavaOptions().setAppTopLevelWindow("0x1A2B3C");

        assertThat(options.getAppTopLevelWindow()).isEqualTo("0x1A2B3C");
    }

    @Test
    @DisplayName("setWaitForAppLaunch should set timeout")
    void setWaitForAppLaunchShouldSetTimeout() {
        var options = new WinJavaOptions().setWaitForAppLaunch(10);

        assertThat(options.getWaitForAppLaunch()).isEqualTo(10);
    }

    @Test
    @DisplayName("Default waitForAppLaunch should be 5")
    void defaultWaitForAppLaunchShouldBeFive() {
        var options = new WinJavaOptions();

        assertThat(options.getWaitForAppLaunch()).isEqualTo(5);
    }

    @Test
    @DisplayName("setShouldCloseApp should set close flag")
    void setShouldCloseAppShouldSetFlag() {
        var options = new WinJavaOptions().setShouldCloseApp(false);

        assertThat(options.isShouldCloseApp()).isFalse();
    }

    @Test
    @DisplayName("setRemoteUser should set winjavadriver:remoteUser capability")
    void setRemoteUserShouldSetCapability() {
        var options = new WinJavaOptions().setRemoteUser("testuser");

        assertThat(options.getRemoteUser()).isEqualTo("testuser");
        assertThat(options.getCapability("winjavadriver:remoteUser")).isEqualTo("testuser");
    }

    @Test
    @DisplayName("setRemotePassword should set winjavadriver:remotePassword capability")
    void setRemotePasswordShouldSetCapability() {
        var options = new WinJavaOptions().setRemotePassword("secret123");

        assertThat(options.getRemotePassword()).isEqualTo("secret123");
        assertThat(options.getCapability("winjavadriver:remotePassword")).isEqualTo("secret123");
    }

    @Test
    @DisplayName("setRemoteDomain should set winjavadriver:remoteDomain capability")
    void setRemoteDomainShouldSetCapability() {
        var options = new WinJavaOptions().setRemoteDomain("CORP");

        assertThat(options.getRemoteDomain()).isEqualTo("CORP");
        assertThat(options.getCapability("winjavadriver:remoteDomain")).isEqualTo("CORP");
    }

    @Test
    @DisplayName("Remote credentials should be null when not set")
    void remoteCredentialsShouldBeNullWhenNotSet() {
        var options = new WinJavaOptions();

        assertThat(options.getRemoteUser()).isNull();
        assertThat(options.getRemotePassword()).isNull();
        assertThat(options.getRemoteDomain()).isNull();
    }

    @Test
    @DisplayName("Fluent API should allow chaining all options")
    void fluentApiShouldAllowChaining() {
        var options = new WinJavaOptions()
                .setApp("myapp.exe")
                .setAppArguments("--flag")
                .setAppWorkingDir("C:\\apps")
                .setWaitForAppLaunch(10)
                .setShouldCloseApp(false)
                .setRemoteUser("admin")
                .setRemotePassword("pass")
                .setRemoteDomain("DOMAIN");

        assertThat(options.getApp()).isEqualTo("myapp.exe");
        assertThat(options.getAppArguments()).isEqualTo("--flag");
        assertThat(options.getAppWorkingDir()).isEqualTo("C:\\apps");
        assertThat(options.getWaitForAppLaunch()).isEqualTo(10);
        assertThat(options.isShouldCloseApp()).isFalse();
        assertThat(options.getRemoteUser()).isEqualTo("admin");
        assertThat(options.getRemotePassword()).isEqualTo("pass");
        assertThat(options.getRemoteDomain()).isEqualTo("DOMAIN");
    }

    @Test
    @DisplayName("merge should combine capabilities from two options")
    void mergeShouldCombineCapabilities() {
        var base_ = new WinJavaOptions().setApp("calc.exe");
        var extra = new WinJavaOptions().setRemoteUser("testuser").setRemoteDomain("CORP");

        var merged = base_.merge(extra);

        assertThat(merged.getApp()).isEqualTo("calc.exe");
        assertThat(merged.getRemoteUser()).isEqualTo("testuser");
        assertThat(merged.getRemoteDomain()).isEqualTo("CORP");
    }
}
