<template>
  <div class="layout">
    <nav class="rail">
      <div class="brand">建筑工地</div>
      <div class="caption">日常台账</div>
      <router-link v-for="m in menu" :key="m.path" :to="m.path"
                   class="entry" :class="{ active: $route.path === m.path }">
        <i class="pip"></i>{{ m.label }}
      </router-link>
    </nav>
    <section class="stage">
      <router-view />
    </section>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      menu: [
        { path: '/yards', label: '堆场分区' },
        { path: '/materials', label: '材料台账' },
        { path: '/reservations', label: '浇筑预扣' },
        { path: '/movements', label: '进出场流水' },
        { path: '/inspections', label: '安全巡检' }
      ]
    }
  }
}
</script>

<style>
html, body { margin: 0; padding: 0; }
body { background: #f7f8f5; font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif; }
.layout { display: flex; min-height: 100vh; }
.rail { width: 196px; flex: 0 0 196px; background: #ffffff; border-right: 1px solid #ececec;
  padding: 22px 0; }
.brand { font-size: 17px; font-weight: 700; color: var(--el-color-primary-dark-2); padding: 0 20px 4px; }
.caption { font-size: 11px; letter-spacing: 2px; color: #c2c2c2; padding: 0 20px 20px; }
.entry { display: flex; align-items: center; gap: 9px; padding: 11px 20px; font-size: 14px;
  color: #666; text-decoration: none; border-left: 3px solid transparent; }
.entry:hover { background: #fafbf9; color: var(--el-color-primary-dark-2); }
.entry.active { background: var(--el-color-primary-light-9); color: var(--el-color-primary-dark-2);
  border-left-color: var(--el-color-primary); font-weight: 600; }
.pip { width: 5px; height: 5px; border-radius: 50%; background: #dcdcdc; }
.entry.active .pip { background: var(--el-color-primary); }
.stage { flex: 1; padding: 24px 28px; min-width: 0; }
</style>
