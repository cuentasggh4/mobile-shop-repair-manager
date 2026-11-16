#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if ! command -v node >/dev/null 2>&1; then
  echo "[ERROR] Node.js is not installed. Please install Node.js LTS from https://nodejs.org/"
  read -r -p "Press Enter to exit..."
  exit 1
fi

if [ ! -f "node_modules/.bin/vite" ]; then
  echo "Installing dependencies for first-time setup..."
  npm install --legacy-peer-deps
fi

if [ ! -f "dist/index.html" ]; then
  echo "Building Mobile Repair Shop POS..."
  npm run build
fi

echo "Starting Mobile Repair Shop POS at http://localhost:3000"
if command -v open >/dev/null 2>&1; then
  (sleep 1 && open "http://localhost:3000") &
elif command -v xdg-open >/dev/null 2>&1; then
  (sleep 1 && xdg-open "http://localhost:3000") &
fi

npm run preview
