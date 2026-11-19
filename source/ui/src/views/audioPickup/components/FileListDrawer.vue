<template>
  <BasicDrawer v-bind="$attrs" @register="register" title="音频文件列表" width="50%">
    <BasicTable
      @register="registerTable">
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)"/>
      </template>
    </BasicTable>
  </BasicDrawer>
</template>

<script setup lang="ts">
import {BasicDrawer, useDrawerInner} from "/@/components/Drawer";
import {deleteFile, getFileList} from "/@/views/audioPickup/AudioPickup.api";
import {BasicColumn, BasicTable, TableAction, useTable} from "/@/components/Table";
import {ref} from "vue";
import {useMessage} from "/@/hooks/web/useMessage";

const {createMessage} = useMessage();

// 音频状态管理
const audioRef = ref<HTMLAudioElement | null>(null)
const isPlaying = ref(false)
const currentAudio = ref<string | null>(null)

const tableData = ref([])
let rowId = 0

const getFiles = async (params: any) => {
  const res = await getFileList(params)
  console.log('获取到的文件列表：', res)
  tableData.value = res
}

const [register] = useDrawerInner((data) => {
  rowId = data.record.id
  getFiles({ id: rowId })
});
const columns: BasicColumn[] = [
  {
    title: '文件名称',
    dataIndex: 'fileName',
    width: 150
  },
  {
    title: '文件大小（M）',
    dataIndex: 'fileSize',
    width: 100
  },
  {
    title: '文件路径',
    dataIndex: 'filePath',
    width: 200
  },
]

const [registerTable] = useTable({
  showIndexColumn: false,
  columns: columns,
  dataSource: tableData,
  actionColumn: {
    width: 200,
    title: '操作',
    dataIndex: 'action',
    slots: {customRender: 'action'},
  },
})

function getTableAction(record) {
  return [
    {
      label: isPlaying.value && currentAudio.value === record.filePath ? '暂停' : '播放',
      onClick: () => {
        if (isPlaying.value && currentAudio.value === record.filePath) {
          pauseAudio()
        } else {
          playAudio(record)
        }
      }
    },
    {
      label: '停止',
      onClick: () => {
        stopAudio()
      }
    },
    {
      label: '下载',
      onClick: () => {
        downloadAudio(record)
      }
    },
    {
      label: '删除',
      onClick: () => {
        deleteFileByPath(record.filePath)
      }
    },
  ]
}

// 播放音频
function playAudio(record: any) {
  stopAudio() // 先停止当前播放

  const baseUrl = `${location.protocol}//${location.host}/alarmImg/`;
  // const baseUrl = `${location.protocol}//192.168.3.145:8038/alarmImg/`;
  const audioUrl = record.filePath.startsWith('/')
    ? baseUrl + record.filePath.substring(1)
    : baseUrl + record.filePath;
  console.log('audioUrl', audioUrl)

  audioRef.value = new Audio(audioUrl)
  currentAudio.value = record.filePath

  audioRef.value.play()
    .then(() => {
      isPlaying.value = true
    })
    .catch(error => {
      console.error('播放失败:', error)
      useMessage().createMessage.error('音频播放失败')
    })

  // 监听播放结束
  audioRef.value.onended = () => {
    isPlaying.value = false
    currentAudio.value = null
  }
}

// 暂停音频
function pauseAudio() {
  if (audioRef.value) {
    audioRef.value.pause()
    isPlaying.value = false
  }
}

// 停止音频
function stopAudio() {
  if (audioRef.value) {
    audioRef.value.pause()
    audioRef.value.currentTime = 0
    isPlaying.value = false
    currentAudio.value = null
  }
}

// 下载音频文件
function downloadAudio(record: any) {
  if (!record?.filePath) {
    createMessage.error('音频路径无效');
    return;
  }

  const baseUrl = `${location.protocol}//${location.host}/alarmImg/`;
  // const baseUrl = `${location.protocol}//192.168.3.145:8038/alarmImg/`;
  const filePath = record.filePath.startsWith('/')
    ? record.filePath.slice(1)
    : record.filePath;
  const audioUrl = `${baseUrl}${filePath}`;

  console.log('audioUrl', audioUrl);
  createMessage.loading({content: '准备下载中...', key: 'download'});

  // 使用 a 标签直接触发浏览器下载
  const link = document.createElement('a');
  link.href = audioUrl;
  link.download = ''; // 浏览器会尝试下载而不是打开
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();

  setTimeout(() => {
    document.body.removeChild(link);
    createMessage.success({content: '下载已开始', key: 'download'});
  }, 1000);
}

async function deleteFileByPath(filePath: string) {
  await deleteFile({filePath: filePath})
  await getFiles({id: rowId})
}
</script>

<style scoped lang="less">

</style>
