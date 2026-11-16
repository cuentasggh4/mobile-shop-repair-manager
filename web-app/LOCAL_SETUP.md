# Mobile Repair Shop POS — Local Laptop Setup (No Hosting Required)

Run this POS directly on your shop Laptop/PC at `http://localhost:3000` with full 2-way Google Sheets sync.

## 1. One-Time Prerequisites
1. Download and extract this project ZIP folder onto your Laptop (e.g. `D:\MobileRepairShopPOS`).
2. In **Google Cloud Console** -> **APIs & Services** -> **Credentials** -> your **Web application** OAuth 2.0 Client ID:
   - Under **Authorized JavaScript origins**, add:
     ```
     http://localhost:3000
     ```
   - Click **Save**.

## 2. One-Click Launch (Windows)
- Double-click **`start-pos.bat`** inside the project folder.
- On the first run, it automatically checks for **Node.js (LTS)** (and installs it via `winget` or the official Node.js `.msi` installer if missing), runs `npm install`, builds the production bundle (`npm run build`), starts the local server on `http://localhost:3000`, and opens your browser.
- On future runs, it launches immediately in ~1 second. Keep the terminal window minimized while the POS is open.

> **macOS / Linux:** Double-click `start-pos.command` (or run `chmod +x start-pos.command && ./start-pos.command`).

## 3. Connect Your Google Sheet
1. Click **Disconnected** / **Settings** in the top header.
2. Paste your **Web OAuth Client ID** and click **Save ID** (or create a `.env` file with `VITE_GOOGLE_CLIENT_ID=your-client-id` and run `npm run pos`).
3. Click **Connect & Sync Now** (or paste your existing Android `MobileRepairShop_DB` Sheet URL to link directly).

## 4. Optional: Pin as a Desktop App Icon
With `http://localhost:3000` open in Chrome or Microsoft Edge:
- Click the **Install** icon in the address bar (or Menu -> **Cast, save, and share** -> **Create shortcut** with **Open as window** checked).
