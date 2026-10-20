import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  // No GitHub Pages o site fica em usuario.github.io/XPV/, não na raiz do
  // dominio - precisa desse prefixo pra achar os arquivos. No dev local
  // (npm run dev) a raiz continua "/" normalmente.
  base: command === 'build' ? '/XPV/' : '/',
  plugins: [react()],
}))
