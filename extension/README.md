# Email Forensics - Gmail Chrome Extension

This Chrome extension helps you check if an email in Gmail is safe or dangerous.

When you open an email in Gmail, it adds an **Analyze Email** button. When you click it, the extension checks the email for phishing, fake links, dangerous text, and shows you a simple security report right inside Gmail.

---

## What It Does

- **Analyze Button in Gmail:** A blue button appears at the top when you open an email.
- **Safety Score:** Shows a score from 0 to 100 so you can quickly see if an email is safe or risky.
- **AI Summary:** Gives you a plain-English explanation of whether the email looks like a scam.
- **Flagged Problems:** Lists any suspicious links, fake sender names, urgent payment requests, or trick questions found in the email.
- **Link to Full Dashboard:** Lets you open the full investigation in your browser with one click.

---

## Simple File Structure

The extension has a clean and simple structure:

- `manifest.json`: Extension settings and permissions.
- `background.js`: Handles Google sign-in and talks to the backend server.
- `content.js`: Adds the button to Gmail and shows the side panel.
- `popup.html` & `popup.js`: The toolbar menu when you click the extension icon.
- `options/options.html` & `options/options.js`: The settings page to change server URLs and sign in with Google.
- `services/backend.js`: Functions to upload emails and get analysis results.
- `services/gmail.js`: Functions to get emails from Gmail.
- `icons/`: Extension icons (16px, 48px, 128px).

---

## Step-by-Step Setup Guide

### Step 1: Start the Backend Server

Make sure your backend server is running:

1. Open your terminal in the `backend` folder.
2. Run this command:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
3. Test that it is running by opening `http://localhost:8080/api/v1/health` in your browser. You should see `{"status":"ok"}`.

### Step 2: Start the Frontend Dashboard

1. Open your terminal in the `frontend` folder.
2. Run this command:
   ```bash
   npm run dev
   ```
3. Open `http://localhost:5173` in your browser to verify it is working.

### Step 3: Load the Extension into Chrome or Brave

1. Open your browser and go to `chrome://extensions` (or `brave://extensions`).
2. Turn ON **Developer mode** in the top right corner.
3. Click the **Load unpacked** button.
4. Select the `extension` folder inside this project.
5. The extension will now appear in your extensions list.

---

## How to Use It

1. Open [Gmail](https://mail.google.com/) in your browser.
2. Click and open any email.
3. You will see a blue **Analyze Email** button in the top action bar.
4. Click the button.
5. If it is your first time, a Google window will open asking for permission to read your email. Click **Allow**.
6. A side panel will slide in from the right showing:
   - The email subject and sender.
   - The Risk Score and Risk Level (LOW, MODERATE, HIGH, CRITICAL).
   - An AI explanation in simple words.
   - Any security problems detected.
7. Click the **View Full Details in Dashboard** button if you want to see even more information in the web app.

---

## Extension Settings

You can open Settings by right-clicking the extension icon and choosing **Options**, or clicking **Extension Settings** in the popup:

- **Backend URL:** The address of your Spring Boot server (default: `http://localhost:8080`).
- **Dashboard URL:** The address of your frontend web app (default: `http://localhost:5173`).
- **Google Sign-in:** Connect or disconnect your Google account.
- **Sign-in Method:** Set to **Web Sign-in (Recommended)**.

---

## Quick Troubleshooting

- **Backend shows Offline:**
  - Make sure your backend server is running in your terminal (`mvnw spring-boot:run`).
  - Open `http://localhost:8080/api/v1/health` in your browser to check if it works.

- **Google sign-in error (access_denied / 403):**
  - Go to Google Cloud Console > APIs & Services > OAuth consent screen.
  - Scroll down to **Test users** and add your Gmail email address.

- **The button does not show up in Gmail:**
  - The button only shows when you **open an email**, not when viewing your inbox list.
  - If you opened an email and don't see it, refresh the Gmail page (`F5`).
