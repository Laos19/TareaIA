#!/usr/bin/env bash
# Cliente mínimo de la API de TARE·IA para la skill (alternativa al servidor MCP).
# Uso: tareia.sh salud | pendientes | registrar "<título>" [AAAA-MM-DD] | accion <id> <acción> | historial <id>
set -euo pipefail
API="${TAREIA_API_URL:-https://tareia-api.onrender.com}"
API="${API%/}"

token() {
  if [ -n "${TAREIA_TOKEN:-}" ]; then echo "$TAREIA_TOKEN"; return; fi
  : "${TAREIA_EMAIL:?Defina TAREIA_EMAIL y TAREIA_PASSWORD (o TAREIA_TOKEN)}"
  curl -fsS -X POST "$API/api/auth/login" -H 'content-type: application/json' \
    -d "$(node -e 'console.log(JSON.stringify({email:process.argv[1],password:process.argv[2],origen:"claude",dispositivo:"Claude Code (skill)"}))' "$TAREIA_EMAIL" "${TAREIA_PASSWORD:-}")" \
    | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>console.log(JSON.parse(s).token))'
}

case "${1:-}" in
  salud) curl -fsS "$API/api/health"; echo ;;
  pendientes)
    curl -fsS "$API/api/actividades/pendientes" -H "authorization: Bearer $(token)" \
      | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{for(const a of JSON.parse(s))console.log(`${a.urgencia.padEnd(9)} ${a.fechaLimite??"sin fecha"}  ${a.titulo}${a.estado==="PENDIENTE_VALIDAR"?"  (por validar)":""}  [${a.id}]`)})' ;;
  registrar)
    id=$(node -e 'console.log(crypto.randomUUID())')
    body=$(node -e 'console.log(JSON.stringify({titulo:process.argv[1],fechaLimite:process.argv[2]||null,origen:"IA"}))' "${2:?título}" "${3:-}")
    curl -fsS -X PUT "$API/api/actividades/$id" -H "authorization: Bearer $(token)" -H 'content-type: application/json' -d "$body"; echo ;;
  accion)
    curl -sS -X POST "$API/api/actividades/${2:?id}/estado" -H "authorization: Bearer $(token)" -H 'content-type: application/json' \
      -d "{\"accion\":\"${3:?confirmar|sincronizar|completar|reabrir}\"}"; echo ;;
  historial) curl -fsS "$API/api/actividades/${2:?id}/historial" -H "authorization: Bearer $(token)"; echo ;;
  *) sed -n '2,3p' "$0"; exit 2 ;;
esac
