import type { CapacitorConfig } from '@capacitor/cli';

import pkg from './package.json';

const config: CapacitorConfig = {
  appId: 'app.capgo.wechat',
  appName: '@capgo/capacitor-wechat',
  webDir: 'dist',
  plugins: {
    CapacitorWechat: {
      appId: 'wx_YOUR_APP_ID',
      universalLink: 'https://your-universal-link.example.com/',
    },
    SplashScreen: {
      launchAutoHide: false,
    },
    CapacitorUpdater: {
      appId: 'app.capgo.wechat',
      autoUpdate: true,
      autoSplashscreen: true,
      directUpdate: 'always',
      version: pkg.version,
    },
  },
};

export default config;
