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

Download a DevSession jar from the [releases](https://github.com/SeanLatimer/DevAuth/releases),
place it in your mods folder and configure it using the configuration section below.

# Configuration

**DevSession defaults to disabled**, in order to be unobtrusive. You must enable DevSession in order for it to log you in.
Additionally, the configuration file will not be created if DevSession is disabled. You should enable DevSession once
via the JVM property, so that it creates the configuration file, then you may configure it via the file.

DevSession is configured through JVM properties and a configuration file.
JVM Properties can set be by adding `-D<propertyName>=<value>` to your JVM arguments
or by using [`System.setProperty`][setProperty] before DevSession is initialized 
(Fabric's `preLaunch` entrypoint for example). Additionally, your specific
toolchain/gradle plugins may have specific ways to configure JVM properties.

## JVM Properties

|        Property         | Description                    | Default                                          |
|:-----------------------:|:-------------------------------|:-------------------------------------------------|
|  `devsession.enabled`   | Enables DevSession             | `false`                                          |
| `devsession.configDir`  | Selects the config directory   | [See below](#default-config-directory-locations) |
|  `devsession.account`   | Select the account to log into | none                                             |

## Configuration File

The configuration file is called `config.toml` and is located in your DevSession config
folder.

### Default config directory locations

|   OS    | Default config directory                                        |
|:-------:|-----------------------------------------------------------------|
| Windows | `C:\Users\<user>\.devsession`                                   |
|  MacOS  | `/Users/<user>/.devsession`                                     |
|  Linux  | `$XDG_CONFIG_HOME/devsession`, defaulting to `~/.config/devsession` |

### Config file format

```toml
# Choose if DevSession should be enabled default. Overriden by the devsession.enabled property.
defaultEnabled = true

# Choose which account to use when devsession.account property is not specified
defaultAccount = "main"

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

A default config will be automatically created when DevSession is first enabled.

# How it works

When logging in with a microsoft account for the first time, you will be given a
link to open in a browser to complete OAuth, after that the token will be stored
in a file called `microsoft_accounts.json` in your config directory. Future logins
will use and refresh the stored tokens as necessary. You will be prompted to go through
OAuth again once your refresh token expires (most likely to occur after a long period
without using DevSession) or is revoked.

# Security

DevSession stores all credentials locally on your machine. The Microsoft account tokens are stored in
`microsoft_accounts.json` inside the DevSession configuration directory. The contents of this file are not
encrypted, so do not share it or open it when it may be seen. If you want to revoke DevSession's permissions
or believe this file may be compromised, DevSession's permissions can be revoked [here][manageConsent].
Note that this does **not** immediately revoke all access tokens, due to design decisions by Microsoft.
See [here][tokenLifetimes] for more information.

[setProperty]: https://docs.oracle.com/en-us/java/javase/21/docs/api/java.base/java/lang/System.html#setProperty(java.lang.String,java.lang.String)
[manageConsent]: https://account.live.com/consent/Manage
[tokenLifetimes]: https://learn.microsoft.com/en-us/entra/identity-platform/configurable-token-lifetimes#access-tokens
