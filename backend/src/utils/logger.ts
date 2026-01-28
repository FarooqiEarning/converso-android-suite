import pino from 'pino';

const loggerConfig: any = {
    level: process.env.LOG_LEVEL || 'info',
};

if (process.env.NODE_ENV === 'development') {
    loggerConfig.transport = {
        target: 'pino-pretty',
        options: {
            colorize: true,
            ignore: 'pid,hostname',
            translateTime: 'SYS:standard',
        },
    };
}

const logger = pino(loggerConfig);

export default logger;
