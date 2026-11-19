// export module 'JSWebrtc' {
export const JSWebrtc: {
  Player: Player;
  VideoElement: VideoElement;
  CreateVideoElements: () => void;
  constFillQuery: (query_string: string, obj: any) => void;
  consturlParams: (rtmp_url: string) => IUrlParams;
  constHttpPost: (url: any, data: any) => any;
} = {};

export interface IUrlParams {
  url: string;
  schema: string;
  server: string;
  port: string;
  vhost: string;
  app: string;
  stream: string;
}

export interface IOptions {
  video?: HTMLVideoElement;
  autoplay?: boolean;
  onPlay?: (player: Player) => void;
  onPause?: (player: Player) => void;
}

export class Player {
  urlParams: IUrlParams;
  options: IOptions;
  autoplay: boolean;

  constructor(url: string, options?: IOptions);

  /** 开始加载 */
  startLoading();

  /** 播放 */
  play();

  /** 暂停 */
  pause();

  /** 停止 */
  stop();

  /** 销毁 */
  destroy();

  /** 更新 */
  update();
}

export class VideoElement {
  constructor(el: HTMLVideoElement);

  onUnlockAudio(el: HTMLVideoElement, ev);

  onClick(ev);

  PLAY_BUTTON: string;

  UNMUTE_BUTTON: string;
}
// }
