<template>
  <div class="wrap">
    <header class="bar">
      <div>
        <h2>进出场流水</h2>
        <p class="tip">结存是一笔一笔累出来的：进场加、出场减，出不掉比结存还多的量</p>
      </div>
      <div class="picker">
        <span>看哪批材料</span>
        <select v-model="currentId" @change="load">
          <option v-for="m in materials" :key="m.id" :value="m.id">{{ m.no }} {{ m.title }}</option>
        </select>
      </div>
    </header>

    <div class="balance-strip" v-if="current">
      <div class="b-item">
        <span class="b-label">当前结存</span>
        <strong class="b-value">{{ current.balance }}</strong>
      </div>
      <div class="b-item"><span class="b-label">进场合计</span><strong>{{ sumOf('进场') }}</strong></div>
      <div class="b-item"><span class="b-label">出场合计</span><strong>{{ sumOf('出场') }}</strong></div>
      <div class="b-item"><span class="b-label">材料状态</span><strong>{{ current.state }}</strong></div>
    </div>

    <div class="ledger">
      <section class="ledger-col">
        <h4 class="head-in">进场</h4>
        <div class="ledger-entry entry-in" v-for="r in incoming" :key="r.id">
          <span class="amt">+{{ r.amount }}</span>
          <span class="meta">{{ r.no }} · {{ r.moveDate }} · {{ r.handler }}</span>
        </div>
        <p class="none" v-if="!incoming.length">还没有进场记录</p>
      </section>
      <section class="ledger-col">
        <h4 class="head-out">出场</h4>
        <div class="ledger-entry entry-out" v-for="r in outgoing" :key="r.id">
          <span class="amt">-{{ r.amount }}</span>
          <span class="meta">{{ r.no }} · {{ r.moveDate }} · {{ r.handler }}</span>
        </div>
        <p class="none" v-if="!outgoing.length">还没有出场记录</p>
      </section>
    </div>

    <section class="recorder">
      <h4>记一笔</h4>
      <div class="rec-row">
        <label>方向</label>
        <div class="seg">
          <button :class="{ on: form.direction === '进场' }" @click="form.direction = '进场'">进场</button>
          <button :class="{ on: form.direction === '出场' }" @click="form.direction = '出场'">出场</button>
        </div>
      </div>
      <div class="rec-row"><label>流水单号</label><el-input v-model="form.no" placeholder="MV-009" /></div>
      <div class="rec-row"><label>数量</label><el-input v-model="form.amount" /></div>
      <div class="rec-row"><label>日期</label><el-input v-model="form.moveDate" placeholder="2026-09-19" /></div>
      <div class="rec-row"><label>经办人</label><el-input v-model="form.handler" /></div>
      <button class="act-btn solid" @click="submit">记一笔</button>
    </section>
  </div>
</template>

<script>
import { materialApi, movementApi } from '../api'

export default {
  name: 'Movements',
  data() {
    return {
      materials: [],
      currentId: null,
      records: [],
      form: { direction: '进场' }
    }
  },
  computed: {
    current() {
      return this.materials.find((m) => m.id === this.currentId) || null
    },
    incoming() {
      return this.records.filter((r) => r.direction === '进场')
    },
    outgoing() {
      return this.records.filter((r) => r.direction === '出场')
    }
  },
  methods: {
    async bootstrap() {
      this.materials = await materialApi.fetch()
      if (!this.currentId && this.materials.length) {
        this.currentId = this.materials[0].id
      }
      await this.load()
    },
    async load() {
      if (this.currentId) {
        this.records = await movementApi.fetch({ materialId: this.currentId })
      }
    },
    sumOf(direction) {
      return this.records
        .filter((r) => r.direction === direction)
        .reduce((total, r) => total + r.amount, 0)
    },
    async submit() {
      if (!this.currentId) {
        this.$message.warning('先选一批材料')
        return
      }
      try {
        await movementApi.add({ ...this.form, materialId: this.currentId })
        const keep = this.form.direction
        this.form = { direction: keep }
        await this.bootstrap()
        this.$message.success('记好了')
      } catch (e) {
        this.$message.error(e.message)
      }
    }
  },
  mounted() {
    this.bootstrap()
  }
}
</script>

<style scoped>
.wrap { max-width: 1180px; }
.bar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; }
.bar h2 { margin: 0; font-size: 20px; }
.tip { margin: 6px 0 0; font-size: 12px; color: #9b9b9b; }
.picker { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #777; }
.picker select { border: 1px solid #e5e5e5; border-radius: 6px; padding: 8px 10px; font-size: 13px;
  background: #fff; min-width: 200px; }
.balance-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1px; background: #ececec;
  border: 1px solid #ececec; border-radius: 10px; overflow: hidden; margin-bottom: 18px; }
.b-item { background: #fff; padding: 14px 16px; display: flex; flex-direction: column; gap: 5px; }
.b-label { font-size: 12px; color: #9b9b9b; }
.b-value { font-size: 24px; color: var(--el-color-primary-dark-2); }
.ledger { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 18px; }
.ledger-col { background: #fff; border: 1px solid #ececec; border-radius: 10px; padding: 14px 16px;
  min-height: 180px; }
.ledger-col h4 { margin: 0 0 12px; font-size: 14px; }
.head-in { color: #2f7a3f; }
.head-out { color: #b3593a; }
.ledger-entry { display: flex; align-items: center; gap: 12px; padding: 9px 10px; border-radius: 7px;
  margin-bottom: 6px; font-size: 13px; }
.entry-in { background: #f2f8f0; }
.entry-out { background: #fbf3ee; }
.amt { font-weight: 700; min-width: 54px; }
.entry-in .amt { color: #2f7a3f; }
.entry-out .amt { color: #b3593a; }
.meta { color: #8b8b8b; font-size: 12px; }
.none { color: #c4c4c4; font-size: 12px; text-align: center; padding: 24px 0; }
.recorder { background: #fff; border: 1px solid #ececec; border-radius: 10px; padding: 16px 18px; }
.recorder h4 { margin: 0 0 14px; font-size: 14px; }
.rec-row { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.rec-row label { width: 78px; text-align: right; font-size: 13px; color: #777; }
.seg { display: flex; gap: 8px; }
.seg button { border: 1px solid #e5e5e5; background: #fff; border-radius: 6px; padding: 6px 18px;
  font-size: 13px; cursor: pointer; color: #666; }
.seg button.on { background: var(--el-color-primary); color: #fff; border-color: var(--el-color-primary); }
.act-btn { border-radius: 6px; font-size: 13px; padding: 8px 20px; cursor: pointer; margin-left: 90px; }
.act-btn.solid { background: var(--el-color-primary); color: #fff; border: none; }
</style>
