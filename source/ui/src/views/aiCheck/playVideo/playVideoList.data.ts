export interface TreeNodeData {
  title: string;
  key: string;
  type: string;
  parentNode: string;
  haveAlarm: boolean;
  parentKey: number;
  runStatus: string;
  children?: TreeNodeData[];
  streamAddress: string;
}

export interface IRawTreeNodeData {
  title: string;
  key: string;
  type: string;
  parentNode: string;
  haveAlarm: boolean;
  parentKey: number;
  cameraCount: number;
  slots: { icon: string };
  children?: IRawTreeNodeData[];
  pic: string;
}
export interface IRawCameraTree {
  deviceTree: IRawTreeNodeData[];
  locationTree: IRawTreeNodeData[];
}

export interface CameraTreeData {
  deviceTree: TreeNodeData[];
  locationTree: TreeNodeData[];
}
export interface TreeNodeData extends IRawTreeNodeData {
  children?: TreeNodeData[];
  parent?: TreeNodeData;
}
function handleTreeData(data: IRawTreeNodeData): TreeNodeData {
  if (!data.children) {
    return data as TreeNodeData;
  }
  const children: TreeNodeData[] = [];
  let child: TreeNodeData;
  for (const c of data.children) {
    children.push((child = handleTreeData(c)));
    child.parent = data;
  }
  data.children = children;
  return data as TreeNodeData;
}

export function handleRawData(raw: IRawCameraTree): CameraTreeData {
  const data = { deviceTree: [], locationTree: [] } as CameraTreeData;

  for (const d of raw.deviceTree) {
    data.deviceTree.push(handleTreeData(d));
  }
  for (const d of raw.locationTree) {
    data.locationTree.push(handleTreeData(d));
  }
  return data;
}

//接口返回字段
export interface backField {
  data: {
    code: number;
    msg: string;
  };
}
