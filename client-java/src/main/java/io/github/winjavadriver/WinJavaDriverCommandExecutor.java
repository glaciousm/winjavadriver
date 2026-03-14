package io.github.winjavadriver;

import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.service.DriverCommandExecutor;
import org.openqa.selenium.remote.service.DriverService;

import java.net.URL;

/**
 * Custom command executors that register alternative find-element commands
 * to bypass Selenium's W3C codec strategy conversion.
 *
 * <p>Selenium's W3C codec converts "class name" and "name" locator strategies
 * to CSS selectors. The WinJavaDriver server expects raw strategy strings.
 * Custom command names mapping to the same endpoints bypass this conversion
 * because the codec only applies it to the standard findElement/findElements names.
 */
class WinJavaDriverCommandExecutor extends DriverCommandExecutor {

    static final String WIN_FIND_ELEMENT = "winjavadriver:findElement";
    static final String WIN_FIND_ELEMENTS = "winjavadriver:findElements";

    // Desktop screenshot command
    static final String WIN_DESKTOP_SCREENSHOT = "winjavadriver:desktopScreenshot";

    // MSFlexGrid extension commands
    static final String WIN_GRID_CELL = "winjavadriver:gridCell";
    static final String WIN_GRID_GET_VALUE = "winjavadriver:gridGetValue";
    static final String WIN_GRID_SET_VALUE = "winjavadriver:gridSetValue";
    static final String WIN_GRID_INFO = "winjavadriver:gridInfo";

    WinJavaDriverCommandExecutor(DriverService service) {
        super(service);
    }

    /**
     * Register custom find commands. Must be called after session creation
     * when the command codec is initialized.
     */
    void registerCustomFindCommands() {
        defineCommand(WIN_FIND_ELEMENT,
                new CommandInfo("/session/:sessionId/element", HttpMethod.POST));
        defineCommand(WIN_FIND_ELEMENTS,
                new CommandInfo("/session/:sessionId/elements", HttpMethod.POST));

        // Desktop screenshot command
        defineCommand(WIN_DESKTOP_SCREENSHOT,
                new CommandInfo("/session/:sessionId/winjavadriver/screenshot/desktop", HttpMethod.GET));

        // MSFlexGrid extension commands
        defineCommand(WIN_GRID_CELL,
                new CommandInfo("/session/:sessionId/winjavadriver/grid/:elementId/cell", HttpMethod.POST));
        defineCommand(WIN_GRID_GET_VALUE,
                new CommandInfo("/session/:sessionId/winjavadriver/grid/:elementId/cell/value", HttpMethod.POST));
        defineCommand(WIN_GRID_SET_VALUE,
                new CommandInfo("/session/:sessionId/winjavadriver/grid/:elementId/cell/value", HttpMethod.PUT));
        defineCommand(WIN_GRID_INFO,
                new CommandInfo("/session/:sessionId/winjavadriver/grid/:elementId/info", HttpMethod.GET));
    }

    /**
     * HTTP-only executor for URL-based connections (no service lifecycle).
     */
    static class ForUrl extends HttpCommandExecutor {

        ForUrl(URL url) {
            super(url);
        }

        void registerCustomFindCommands() {
            defineCommand(WIN_FIND_ELEMENT,
                    new CommandInfo("/session/:sessionId/element", HttpMethod.POST));
            defineCommand(WIN_FIND_ELEMENTS,
                    new CommandInfo("/session/:sessionId/elements", HttpMethod.POST));

            // Desktop screenshot command
            defineCommand(WIN_DESKTOP_SCREENSHOT,
                    new CommandInfo("/session/:sessionId/winjavadriver/screenshot/desktop", HttpMethod.GET));

            // MSFlexGrid extension commands
            defineCommand(WIN_GRID_CELL,
                    new CommandInfo("/session/:sessionId/winjavadriver/grid/:elementId/cell", HttpMethod.POST));
            defineCommand(WIN_GRID_GET_VALUE,
                    new CommandInfo("/session/:sessionId/winjavadriver/grid/:elementId/cell/value", HttpMethod.POST));
            defineCommand(WIN_GRID_SET_VALUE,
                    new CommandInfo("/session/:sessionId/winjavadriver/grid/:elementId/cell/value", HttpMethod.PUT));
            defineCommand(WIN_GRID_INFO,
                    new CommandInfo("/session/:sessionId/winjavadriver/grid/:elementId/info", HttpMethod.GET));
        }
    }
}
