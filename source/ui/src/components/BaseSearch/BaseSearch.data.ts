//需要检索选择的父节点
export interface selectParentNode {
  id: string | number;
  parent: string;
  type: string;
  isAlarm: boolean;
  isRootNode: boolean;
}

export interface treeValue {
  deviceTree: [];
  locationTree: [];
}

export interface IRawTreeNodeData {
  title: string;
  key: string;
  type: string;
  parentNode: string;
  haveAlarm: boolean;
  parentKey: number;
  slots: { icon: string };
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

// export class CameraTreeData implements IRawCameraTree {
//   deviceTree: TreeNodeData[] = [];
//   locationTree: TreeNodeData[] = [];

//   constructor(data: IRawCameraTree) {
//     for (const d of data.deviceTree) {
//       this.deviceTree.push(new TreeNodeData(d));
//     }
//     for (const d of data.locationTree) {
//       this.locationTree.push(new TreeNodeData(d));
//     }
//   }
// }

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

// export class TreeNodeData implements IRawTreeNodeData {
//   title: string;
//   key: string;
//   type: string;
//   parentNode: string;
//   haveAlarm: boolean;
//   parentKey: number;
//   parent?: TreeNodeData;
//   children?: TreeNodeData[];

//   constructor(data: IRawTreeNodeData) {
//     // data.prototype = TreeNodeData.constructor;
//     this.title = data.title;
//     this.key = data.key;
//     this.type = data.type;
//     this.parentNode = data.parentNode;
//     this.haveAlarm = data.haveAlarm;
//     this.parentKey = data.parentKey;
//     if (data.children) {
//       this.children = [];
//       let child: TreeNodeData;
//       for (const c of data.children) {
//         this.children.push((child = new TreeNodeData(c)));
//         child.parent = this;
//       }
//     }
//   }
// }

function copy(data: TreeNodeData): TreeNodeData {
  const clone = { ...data };
  delete clone.children;
  delete clone.parent;
  return clone;
  // {
  //   title: data.title,
  //   key: data.key,
  //   type: data.type,
  //   parentNode: data.parentNode,
  //   haveAlarm: data.haveAlarm,
  //   parentKey: data.parentKey,
  //   slots: data.slots,
  // };
}

export function filter(datas: TreeNodeData[], str: string, parent?: TreeNodeData): TreeNodeData[] {
  const res: TreeNodeData[] = [];
  let clone: TreeNodeData;
  for (const data of datas) {
    clone = copy(data);
    // console.log(clone, 'clone');
    clone.parent = parent;
    if (!data.children || !data.children.length) {
      if (clone.title.indexOf(str) > -1) {
        res.push(clone);
      }
      continue;
    }
    res.push(clone);
    clone.children = filter(data.children, str, clone);
    if (!clone.children.length) {
      delete clone.children;
      res.pop();
    }
  }
  return res;
}
