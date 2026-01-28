module.exports = {
    apps: [{
        name: 'converso-backend',
        script: 'dist/index.js',
        instances: 'max',
        exec_mode: 'cluster',
        env_production: {
            NODE_ENV: 'production',
            PORT: 5000
        }
    }]
};
