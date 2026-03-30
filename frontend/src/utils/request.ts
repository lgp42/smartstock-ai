import axios, { type AxiosRequestConfig } from 'axios';

const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

service.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code === 200) {
      return res.data;
    } else {
      if (res.code === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
      return Promise.reject(new Error(res.message || 'Error'));
    }
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(
      new Error(error.response?.data?.message || error.message || '请求失败')
    );
  }
);

const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig) {
    return service.get<any, T>(url, config);
  },
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig) {
    return service.post<any, T>(url, data, config);
  },
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig) {
    return service.put<any, T>(url, data, config);
  },
  delete<T = any>(url: string, config?: AxiosRequestConfig) {
    return service.delete<any, T>(url, config);
  }
};

export default request;
