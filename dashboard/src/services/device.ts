import api from '../services/api';
const deviceService = {
    getDevices: () => api.get('/devices'),
    registerDevice: (data: any) => api.post('/devices/register', data)
};
export default deviceService;
