import axios from 'axios'

const client = axios.create({ baseURL: '/api', timeout: 10000 })

client.interceptors.response.use(
  (resp) => resp.data,
  (error) => {
    const detail = error && error.response && error.response.data
    const text = (detail && detail.message) || error.message || '接口没通'
    return Promise.reject(new Error(text))
  }
)

/** 材料堆场 */
export const yardApi = {
  fetch: (params) => client.get('/yards', { params }),
  add: (payload) => client.post('/yards', payload),
  save: (id, payload) => client.put(`/yards/${id}`, payload)
}

/** 材料台账 */
export const materialApi = {
  fetch: (params) => client.get('/materials', { params }),
  add: (payload) => client.post('/materials', payload),
  save: (id, payload) => client.put(`/materials/${id}`, payload)
}

/** 进出场流水 */
export const movementApi = {
  fetch: (params) => client.get('/movements', { params }),
  add: (payload) => client.post('/movements', payload),
  save: (id, payload) => client.put(`/movements/${id}`, payload)
}

/** 浇筑配料预扣 */
export const reservationApi = {
  fetch: (params) => client.get('/reservations', { params }),
  add: (payload) => client.post('/reservations', payload),
  save: (id, payload) => client.put(`/reservations/${id}`, payload),
  fulfill: (id, payload) => client.post(`/reservations/${id}/fulfill`, payload),
  void: (id, payload) => client.post(`/reservations/${id}/void`, payload)
}

/** 安全巡检 */
export const inspectionApi = {
  fetch: (params) => client.get('/inspections', { params }),
  add: (payload) => client.post('/inspections', payload),
  save: (id, payload) => client.put(`/inspections/${id}`, payload)
}
