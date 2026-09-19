<template>
  <div class="wrap">
    <header class="bar">
      <div>
        <h2>浇筑配料预扣</h2>
        <p class="tip">预扣只占住可再预扣余量，不动账面结存；开盘兑现才补出场流水，取消或泵车故障就作废、把余量还回去</p>
      </div>
      <button class="act-btn solid" @click="openCreate">新开预扣</button>
    </header>

    <table class="data-table">
      <thead>
        <tr>
          <th>预扣单号</th><th>材料</th><th>堆场</th><th class="right">预扣数量</th>
          <th>计划开盘日</th><th>经办人</th><th>状态</th><th>兑现流水 / 备注</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in list" :key="r.id">
          <td class="mono">{{ r.no }}</td>
          <td>{{ materialName(r.materialId) }}</td>
          <td>{{ yardName(r.yardId) }}</td>
          <td class="right num">{{ r.amount }}</td>
          <td>{{ r.planDate }}</td>
          <td>{{ r.handler }}</td>
          <td><span class="chip" :class="chipClass(r.state)">{{ r.state }}</span></td>
          <td class="mute">{{ tailText(r) }}</td>
          <td class="ops">
            <template v-if="r.state === '占用中'">
              <button class="act-btn ghost" @click="openEdit(r)">修改</button>
              <button class="act-btn solid" @click="openFulfill(r)">兑现</button>
              <button class="act-btn danger" @click="voidIt(r)">作废</button>
            </template>
            <span v-else class="mute">—</span>
          </td>
        </tr>
        <tr v-if="!list.length"><td colspan="9" class="none">还没有预扣单，点右上角「新开预扣」占一批料</td></tr>
      </tbody>
    </table>

    <el-dialog v-model="createDialog" title="新开预扣" width="480px">
      <div class="form-row"><label>预扣单号</label><el-input v-model="form.no" /></div>
      <div class="form-row">
        <label>堆场</label>
        <el-select v-model="form.yardId" placeholder="选一个堆场" style="flex:1" @change="form.materialId = null">
          <el-option v-for="y in yards" :key="y.id" :label="yardLabel(y)" :value="y.id" />
        </el-select>
      </div>
      <p class="warn" v-if="yardBlockReason">⚠ {{ yardBlockReason }}，这个堆场开不了预扣</p>
      <div class="form-row">
        <label>材料</label>
        <el-select v-model="form.materialId" placeholder="选这批要占的料" style="flex:1">
          <el-option v-for="m in yardMaterials" :key="m.id" :label="materialLabel(m)" :value="m.id" />
        </el-select>
      </div>
      <p class="note" v-if="picked">
        账面结存 {{ picked.balance }} · 占用中预扣 {{ picked.reserved }} · 可再预扣 <b>{{ picked.available }}</b>
      </p>
      <div class="form-row"><label>预扣数量</label><el-input v-model="form.amount" /></div>
      <div class="form-row"><label>计划开盘日</label><el-input v-model="form.planDate" placeholder="2026-09-21" /></div>
      <div class="form-row"><label>经办人</label><el-input v-model="form.handler" /></div>
      <div class="form-row"><label>备注</label><el-input v-model="form.note" placeholder="浇筑部位等，可空" /></div>
      <p class="note">只占余量、不写出场流水；开盘兑现时才按这个数量补一笔出场。</p>
      <template #footer>
        <el-button @click="createDialog = false">先不填</el-button>
        <el-button type="primary" @click="submitCreate">占住余量</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" :title="'修改预扣 ' + form.no" width="430px">
      <div class="form-row"><label>预扣数量</label><el-input v-model="form.amount" /></div>
      <div class="form-row"><label>计划开盘日</label><el-input v-model="form.planDate" /></div>
      <div class="form-row"><label>经办人</label><el-input v-model="form.handler" /></div>
      <p class="note">只能改数量、开盘日、经办人；材料换不了，要换料就先作废再新开。</p>
      <template #footer>
        <el-button @click="editDialog = false">先不改</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fulfillDialog" :title="'开盘兑现 ' + form.no" width="430px">
      <p class="note">
        按预扣数量 <b>{{ form.amount }}</b> 补一笔出场流水，材料还是「{{ form.materialText }}」，
        数量和方向都定死了改不了。
      </p>
      <div class="form-row"><label>流水单号</label><el-input v-model="form.movementNo" /></div>
      <div class="form-row"><label>出场日期</label><el-input v-model="form.moveDate" placeholder="2026-09-19" /></div>
      <div class="form-row"><label>经办人</label><el-input v-model="form.handler" /></div>
      <template #footer>
        <el-button @click="fulfillDialog = false">再想想</el-button>
        <el-button type="primary" @click="submitFulfill">兑现出场</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { inspectionApi, materialApi, movementApi, reservationApi, yardApi } from '../api'

