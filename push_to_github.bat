@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo   SRE-Sentinel GitHub Setup and Push Script
echo ===================================================
echo.

:: 1. Check if git is installed
where git >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Git is not installed or not in your PATH.
    echo Please install Git from https://git-scm.com/ and try again.
    pause
    exit /b 1
)

:: 2. Initialize Git if needed
if not exist .git (
    echo [INFO] Initializing new Git repository...
    git init
    if %errorlevel% neq 0 (
        echo [ERROR] Failed to initialize Git repository.
        pause
        exit /b 1
    )
) else (
    echo [INFO] Git repository already initialized.
)

:: 3. Configure branch name to main
git branch -M main

:: 4. Get Remote Repository URL
echo.
echo Please create a new repository on GitHub (https://github.com/new).
echo Do NOT initialize it with a README, .gitignore, or license on GitHub.
echo.
set /p REPO_URL="Enter your GitHub repository URL (e.g., https://github.com/username/repo-name.git): "

if "%REPO_URL%"=="" (
    echo [ERROR] Repository URL cannot be empty.
    pause
    exit /b 1
)

:: 5. Add or Update Remote
git remote remove origin >nul 2>nul
git remote add origin %REPO_URL%
if %errorlevel% neq 0 (
    echo [ERROR] Failed to add remote origin. Please check the URL.
    pause
    exit /b 1
)

:: 6. Stage all files
echo [INFO] Staging files...
git add .
if %errorlevel% neq 0 (
    echo [ERROR] Failed to stage files.
    pause
    exit /b 1
)

:: 7. Commit files
echo [INFO] Committing files...
git commit -m "Initial commit: SRE-Sentinel project"
if %errorlevel% neq 0 (
    echo [WARNING] Nothing to commit or commit failed. (Maybe already committed?)
)

:: 8. Push to GitHub
echo [INFO] Pushing to GitHub (main branch)...
echo.
echo Note: If this is your first time pushing, a GitHub authentication prompt may appear.
echo.
git push -u origin main
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Failed to push to GitHub.
    echo Please check if:
    echo  1. The repository URL is correct.
    echo  2. You have write permissions to the repository.
    echo  3. You are authenticated with Git (e.g. via GitHub CLI, SSH key, or personal access token).
    pause
    exit /b 1
)

echo.
echo ===================================================
echo [SUCCESS] Project successfully pushed to GitHub!
echo ===================================================
echo.
pause
