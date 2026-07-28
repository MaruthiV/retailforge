import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// route each api subpath to the service that owns it so the browser never hits CORS
const proxy = (target: string) => ({ target, changeOrigin: true });

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api/carts": proxy("http://localhost:8082"),
      "/api/transactions": proxy("http://localhost:8082"),
      "/api/loyalty": proxy("http://localhost:8083"),
      "/api/pricing": proxy("http://localhost:8081"),
      "/api/promotions": proxy("http://localhost:8081"),
      "/api/products": proxy("http://localhost:8081"),
      "/api/inventory": proxy("http://localhost:8084"),
    },
  },
});
