# GitHub Personal Access Token Setup Guide

To publish posts from HugoWriter to your GitHub repository, you need to create a Personal Access Token (PAT) with the appropriate permissions.

## Step-by-Step Instructions

### 1. Generate a Personal Access Token

1. Go to GitHub.com and sign in
2. Click your profile picture in the top right → **Settings**
3. Scroll down and click **Developer settings** (bottom of left sidebar)
4. Click **Personal access tokens** → **Tokens (classic)**
5. Click **Generate new token** → **Generate new token (classic)**

### 2. Configure Token Settings

1. **Note**: Give it a descriptive name like "HugoWriter App"
2. **Expiration**: Choose your preferred expiration (30 days, 60 days, 90 days, or No expiration)
3. **Select scopes**: Check the following permissions:
   - ✅ **repo** (Full control of private repositories)
     - This includes:
       - `repo:status`
       - `repo_deployment`
       - `public_repo`
       - `repo:invite`
       - `security_events`

### 3. Generate and Copy Token

1. Click **Generate token** at the bottom
2. **IMPORTANT**: Copy the token immediately - you won't be able to see it again!
3. The token will look like: `ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 4. Configure HugoWriter

1. Open HugoWriter app
2. Navigate to **Settings** tab (bottom navigation)
3. Enter the following information:
   - **Personal Access Token**: Paste the token you copied
   - **Repository Owner**: Your GitHub username or organization name
     - Example: If your repo is `github.com/johndoe/my-blog`, enter `johndoe`
   - **Repository Name**: The name of your repository
     - Example: If your repo is `github.com/johndoe/my-blog`, enter `my-blog`
   - **Branch**: The branch to publish to (usually `main` or `master`)
   - **Target Directory**: Directory where posts will be saved
     - Default: `content/posts/`
     - For Hugo blogs, this is typically `content/posts/` or `content/blog/`
4. Click **Save Settings**

## Required Permissions Explained

The `repo` scope gives HugoWriter permission to:
- Create and update files in your repository
- Commit changes
- Access repository contents

This is necessary because HugoWriter needs to:
1. Check if a file already exists (to update instead of create)
2. Create new markdown files in your repository
3. Update existing posts when you republish

## Security Notes

- ⚠️ **Never share your Personal Access Token** - treat it like a password
- ⚠️ The token provides full access to your repositories, so keep it secure
- ⚠️ The token is stored locally on your device in encrypted preferences
- 💡 Use token expiration and regenerate periodically for better security
- 💡 You can revoke the token anytime from GitHub Settings → Developer Settings

## Testing Your Configuration

1. Create a test post in HugoWriter
2. Add a title and some content
3. Click **Publish to GitHub**
4. If successful, you'll see "Published successfully!" message
5. Check your GitHub repository - the file should appear in the target directory

## Troubleshooting

### "GitHub configuration is incomplete"
- Ensure all fields in Settings are filled out
- Check that Repository Owner and Repository Name are correct

### "Failed to publish: 404"
- Repository doesn't exist or token doesn't have access
- Check Repository Owner and Repository Name spelling
- Verify the repository exists and you have access to it

### "Failed to publish: 401"
- Token is invalid or expired
- Generate a new token and update in Settings

### "Failed to publish: 403"
- Token doesn't have sufficient permissions
- Regenerate token with `repo` scope selected

## Example Configuration

For a Hugo blog hosted at `github.com/johndoe/my-blog`:

```
Personal Access Token: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
Repository Owner: johndoe
Repository Name: my-blog
Branch: main
Target Directory: content/posts/
```

When you publish a post titled "My First Post", it will be saved as:
`content/posts/My-First-Post.md` in your repository.
