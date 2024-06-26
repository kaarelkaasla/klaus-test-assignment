import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  // Load env file based on mode
  const env = loadEnv(mode, process.cwd());

  return {
    plugins: [vue()],
    // Ensure the environment variables are loaded
    define: {
      'process.env': {
        VITE_APP_API_KEY: JSON.stringify(env.VITE_APP_API_KEY),
        VITE_APP_LOCAL_API_BASE_URL: JSON.stringify(
          env.VITE_APP_LOCAL_API_BASE_URL,
        ),
        VITE_APP_SKAFFOLD_API_BASE_URL: JSON.stringify(
          env.VITE_APP_SKAFFOLD_API_BASE_URL,
        ),
      },
    },
    build: {
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: true,
        },
        format: {
          comments: false,
        },
      },
    },
  };
});
