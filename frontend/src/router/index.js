import { createRouter, createWebHistory } from 'vue-router'
import Yards from '../views/Yards.vue'
import Materials from '../views/Materials.vue'
import Movements from '../views/Movements.vue'
import Inspections from '../views/Inspections.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/yards' },
    { path: '/yards', name: 'yard', component: Yards, meta: { title: '堆场分区' } },
    { path: '/materials', name: 'material', component: Materials, meta: { title: '材料台账' } },
    { path: '/movements', name: 'movement', component: Movements, meta: { title: '进出场流水' } },
    { path: '/inspections', name: 'inspection', component: Inspections, meta: { title: '安全巡检' } }
  ]
})

export default router
