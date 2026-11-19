export interface groupList {
  groupName: string;
  id: string;
  createTime: string;
  runStatus: number;
  cameraList: cameraList[];
  algorithmList: algorithmList[];
  cameraIds: string;
  pollingPeriod: number;
}
export interface cameraList {
  cameraId: string;
  cameraName: string;
}
export interface algorithmList {
  algorithmId: string;
  algorithmName: string;
}
export interface allAlgInfo {
  label: string;
  text: string;
  title: string;
  value: string;
}
export interface FormState {
  layout: 'horizontal' | 'vertical' | 'inline';
  field: string;
}
export interface cameraIds {
  id: string;
}
export interface selectCamera {
  id: string;
  title: string;
}

export interface TreeNodeData {
  title: string;
  key: string;
  type: string;
  parentNode: string;
  haveAlarm: boolean;
  parentKey: number;
  runStatus: string;
  children?: TreeNodeData[];
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
  runStatus: string;
  children?: IRawTreeNodeData[];
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

export type CheckPoint = [number, number];

export type CheckArea = CheckPoint[];

export type CanvasPoint = { x: number; y: number };

export type CanvasArea = CanvasPoint[];

export enum ROIAreaType {
  Full = 0,
  Rect,
  Polygon,
  InvalidArea,
}
export interface roiType {
  checkArea;
  invalidArea;
}
export interface algMsg {
  alarmCycle: number;
  alarmLevel: string;
  alarmType: string;
  algorithmId: string;
  belongTo: string;
  checkArea: [];
  checkCycle: number;
  checkDate: string;
  checkTime: string;
  createTime: string;
  groupId: string;
  hitCount: number;
  id: string;
  // isRotate: null;
  modelName: string;
  runStatus: number;
  sensitivity: number;
  taskRemark: string;
  threshold: number;
  timeSpace: number;
}
