import { defineStore } from 'pinia';
export const useStore = defineStore('main', {
  state: () => {
    return {
      index: '',
    };
  },
  actions: {
    setState(key) {
      this.index = key;
    },
  },
});
