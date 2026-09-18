<template>
  <div class="wrap">
    <header class="bar">
      <div>
        <h2>安全巡检</h2>
        <p class="tip">不合格的先整改；分数没改上来，直接闭环是点不动的</p>
      </div>
      <button class="act-btn solid" @click="openCreate">登记巡检</button>
    </header>

    <div class="score-grid">
      <article class="score-card" v-for="r in list" :key="r.id"
               :class="r.verdict === '合格' ? 'card-pass' : 'card-fail'">
        <div class="score-num">{{ r.score }}</div>
        <div class="verdict">{{ r.verdict }}</div>
        <div class="line">{{ r.no }} · {{ yardName(r.yardId) }}</div>
        <div class="line">{{ r.inspectDate }} · {{ r.inspector }}</div>
        <div class="card-foot">
          <span class="chip" :class="{ done: r.state === '已闭环' }">{{ r.state }}</span>
          <button class="act-btn ghost" @click="openEdit(r)">修改</button>
        </div>
      </article>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '修改巡检' : '登记巡检'" width="440px">
      <div class="form-row"><label>巡检单号</label><el-input v-model="form.no" /></div>
      <div class="form-row">
        <label>巡检堆场</label>
        <el-select v-model="form.yardId" placeholder="选一个堆场" style="flex:1">
          <el-option v-for="y in yards" :key="y.id" :label="y.title" :value="y.id" />
        </el-select>
      </div>
      <div class="form-row"><label>巡检日期</label><el-input v-model="form.inspectDate" placeholder="2026-09-19" /></div>
      <div class="form-row"><label>巡检人</label><el-input v-model="form.inspector" /></div>
      <div class="form-row"><label>得分</label><el-input v-model="form.score" /></div>
      <div class="form-row"><label>判定</label><el-input v-model="form.verdict" placeholder="合格 / 不合格" /></div>
      <div class="form-row"><label>整改状态</label><el-input v-model="form.state" placeholder="待整改 / 已闭环" /></div>
      <template #footer>
        <el-button @click="dialog = false">先不填</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { inspectionApi, yardApi } from '../api'

export default {
  name: 'Inspections',
  data() {
    return { list: [], yards: [], dialog: false, form: {} }
  },
  methods: {
    async load() {
      this.list = await inspectionApi.fetch()
      this.yards = await yardApi.fetch()
    },
    yardName(id) {
      const y = this.yards.find((item) => item.id === id)
      return y ? y.title : '未知堆场'
    },
    openCreate() {
      this.form = { verdict: '合格' }
      this.dialog = true
    },
    openEdit(row) {
      this.form = { ...row }
      this.dialog = true
    },
    async submit() {
      try {
        if (this.form.id) {
          await inspectionApi.save(this.form.id, this.form)
        } else {
          await inspectionApi.add(this.form)
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
.bar h2 { margin: 0; font-size: 20px; }
.tip { margin: 6px 0 0; font-size: 12px; color: #9b9b9b; }
.score-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(204px, 1fr)); gap: 16px; }
.score-card { border-radius: 12px; padding: 20px 18px 16px; text-align: center; border: 1px solid #ececec;
  background: #fff; }
.card-pass { border-top: 4px solid var(--el-color-primary); }
.card-fail { border-top: 4px solid #d9534f; }
.score-num { font-size: 40px; font-weight: 800; line-height: 1; color: #3a3a3a; }
.card-pass .score-num { color: var(--el-color-primary-dark-2); }
.card-fail .score-num { color: #c0392b; }
.verdict { font-size: 13px; margin: 8px 0 14px; color: #8b8b8b; }
.line { font-size: 12px; color: #9b9b9b; line-height: 1.7; }
.card-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
.chip { font-size: 11px; border-radius: 10px; padding: 2px 10px; background: #fdecec; color: #c0392b; }
.chip.done { background: #eef7ec; color: #2f7a3f; }
.act-btn { border-radius: 6px; font-size: 12px; padding: 5px 12px; cursor: pointer; }
.act-btn.solid { background: var(--el-color-primary); color: #fff; border: none; padding: 7px 16px; font-size: 13px; }
.act-btn.ghost { background: #fff; color: var(--el-color-primary-dark-2);
  border: 1px solid var(--el-color-primary-light-7); }
.form-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.form-row label { width: 82px; text-align: right; font-size: 13px; color: #777; }
</style>
