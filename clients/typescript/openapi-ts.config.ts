import { defineConfig } from '@hey-api/openapi-ts'

export default defineConfig({
  input: '../../client-spec/openapi/auth-api.json',
  output: {
    path: 'src/generated',
  },
  plugins: [
    '@hey-api/client-fetch',
    '@hey-api/typescript',
    'zod',
    {
      name: '@hey-api/sdk',
      validator: true,
    },
  ],
})
