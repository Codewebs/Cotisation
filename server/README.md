# Cotisation Mock Server

Simple Node.js server to support the development of the Cotisation Android app.

## Setup

1. Make sure you have [Node.js](https://nodejs.org/) installed.
2. Navigate to this directory in your terminal.
3. Install dependencies:
   ```bash
   npm install
   ```

## Running the Server

Start the server with:
```bash
npm start
```
The server will be available at `http://localhost:3000`.

## Android App Configuration

To connect the Android app to this local server:
1. Find your computer's local IP address (e.g., `192.168.1.x`).
2. Open `app/src/main/java/com/indiza/cotisation/network/NetworkModule.kt`.
3. Change `BASE_URL` to `http://<YOUR_IP>:3000/`.
4. Ensure your Android device/emulator is on the same network.
