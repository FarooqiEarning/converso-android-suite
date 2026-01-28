import server from './app.js';
import logger from './utils/logger.js';
import { workersService } from './services/workers.service.js';

const PORT = process.env.PORT || 5000;

server.listen(PORT, () => {
  logger.info(`🚀 Converso Backend running on http://localhost:${PORT}`);
  workersService.start();
});