export default {
  name: 'Reservations',
  data() {
    return {
      list: [],
      materials: [],
      yards: [],
      inspections: [],
      movements: [],
      createDialog: false,
      editDialog: false,
      fulfillDialog: false,
      form: {}
    }
  },
  computed: {
    yardMaterials() {
      return this.materials.filter((m) => m.yardId === this.form.yardId)
    },
    picked() {
      return this.materials.find((m) => m.id === this.form.materialId) || null
    },
    blockedYardIds() {
      return new Set(
        this.inspections
          .filter((i) => i.verdict === '不合格' && i.state === '待整改')
          .map((i) => i.yardId)
      )
    },
    yardBlockReason() {
      const y = this.yards.find((item) => item.id === this.form.yardId)
      if (!y) return ''
      if (y.state === '停用') return '堆场已停用'
      if (this.blockedYardIds.has(y.id)) return '堆场还挂着待整改的不合格巡检'
      return ''
    }
  },
  methods: {
    async load() {
      const [list, materials, yards, inspections, movements] = await Promise.all([
        reservationApi.fetch(),
        materialApi.fetch(),
        yardApi.fetch(),
        inspectionApi.fetch(),
        movementApi.fetch()
      ])
      this.list = list
      this.materials = materials
      this.yards = yards
      this.inspections = inspections
      this.movements = movements
    },
    materialName(id) {
      const m = this.materials.find((item) => item.id === id)
      return m ? `${m.no} ${m.title}` : '未知材料'
    },
    yardName(id) {
      const y = this.yards.find((item) => item.id === id)
      return y ? y.title : '未知堆场'
    },
    yardLabel(y) {
      if (y.state === '停用') return `${y.title}（已停用）`
      if (this.blockedYardIds.has(y.id)) return `${y.title}（有待整改巡检）`
      return y.title
    },
    materialLabel(m) {
      return `${m.no} ${m.title}（可再预扣 ${m.available}）`
    },
    chipClass(state) {
      return { occupied: state === '占用中', done: state === '已兑现', dead: state === '已作废' }
    },
    tailText(r) {
      if (r.state === '已兑现') return r.movementNo || '—'
      return r.note || '—'
    },
    nextNo(prefix, nos) {
      const max = nos.reduce((top, no) => {
        const hit = new RegExp(`^${prefix}-(\\d+)$`).exec(no || '')
        return hit ? Math.max(top, Number(hit[1])) : top
      }, 0)
      return `${prefix}-${String(max + 1).padStart(3, '0')}`
    },
    today() {
      return new Date().toLocaleDateString('sv')
    },
    openCreate() {
      this.form = {
        no: this.nextNo('PR', this.list.map((r) => r.no)),
        planDate: this.today()
      }
      this.createDialog = true
    },
    openEdit(row) {
      this.form = { id: row.id, no: row.no, amount: row.amount, planDate: row.planDate, handler: row.handler }
      this.editDialog = true
    },
    openFulfill(row) {
      this.form = {
        id: row.id,
        no: row.no,
        amount: row.amount,
        materialText: this.materialName(row.materialId),
        movementNo: this.nextNo('MV', this.movements.map((m) => m.no)),
        moveDate: this.today(),
        handler: row.handler
      }
      this.fulfillDialog = true
    },
    async submitCreate() {
      try {
        await reservationApi.add(this.form)
        this.createDialog = false
        await this.load()
        this.$message.success('余量占住了')
      } catch (e) {
        this.$message.error(e.message)
      }
    },
    async submitEdit() {
      try {
        await reservationApi.save(this.form.id, {
          amount: Number(this.form.amount),
          planDate: this.form.planDate,
          handler: this.form.handler
        })
        this.editDialog = false
        await this.load()
        this.$message.success('改好了')
      } catch (e) {
        this.$message.error(e.message)
      }
    },
    async submitFulfill() {
      try {
        await reservationApi.fulfill(this.form.id, {
          no: this.form.movementNo,
          moveDate: this.form.moveDate,
          handler: this.form.handler
        })
        this.fulfillDialog = false
        await this.load()
        this.$message.success('兑现了，出场流水已补上')
      } catch (e) {
        this.$message.error(e.message)
      }
    },
    voidIt(row) {
      this.$prompt('写个作废原因（泵车故障、突然下雨、计划取消……）', `作废预扣 ${row.no}`, {
        confirmButtonText: '作废还余量',
        cancelButtonText: '先留着',
        inputPlaceholder: '可空'
      }).then(async ({ value }) => {
        try {
          await reservationApi.void(row.id, { note: value || '' })
          await this.load()
          this.$message.success('作废了，余量还回去了')
        } catch (e) {
          this.$message.error(e.message)
        }
      }).catch(() => {})
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
.data-table { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #ececec;
  border-radius: 10px; overflow: hidden; }
.data-table th { text-align: left; font-size: 12px; color: #8f8f8f; font-weight: 500;
  padding: 12px 14px; background: #fafbf9; }
.data-table td { padding: 12px 14px; font-size: 13px; border-top: 1px solid #f3f4f1; }
.data-table tr:hover td { background: #fcfdfb; }
.right { text-align: right; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #8f8f8f; }
.num { font-weight: 600; color: var(--el-color-primary-dark-2); }
.mute { color: #b5b5b5; }
.none { color: #c4c4c4; font-size: 12px; text-align: center; padding: 24px 0; }
.chip { font-size: 11px; border-radius: 10px; padding: 2px 10px; }
.chip.occupied { background: #fdf3e3; color: #b5793a; }
.chip.done { background: #eef7ec; color: #2f7a3f; }
.chip.dead { background: #f3f3f3; color: #9b9b9b; }
.ops { white-space: nowrap; }
.ops .act-btn { margin-right: 6px; }
.act-btn { border-radius: 6px; font-size: 12px; padding: 5px 12px; cursor: pointer; }
.act-btn.solid { background: var(--el-color-primary); color: #fff; border: none; }
.bar .act-btn.solid { padding: 7px 16px; font-size: 13px; }
.act-btn.ghost { background: #fff; color: var(--el-color-primary-dark-2);
  border: 1px solid var(--el-color-primary-light-7); }
.act-btn.danger { background: #fff; color: #c0392b; border: 1px solid #ecc7c2; }
.form-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.form-row label { width: 82px; text-align: right; font-size: 13px; color: #777; flex: 0 0 82px; }
.note { font-size: 12px; color: #b0b0b0; margin: 4px 0 10px 92px; line-height: 1.7; }
.warn { font-size: 12px; color: #c0392b; margin: -6px 0 10px 92px; }
</style>
