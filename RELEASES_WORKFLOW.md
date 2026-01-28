# Releases Workflow - Quick Reference

## How It Works

```
Push to main/master
       ↓
Version auto-increment (1.0.0 → 1.0.1)
       ↓
Run tests
       ↓
Build APKs (debug + release)
       ↓
Create Git tag (v1.0.1)
       ↓
Create GitHub Release
       ↓
Attach APKs as assets
       ↓
✅ Done! Download from Releases page
```

## Semantic Versioning Examples

| Current Version | Bump Type | New Version | Use Case |
|----------------|-----------|-------------|----------|
| v1.0.0 | **patch** | v1.0.1 | Bug fixes, minor tweaks |
| v1.0.1 | **patch** | v1.0.2 | More bug fixes |
| v1.0.2 | **minor** | v1.1.0 | New features added |
| v1.1.0 | **major** | v2.0.0 | Breaking changes |

## Triggering Releases

### Automatic (Patch Bump)
```bash
git add .
git commit -m "fix: resolve sync issue"
git push origin main
```
→ Creates release `v1.0.1`

### Manual (Choose Bump Type)
1. Go to GitHub Actions tab
2. Click "Build and Release APKs"
3. Click "Run workflow"
4. Select bump type: patch/minor/major
5. Click "Run workflow"

## Downloading APKs

### Option 1: From Releases (Recommended)
```
Your Repo → Releases → Latest Release → Assets
  ↓
- bloggo-v1.0.1-release.apk (Install this)
- bloggo-v1.0.1-debug.apk (For testing)
```

### Option 2: From Actions Artifacts
```
Your Repo → Actions → Latest Workflow → Artifacts
  ↓
Download and extract zip file
```

## File Naming Convention

- **Release builds**: `bloggo-v{version}-release.apk`
  - Example: `bloggo-v1.2.3-release.apk`
  - Signed (if keystore configured)
  - Ready for distribution

- **Debug builds**: `bloggo-v{version}-debug.apk`
  - Example: `bloggo-v1.2.3-debug.apk`
  - Unsigned
  - For testing only

## Release Contents

Each release includes:
- ✅ Release APK (signed)
- ✅ Debug APK
- ✅ Release notes with commit history
- ✅ Build information (version code, date, commit)
- ✅ Installation instructions

## What Happens Automatically

1. ✅ Version number incremented in `build.gradle.kts`
2. ✅ Changes committed to repository
3. ✅ Git tag created (`v1.0.1`)
4. ✅ GitHub Release created
5. ✅ APKs built and attached
6. ✅ Release notes generated from commits

## Important Notes

⚠️ **Version in `build.gradle.kts` will be auto-updated**
- The workflow modifies `versionCode` and `versionName`
- This change is committed automatically
- Don't manually edit version numbers (unless you need to)

⚠️ **Each push to main/master creates a release**
- Use feature branches for development
- Only merge to main when ready to release
- Or disable auto-release and trigger manually only

⚠️ **Tags are permanent**
- Can't reuse a version number
- Delete tag first if you need to recreate a release

## Common Workflows

### Regular Development
```bash
# Work on feature branch
git checkout -b feature/new-feature
# ... make changes ...
git commit -m "feat: add new feature"
git push origin feature/new-feature

# Create PR and merge to main
# → Automatic release created with patch bump
```

### Preparing a Major Release
```bash
# Merge all features to main
git checkout main
git pull

# Manually trigger workflow with "major" bump
# → Creates v2.0.0 release
```

### Hotfix Release
```bash
# Create hotfix branch
git checkout -b hotfix/critical-bug
# ... fix bug ...
git commit -m "fix: critical security issue"

# Merge to main
git checkout main
git merge hotfix/critical-bug
git push

# → Automatic v1.0.1 release created
```

## Checking Release Status

### View Latest Release
```
https://github.com/YOUR-USERNAME/HugoWriter/releases/latest
```

### View All Releases
```
https://github.com/YOUR-USERNAME/HugoWriter/releases
```

### View Workflow Runs
```
https://github.com/YOUR-USERNAME/HugoWriter/actions
```

## Troubleshooting Quick Fixes

| Problem | Solution |
|---------|----------|
| Tag already exists | Delete tag: `git push origin :refs/tags/v1.0.1` |
| Release APK unsigned | Add GitHub secrets for keystore |
| Version not incrementing | Check version format in `build.gradle.kts` (must be X.Y.Z) |
| Workflow not running | Check you pushed to `main` or `master` branch |
| APKs not in release | Check workflow logs for build errors |

## Best Practices

✅ Use feature branches for development
✅ Merge to main only when ready to release
✅ Write descriptive commit messages (they become release notes)
✅ Test debug builds before merging to main
✅ Use patch bumps for fixes, minor for features, major for breaking changes
✅ Keep keystore secrets secure in GitHub Secrets
✅ Download releases from the Releases page, not Actions artifacts

## Resources

- Full setup guide: `GITHUB_ACTIONS_SETUP.md`
- Workflow file: `.github/workflows/build-apk.yml`
- Semantic Versioning: https://semver.org/
