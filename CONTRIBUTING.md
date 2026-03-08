# Contributing to WinJavaDriver

Thanks for your interest in contributing! This guide covers how to get started.

## What You Can Contribute

The public repo contains:
- **Java client** (`client-java/`) — extends Selenium's RemoteWebDriver
- **MCP server** (`mcp/`) — AI-driven desktop automation tools
- **Examples** (`examples/`) — Cucumber BDD test projects
- **Documentation** (`docs/`) — setup guides, troubleshooting
- **CI/CD** (`jenkins/`, `configs/`, `scripts/`) — Grid node setup, Jenkins pipelines

The server binary (`winjavadriver.exe`) is closed-source and distributed via [GitHub Releases](https://github.com/glaciousm/winjavadriver/releases).

## Getting Started

### Prerequisites

- Windows 10/11
- Java 21+
- Maven
- Node.js 18+ (for MCP server)
- `winjavadriver.exe` from [Releases](https://github.com/glaciousm/winjavadriver/releases)

### Build

```bash
# Java client
cd client-java
mvn clean install

# MCP server
cd mcp
npm install
npm run build
```

### Run Tests

```bash
# Unit tests (no winjavadriver.exe needed)
cd client-java
mvn test

# Integration tests (requires winjavadriver.exe on PATH + Windows desktop)
cd examples/calculator-tests
mvn test -Dcucumber.filter.tags="@Modern"
```

## How to Contribute

1. **Fork** the repository
2. **Create a branch** from `main` (`git checkout -b feature/my-feature`)
3. **Make your changes** and ensure they build
4. **Commit** with [Conventional Commits](https://www.conventionalcommits.org/) format:
   - `feat(client): add timeout configuration`
   - `fix(mcp): handle missing window handle`
   - `docs: update grid node setup guide`
5. **Push** and open a **Pull Request**

## Code Style

- **Java**: Standard Java conventions, camelCase methods/variables, PascalCase classes
- **TypeScript**: camelCase, async/await over callbacks
- **Commits**: Conventional Commits (`type(scope): description`)

## Reporting Bugs

Use the [Bug Report](https://github.com/glaciousm/winjavadriver/issues/new?template=bug_report.md) template. Include:
- Steps to reproduce
- Expected vs actual behavior
- WinJavaDriver version and Windows version
- Code sample if possible
- Verbose logs (`--verbose` flag) for server-side issues

## Questions?

Open a [Discussion](https://github.com/glaciousm/winjavadriver/discussions) or an issue.
