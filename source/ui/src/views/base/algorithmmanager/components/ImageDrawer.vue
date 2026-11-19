<template>
  <BasicDrawer @register="register"
               title="底图配置"
               width="800"
               destroyOnClose
               :show-footer="true"
               @ok="handleOk">
    <div class="container">
      <div style="min-width: 75px">选择底图：</div>
      <a-select placeholder="请选择底图" style="width: 100%" @change="handleChange" v-model:value="sourceImgId">
        <a-select-option v-for="item in selectOptions" :value="item.id">
          {{ item.fileName }}
        </a-select-option>
      </a-select>
    </div>
    <div style="width: 100%; margin-top: 25px; display: inline-flex; justify-content: center">
      <img style="width: 400px; height: 200px;" :src="imgUrl" alt=""/>
    </div>
  </BasicDrawer>
</template>

<script setup lang="ts">
import BasicDrawer from "/@/components/Drawer/src/BasicDrawer.vue";
import {useDrawerInner} from "/@/components/Drawer";
import {ref} from "vue";

const selectOptions = ref([])
const emits = defineEmits(['ok'])

const [register, { closeDrawer }] = useDrawerInner((data: any)=>{
  imgUrl.value = ''
  selectOptions.value = []

  selectOptions.value = data.selectList
  sourceImgId.value = data.sourceImageId
  if (data.sourceImageId) {
    getImgUrl(data.sourceImageId)
  }
})

const sourceImgId = ref('')
const imgUrl = ref('')
const handleChange = (val)=> {
  getImgUrl(val)
}

const getImgUrl = (id)=> {
  for (let i = 0; i < selectOptions.value.length; i++) {
    let item = selectOptions.value[i]
    if (item && item.id == id) {
      imgUrl.value = item.thumbnail
      break
    }
  }
}

const handleOk = () => {
  console.log(sourceImgId.value)
  emits('ok', {
    sourceImgId: sourceImgId.value
  })
  closeDrawer()
}
</script>

<style scoped lang="less">
  .container{
    width: 100%;
    display: inline-flex;
    align-items: center;
  }
</style>
