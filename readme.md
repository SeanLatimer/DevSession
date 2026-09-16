# DevSession

A fork of [DevAuth](https://github.com/DJtheRedstoner/DevAuth).

Safely authenticate Minecraft accounts in development environments.

**Incompatible with [DevAuth](https://github.com/DJtheRedstoner/DevAuth)**: the two cannot be installed together,
and DevSession keeps its config and tokens in a separate directory
(`~/.devsession` instead of `~/.devauth`), so existing DevAuth logins do not carry over.

# Minecraft Version Support

| Versions                          | Module     | Supported |
|-----------------------------------|------------|:---------:|
| 1.20.1, 1.21.1, 26.1.x Fabric     | `fabric`   |     ✅     |
| 1.20.4, 1.21.1, 26.1.x NeoForge   | `neoforge` |     ✅     |

One jar per Minecraft version is published per loader (e.g.
`devsession-fabric-1.2.2+1.21.1.jar`), so pick the jar matching your game version.

**Note:** If a version isn't listed above as supported, just try it.
Additionally, the fabric module may work on other fabric-based loaders (such as legacy-fabric).

# Usage

Download a DevSession jar from the [releases](https://github.com/SeanLatimer/DevSession/releases),
place it in your mods folder and configure it using the configuration section below.

## Maven

DevSession is also published to [GitHub Packages](https://github.com/SeanLatimer/DevSession/packages),
one artifact per loader (`devsession-fabric` / `devsession-neoforge`). Note that GitHub Packages
requires authentication even for reading: each consumer needs a GitHub
[personal access token](https://github.com/settings/tokens) with the `read:packages` scope.

```gradle
repositories {
    maven {
        name = "DevSession"
        url = uri("https://maven.pkg.github.com/SeanLatimer/DevSession")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    localRuntime 'dev.silentsean.mod.devsession:devsession-fabric:0.1.0+1.21.1'
}
```

Set `gpr.user` / `gpr.key` in your `~/.gradle/gradle.properties`, or use the
`GITHUB_ACTOR` / `GITHUB_TOKEN` environment variables.

# Configuration

**DevSession defaults to disabled**, in order to be unobtrusive. You must enable DevSession in order for it to log you in.
A default configuration file is created on the first launch of a project using DevSession; edit it to enable DevSession
permanently, or enable it once via the options below.

DevSession is configured through environment variables, JVM properties and a configuration file,
in order of priority:

1. Environment variables, prefixed with `DEVSESSION_` (e.g. `DEVSESSION_ACCOUNT`),
   `UPPERCASE` with `_` instead of `.` or camelCase
2. JVM properties, prefixed with `devsession.` (e.g. `devsession.account`),
   which can be set by adding `-D<propertyName>=<value>` to your JVM arguments
   or by using [`System.setProperty`][setProperty] before DevSession is initialized 
   (Fabric's `preLaunch` entrypoint for example). Additionally, your specific
   toolchain/gradle plugins may have specific ways to configure JVM properties.
3. The configuration file

## Options

Every option below can be set through all three layers.

|             Option             | Description                                                             | Default      |
|:------------------------------:|:------------------------------------------------------------------------|:-------------|
|        `devsession.enabled`        | Enables DevSession                                                      | `false`      |
|       `devsession.configDir`       | Selects the config directory (environment variable and JVM property only) | [See below](#default-config-directory-locations) |
|         `devsession.account`       | Select the account to log into                                          | none         |
|       `devsession.tokenStorage`    | How tokens are stored: `auto`, `keyring` or `file` ([see security](#security)) | `auto`   |
|       `devsession.tokenCache`      | Which tokens are cached between launches: `all` or `refresh` ([see security](#security)) | `all` |
|    `devsession.forceTokenRefresh`  | Treat all stored tokens as expired and refresh them on the next launch  | `false`      |
|   `devsession.profileCacheMinutes` | How long cached profile information (uuid and name) stays valid, in minutes | `360`    |
|  `devsession.microsoft.grantFlow`  | Authentication flow: `browser` or `device-code` ([see below](#authentication-flows)) | `browser` |
| `devsession.microsoft.deviceCodeProvider` | OAuth client used for the device-code flow: `multimc`, `devlogin` or `prism` | `multimc` |
|     `devsession.microsoft.clientId` | Override the OAuth client id (advanced)                                 | none         |

## Configuration File

The configuration file is called `config.toml` and is located in your DevSession config
folder.

### Default config directory locations

|   OS    | Default config directory                                        |
|:-------:|-----------------------------------------------------------------|
| Windows | `C:\Users\<user>\.devsession`                                   |
|  MacOS  | `/Users/<user>/​.devsession`                                     |
|  Linux  | `$XDG_CONFIG_HOME/devsession`, defaulting to `~/.config/devsession` |

### Config file format

```toml
# Choose if DevSession should be enabled default. Overriden by the devsession.enabled property.
defaultEnabled = true

# Choose which account to use when devsession.account property is not specified
defaultAccount = "main"

# How authentication tokens are stored:
#   auto    - use the operating system credential store when one is available
#             (Windows Credential Manager, macOS Keychain, GNOME Keyring/KWallet),
#             otherwise fall back to microsoft_accounts.json with a warning
#   keyring - require an operating system credential store, fail without one
#   file    - always store tokens in microsoft_accounts.json
tokenStorage = "auto"

# Which tokens are cached between launches:
#   all     - every token of the login chain
#   refresh - only the refresh token, the rest is re-derived on each launch
tokenCache = "all"

# Treat all stored tokens as expired on the next launch
forceTokenRefresh = false

# How long cached profile information stays valid, in minutes
profileCacheMinutes = 360

[microsoft]
# How to authenticate with Microsoft: "browser" or "device-code"
grantFlow = "browser"

# Which approved OAuth client is used when grantFlow is "device-code"
deviceCodeProvider = "multimc"

# Override the OAuth client id (advanced)
#clientId = "00000000-0000-0000-0000-000000000000"

# A Microsoft account
# You do not need to put any credentials in the configuration file, as OAuth is used to sign in
[accounts.main]
type = "microsoft"

# A second account, which can be selected by changing defaultAccount above or using the devsession.account property
[accounts.alt]
type = "microsoft"
```
When the `devsession.account` property is specified it takes precedence over the
`defaultAccount` config option.

A default config will be automatically created on the first launch.

# Authentication flows

## Browser flow (default)

When logging in with a microsoft account for the first time, you will be given a
link to open in a browser to complete OAuth, after that the token will be stored.
Future logins will use and refresh the stored tokens as necessary. You will be prompted
to go through OAuth again once your refresh token expires (most likely to occur after a
long period without using DevSession) or is revoked.

## Device-code flow

Set `microsoft.grantFlow = "device-code"` (or `DEVSESSION_MICROSOFT_GRANT_FLOW=device-code`) to log in
without a locally hosted redirect. DevSession prints a code and a link
(`https://www.microsoft.com/link`); open it on any device, enter the code and approve.
This is useful for remote/headless development environments.

Because Microsoft requires an approved OAuth client for Minecraft sign-in, this flow
reuses the public client IDs of well-known open-source launchers (`multimc`, `devlogin`
or `prism`), which can be revoked or changed by their owners. A custom client id can be
set with `microsoft.clientId`.

# Security

DevSession caches authentication tokens in your **operating system credential store** when one is
available: Windows Credential Manager, macOS Keychain, GNOME Keyring or KWallet. One credential per
token is stored (`<account>:oauth`, `:xbl`, `:xsts`, `:session`), so launches need no network calls
until tokens expire. With `tokenCache = "refresh"` only the OAuth refresh token is cached and the
rest of the chain is re-derived on every launch.

When a token does not fit the credential store (some Windows credential blobs are capped at 2560
bytes, which the longest tokens can exceed), it is simply not cached: a warning is logged and the
token is re-derived from the refresh token when needed. A token is never written to plain text
because it is too large.

Cached profile information (uuid and name) stays in `microsoft_accounts.json` in plain text; it
contains no credentials. When no supported credential store is available (headless Linux servers,
containers, CI), DevSession falls back to storing tokens in `microsoft_accounts.json` and prints a
prominent warning on launch, or fails outright in `keyring` mode; `tokenStorage = "file"` always
uses the file and logs a warning. If a credential store was previously used, switching to `file`
mode recovers the refresh token from it automatically, and switching back re-uses the file's tokens.

Note that no storage here is bulletproof: in a development environment the game runs
with a standard JVM that is not packaged or sandboxed, so a credential store mainly
protects against the tokens sitting in an easily copied plain text file, not against
malware running in your user session. If you believe your tokens are compromised,
DevSession's permissions can be revoked [here][manageConsent].
Note that this does **not** immediately revoke all access tokens, due to design
decisions by Microsoft. See [here][tokenLifetimes] for more information.

# Credits

DevSession is a fork of [DevAuth](https://github.com/DJtheRedstoner/DevAuth).
All credit for the original idea, design, and implementation belongs to
[DJtheRedstoner](https://github.com/DJtheRedstoner); this fork exists only to
maintain the concept on newer Minecraft versions under a different name.
The original MIT license and copyright notice are preserved in
[LICENSE](LICENSE) and in every built jar.

The device-code flow reuses the public OAuth client IDs published by
[MultiMC](https://github.com/MultiMC/Launcher), [DevLogin](https://github.com/covers1624/DevLogin)
and [Prism Launcher](https://github.com/PrismLauncher/PrismLauncher).

OS credential store access uses the BSD-licensed
[java-keyring](https://github.com/javakeyring/java-keyring) library and its MIT/BSD/Apache-licensed
dependencies; all are bundled unmodified in the published jars.

[setProperty]: https://docs.oracle.com/en-us/java/javase/21/docs/api/java.base/java/lang/System.html#setProperty(java.lang.String,java.lang.String)
[manageConsent]: https://account.live.com/consent/Manage
[tokenLifetimes]: https://learn.microsoft.com/en-us/entra/identity-platform/configurable-token-lifetimes#access-tokens
