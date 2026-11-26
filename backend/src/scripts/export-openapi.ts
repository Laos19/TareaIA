/** Exporta la especificación OpenAPI a docs/api/openapi.json (sin conectarse a la BD). */
import { writeFileSync, mkdirSync } from 'node:fs';
import { buildApp } from '../app.js';
import { loadConfig } from '../config.js';

const app = await buildApp(loadConfig({ DATABASE_URL: 'postgres://x@localhost/x', JWT_SECRET: 'x'.repeat(40), NODE_ENV: 'test' }));
await app.ready();
mkdirSync('../docs/api', { recursive: true });
writeFileSync('../docs/api/openapi.json', JSON.stringify(app.swagger(), null, 2));
await app.close();
console.log('OpenAPI exportado en docs/api/openapi.json');
