# GitHub Actions Setup Guide

This guide explains how to set up automated APK builds and releases using GitHub Actions for the Bloggo app.

## Quick Start

1. **Push to main/master** → Automatic release created with patch version bump (v1.0.0 → v1.0.1)
2. **Go to Releases** → Download APKs from latest release Assets
3. **Want major/minor bump?** → Actions tab → Run workflow manually

## Overview

The GitHub Actions workflow automatically:
- **Builds** both debug and release APKs
- **Creates GitHub Releases** with semantic versioning
- **Attaches APKs** as downloadable assets to each release
- **Auto-increments** version numbers following semantic versioning

## Workflow Features

- ✅ Automatically creates releases on every push to main/master
- ✅ Follows semantic versioning (v1.0.0, v1.0.1, etc.)
- ✅ Runs unit tests before building
- ✅ Builds debug APK (unsigned)
- ✅ Builds release APK (signed if keystore is configured)
- ✅ Creates Git tags for each release
- ✅ Generates release notes from commits
- ✅ Uploads APKs as release assets (directly downloadable)
- ✅ Also uploads APKs as workflow artifacts
- ✅ Can be triggered manually with custom version bump (patch/minor/major)

## Setting Up Release APK Signing

To generate **signed release APKs**, you need to configure a keystore and add secrets to GitHub.

### Step 1: Generate a Keystore (if you don't have one)

Run this command in your terminal:

```bash
keytool -genkey -v -keystore bloggo-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias bloggo-key
```

You'll be prompted to enter:
- Keystore password
- Key password
- Your name, organization, location, etc.

**⚠️ IMPORTANT:** Save these passwords securely! You'll need them for GitHub secrets.

### Step 2: Convert Keystore to Base64

Convert your keystore file to base64 format:

**On macOS/Linux:**
```bash
base64 -i bloggo-release.jks -o keystore.txt
```

**On Windows (PowerShell):**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("bloggo-release.jks")) | Out-File keystore.txt
```

This creates a `keystore.txt` file containing the base64-encoded keystore.

### Step 3: Add Secrets to GitHub

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** and add these four secrets:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `KEYSTORE_BASE64` | Contents of `keystore.txt` | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Your keystore password | Password for the keystore file |
| `KEY_ALIAS` | `bloggo-key` (or your alias) | Alias name used when creating keystore |
| `KEY_PASSWORD` | Your key password | Password for the key alias |

### Step 4: Test the Workflow

1. Push any change to your repository
2. Go to **Actions** tab on GitHub
3. Click on the latest workflow run
4. Once completed, download the APKs from the **Artifacts** section

## Building Signed APKs Locally

If you want to build signed release APKs on your local machine:

1. Create a file called `keystore.properties` in the project root:

```properties
KEYSTORE_FILE=path/to/your/bloggo-release.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=bloggo-key
KEY_PASSWORD=your_key_password
```

2. Run the build command:

```bash
./gradlew assembleRelease
```

The signed APK will be at: `app/build/outputs/apk/release/app-release.apk`

**⚠️ Note:** The `keystore.properties` file is ignored by git and will not be committed.

## Building Debug APKs

Debug APKs don't require signing configuration. You can build them anytime:

```bash
./gradlew assembleDebug
```

The debug APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## How Semantic Versioning Works

Every push to `main` or `master` automatically creates a new release:

1. **Auto-increment (default)**: Patch version is incremented automatically
   - `v1.0.0` → `v1.0.1` → `v1.0.2`
   - Happens on every push to main/master

2. **Manual version bump**: Trigger workflow manually with custom bump type
   - Go to **Actions** → **Build and Release APKs** → **Run workflow**
   - Choose version bump:
     - **patch** (1.0.0 → 1.0.1) - Bug fixes, small changes
     - **minor** (1.0.0 → 1.1.0) - New features, backward compatible
     - **major** (1.0.0 → 2.0.0) - Breaking changes

3. **Version Code**: Auto-generated Unix timestamp (ensures uniqueness)

## Workflow Triggers

The workflow runs:
- ✅ Automatically on every push to `main` or `master` (patch version bump)
- ✅ Manually from Actions tab (choose version bump type)

## Downloading APKs

### From GitHub Releases (Recommended)

1. Go to your repository on GitHub
2. Click **Releases** (right sidebar or `/releases` in URL)
3. Click on the latest release (e.g., "Bloggo v1.0.1")
4. Scroll to **Assets** section
5. Download:
   - `bloggo-v1.0.1-release.apk` - **Recommended** (signed, for installation)
   - `bloggo-v1.0.1-debug.apk` - For testing/development

### From Workflow Artifacts (Alternative)

If you prefer, you can also download from the Actions tab:

1. Go to **Actions** tab in your repository
2. Click on the workflow run you want
3. Scroll down to **Artifacts** section
4. Download the APK artifacts

**Note**: Release assets are permanent (until you delete them), while artifacts expire after 30 days.

## Release Notes

Each release automatically includes:
- **What's New** - List of commits since last release
- **Installation Instructions** - Which APK to download
- **Requirements** - Minimum Android version
- **Build Information** - Version, commit hash, build date

## Managing Releases

### Deleting a Release

1. Go to **Releases** in your repository
2. Click on the release you want to delete
3. Click **Delete** (top right)
4. Confirm deletion

**Note**: This doesn't delete the Git tag. To delete the tag:
```bash
git tag -d v1.0.1
git push origin :refs/tags/v1.0.1
```

### Editing Release Notes

1. Go to **Releases** in your repository
2. Click on the release you want to edit
3. Click **Edit** (top right)
4. Update the description
5. Click **Update release**

## Troubleshooting

### Release APK is unsigned
- Check that all four GitHub secrets are set correctly
- Verify the base64 encoding didn't introduce line breaks
- Check the workflow logs for error messages

### "Tag already exists" error
- This means a release with that version already exists
- Either delete the existing tag/release or manually bump the version in `build.gradle.kts`
- To delete: Go to Releases → Delete release → Delete tag via git

### Workflow fails with "permission denied"
- The workflow automatically makes gradlew executable
- If it still fails, check that gradlew is committed to the repository

### Build fails with Java version error
- The workflow uses JDK 17, which is compatible with this project
- If you need a different version, edit `.github/workflows/build-apk.yml`

### Version not incrementing correctly
- Check that the version in `build.gradle.kts` follows semantic versioning (X.Y.Z)
- The workflow reads the current version and increments it
- If the version is malformed, the workflow will fail

## Security Best Practices

✅ **DO:**
- Keep your keystore passwords secret
- Use GitHub secrets for sensitive data
- Regularly backup your keystore file (store it securely offline)
- Use different keystores for debug and release builds

❌ **DON'T:**
- Commit keystore files to git (they're in .gitignore)
- Share your keystore passwords in plain text
- Use weak passwords for keystores
- Lose your release keystore (you can't update apps without it!)

## Version Management

To update the app version:

1. Edit `app/build.gradle.kts`
2. Update `versionCode` (increment by 1 for each release)
3. Update `versionName` (e.g., "1.0" → "1.1")

```kotlin
defaultConfig {
    applicationId = "com.rrajath.hugowriter"
    minSdk = 33
    targetSdk = 36
    versionCode = 2        // Increment this
    versionName = "1.1"    // Update this
    // ...
}
```

## Additional Resources

- [Android App Signing Documentation](https://developer.android.com/studio/publish/app-signing)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [GitHub Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
