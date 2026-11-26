#!/usr/bin/env node
/**
 * Servidor MCP de TARE·IA (transporte stdio).
 *
 * Variables de entorno:
 * - TAREIA_API_URL     URL de la API (por defecto https://tareia-api.onrender.com)
 * - TAREIA_EMAIL / TAREIA_PASSWORD   cuenta con la que se abre la sesión (origen `mcp`)
 * - TAREIA_TOKEN       JWT ya emitido (alternativa a correo y contraseña)
 * - TAREIA_TELEMETRIA  "0" para no registrar el uso de las herramientas
 *
 * En stdio, stdout es el canal del protocolo: los mensajes de diagnóstico van a stderr.
 */
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { ClienteTareia } from './api.js';
import { crearServidor, VERSION } from './servidor.js';

const cliente = new ClienteTareia({
  baseUrl: process.env.TAREIA_API_URL || 'https://tareia-api.onrender.com',
  token: process.env.TAREIA_TOKEN || undefined,
  email: process.env.TAREIA_EMAIL || undefined,
  password: process.env.TAREIA_PASSWORD || undefined,
});
const server = crearServidor({ cliente, telemetria: process.env.TAREIA_TELEMETRIA !== '0' });
await server.connect(new StdioServerTransport());
console.error(`[tareia-mcp ${VERSION}] listo · API ${cliente.baseUrl}${cliente.configurado ? '' : ' · sin credenciales (solo estado_api)'}`);

const cerrar = async (): Promise<void> => {
  await server.close();
  process.exit(0);
};
process.on('SIGINT', () => void cerrar());
process.on('SIGTERM', () => void cerrar());
