/**
 * RadioAuto ES - Servidor Mock Local de Emisoras
 * Servidor HTTP ligero en Node.js puro sin dependencias externas (npm)
 * Puerto de escucha estricto: 4112
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = 4112;
const DATA_FILE = path.join(__dirname, 'stations.json');
const DEFAULT_DATA_FILE = path.join(__dirname, 'default-stations.json');
const PUBLIC_DIR = path.join(__dirname, 'public');

// Mapeo de tipos MIME para archivos estáticos
const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon'
};

/**
 * Lee el archivo JSON de emisoras de forma segura
 * @returns {Array} Lista de emisoras en memoria
 */
function readStations() {
  try {
    if (!fs.existsSync(DATA_FILE)) {
      if (fs.existsSync(DEFAULT_DATA_FILE)) {
        fs.copyFileSync(DEFAULT_DATA_FILE, DATA_FILE);
      } else {
        return [];
      }
    }
    const raw = fs.readFileSync(DATA_FILE, 'utf-8');
    return JSON.parse(raw);
  } catch (error) {
    console.error('Error al leer stations.json:', error);
    return [];
  }
}

/**
 * Escribe la lista de emisoras actualizada en stations.json
 * @param {Array} stations - Lista completa de emisoras
 */
function writeStations(stations) {
  fs.writeFileSync(DATA_FILE, JSON.stringify(stations, null, 2), 'utf-8');
}

/**
 * Aplica cabeceras CORS a la respuesta HTTP para permitir peticiones desde cualquier origen
 * @param {http.ServerResponse} res - Objeto de respuesta HTTP
 */
function setCorsHeaders(res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
}

/**
 * Envía una respuesta en formato JSON
 * @param {http.ServerResponse} res - Objeto de respuesta
 * @param {number} statusCode - Código de estado HTTP
 * @param {Object|Array} data - Datos a serializar
 */
function sendJsonResponse(res, statusCode, data) {
  setCorsHeaders(res);
  res.writeHead(statusCode, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(data));
}

/**
 * Sirve archivos estáticos desde la carpeta public/
 * @param {string} pathname - Ruta del archivo solicitada
 * @param {http.ServerResponse} res - Objeto de respuesta HTTP
 */
function serveStaticFile(pathname, res) {
  let relativePath = pathname === '/' ? '/index.html' : pathname;
  let safePath = path.normalize(relativePath).replace(/^(\.\.[\/\\])+/, '');
  let filePath = path.join(PUBLIC_DIR, safePath);

  // Evitar salida del directorio public
  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('403 Prohibido');
    return;
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('404 Archivo no encontrado');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    setCorsHeaders(res);
    res.writeHead(200, { 'Content-Type': contentType });
    const stream = fs.createReadStream(filePath);
    stream.pipe(res);
  });
}

/**
 * Extrae y parsea el cuerpo de una petición HTTP en formato JSON
 * @param {http.IncomingMessage} req - Objeto de petición HTTP
 * @returns {Promise<Object>} Promesa con los datos parseados
 */
function parseRequestBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => {
      body += chunk.toString();
    });
    req.on('end', () => {
      try {
        if (!body) {
          resolve({});
        } else {
          resolve(JSON.parse(body));
        }
      } catch (err) {
        reject(err);
      }
    });
    req.on('error', reject);
  });
}

/**
 * Enrutador principal de peticiones del servidor
 * @param {http.IncomingMessage} req - Petición HTTP
 * @param {http.ServerResponse} res - Respuesta HTTP
 */
async function handleRequest(req, res) {
  const parsedUrl = url.parse(req.url, true);
  const { pathname } = parsedUrl;
  const method = req.method;

  // Manejo de preflight CORS (OPTIONS)
  if (method === 'OPTIONS') {
    setCorsHeaders(res);
    res.writeHead(204);
    res.end();
    return;
  }

  // API REST: GET /api/stations
  if (pathname === '/api/stations' && method === 'GET') {
    const stations = readStations();
    sendJsonResponse(res, 200, stations);
    return;
  }

  // API REST: POST /api/stations (Añadir emisora)
  if (pathname === '/api/stations' && method === 'POST') {
    try {
      const newStation = await parseRequestBody(req);
      if (!newStation.id || !newStation.name || !newStation.streamUrl) {
        sendJsonResponse(res, 400, { error: 'Campos requeridos: id, name, streamUrl' });
        return;
      }

      const stations = readStations();
      const exists = stations.some(s => s.id === newStation.id);
      if (exists) {
        sendJsonResponse(res, 409, { error: `Ya existe una emisora con el ID "${newStation.id}"` });
        return;
      }

      stations.push(newStation);
      writeStations(stations);
      sendJsonResponse(res, 201, { success: true, station: newStation });
    } catch (error) {
      sendJsonResponse(res, 400, { error: 'JSON inválido en el cuerpo de la petición' });
    }
    return;
  }

  // API REST: PUT /api/stations/:id (Editar emisora)
  if (pathname.startsWith('/api/stations/') && method === 'PUT') {
    const stationId = pathname.replace('/api/stations/', '');
    try {
      const updatedData = await parseRequestBody(req);
      const stations = readStations();
      const index = stations.findIndex(s => s.id === stationId);

      if (index === -1) {
        sendJsonResponse(res, 404, { error: `Emisora "${stationId}" no encontrada` });
        return;
      }

      stations[index] = { ...stations[index], ...updatedData, id: stationId };
      writeStations(stations);
      sendJsonResponse(res, 200, { success: true, station: stations[index] });
    } catch (error) {
      sendJsonResponse(res, 400, { error: 'Error procesando los datos enviados' });
    }
    return;
  }

  // API REST: DELETE /api/stations/:id (Eliminar emisora)
  if (pathname.startsWith('/api/stations/') && method === 'DELETE') {
    const stationId = pathname.replace('/api/stations/', '');
    const stations = readStations();
    const filtered = stations.filter(s => s.id !== stationId);

    if (filtered.length === stations.length) {
      sendJsonResponse(res, 404, { error: `Emisora "${stationId}" no encontrada` });
      return;
    }

    writeStations(filtered);
    sendJsonResponse(res, 200, { success: true, deletedId: stationId });
    return;
  }

  // API REST: POST /api/reset (Restaurar catálogo por defecto)
  if (pathname === '/api/reset' && method === 'POST') {
    try {
      if (fs.existsSync(DEFAULT_DATA_FILE)) {
        fs.copyFileSync(DEFAULT_DATA_FILE, DATA_FILE);
        const restored = readStations();
        sendJsonResponse(res, 200, { success: true, count: restored.length });
      } else {
        sendJsonResponse(res, 404, { error: 'Archivo de catálogo por defecto no disponible' });
      }
    } catch (error) {
      sendJsonResponse(res, 500, { error: 'Error al restaurar catálogo' });
    }
    return;
  }

  // Servir frontend web estático (HTML, CSS, JS, SVG logos)
  serveStaticFile(pathname, res);
}

// Inicialización del servidor HTTP
const server = http.createServer(handleRequest);

server.listen(PORT, '0.0.0.0', () => {
  console.log(`=======================================================`);
  console.log(` RadioAuto ES - Servidor Mock activo en puerto ${PORT}`);
  console.log(` Panel de pruebas web:  http://localhost:${PORT}/`);
  console.log(` Endpoint API JSON:    http://localhost:${PORT}/api/stations`);
  console.log(` Endpoint Android Auto: http://10.0.2.2:${PORT}/api/stations`);
  console.log(`=======================================================`);
});
