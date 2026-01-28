# GitHub Actions Setup Guide

This guide explains how to set up automated APK builds using GitHub Actions for the Bloggo app.

## Overview

The GitHub Actions workflow automatically builds both **debug** and **release** APKs on every push to main/master/develop branches. The APKs are available as downloadable artifacts from the Actions tab.

## Workflow Features

- ✅ Builds on every push to main/master/develop branches
- ✅ Runs unit tests before building
- ✅ Builds debug APK (unsigned)
- ✅ Builds release APK (signed if keystore is configured)
- ✅ Uploads both APKs as artifacts (available for 30 days)
- ✅ Provides build summary with APK sizes
- ✅ Can be triggered manually from Actions tab

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

## Workflow Triggers

The workflow runs automatically on:
- Push to `main`, `master`, or `develop` branches
- Pull requests to `main` or `master`
- Manual trigger (click "Run workflow" in Actions tab)

## Downloading APKs from GitHub Actions

1. Go to **Actions** tab in your repository
2. Click on the workflow run you want
3. Scroll down to **Artifacts** section
4. Download:
   - `bloggo-debug-{commit-sha}` - Debug APK
   - `bloggo-release-{commit-sha}` - Release APK (signed if configured)

Artifacts are kept for 30 days.

## Troubleshooting

### Release APK is unsigned
- Check that all four GitHub secrets are set correctly
- Verify the base64 encoding didn't introduce line breaks
- Check the workflow logs for error messages

### Workflow fails with "permission denied"
- The workflow automatically makes gradlew executable
- If it still fails, check that gradlew is committed to the repository

### Build fails with Java version error
- The workflow uses JDK 17, which is compatible with this project
- If you need a different version, edit `.github/workflows/build-apk.yml`

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
