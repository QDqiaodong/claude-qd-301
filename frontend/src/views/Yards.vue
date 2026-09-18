<template>
  <div class="wrap">
    <header class="bar">
      <div>
        <h2>堆场分区</h2>
        <p class="tip">下面那条占用量是按「堆在这个堆场里的材料结存合计」算出来的</p>
      </div>
      <button class="act-btn solid" @click="openCreate">登记新堆场</button>
    </header>

    <div class="yard-grid">
      <article class="yard-box" v-for="y in list" :key="y.id" :class="{ dim: y.state === '停用' }">
        <div class="box-top">
          <span class="code">{{ y.no }}</span>
          <span class="state">{{ y.state }}</span>
        </div>
        <h3>{{ y.title }}</h3>
        <div class="facts">
          <span>占地 {{ y.areaSize ?? '-' }} ㎡</span>
          <span>可堆 {{ y.maxLoad ?? '-' }}</span>
        </div>
        <div class="load-bar">
          <div class="load-fill" :style="{ width: loadPct(y) + '%' }"></div>
        </div>
        <div class="load-text">已堆 {{ loaded(y) }} / {{ y.maxLoad ?? '-' }}</div>
        <button class="act-btn ghost" @click="openEdit(y)">修改</button>
      </article>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '修改堆场' : '登记新堆场'" width="430px">
      <div class="form-row"><label>堆场编号</label><el-input v-model="form.no" /></div>
      <div class="form-row"><label>堆场名称</label><el-input v-model="form.title" /></div>
      <div class="form-row"><label>占地面积</label><el-input v-model="form.areaSize" /></div>
      <div class="form-row"><label>可堆量</label><el-input v-model="form.maxLoad" /></div>
      <div class="form-row"><label>状态</label><el-input v-model="form.state" placeholder="可用 / 停用" /></div>
      <template #footer>
        <el-button @click="dialog = false">先不填</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { yardApi, materialApi } from '../api'

export default {
  name: 'Yards',
  data() {
    return { list: [], materials: [], dialog: false, form: {} }
  },
  methods: {
    async load() {
      this.list = await yardApi.fetch()
      this.materials = await materialApi.fetch()
    },
    loaded(yard) {
      return this.materials
        .filter((m) => m.yardId === yard.id)
        .reduce((sum, m) => sum + (m.balance || 0), 0)
    },
    loadPct(yard) {
      if (!yard.maxLoad) return 0
      return Math.min(100, Math.round((this.loaded(yard) * 100) / yard.maxLoad))
    },
    openCreate() {
      this.form = { state: '可用' }
      this.dialog = true
    },
    openEdit(row) {
      this.form = { ...row }
      this.dialog = true
    },
    async submit() {
      try {
        if (this.form.id) {
          await yardApi.save(this.form.id, this.form)
        } else {
          await yardApi.add(this.form)
        }
        this.dialog = false
        await this.load()
        this.$message.success('保存好了')
      } catch (e) {
        this.$message.error(e.message)
      }
    }
  },
  mounted() {
    this.load()
  }
}
</script>

<style scoped>
.wrap { max-width: 1180px; }
.bar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.bar h2 { margin: 0; font-size: 20px; color: #2c2c2c; }
.tip { margin: 6px 0 0; font-size: 12px; color: #9b9b9b; }
.yard-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(256px, 1fr)); gap: 16px; }
.yard-box { background: #fff; border: 1px solid #ececec; border-radius: 12px; padding: 18px 18px 16px; }
.yard-box.dim { background: #fbfbfb; }
.box-top { display: flex; justify-content: space-between; align-items: center; }
.code { font-size: 12px; color: #a9a9a9; letter-spacing: .5px; }
.state { font-size: 12px; color: var(--el-color-primary-dark-2); background: var(--el-color-primary-light-9);
  border-radius: 10px; padding: 2px 10px; }
.yard-box h3 { margin: 10px 0 12px; font-size: 16px; font-weight: 600; }
.facts { display: flex; gap: 14px; font-size: 12px; color: #8a8a8a; margin-bottom: 12px; }
.load-bar { height: 9px; background: #f1f2ef; border-radius: 5px; overflow: hidden; }
.load-fill { height: 100%; background: var(--el-color-primary); border-radius: 5px; transition: width .3s; }
.load-text { font-size: 12px; color: #9b9b9b; margin: 8px 0 14px; }
.act-btn { border-radius: 6px; font-size: 13px; padding: 7px 16px; cursor: pointer; }
.act-btn.solid { background: var(--el-color-primary); color: #fff; border: none; }
.act-btn.ghost { background: #fff; color: var(--el-color-primary-dark-2);
  border: 1px solid var(--el-color-primary-light-7); }
.form-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.form-row label { width: 82px; text-align: right; font-size: 13px; color: #777; }
</style>
