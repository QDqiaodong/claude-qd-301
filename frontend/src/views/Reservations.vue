<template>
  <div class="wrap">
    <header class="bar">
      <div>
        <h2>浇筑预扣</h2>
        <p class="tip">开盘前先把料占住：只占可用余量、不动账面结存；开盘兑现才补出场流水，泵车坏了或下雨就作废还回余量</p>
      </div>
      <div class="head-side">
        <select v-model="stateFilter" class="filter-select">
          <option value="">全部状态</option>
          <option>占用中</option>
          <option>已兑现</option>
          <option>已作废</option>
        </select>
        <button class="act-btn solid" @click="openCreate">新开预扣</button>
      </div>
    </header>

    <table class="data-table">
      <thead>
        <tr>
          <th>预扣单号</th><th>堆场</th><th>材料</th><th class="right">预扣数量</th>
          <th>计划开盘日</th><th>状态</th><th>兑现流水</th><th class="right">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in shown" :key="r.id">
          <td class="mono">{{ r.no }}</td>
          <td>{{ yardName(r.yardId) }}</td>
          <td>{{ materialName(r.materialId) }}</td>
          <td class="right num">{{ r.amount }}</td>
          <td>{{ r.planDate }}</td>
          <td><span class="chip" :class="chipOf(r.state)">{{ r.state }}</span></td>
          <td class="mono">{{ r.movementNo || '—' }}</td>
          <td class="right ops">
            <template v-if="r.state === '占用中'">
              <button class="act-btn ghost" @click="openFulfill(r)">兑现</button>
              <button class="act-btn ghost" @click="openEdit(r)">改量</button>
              <button class="act-btn danger" @click="voidIt(r)">作废</button>
            </template>
            <span v-else class="mute">—</span>
          </td>
        </tr>
        <tr v-if="!shown.length"><td colspan="8" class="none">还没有预扣记录</td></tr>
      </tbody>
    </table>

    <el-dialog v-model="createDialog" title="新开浇筑预扣" width="480px">
      <div class="form-row"><label>预扣单号</label><el-input v-model="form.no" placeholder="PR-04" /></div>
      <div class="form-row">
        <label>从哪个堆场</label>
        <el-select v-model="form.yardId" placeholder="选一个堆场" style="flex:1" @change="form.materialId = null">
          <el-option v-for="y in yards" :key="y.id" :label="`${y.title}（${y.state}）`" :value="y.id" />
        </el-select>
      </div>
      <div class="form-row">
        <label>占哪批材料</label>
        <el-select v-model="form.materialId" placeholder="先选堆场，再挑材料" style="flex:1">
          <el-option v-for="m in yardMaterials" :key="m.id"
                     :label="`${m.no} ${m.title}（还可预扣 ${num(m.available)}）`" :value="m.id" />
        </el-select>
      </div>
      <p class="note" v-if="picked">
        账面结存 {{ num(picked.balance) }} · 占用中预扣 {{ num(picked.occupied) }} · 还可再预扣 {{ num(picked.available) }}
      </p>
      <div class="form-row"><label>预扣数量</label><el-input v-model="form.amount" /></div>
      <div class="form-row">
        <label>计划开盘日</label>
        <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD"
                        placeholder="选一天" style="flex:1" />
      </div>
      <p class="note">预扣只占余量、不写出场流水；堆场停用或挂着待整改的不合格巡检，开不了。</p>
      <template #footer>
        <el-button @click="createDialog = false">先不填</el-button>
        <el-button type="primary" @click="submitCreate">占住这批料</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" :title="`改预扣 ${editForm.no || ''}`" width="420px">
      <div class="form-row"><label>预扣数量</label><el-input v-model="editForm.amount" /></div>
      <div class="form-row">
        <label>计划开盘日</label>
        <el-date-picker v-model="editForm.planDate" type="date" value-format="YYYY-MM-DD" style="flex:1" />
      </div>
      <p class="note">数量改大要重新过堆场门禁和余量校验；改小随时行，余量当场还回。</p>
      <template #footer>
        <el-button @click="editDialog = false">先不改</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fulfillDialog" title="开盘兑现" width="460px">
      <p class="fulfill-sum" v-if="fulfillTarget">
        {{ materialName(fulfillTarget.materialId) }} · 出场 {{ fulfillTarget.amount }} · 数量跟着预扣走，改不了
      </p>
      <div class="form-row"><label>流水单号</label><el-input v-model="fulfillForm.movementNo" placeholder="MV-010" /></div>
      <div class="form-row">
        <label>出场日期</label>
        <el-date-picker v-model="fulfillForm.moveDate" type="date" value-format="YYYY-MM-DD" style="flex:1" />
      </div>
      <div class="form-row"><label>经办人</label><el-input v-model="fulfillForm.handler" /></div>
      <p class="note">兑现会补一笔等量出场流水；结存不够、堆场停用或单号撞车，整笔留在占用中。</p>
      <template #footer>
        <el-button @click="fulfillDialog = false">先不兑现</el-button>
        <el-button type="primary" @click="submitFulfill">兑现出场</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { materialApi, reservationApi, yardApi } from '../api'

