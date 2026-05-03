import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path"; // Changed from "node:path"
import { fileURLToPath } from "url"; // Added this import
export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            "@": path.resolve(path.dirname(fileURLToPath(import.meta.url)), "src"), // Replaced __dirname
        },
    },
    server: {
        port: 5173,
        proxy: {
            // Forward API + websocket calls to the Ktor backend in dev so the
            // frontend can use relative URLs without CORS friction.
            "/api": { target: "http://localhost:8000", changeOrigin: true, ws: true },
        },
    },
});
