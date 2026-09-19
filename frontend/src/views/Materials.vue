<template>
  <div class="wrap">
    <header class="bar">
      <div>
        <h2>材料台账</h2>
        <p class="tip">账面结存是流水累出来的，这里改不动；可用余量 = 账面结存 − 占用中的浇筑预扣，能再预扣、也能出场的就看它</p>
      </div>
      <button class="act-btn solid" @click="openCreate">登记材料</button>
    </header>

    <table class="data-table">
      <thead>
        <tr>
          <th>编号</th><th>名称</th><th>类别</th><th>所在堆场</th>
          <th class="right">账面结存</th><th class="right">占用中预扣</th><th class="right">可用余量</th><th>状态</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="m in list" :key="m.id">
          <td class="mono">{{ m.no }}</td>
          <td>{{ m.title }}</td>
          <td>{{ m.category }}</td>
          <td>
            <select class="inline-select" :value="m.yardId" @change="moveTo(m, $event.target.value)">
              <option v-for="y in yards" :key="y.id" :value="y.id">{{ y.title }}</option>
            </select>
          </td>
          <td class="right">{{ m.balance }}</td>
          <td class="right" :class="num(m.occupied) > 0 ? 'hold' : 'mute'">{{ num(m.occupied) }}</td>
          <td class="right num">{{ num(m.available) }}</td>
          <td :class="m.state === '在库' ? 'ok' : 'mute'">{{ m.state }}</td>
        </tr>
      </tbody>
    </table>

    <el-dialog v-model="dialog" :title="form.id ? '修改材料' : '登记材料'" width="430px">
      <div class="form-row"><label>材料编号</label><el-input v-model="form.no" /></div>
      <div class="form-row"><label>材料名称</label><el-input v-model="form.title" /></div>
      <div class="form-row"><label>类别</label><el-input v-model="form.category" /></div>
      <div class="form-row">
        <label>所在堆场</label>
        <el-select v-model="form.yardId" placeholder="选一个堆场" style="flex:1">
          <el-option v-for="y in yards" :key="y.id" :label="y.title" :value="y.id" />
        </el-select>
      </div>
      <p class="note">新登记的材料结存从 0 开始，有货没货靠登记进出场流水。</p>
      <template #footer>
        <el-button @click="dialog = false">先不填</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { materialApi, yardApi } from '../api'

export default {
  name: 'Materials',
  data() {
    return { list: [], yards: [], dialog: false, form: {} }
  },
  methods: {
    async load() {
      this.list = await materialApi.fetch()
      this.yards = await yardApi.fetch()
    },
    num(v) {
      return v == null ? 0 : v
    },
    async moveTo(row, yardId) {
      try {
        await materialApi.save(row.id, { yardId: Number(yardId) })
        await this.load()
        this.$message.success('换好了')
      } catch (e) {
        this.$message.error(e.message)
        await this.load()
      }
    },
    openCreate() {
      this.form = { category: '钢筋' }
      this.dialog = true
    },
    async submit() {
      try {
        await materialApi.add(this.form)
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
.data-table { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #ececec;
  border-radius: 10px; overflow: hidden; }
.data-table th { text-align: left; font-size: 12px; color: #8f8f8f; font-weight: 500;
  padding: 12px 14px; background: #fafbf9; }
.data-table td { padding: 12px 14px; font-size: 13px; border-top: 1px solid #f3f4f1; }
.data-table tr:hover td { background: #fcfdfb; }
.right { text-align: right; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #8f8f8f; }
.num { font-weight: 600; color: var(--el-color-primary-dark-2); }
.ok { color: var(--el-color-primary-dark-2); }
.mute { color: #b5b5b5; }
.hold { color: #b07a1e; font-weight: 600; }
.inline-select { border: 1px solid #e5e5e5; border-radius: 5px; padding: 3px 8px; font-size: 12px;
  background: #fff; color: #555; max-width: 170px; }
.act-btn { border-radius: 6px; font-size: 13px; padding: 7px 16px; cursor: pointer; }
.act-btn.solid { background: var(--el-color-primary); color: #fff; border: none; }
.form-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.form-row label { width: 82px; text-align: right; font-size: 13px; color: #777; }
.note { font-size: 12px; color: #b0b0b0; margin: 4px 0 0 92px; }
</style>
