import axios from 'axios';

const API_BASE_URL = 'http://localhost:28080/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // 更新Content-Type以支持multipart/form-data
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      // 可以在这里添加跳转到登录页的逻辑
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

// 导出所有API
export { userApi } from './user';
export { postApi } from './post';
export { commentApi } from './comment';