export default {
  name: 'Reservations',
  data() {
    return {
      list: [],
      materials: [],
      yards: [],
      stateFilter: '',
      createDialog: false,
      editDialog: false,
      fulfillDialog: false,
      form: {},
      editForm: {},
      fulfillForm: {},
      fulfillTarget: null
    }
  },
  computed: {
    shown() {
      return this.stateFilter ? this.list.filter((r) => r.state === this.stateFilter) : this.list
    },
    yardMaterials() {
      return this.materials.filter((m) => m.yardId === this.form.yardId)
    },
    picked() {
      return this.materials.find((m) => m.id === this.form.materialId) || null
    }
  },
  methods: {
    async load() {
      const [rs, ms, ys] = await Promise.all([reservationApi.fetch(), materialApi.fetch(), yardApi.fetch()])
      this.list = rs
      this.materials = ms
      this.yards = ys
    },
    num(v) {
      return v == null ? 0 : v
    },
    yardName(id) {
      const y = this.yards.find((item) => item.id === id)
      return y ? y.title : '未知堆场'
    },
    materialName(id) {
      const m = this.materials.find((item) => item.id === id)
      return m ? `${m.no} ${m.title}` : '未知材料'
    },
    chipOf(state) {
      if (state === '占用中') return 'chip-hold'
      if (state === '已兑现') return 'chip-done'
      return 'chip-void'
    },
    openCreate() {
      this.form = {}
      this.createDialog = true
    },
    async submitCreate() {
      try {
        await reservationApi.add(this.form)
        this.createDialog = false
        await this.load()
        this.$message.success('占住了，余量已经压上')
      } catch (e) {
        this.$message.error(e.message)
      }
    },
    openEdit(row) {
      this.editForm = { id: row.id, no: row.no, amount: row.amount, planDate: row.planDate }
      this.editDialog = true
    },
    async submitEdit() {
      try {
        await reservationApi.save(this.editForm.id, { amount: this.editForm.amount, planDate: this.editForm.planDate })
        this.editDialog = false
        await this.load()
        this.$message.success('改好了')
      } catch (e) {
        this.$message.error(e.message)
      }
    },
    openFulfill(row) {
      this.fulfillTarget = row
      this.fulfillForm = { moveDate: row.planDate }
      this.fulfillDialog = true
    },
    async submitFulfill() {
      try {
        await reservationApi.fulfill(this.fulfillTarget.id, this.fulfillForm)
        this.fulfillDialog = false
        await this.load()
        this.$message.success('兑现了，出场流水已补上')
      } catch (e) {
        this.$message.error(e.message)
      }
    },
    async voidIt(row) {
      try {
        await this.$confirm(`作废 ${row.no}？占着的 ${row.amount} 会当场还回余量，作废后不能再兑现。`, '作废预扣', { type: 'warning' })
      } catch {
        return
      }
      try {
        await reservationApi.void(row.id)
        await this.load()
        this.$message.success('作废了，余量还回去了')
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
.head-side { display: flex; align-items: center; gap: 10px; }
.filter-select { border: 1px solid #e5e5e5; border-radius: 6px; padding: 7px 10px; font-size: 13px;
  background: #fff; color: #555; }
.data-table { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #ececec;
  border-radius: 10px; overflow: hidden; }
.data-table th { text-align: left; font-size: 12px; color: #8f8f8f; font-weight: 500;
  padding: 12px 14px; background: #fafbf9; }
.data-table td { padding: 12px 14px; font-size: 13px; border-top: 1px solid #f3f4f1; }
.data-table tr:hover td { background: #fcfdfb; }
.right { text-align: right; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #8f8f8f; }
.num { font-weight: 600; color: var(--el-color-primary-dark-2); }
.mute { color: #c4c4c4; }
.none { color: #c4c4c4; font-size: 12px; text-align: center; padding: 24px 0; }
.chip { font-size: 11px; border-radius: 10px; padding: 2px 10px; }
.chip-hold { background: #fdf3e3; color: #b07a1e; }
.chip-done { background: #eef7ec; color: #2f7a3f; }
.chip-void { background: #f3f3f3; color: #9b9b9b; }
.ops { white-space: nowrap; }
.ops .act-btn { margin-left: 6px; }
.act-btn { border-radius: 6px; font-size: 12px; padding: 5px 12px; cursor: pointer; }
.act-btn.solid { background: var(--el-color-primary); color: #fff; border: none; padding: 7px 16px; font-size: 13px; }
.act-btn.ghost { background: #fff; color: var(--el-color-primary-dark-2);
  border: 1px solid var(--el-color-primary-light-7); }
.act-btn.danger { background: #fff; color: #c0392b; border: 1px solid #f0c9c2; }
.form-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.form-row label { width: 82px; text-align: right; font-size: 13px; color: #777; flex: 0 0 82px; }
.note { font-size: 12px; color: #b0b0b0; margin: 4px 0 12px 92px; }
.fulfill-sum { margin: 0 0 14px; padding: 10px 12px; border-radius: 8px; background: #fafbf9;
  font-size: 13px; color: #555; }
</style>
