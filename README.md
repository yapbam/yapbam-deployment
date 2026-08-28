# Yapbam Deployment

A Swing application that deploys Yapbam updates to SourceForge.

It uploads the build artifacts (zip, exe, updater.jar), the release notes,
the PAD files, and the auto-update info files to the SourceForge project
hosting via SFTP.

## Usage

Run `net.yapbam.deployment.YapbamDeployer` (the main class) and fill in the
form with:

- The SourceForge credentials (user / password).
- The path to the Yapbam source folder (the one containing the built zip, exe,
  and `updater.jar`).
- The new version number and the previous version number.
- Whether to deploy to the **beta** channel only.

## What gets deployed

### Release deployment (`onlyBeta = false`)

| Action | Destination |
|--------|-------------|
| Create `updateXXX/` folder | `WEB_ROOT/updateXXX/` |
| Copy `yapbam-XXX.zip` | `WEB_ROOT/updateXXX/` |
| Copy `updater.jar` | `WEB_ROOT/updateXXX/` |
| Upload `updateInfoInclude.txt` | `WEB_ROOT/` (release channel) |
| Upload `updateInfoBetaInclude.txt` | `WEB_ROOT/` (beta channel) |
| Copy `yapbam-XXX.zip` | `RELEASE_ROOT/yapbam/` (SourceForge Files) |
| Copy `yapbam-XXX.exe` | `RELEASE_ROOT/yapbam/` (SourceForge Files) |
| Copy `yapbam-XXX.exe` | `WEB_ROOT/directDownload/` |
| Copy release notes (en) | `WEB_ROOT/en/doc/` |
| Copy release notes (fr) | `WEB_ROOT/fr/doc/` |
| Upload `pad_file.xml` | `WEB_ROOT/` |
| Delete old `updateOldVersion/` folder | `WEB_ROOT/updateOldVersion/` |

### Beta-only deployment (`onlyBeta = true`)

Only the minimum needed for the beta auto-update channel is deployed:

| Action | Destination |
|--------|-------------|
| Create `updateXXX/` folder | `WEB_ROOT/updateXXX/` |
| Copy `yapbam-XXX.zip` | `WEB_ROOT/updateXXX/` |
| Copy `updater.jar` | `WEB_ROOT/updateXXX/` |
| Upload `updateInfoBetaInclude.txt` | `WEB_ROOT/` (beta channel only) |
| Delete old `updateOldVersion/` folder | `WEB_ROOT/updateOldVersion/` |

The following are **not** done in beta mode:

- No upload of `updateInfoInclude.txt` (the release channel is untouched).
- No copy to SourceForge Files (`RELEASE_ROOT/yapbam/`).
- No copy of the exe to `directDownload/`.
- No copy of release notes.
- No PAD file updates.

This allows publishing a beta for testing without affecting the release
channel or the public download pages.

> **Note**: The old `updateOldVersion/` folder is deleted even in beta mode.
> Make sure `oldVersion` refers to the previous beta (not the current release)
> to avoid breaking the release auto-update channel.

## How it works

`DeployYapbam.doIt()` is the entry point:

1. `doAutoUpdate()` — always executed. Creates the `updateXXX/` folder, copies
   the zip and updater.jar, generates and uploads the `updateInfoInclude.txt`
   (release channel, unless beta-only) and `updateInfoBetaInclude.txt` (beta
   channel, always). Also deletes the previous version's update folder.
2. `doRelease()` — skipped in beta mode. Copies the zip and exe to the
   SourceForge Files section and to `directDownload/`.
3. `doDoc()` — skipped in beta mode. Copies the release notes (English and
   French).
4. `doPad()` — skipped in beta mode. Generates and uploads the PAD file
   (`pad_file.xml`, an XML template with version, date, and file size
   placeholders).

The `updateInfoInclude.txt` / `updateInfoBetaInclude.txt` files are generated
by `buildUpdateInfo()` and contain the properties read by Yapbam's
`UpdateInformation` class (version, URLs, checksums, sizes). See
[yapbam/UPDATE_PROCESS.md](../yapbam/UPDATE_PROCESS.md) for the full format.

## Build

```
mvn package
```

Java 8 source/target. Depends on `ajlib`, `commons-vfs2`, `jsch`, and
`yapbam-commons`.
